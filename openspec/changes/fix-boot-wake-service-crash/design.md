# Design

## Context

See proposal.md, Why. The wake path today:

`SystemEventsReceiver` maps the broadcast to a `WakeReason` and calls `handOffWake`. When the user is unlocked, `handOffWake` calls `ReminderWakeService.startWake`. If that returns `false`, it records `SERVICE_REFUSED` and runs `onWake` inside `goAsync()`. `startWake` returns `false` only when `startForegroundService()` throws. On Android 15+, a boot broadcast's start passes that call, and the refusal comes later from `startForeground()` inside `onStartCommand`. Nothing catches it there, so the process crashes. `LOCKED_BOOT_COMPLETED` already skips the service (it only re-arms stored alarms). `USER_UNLOCKED`, `MY_PACKAGE_REPLACED`, time changes and alarms are not boot broadcasts and are not affected by this restriction.

## Goals / Non-Goals

**Goals:**
- A reboot never crashes the app, and the boot wake still completes.
- A refused `startForeground()` on any route is survived, logged and followed by the work.

**Non-Goals:**
- Changing the service's foreground type, alarm tiers, the watchdog or what `onWake` does.
- Reworking the locked-boot or first-unlock paths.

## Decisions

**D1. The boot wake skips the service and runs in the receiver.** In `handOffWake`, `WakeReason.BOOT` goes straight to the `goAsync()` path, the same way the locked case already does. This is a known platform rule, not a refusal, so it is not logged as `SERVICE_REFUSED`. The boot wake gets the receiver's budget (`onWake`'s default timeout, not `SERVICE_WAKE_TIMEOUT_MILLIS`). That is acceptable. On a device with a secure lock screen, `USER_UNLOCKED` usually runs the full wake inside the service anyway. If the receiver budget runs out, the wake's retry and the watchdog already enqueued by `SystemEventsReceiver` are the backstop.
- *Alternative: switch to a foreground type allowed from boot* (for example `specialUse`, or `systemExempted` through the exact-alarm permission). Rejected: this needs new manifest declarations and Play policy justification, and the type would still apply to every route for a single edge case. Google's list of boot-restricted types has also grown between releases.
- *Alternative: enqueue expedited WorkManager work for boot.* Rejected for now: the receiver path already exists and is tested, and expedited work can itself become a foreground service.

**D2. `goForeground()` becomes fallible.** It wraps `startForeground()` and returns whether it succeeded. It catches `ForegroundServiceStartNotAllowedException` on API 31+, plus `SecurityException` and `IllegalStateException` (thrown for a missing type permission or invalid type). On failure, `onStartCommand` records `SERVICE_REFUSED` with the command's reason, then still parses and runs the command in the service's coroutine scope and calls `stopSelf(startId)` when it finishes, exactly as it does on success. The process stays alive while the started service runs, and the 9-second wake budget is far below any background-service idle limit. This is defence in depth: D1 removes the known trigger, D2 makes sure the next platform restriction cannot crash the app.
- *Alternative: stop at once and let the watchdog repair.* Rejected: the work is cheap and the process is already running, so throwing it away gives a later reminder for no gain.

**D3. Detect "boot" from the reason, not the platform version.** Skip the service for `BOOT` on every API level. Below Android 15 the rule does not apply, but one path is simpler to reason about and test than a version branch. The cost is a smaller budget on old devices at boot, and the retry already covers that.

## Risks / Trade-offs

- [After a refused `startForeground()`, the platform may still raise `ForegroundServiceDidNotStartInTimeException` for a service started with `startForegroundService()`.] → In apply, verify on an Android 15+ device or emulator that a refused promotion followed by `stopSelf` does not trigger it. If it does, change D2 to call `stopSelf(startId)` immediately and run the work in an application-scoped coroutine instead. That is an implementation detail and does not change the spec.
- [Boot wake with a receiver budget can time out on a slow cold start.] → Covered by the existing wake retry and the watchdog, both already enqueued on boot.

## Migration Plan

None. Code-only fix with no data or manifest change. Roll back by reverting the commit.
