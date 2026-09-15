## Why

Two defects in the current reminder path can each lose a reminder without a trace, and neither
needs the platform to misbehave first — the app does it to itself:

1. `ReminderNotifier.show` returns silently when notification permission is missing, but
   `ReminderCoordinator` records `setFirstReminded` regardless. Because both `DueDoses` and
   `ComputeNextWake` gate on `firstRemindedAt == null`, that dose can never be announced again. The
   user grants the permission ten minutes later and the dose still goes to missed, unannounced.
2. `ReminderCoordinator.onWake` wraps the posting work in `withTimeout(9_000)` but puts
   `rescheduleNextWake()` in the `finally` outside it. When an alarm starts a dead process and the
   body times out — Room opening, migrations, two `runBlocking` DataStore reads in
   `PillsnerApplication.onCreate` and `AppContainer.theme`, and an `APP_START` wake already holding
   the mutex ahead of the alarm — nothing is posted, nothing is logged above debug, and
   `ComputeNextWake` then arms the dose's *lapse* moment. The dose becomes missed having never been
   announced.

Separately, the manifest listens for `BOOT_COMPLETED` only. On a phone with a lock screen that
broadcast does not arrive until the user unlocks, so after a reboot the app has no alarm set at all
for however long the phone sits locked. A reboot overnight means no morning reminder until the user
picks the phone up — by which time the reminder is pointless.

These are small, contained and independent of any redesign. They are worth fixing on their own,
before the larger `reminder-delivery-reliability` change, so that a user gets the benefit sooner and
so that change has a correct foundation to build on.

## What Changes

- `ReminderNotifier.show` reports whether it actually posted. `ReminderCoordinator` records a dose as
  reminded only when it did, so a dose that could not be announced stays announceable.
- A wake that does not complete within its budget arms a retry two minutes out rather than falling
  through to the dose's lapse moment. Retries are bounded at three, after which the ordinary lapse
  rule applies.
- `rescheduleNextWake` moves out of the blanket `finally` into a path that knows whether the wake
  body completed, so "reschedule even on failure" and "reschedule as though everything succeeded"
  stop being the same code.
- The app mirrors its next alarm moment into a small device-protected store, and re-arms that alarm
  on `LOCKED_BOOT_COMPLETED` — before first unlock. A full wake follows on `ACTION_USER_UNLOCKED`.
- The wake cycle refuses to run while the user is still locked, rather than failing on an unreadable
  database, and re-arms instead.

## Capabilities

### New Capabilities
<!-- None. Every requirement here corrects behaviour that reminder-scheduling and medicine-reminders
     already promise; none of it introduces a new user-facing capability. -->

### Modified Capabilities
- `reminder-scheduling`: recovery after reboot extends to direct boot and first unlock; a dose is
  recorded as reminded only when its notification was posted; a wake that does not complete is
  retried rather than allowed to lapse the dose.
- `medicine-reminders`: a dose that fell due while notification permission was absent stays
  un-reminded, so granting the permission before the dose lapses still produces its reminder.

## Impact

**Code** — `data/reminders/`: `ReminderNotifier` (`show` returns a result, `post` reports refusal),
`ReminderCoordinator` (record-when-posted, retry, reschedule moved out of the blanket `finally`),
`ReminderAlarmScheduler` (mirror the armed moment), `SystemEventsReceiver` (`LOCKED_BOOT_COMPLETED`,
`ACTION_USER_UNLOCKED`, two new `WakeReason` values). New: a small device-protected store holding the
next alarm moment.

**Manifest** — adds `LOCKED_BOOT_COMPLETED` and `ACTION_USER_UNLOCKED` to the system events filter;
marks `SystemEventsReceiver` and `ReminderAlarmReceiver` `directBootAware`. No new permissions:
`RECEIVE_BOOT_COMPLETED` already covers the locked variant.

**Dependencies** — none. No new library, no version catalog change.

**Privacy** — the device-protected store holds one timestamp and nothing else: no medicine name, no
amount, no dose identifier. Device-protected storage is readable before unlock, which is precisely
why nothing identifying may go in it.

**Schema** — none. No Room migration.

**Relationship to `reminder-delivery-reliability`** — that change originally carried this work as its
task groups 5 and 9. It has been narrowed to exclude them and now builds on this change: its
per-dose alarm set extends the single-moment store introduced here into a set, and its foreground
service replaces the retry introduced here as the primary defence while keeping the retry as a
backstop. This change ships first and stands alone.
