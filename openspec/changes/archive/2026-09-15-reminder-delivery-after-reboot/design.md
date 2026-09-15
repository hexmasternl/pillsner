## Context

Three faults, all in `data/reminders/`, all present in the code shipped by `app-medicine-alarm`, and
all independent of the single-alarm architecture that `reminder-delivery-reliability` replaces. That
independence is the reason this change exists separately: it can be written, tested and shipped
against the code as it stands today, and it leaves the larger change a correct base rather than a
base with three holes in it.

**Fault 1 — a dose is marked reminded when nothing was shown.** `ReminderCoordinator.onWake` does:

```kotlin
due.forEach { dose ->
    notifier.show(dose, due.size)
    if (dose.firstRemindedAt == null) doseRepository.setFirstReminded(dose.id, clock.instant())
    ...
}
```

`ReminderNotifier.show` opens with `if (!appContext.hasNotificationPermission()) return`, and `post`
swallows a `SecurityException`. Either way `show` returns normally and the dose is written as
reminded. `DueDoses` and `ComputeNextWake` both require `firstRemindedAt == null`, so the dose is now
invisible to both: it will never be announced and will never be a wake candidate. It simply lapses.

**Fault 2 — a timed-out wake converts a due dose into a missed one.** The `finally` that guarantees a
next alarm is outside `withTimeout`, so when the body is cancelled the code still runs
`rescheduleNextWake()` as though the wake had succeeded. `ComputeNextWake` then finds a dose whose
`scheduledAt` is in the past — no longer a candidate — and offers `markMissedDoses.lapseAt(dose)`
instead. The app goes back to sleep until the dose lapses, and wakes only to record it missed.

The `finally` is doing two jobs that look identical and are not: *always leave an alarm set* (right,
and this change keeps it) and *leave the alarm the successful path would have left* (wrong when the
body never ran).

**Fault 3 — no alarms between reboot and first unlock.** The manifest filters `BOOT_COMPLETED`, which
on a phone with a secure lock screen is delivered only after the user unlocks. Everything the app
needs to compute a wake — Room, the reminder DataStore — lives in credential-encrypted storage and is
unreadable before that point, so the app cannot simply listen for the locked variant and carry on as
normal.

Constraints: no network, no third-party SDKs, no new dependency for this change; Kotlin, AndroidX,
minSdk 26; the domain layer stays free of Android framework types; nothing identifying may be logged
or stored where it can be read before unlock.

## Goals / Non-Goals

**Goals:**

- A dose that could not be announced stays announceable.
- A wake that does not finish is retried, and never converts a due dose into a missed one.
- An alarm exists across a reboot, before the user unlocks the phone.
- No new dependency, no schema migration, no change to what a reminder says or how it is answered.

**Non-Goals:**

- Fixing why a cold-start wake is slow. That is the foreground service in
  `reminder-delivery-reliability`; this change makes the slow case survivable, not fast.
- Per-dose alarms, the alarm-clock tier, the watchdog, the battery-optimisation banner or repeating
  reminders. All of those are `reminder-delivery-reliability`.
- Reading medicines or doses before first unlock. The data is encrypted and moving it would be a
  privacy regression, not a fix.
- Any change to the notification's text, actions, snooze rule or wearable behaviour.

## Decisions

### D1 — `show` reports whether it posted, and the caller believes it

`ReminderNotifier.show` returns a `Boolean`: true only when `notificationManager.notify` was called
and did not throw. `post` returns the same. `ReminderCoordinator` writes `setFirstReminded` only when
`show` returned true.

A `Boolean` rather than a sealed result type: the caller has exactly one decision to make, and the
two reasons posting can fail — permission absent, `notify` refused — lead to the same behaviour and
are already distinguished for the user by the Home banner. A richer type would be read once and
discarded.

Consequence worth stating: a dose that could not be announced stays in `DueDoses` and stays a wake
candidate in `ComputeNextWake`. It is retried on every wake until it lapses. That is the intended
behaviour — the moment the user grants the permission, the next wake announces it — and it is bounded
by the existing lapse rule, so an undeliverable dose cannot accumulate wakes indefinitely.

*Alternative considered.* Checking the permission in `ReminderCoordinator` before calling `show`.
That leaves the `SecurityException` path still lying, and puts an Android permission check in the
coordinator, which is the wrong layer for it.

### D2 — Separate "always leave an alarm" from "leave the right alarm"

The wake body's outcome becomes explicit rather than inferred:

```
completed  -> reconcile normally
timed out  -> arm a retry two minutes out, unless retries are exhausted
failed     -> reconcile normally (an exception in one step is not a reason to retry the whole wake)
```

`rescheduleNextWake()` stays guaranteed — every branch above ends with an alarm set, which is what
`reminder-scheduling` requires and what the original `finally` was protecting. What changes is that
the timed-out branch no longer hands `ComputeNextWake` a world it will misread.

Two minutes, because it is long enough for the cold start that caused the timeout to have finished
and short enough that the reminder is still worth delivering. Three attempts, because a fourth
consecutive nine-second timeout means something is wrong that a fifth will not fix, and at that point
the ordinary lapse rule is the honest outcome.

The retry count is held in memory on the coordinator, not persisted. A retry sequence lives inside
one episode of trouble; if the process dies between retries the next wake starts fresh, which is the
behaviour that errs toward delivering the reminder.

*Alternative considered.* Raising the nine-second budget. It cannot be raised past the receiver's own
ten seconds, which is the actual ceiling, so it buys nothing. The real fix for slowness is the
foreground service in the follow-up change; this decision makes the timeout non-destructive in the
meantime and stays useful afterwards as a backstop.

### D3 — Mirror one timestamp into device-protected storage

The current alarm carries no payload: `ReminderAlarmReceiver` starts a wake and the wake works out
everything else from the database. So re-arming the alarm after a locked boot needs exactly one piece
of information — *when* — and nothing about which dose or which medicine.

`ReminderAlarmScheduler` writes that `Instant` to a Preferences DataStore built on
`createDeviceProtectedStorageContext()` every time it arms an alarm, and clears it when it cancels.
`SystemEventsReceiver`, on `LOCKED_BOOT_COMPLETED`, reads it and arms that one alarm. That is all it
does: no wake, no database.

The store holds one timestamp. No dose id, no medicine name, no amount. Device-protected storage is
readable before the user authenticates, which is exactly why nothing identifying may go in it — a
dose id would be a weak identifier but still an identifier, and the privacy rule in CLAUDE.md is
about not putting medication data anywhere it need not be.

*Alternative considered.* Moving the reminder DataStore to device-protected storage wholesale. It
would work and it would be less code, but it would place the notification-permission flag — and
anything a later change adds to that file — outside credential encryption for no benefit.

*Alternative considered.* Using `AlarmManager`'s own persistence. There is none; alarms do not survive
a reboot, which is the whole reason `BOOT_COMPLETED` is handled at all.

### D4 — The wake cycle refuses to run while the user is locked

With `directBootAware` receivers, an alarm armed by D3 can fire before the user has unlocked. The
wake that follows would try to open Room and fail on encrypted storage.

`ReminderCoordinator.onWake` therefore checks `UserManager.isUserUnlocked` first. While locked it
does one thing: re-arm the stored moment a few minutes out and return. It does not mark doses missed,
does not refresh the window and does not post anything, because it cannot read what any of those
need.

`ACTION_USER_UNLOCKED` then drives a full wake, which catches up on everything the locked period
accumulated — exactly as the existing reboot recovery already does, via the same code path.

The `isUserUnlocked` check lives in a small interface with an Android-backed implementation, so the
coordinator stays unit-testable and the domain layer stays free of framework types.

*Alternative considered.* Letting the wake throw and relying on the existing `catch`. The `catch`
would swallow it at debug level and the `finally` would arm the lapse moment — fault 2 again, by a
different route.

### D5 — Which broadcasts, and why both

`LOCKED_BOOT_COMPLETED` alone is not enough: it arrives before unlock, when nothing can be computed.
`ACTION_USER_UNLOCKED` alone is not enough: it arrives only when the user picks the phone up, which
is the gap this change exists to close. Both are needed, and `BOOT_COMPLETED` stays because on a
device with no secure lock screen it is the one that arrives.

All three map to existing or new `WakeReason` values and converge on the same wake cycle. Nothing
about the recovery logic is duplicated per broadcast.

Note that `LOCKED_BOOT_COMPLETED` needs no new permission — `RECEIVE_BOOT_COMPLETED`, already
declared, covers it — and that the receiver must be `directBootAware` or the broadcast is not
delivered to it at all.

## Risks / Trade-offs

- **An undeliverable dose is retried on every wake until it lapses.** → Bounded by the existing lapse
  rule (next dose of the same medicine, or 24 hours). The work per wake is one query and one
  early-returning `show`. This is the correct trade: the alternative is the current behaviour, which
  is losing the reminder.
- **The in-memory retry count resets when the process dies.** → Deliberate (D2). A fresh process is a
  fresh attempt, and erring toward delivering a reminder is the right direction for this product.
- **A timestamp in device-protected storage is readable before unlock.** → It is a bare `Instant`
  with no subject. Someone reading it learns that the phone will wake at a time, which the system's
  own next-alarm slot already tells them.
- **`directBootAware` receivers run in a context where most of the app is unusable.** → D4 makes that
  explicit rather than incidental: the locked path touches the device-protected store and
  `AlarmManager` and nothing else. The risk is a future change adding work to the wake cycle without
  noticing the locked branch; the KDoc on `onWake` says so and a test covers it.
- **This change and `reminder-delivery-reliability` touch the same files.** → They are sequenced, not
  parallel: this one ships first and the other has been narrowed to build on it. Both carry spec
  deltas against `reminder-scheduling`, so archiving them out of order would conflict.
- **`isUserUnlocked` is meaningful only on devices with file-based encryption and a secure lock
  screen.** → On devices without one it returns true always and the locked branch never runs, which
  is correct: `BOOT_COMPLETED` arrives immediately there and the ordinary path handles it.

## Migration Plan

No schema change, no migration, no new dependency.

Existing installs: the device-protected store is empty until the scheduler next arms an alarm, which
happens on the `MY_PACKAGE_REPLACED` wake the app already handles. So the first locked boot after the
update is protected. A locked boot in the window between installing the update and the first wake
falls back to today's behaviour — no alarm until unlock — which is no worse than before.

Rollback: reverting the app leaves an orphaned Preferences file in device-protected storage, holding
one timestamp. It is inert and is removed with the app.

## Open Questions

- Should a dose that repeatedly cannot be announced surface anywhere beyond the Home banner? The
  banner says reminders cannot be delivered, which is the cause; it does not say that three specific
  doses were lost to it. Held out of this change — it is a UI question and needs the designer — but
  the data to answer it exists once D1 lands.
- Two minutes and three attempts (D2) are chosen, not measured. Once the foreground service in
  `reminder-delivery-reliability` lands, the retry becomes a backstop that should almost never fire,
  and if it still fires regularly that is a signal worth acting on rather than a constant worth
  tuning.
