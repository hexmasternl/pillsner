## Context

`reminder-delivery-reliability` set out to make a reminder arrive on the minute and made two
independent interventions to get there. D1 moved reminders to `AlarmManager.setAlarmClock`. D6 added
a battery-optimisation exemption request and a Home banner for when it is absent.

D1 worked, and it is what the reliability actually rests on. D6 is the one the user sees, and it is
the one that is not paying for itself. Re-reading the platform documentation against the code as it
now stands:

- **Doze.** `setAlarmClock()` alarms "continue to fire normally. The system exits Doze shortly before
  those alarms fire." The exemption changes nothing for an alarm on that tier.
- **What the exemption actually grants.** A whitelisted app "can use the network and hold partial
  wake locks during Doze and App Standby. However, other restrictions still apply." Pillsner has no
  `INTERNET` permission by design, and holds no `PowerManager.WakeLock` of its own — a search for
  `newWakeLock` across `src/app/src/main` returns nothing. The alarm broadcast's own wake lock plus
  `ReminderWakeService`'s foreground status cover the wake. Both grants land on nothing.
- **Standby buckets.** Apps granted `USE_EXACT_ALARM` are already exempt from the restricted bucket —
  the same protection the Doze exemption list confers. `AndroidManifest.xml:11` declares it.
- **Starting the foreground service from the background.** This was the one place a real dependency
  was plausible. There is none: "exact alarms aren't affected by foreground service launch
  restrictions because Android considers exact alarms to be critical, time-sensitive interruptions."
  `ReminderAlarmReceiver` to `ReminderWakeService` is exempt by virtue of the alarm.
- **The watchdog.** `WorkManager` is deferred in Doze, and the exemption does not rescue jobs either.
  It is a repair net by construction (D3), late by design when it runs at all.
- **OEM power managers.** The platform exemption list and MIUI's autostart list are different lists.
  The system dialog does not touch the one that matters on those devices; `backgroundRunIntent()`
  already knows this and offers the vendor screen separately.

Against that, the dialog costs a system prompt over the Home screen the first time a user has an
upcoming dose, a banner that is permanently true on the majority of devices that were never going to
throttle anything, a Play-policy-sensitive permission with a `@SuppressLint("BatteryLife")` beside
it, and a standing instrumented-test hazard that `reminder-delivery-reliability` task 9.3 had to
write a precondition for.

Constraints unchanged: no network, no third-party SDKs, no analytics; Kotlin and AndroidX only;
minSdk 26, targetSdk 37; the domain layer stays free of Android framework types.

## Goals / Non-Goals

**Goals:**

- Pillsner never opens a system dialog the user did not ask for.
- The Home banner means something when it appears: it reports an observed failure, not a
  configuration the app disapproves of.
- The exemption stays reachable for the user who needs it, without the app holding a permission to
  offer it.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` leaves the manifest, and the Play declaration with it.
- The instrumented suite stops needing a whitelist precondition.

**Non-Goals:**

- Changing how a reminder is scheduled, posted, repeated, snoozed or answered. `setAlarmClock`, the
  per-dose alarm set, the wake service, the watchdog and the repeat rule are all untouched.
- Removing the vendor auto-start guidance. It is the part of D6 that addresses a real cause.
- Adding a settings screen entry for background running. If one is wanted it is its own change.
- Detecting *why* a reminder was missed. The app can observe that one was; it cannot know whether the
  cause was an OEM, a force-stop or a flat battery, and it should not guess in the banner copy.

## Decisions

### D1 — The app stops asking; the exemption becomes reachable, not requested

`BatteryOptimisationEffect`'s request branch goes, and with it
`HomeViewModel.shouldRequestBatteryExemption`, `ReminderPreferences.hasRequestedBatteryExemption`,
`markBatteryExemptionRequested` and the `battery_exemption_requested` key. Reading the exemption
state stays available — it is a cheap, silent `PowerManager` call — but nothing in the app consults
it any more: D3 settles that the banner's destination does not depend on it either. It is kept
because the spec permits reading and a later change may want it, not because anything reads it now.

With nothing in the app launching the dialog, `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
has no caller, so `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` comes out of the manifest. The remaining
route is `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` — the system list, which
`backgroundRunIntent()` already falls back to and which requires no permission at all. The user
taking themselves off the list there is two taps instead of one, which is the right price for not
being interrupted.

*Alternatives considered.* Keeping the permission and the one-tap dialog behind a user-initiated
button. It preserves the nicer interaction but keeps the Play declaration, the lint suppression and a
sensitive permission on a privacy-first app, for one saved tap on a screen the user reaches only when
something has already gone wrong. Not worth it.

### D2 — The banner is raised by a missed reminder, not by a missing exemption

Today `HomeUiState.remindersAreUnreliable` raises `BATTERY_OPTIMISED` whenever `!batteryExempt`,
which is true on most phones, forever, for a condition that is mostly harmless. A banner that is
always on is a banner the user stops reading, and it would still be on for the many users whose
reminders arrive perfectly.

The replacement trigger is the symptom that started this whole line of work: **a dose lapsed without
a reminder ever being posted for it.** That is exactly an alarm the platform did not deliver, and it
is observable. `ReminderCoordinator.wake` already has the moment — `markMissedDoses()` returns the
lapsed doses and each carries `firstRemindedAt`.

Two guards keep the signal honest:

- **The dose must have existed before it was due** (D4). Otherwise every medicine added in the
  evening with a morning schedule trips it, because those doses are generated already lapsed.
- **Notifications must be allowed.** A dose that lapsed un-reminded because the user denied
  notifications has a known cause with its own banner. Recording it here would leave a second,
  wronger banner behind once notifications were granted. `ReminderNotifier.show` already reports
  whether a post succeeded (inherited D7), so the coordinator can tell the two apart.

*Alternatives considered.* Comparing armed alarms against wall-clock time to catch a dropped alarm
directly. `AlarmManager` will not say what it holds — the reason `ArmedAlarmStore` exists — so the
only observable is the consequence, which is the missed reminder. Watching for the watchdog finding
work to do was the other candidate: it finds work on a healthy device after a reboot too, so it is
noisier than the thing it would be reporting.

### D3 — What the banner says and where its button goes

Copy moves from a claim about the device's settings to a statement of what happened: a reminder did
not arrive, and here is where to stop it happening again. The exact wording is the designer's, not
this document's; `pillsner-ui-review` gates it.

One action, as `ReminderBanner` has always had. Its destination is chosen at tap time:

1. the vendor auto-start screen, when `Build.MANUFACTURER` is on the known list and the intent
   resolves — unchanged from D6, and on those devices it is the screen that actually matters;
2. otherwise the system battery-optimisation list.

The exemption state decides nothing in that order, and that is deliberate: the user who is already
exempt and still missing reminders is precisely the user the vendor screen is for.

**The button is always there, even when there is nothing good to offer.** On a device that is
already exempt, has missed a reminder, and whose manufacturer is not on the vendor list, the button
opens the system battery-optimisation list — a screen that will tell that user nothing they do not
already know. It still appears, for two reasons. The banner cannot diagnose the cause, so "nothing
left to offer" is a conclusion the app is not entitled to draw; the list is also where a user
confirms for themselves that the app is exempt, which is worth something when a reminder has just
gone missing. And more decisively, activating the button is what acknowledges the miss and clears
the record (D5). A banner with no button would have no way to be dismissed and would sit on Home
for good. If a later change gives the banner a separate dismissal, this is worth revisiting.

### D4 — A `plannedAt` column, so "never reminded" can be told from "never had a chance"

`RefreshPlannedDoses` plans today and tomorrow, today included in full, so a medicine saved at 20:00
with an 08:00 schedule produces an 08:00 dose that is already past its lapse moment. Marked missed on
the very next wake, with `firstRemindedAt` null, it is indistinguishable from a dropped alarm without
knowing when the dose was stored.

So `DoseEntity` gains `plannedAt: Instant`, set once when the dose row is inserted and never
rewritten. The signal is recorded only when `plannedAt` is before `scheduledAt`: the dose existed
while it was still in the future, so the app had a window in which to remind and did not use it.

A Room migration ships with it, and a migration test, as every schema change here does. Existing rows
take `scheduledAt` as their `plannedAt`, which makes every pre-migration dose fail the test and
therefore never trip the banner. Silent on history is the right default: the app should report what
it observes from now on, not re-litigate doses from before it could tell.

*Alternatives considered.* Deriving the same thing from `ArmedAlarmStore` — whether the dose's moment
was ever armed. The store is replaced wholesale on every reconcile, so by the time a dose lapses its
moment is long gone from it. Keeping a separate "doses the app has seen" set in DataStore is the same
column with worse consistency guarantees and no migration test.

### D5 — The signal is sticky until the user acts on it

Stored in `ReminderPreferences` as a nullable instant — the moment of the most recent silent miss —
rather than a flag, so the banner has something to say and a test has something to assert.

It is set by the coordinator when a lapsed dose passes both guards, and cleared when the user taps
the banner's action, and by the danger-zone reset (`app-settings-reset`) along with everything else.
It is deliberately *not* cleared by the next reminder posting successfully: on a phone that delivers
three reminders in four, self-clearing would make the banner flicker on and off, and a warning that
comes and goes is worse than one that waits to be acknowledged. Tapping the action is the
acknowledgement; if it happens again, the banner comes back.

### D6 — Precedence keeps three entries, with a changed middle

`HomeUiState.remindersAreUnreliable` still reports the most severe of three, in the same order:
notifications not allowed, then reminders have been missed, then alarms not exact. Only the middle
one's meaning changes. Notifications first is still right — denied notifications mean no reminder at
all, which subsumes a missed one — and it is what keeps the notifications case from being reported
twice.

## Risks / Trade-offs

- **A user on a genuinely hostile OEM now learns about it after the first missed dose rather than
  before it.** → One dose of latency, in exchange for not interrupting every user with a system
  prompt and not showing a permanent banner to the majority whose reminders are fine. The banner that
  does appear is true, which is what makes it worth reading.
- **Two taps instead of one to reach the exemption.** → Accepted; it is the price of dropping a
  sensitive permission, and the screen is reached only after something has gone wrong.
- **`plannedAt` is a schema change on the app's busiest table for a diagnostic.** → One
  `INTEGER NOT NULL` column with a defaulted backfill; the migration test covers it. It also removes
  a whole class of false positives that would otherwise have made the banner untrustworthy from the
  first release.
- **The signal cannot distinguish a dropped alarm from a force-stopped app or a dead battery.** →
  Correct, and the copy says what was observed rather than naming a cause. D3 resolves this by not
  guessing.
- **Removing the permission is visible to Play as a permission change.** → In the harmless direction.
  The README and PRIVACY notes come with it.
- **This change contradicts a decision in `reminder-delivery-reliability` that is not yet archived.**
  → It is written as a delta against that change's `reminder-delivery-resilience` spec and cannot be
  applied until that change archives. Its two open tasks are instrumented-test runs, one of which
  (9.3) exists only because of the dialog this change removes.

## Migration Plan

1. Archive `reminder-delivery-reliability` first, so `reminder-delivery-resilience` exists in
   `openspec/specs/` for this delta to modify.
2. Ship the Room migration with the code that reads `plannedAt`; the backfill makes existing rows
   inert rather than retroactively noisy.
3. No rollback concern in the data: `plannedAt` is additive, and a build without this change ignores
   the column. The DataStore key it removes (`battery_exemption_requested`) is simply abandoned;
   nothing reads it, and leaving it in the file is cheaper than a preferences migration.

## Open Questions

None. The one that stood at proposal time — what the banner's action should do on a device that is
already exempt, has missed a reminder and has no vendor screen — is answered in D3 above: the button
stays, because activating it is what acknowledges the miss.
