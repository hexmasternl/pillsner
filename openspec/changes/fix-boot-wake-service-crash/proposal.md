# Proposal

## Why

Pillsner crashes right after the phone reboots. On Android 15 and later, a `BOOT_COMPLETED` broadcast may not start a `shortService` foreground service. The boot wake still goes to `ReminderWakeService`. `startForegroundService()` succeeds, then `startForeground(..., FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)` throws `ForegroundServiceStartNotAllowedException: FGS type shortService not allowed to start from BOOT_COMPLETED!` (`ReminderWakeService.kt:100`, from `onStartCommand`). The existing guard only wraps `startForegroundService()`, so the receiver's fallback never runs. The process dies and the reboot wake is lost: no recompute, no catch-up reminders, no new alarm. This breaks the product's core promise to remind reliably.

## What Changes

- The wake that follows `BOOT_COMPLETED` no longer goes through the foreground service. It runs inside the boot receiver's own asynchronous budget, the same path the existing fallback uses. Every other route still uses the service.
- The wake service handles the platform refusing foreground status. If `startForeground()` throws for any route, the service records the refusal in the reminder delivery log, finishes the wake or answer it was started for, and stops itself without crashing the process.
- Tests cover both points: a boot wake bypasses the service, and a refused foreground start still runs the work without a crash.
- No change to exact alarms, alarm tiers, the locked-boot path or what a wake does.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `reminder-scheduling`: "Processing on wake" now says the work still completes, without crashing, when the platform refuses the foreground service. "Recovery after reboot and app update" now says the boot wake runs without a foreground service, which the platform forbids from a boot broadcast.

## Impact

- Code: `data/reminders/ReminderWakeService.kt`, `ReminderAlarmReceiver.kt` (`handOffWake`), `SystemEventsReceiver.kt`.
- Tests: new instrumented coverage next to `ReminderRecoveryTest` / `ReminderWakeTest`.
- No new permissions, dependencies, schema changes or UI changes. The diagnostics screen already shows `SERVICE_REFUSED`.
