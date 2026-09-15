## Context

The reminder path built by `app-medicine-alarm` rests on one decision, its D5: the app keeps exactly
one alarm, for "the next thing that has to happen", and every wake recomputes it. That decision was
made to stay well inside the platform's limits on waking an idle device, and as a way of keeping the
scheduling trivially correct. It achieves both. It also makes the schedule a chain, and a chain has
the property that breaking one link stops everything after it.

That is what happened in the field. `ReminderAlarmScheduler.scheduleAt` uses
`setExactAndAllowWhileIdle`, which is exempt from Doze but is *not* exempt from app-standby-bucket
quotas and is *not* respected by the aggressive power managers Samsung, Xiaomi, Oppo, OnePlus and
Huawei ship. After the app sat idle in the background through a phone call, the alarm was deferred
or dropped. The only place the next alarm is armed is inside `ReminderCoordinator.onWake`, which
never ran, so nothing re-armed anything. Opening the app called `requestWake(APP_START)`, `DueDoses`
found a dose past its moment with `firstRemindedAt` still null, and posted it — which is both the
symptom the user saw and the proof the alarm wake never executed.

Three smaller defects sit in the same path and each can silently lose a reminder on its own:

1. `ReminderCoordinator.onWake` wraps the posting work in `withTimeout(9_000)` but puts
   `rescheduleNextWake()` in the `finally` outside it. On a cold start — Room opening, two
   `runBlocking` DataStore reads in `PillsnerApplication.onCreate` and `AppContainer.theme`, and an
   `APP_START` wake already holding the mutex ahead of the alarm — the body can time out. Nothing is
   posted, nothing is logged above debug, and `ComputeNextWake` then arms the dose's *lapse* moment,
   so the dose goes straight to missed without ever having been announced.
2. `ReminderNotifier.show` returns silently when notification permission is missing, but
   `ReminderCoordinator` writes `setFirstReminded` regardless. Since both `DueDoses` and
   `ComputeNextWake` gate on `firstRemindedAt == null`, that dose can never be announced again.
3. The manifest listens for `BOOT_COMPLETED` only, so after a reboot there are no alarms at all
   until the user unlocks the phone.

Constraints this design works within: no network, no third-party SDKs, no analytics; Kotlin and
AndroidX only; minSdk 26, targetSdk 37; the domain layer stays free of Android framework types.

## Goals / Non-Goals

**Goals:**

- A dose's reminder arrives within the minute it is due, with the app in the background, the process
  dead, the device idle, or the user on a call.
- One deferred or dropped alarm costs one reminder, not every reminder after it.
- The app can recover a broken schedule by itself, without the user opening it.
- When the platform or the OEM is throttling Pillsner, the user is told, in the place they already
  look for this (the Home banner) and with a route to fix it.
- Every path that can fail to post a reminder either retries or leaves the dose announceable. No
  silent loss.

**Non-Goals:**

- Guaranteeing delivery on a device where the user has force-stopped the app. Nothing can, and the
  app should not pretend otherwise.
- Using periodic background work to *deliver* the reminder moment. `reminder-scheduling` forbids it
  and CLAUDE.md forbids it; the watchdog introduced here repairs the schedule, it does not replace
  it.
- Changing what a reminder says, its three actions, its snooze rule or its wearable behaviour.
- Network-based delivery of any kind.
- Reworking dose generation, the two-day planning window or the lapse rule.

## Decisions

### D1 — Reminder alarms move to `AlarmManager.setAlarmClock`

`setAlarmClock` is the top tier: the platform treats it as a user-visible alarm, it survives Doze
and app standby, and — decisively — it is the one tier OEM power managers honour, because suppressing
it would break the clock app. Pillsner already declares `USE_EXACT_ALARM`, whose stated purpose is
apps whose core function is alarms or reminders, so the app qualifies without asking for anything
new.

The cost is a visible alarm icon in the status bar and an entry in the system's "next alarm" slot.
For a medication reminder that is honest rather than intrusive: the user *has* set an alarm.

*Alternatives considered.* Staying on `setExactAndAllowWhileIdle` is what failed. `setExact` is
weaker still — it is not even Doze-exempt. A foreground service holding a long-lived timer would
work but means a permanent notification and would not survive the process being killed.

The housekeeping alarm (daily refresh, next lapse) stays on `setExactAndAllowWhileIdle`: nothing
user-facing depends on it firing to the minute, and it should not put an alarm icon in the status
bar for a bookkeeping task.

### D2 — One alarm per due dose, plus one housekeeping alarm

This reverses `app-medicine-alarm` D5, and the reason D5 gave — platform limits on waking an idle
device — does not apply to `setAlarmClock`, which is not quota-limited.

The planning window is today and tomorrow (`RefreshPlannedDoses.WINDOW_DAYS`), and a medicine is due
a handful of times a day, so the realistic alarm count is single digits and bounded by the window.
`ComputeNextWake` becomes `ComputeWakeSchedule`, returning the full set of moments rather than the
minimum: every un-reminded pending dose's `scheduledAt`, every outstanding snooze end, every
outstanding repeat, and one housekeeping moment (the earliest of the next lapse and the next daily
refresh).

Alarm identity becomes the dose id, so a `PendingIntent` request code derived from it replaces the
single fixed request code 1. `ReminderAlarmScheduler` therefore needs to know which alarms it
currently has set, so it can cancel the ones no longer wanted: it keeps the armed set in its own
DataStore file. That store is also what the watchdog (D3) compares against.

This is the change that turns the failure mode from catastrophic into local. A dropped 08:00 alarm
no longer takes 20:00 with it.

*Alternative considered.* Keeping one alarm and relying solely on the watchdog to repair it. That
leaves the repair latency at the watchdog's period — up to 15 minutes late for every reminder after
any hiccup — which is not "within the same minute".

### D3 — A `WorkManager` watchdog that repairs, never delivers

A periodic worker at the platform minimum of 15 minutes. On each run it: recomputes the wake
schedule, compares it against the armed set `ReminderAlarmScheduler` recorded, re-arms anything
missing, and runs a normal wake so that anything already due is posted.

The distinction that keeps this inside `reminder-scheduling`'s prohibition matters and is worth
stating plainly: the worker is not how a reminder is delivered. On a healthy device it finds nothing
to do and the alarms deliver every reminder to the minute. The worker exists for the case where the
platform has already broken its own promise, where the choice is a reminder up to 15 minutes late or
no reminder at all.

`WorkManager` (`androidx.work:work-runtime-ktx`) is added to the version catalog. It is AndroidX, it
is the only supported way to run periodic work that survives process death and reboot, and hand-rolling
it would mean re-implementing its persistence, scheduling and backoff.

The worker is enqueued as unique periodic work with `ExistingPeriodicWorkPolicy.KEEP`, from
`PillsnerApplication.onCreate` and from the boot receiver. It returns immediately when no medicine
produces doses.

*Alternative considered.* A self-rescheduling chain of `setExactAndAllowWhileIdle` alarms as its own
watchdog. That is the same chain topology that failed, one level up.

### D4 — Wake processing runs in a short foreground service

`goAsync()` gives roughly ten seconds at background process priority, and the alarm's own wake lock
is not a durable guarantee across it. A cold start that has to open Room, run migrations and get
past the two `runBlocking` DataStore reads can exceed it, and today exceeding it loses the reminder
silently (see Context, defect 1).

The receivers become thin: they start `ReminderWakeService`, a foreground service with
`foregroundServiceType="shortService"`, passing the `WakeReason`. The service holds a real wake lock
for a real window and shows a minimal "Checking your medicines" notification on a low-importance
channel, which the platform requires and which disappears in a second or two.

`shortService` is available from API 34. Below that the same service runs as an ordinary foreground
service with the same lifetime; the type attribute is simply ignored.

*Alternative considered.* Keeping `goAsync()` and shortening the wake path. It reduces the odds
without removing the failure, and the failure is total.

### D5 — A reminder repeats until it is answered

Today a dose is announced exactly once, ever: `DueDoses` and `ComputeNextWake` both gate on
`firstRemindedAt == null`. If that one posting lands while the phone is face-down, on a call, or in
a pocket, that is the end of it until the dose lapses.

A pending dose that has been reminded and is not snoozed is re-posted every 15 minutes, up to four
times, and never past its lapse moment. Fifteen minutes matches the existing snooze period, so the
two rules read as one behaviour rather than two. Each repeat re-alerts, which the channel already
allows (`setOnlyAlertOnce(false)`).

This needs a repeat count on the dose so the rule survives process death: a `reminderCount` column,
with a Room migration and a migration test, incremented where `setFirstReminded` is written today.

"Not yet" still means a clean 15-minute snooze and resets the repeat sequence; the user has
acknowledged the dose, so the app should not then start counting down against them.

A due dose also gets `setFullScreenIntent`, so on a locked or busy phone it presents rather than
sitting silently in the shade. `USE_FULL_SCREEN_INTENT` is granted at install for apps in the
calendar and alarm categories, which is Pillsner; where it is not granted the notification degrades
to a heads-up, which is what happens today.

### D6 — Battery-optimisation state joins the Home banner

`HomeUiState.remindersAreUnreliable` already combines "notifications not allowed" and "alarms not
exact". A third input joins it: `PowerManager.isIgnoringBatteryOptimizations` is false.

The app requests the exemption once, when the first medicine with a schedule is saved — the same
moment it already requests notification permission — reusing `ReminderPreferences` to remember that
it has asked. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is justified here in the terms Google Play
requires: the app's core function is exact-time alarms and it has no other way to deliver them.

`ReminderBanner` takes one message and one action today, so the three states need an ordering. Most
severe first: notifications not allowed, then battery optimisation on, then alarms inexact. Only one
banner is ever shown.

On OEMs known to kill background apps the banner's button additionally offers the vendor's own
auto-start screen. This is matched on `Build.MANUFACTURER` against a small table of known intents,
each guarded by `resolveActivity` so an intent that no longer exists is never offered. It is
inherently best-effort and the design says so; the generic system setting is always the fallback.

### D7 — No path may lose a reminder silently

Three fixes, each small and each closing one hole:

- `ReminderNotifier.show` returns whether it actually posted. `ReminderCoordinator` writes
  `setFirstReminded` only when it did. A dose that could not be shown stays announceable, and the
  banner from D6 is what tells the user why nothing is arriving.
- A wake whose body times out arms a retry two minutes out instead of falling through to the lapse
  moment. The retry is bounded — three attempts — after which the dose follows the ordinary lapse
  rule.
- `rescheduleNextWake` moves out of the `finally` into a path that knows whether the body completed,
  so "reschedule even on failure" (which `reminder-scheduling` requires and this design keeps) and
  "reschedule as if everything succeeded" stop being the same thing.

### D8 — Direct boot and unlock

`SystemEventsReceiver` adds `LOCKED_BOOT_COMPLETED` and `ACTION_USER_UNLOCKED`. The receiver and the
wake service become `directBootAware`, so an alarm can be re-armed before first unlock.

Room and DataStore both live in credential-encrypted storage and are unreadable before unlock, so a
locked-boot wake cannot read doses. It does the one thing it can: re-arms from the armed-alarm set
in a small device-protected DataStore that `ReminderAlarmScheduler` mirrors, and a full wake follows
on `ACTION_USER_UNLOCKED`. Mirroring only alarm moments and dose ids keeps medication names and
amounts out of device-protected storage, which the privacy rule requires.

## Risks / Trade-offs

- **The alarm icon is now permanently in the status bar for any user with a scheduled medicine.** →
  Accepted, and it is honest: the user has set an alarm. It is the price of the only tier OEMs
  respect. Mentioned in the README so it is not a surprise.
- **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is a Play-policy-sensitive permission and a review can
  reject it.** → The exemption is requested, never required: every path works without it and the
  banner is what the user sees when it is absent. The Play declaration cites the exact-alarm core
  function, which is the listed acceptable use for a medication reminder app.
- **Per-dose alarms plus a 15-minute worker cost more battery than one idle alarm.** → Bounded by the
  two-day planning window, so a handful of alarms; the worker exits immediately when no medicine
  produces doses. This is the trade the product promise requires, and it is stated in the proposal.
- **Repeating reminders can become nagging.** → Bounded at four repeats and never past the lapse
  moment, and any of the three answers stops it at once. If it proves wrong, the interval and the cap
  are two constants.
- **The OEM auto-start intents are undocumented and break between versions.** → Every one is guarded
  by `resolveActivity`, and the generic battery-optimisation setting is always offered. The table is
  a convenience; nothing depends on it.
- **`shortService` caps the foreground service at about three minutes and throws if it overruns.** →
  A wake is a few database queries; the existing budget is nine seconds. The service keeps a timeout
  well inside the cap and stops itself.
- **Reversing D5 of `app-medicine-alarm` means the archived design and this one disagree.** → The
  archive is read-only history and stays as it is; this design states the reversal and the reason
  explicitly, and the `reminder-scheduling` delta is what the specs will carry forward.
- **A `reminderCount` column needs a migration on a live database.** → An additive column with a
  default, a migration and a migration test, which the project already requires for every schema
  change.

## Migration Plan

Schema: one additive `reminderCount` column on the dose table, default 0, with its migration and
migration test. No destructive fallback.

Existing installs: on first run after the update, `MY_PACKAGE_REPLACED` already triggers a wake. That
wake arms the new per-dose alarms and enqueues the watchdog; the old single alarm is cancelled by its
request code as part of the scheduler's first reconcile, so no stale alarm survives.

Rollback: reverting the app reverts to the single alarm. The extra column is harmless to an older
build only if the older build's schema version still matches, so rollback means reinstall. This is
stated rather than engineered around; it is a local-only app with no server contract.

## Open Questions

- Should the repeat interval and cap (15 minutes, four times) be user-configurable? Held out of this
  change deliberately — it is a settings change and a separate proposal — but the constants are
  placed so that a later change only has to read them from a repository.
- Is four repeats the right cap for a medicine whose lapse moment is 24 hours away? It means the app
  goes quiet an hour after the dose was due. An alternative is repeating until lapse at a widening
  interval. Resolving this needs a real user, so the simpler bounded rule ships first.
