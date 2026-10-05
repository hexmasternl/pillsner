## 1. Preconditions

- [x] 1.1 Verify the project scaffold from `app-welcome-screen` exists under `src/` (Gradle project, `app` module, `ReminderWakeService`, `SystemEventsReceiver`); stop if it does not

## 2. Boot wake bypasses the service (D1, D3)

- [x] 2.1 In `handOffWake`, route `WakeReason.BOOT` straight to the in-receiver `goAsync()` path without calling `ReminderWakeService.startWake` and without recording `SERVICE_REFUSED`
- [x] 2.2 Update the KDoc on `handOffWake` and `SystemEventsReceiver` to explain why the boot wake skips the service

## 3. Survive a refused foreground promotion (D2)

- [x] 3.1 Make `ReminderWakeService.goForeground()` return whether `startForeground()` succeeded, catching `ForegroundServiceStartNotAllowedException` (API 31+), `SecurityException` and `IllegalStateException`
- [x] 3.2 On failure, record `DeliveryEvent.SERVICE_REFUSED` with the command's reason (no medicine names or amounts) and still run the command, then `stopSelf(startId)`
- [x] 3.3 Fix the comment in the companion `start()` that says the boot broadcasts are allowed to start the service

## 4. Tests

- [x] 4.1 Instrumented test: a `BOOT_COMPLETED` delivered to `SystemEventsReceiver` runs the wake (window refreshed, alarm armed) without starting `ReminderWakeService`
- [x] 4.2 Instrumented or unit test: when the foreground promotion is refused, the service does not throw, `SERVICE_REFUSED` is logged, and the wake still reconciles the alarm set (use a test seam for the promotion)
- [ ] 4.3 Manual test on an Android 15+ device or emulator: reboot with a scheduled medicine, confirm no crash, that a catch-up reminder appears and that the next alarm is set; also confirm a refused promotion does not raise `ForegroundServiceDidNotStartInTimeException` (design Risks)

## 5. Verification

- [x] 5.1 Run the unit tests and lint from `src/`
- [ ] 5.2 Run the instrumented tests (scheduling code changed)
