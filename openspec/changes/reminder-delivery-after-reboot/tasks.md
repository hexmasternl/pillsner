## 1. Scaffold check

- [x] 1.1 Verify the project scaffold exists (Gradle project at `src`, wrapper, version catalog, `app` module, manifest, `MainActivity`, `AppContainer`, theme) and stop if it does not; this change creates none of it
- [x] 1.2 Confirm this change adds no dependency and no version catalog entry, and that no Room schema version changes

## 2. Report whether a reminder was actually posted (design D1)

- [x] 2.1 Make `ReminderNotifier.post` return whether `notify` was called and did not throw
- [x] 2.2 Make `ReminderNotifier.show` return a `Boolean`: false when notification permission is absent, false when the post was refused, true otherwise
- [x] 2.3 Keep the group summary's own post from affecting the returned value; the dose's own notification is what decides it
- [x] 2.4 In `ReminderCoordinator.onWake`, write `setFirstReminded` only when `show` returned true
- [x] 2.5 Unit-test: permission absent leaves the dose un-reminded; a refused post leaves it un-reminded; a successful post records it; the dose stays a `DueDoses` and `ComputeNextWake` candidate while un-reminded
- [x] 2.6 Unit-test that an undeliverable dose still lapses as missed under the ordinary rule

## 3. Retry a wake that did not complete (design D2)

- [x] 3.1 Make the wake body's outcome explicit — completed, timed out, or failed — instead of inferring it from a blanket `finally`
- [x] 3.2 Move `rescheduleNextWake` out of the blanket `finally` into each branch, keeping "an alarm is set on every path" true
- [x] 3.3 On the timed-out branch, arm a retry two minutes out instead of the computed next wake
- [x] 3.4 Hold the consecutive retry count in memory on the coordinator, bounded at three, reset on a completed wake
- [x] 3.5 Add a `WakeReason` for the retry so the reason is visible in debug logs
- [x] 3.6 Unit-test: a timed-out wake arms the retry and not the dose's lapse moment; a successful retry clears the count; three consecutive timeouts stop retrying and still leave an alarm set; a thrown exception reschedules normally and arms no retry

## 4. Mirror the armed alarm moment (design D3)

- [x] 4.1 Add a Preferences DataStore built on `createDeviceProtectedStorageContext()` holding one `Instant`
- [x] 4.2 Write the moment from `ReminderAlarmScheduler.scheduleAt` and clear it from `cancel`
- [x] 4.3 Confirm by inspection and by test that nothing else is ever written to that store: no medicine name, no amount, no dose id
- [x] 4.4 Unit-test the store round-trip and that cancelling clears it

## 5. Refuse to wake while locked (design D4)

- [x] 5.1 Add a small interface reporting whether the user is unlocked, with an Android implementation over `UserManager.isUserUnlocked`, so `ReminderCoordinator` stays unit-testable
- [x] 5.2 In `ReminderCoordinator.onWake`, return early while locked after re-arming the stored moment a few minutes out; do not mark missed, refresh the window or post
- [x] 5.3 Document the locked branch in `onWake`'s KDoc so a later change does not add work that cannot run there
- [x] 5.4 Unit-test: locked wake posts nothing, marks nothing missed and still leaves an alarm set; unlocked wake behaves as before

## 6. Direct boot and unlock broadcasts (design D5)

- [x] 6.1 Add `WakeReason` values for locked boot and for user unlock
- [x] 6.2 Handle `LOCKED_BOOT_COMPLETED` in `SystemEventsReceiver` by arming the stored moment only, with no wake and no database access
- [x] 6.3 Handle `ACTION_USER_UNLOCKED` by running a full wake
- [x] 6.4 Add both actions to the receiver's manifest intent filter and mark `SystemEventsReceiver` and `ReminderAlarmReceiver` `directBootAware`
- [x] 6.5 Confirm no new permission is needed: `RECEIVE_BOOT_COMPLETED` already covers the locked variant
- [x] 6.6 Keep `BOOT_COMPLETED` handled as it is today, for devices with no secure lock screen

## 7. Wiring

- [x] 7.1 Register the device-protected store and the unlock-state source in `AppContainer`, keeping construction cheap and side-effect free
- [x] 7.2 Confirm a receiver can still build the container and reach the locked path without touching Room or the credential-encrypted DataStore

## 8. Verification

- [x] 8.1 Run the unit test task from `src` and report any failure verbatim
- [x] 8.2 Run the lint task from `src` and clear anything it raises
- [ ] 8.3 Run the instrumented tests; alarm scheduling and the boot receivers are both touched by this change
- [x] 8.4 Instrumented test: alarm armed, reboot, no unlock, confirm an alarm exists before first unlock. **Covered as far as an instrumented test can go**: `ReminderRecoveryTest` proves a recorded moment is put back (in the past → a few minutes out, still ahead → exactly where it was, nothing recorded → nothing armed) and `ReminderWakeTest` proves `LOCKED_BOOT_COMPLETED` reaches the receiver. A real reboot with no unlock is manual test 2.
- [x] 8.5 Instrumented test: unlock after a reboot that spanned a due dose, confirm the full wake runs and the reminder is posted. `ReminderRecoveryTest` proves the locked wake posts nothing and marks nothing missed, and that an unlocked wake reminds as before; `ReminderWakeTest` proves `ACTION_USER_UNLOCKED` reaches the receiver. The real reboot is manual test 3.
- [x] 8.6 Documented as manual test 1 in `manual-tests.md`; needs a real device, so it is written out rather than run here: revoke notification permission, let a dose fall due, grant the permission, confirm the reminder arrives on the next wake
- [x] 8.7 Documented as manual test 2 in `manual-tests.md`; needs a real device: reboot the phone overnight without unlocking, confirm the morning reminder arrives

## 9. Documentation and sequencing

- [x] 9.1 Confirm no medicine name or amount is logged at info level or above anywhere this change touches
- [x] 9.2 Confirm no network permission, third-party SDK or telemetry was added
- [x] 9.3 Re-check that `reminder-delivery-reliability` still reads correctly on top of this change, and that its task groups no longer duplicate anything here
