## Why

A reminder scheduled for a time today never arrived: the phone was put on a call, the app went to
the background, and the notification only appeared the moment the app was opened again. The dose's
`firstRemindedAt` was still unset at that point, so the alarm wake had never run at all — the
platform deferred or dropped the app's one alarm while it sat idle, and because that alarm is also
the only thing that arms the next one, reminding stopped dead until the user happened to open the
app.

Pillsner's entire product promise is "remind reliably". A reminder that arrives only when the user
is already looking at the app is not a reminder, and every other feature in this repository rests on
this one working.

## What Changes

- **BREAKING** The single next-wake alarm becomes a set of alarms: one `setAlarmClock` alarm per due
  dose inside the planning window, plus one housekeeping alarm for the daily refresh and the next
  lapse. Losing one alarm now costs one reminder instead of every reminder from that point on.
- Reminder alarms move from `setExactAndAllowWhileIdle` to `AlarmManager.setAlarmClock`, the only
  tier the platform and OEM power managers treat as inviolable. The housekeeping alarm stays on
  `setExactAndAllowWhileIdle`, where a few minutes of drift costs nothing.
- A periodic `WorkManager` watchdog runs every 15 minutes. It never delivers a reminder itself; it
  verifies that the alarms the app expects are still armed, re-arms them if not, and posts anything
  that fell due unnoticed. This is the net that catches a broken chain without the user opening the
  app.
- The app asks to be exempted from battery optimisation, checks whether it is, and the Home banner
  gains a third state telling the user when the system is throttling reminders — with a button to
  the system setting, and to the vendor's auto-start screen on OEMs known to kill background apps.
- A reminder repeats until it is answered or lapses, rather than being announced exactly once.
  A due dose also gets a full-screen intent so it can break through on a locked or busy phone.
- Wake processing moves off the broadcast-receiver budget into a short foreground service, so a
  cold start that has to open the database cannot run out of time and lose the reminder silently.
- Two silent-loss defects are fixed: a dose is no longer recorded as reminded when the notification
  was not actually posted, and a wake that times out now re-arms a short retry instead of falling
  through to the dose's lapse moment.
- The app re-arms its alarms on `LOCKED_BOOT_COMPLETED` and on user unlock, so a reboot does not
  leave the user unreminded until they unlock the phone.

## Capabilities

### New Capabilities
- `reminder-delivery-resilience`: how the app keeps reminding when the platform works against it —
  the battery-optimisation exemption and its Home banner, the periodic watchdog that repairs a
  broken alarm chain, and the OEM-specific guidance the user needs to keep Pillsner alive.

### Modified Capabilities
- `reminder-scheduling`: the single next-wake alarm is replaced by per-dose alarms plus one
  housekeeping alarm; reminder alarms use the alarm-clock tier; wake processing runs in a short
  foreground service rather than inside the receiver budget; recovery extends to direct boot and
  user unlock; a timed-out wake retries rather than lapsing the dose.
- `medicine-reminders`: a reminder repeats until answered rather than being posted once; a due dose
  uses a full-screen intent; a dose is only recorded as reminded when the notification was posted;
  the Home banner gains the battery-optimisation state.

## Impact

**Code** — `data/reminders/`: `ReminderAlarmScheduler` (per-dose alarms, alarm-clock tier),
`ReminderCoordinator` (repeat, retry, reminded-only-when-shown), `ReminderAlarmReceiver` and
`SystemEventsReceiver` (hand off to the foreground service), `ReminderNotifier` (full-screen intent,
`show` reports whether it posted). New: a short foreground service, a `WorkManager` watchdog worker,
a battery-optimisation status source. `domain/scheduling/`: `ComputeNextWake` becomes
`ComputeWakeSchedule` returning a set of moments; `DueDoses` gains the repeat rule.
`ui/home/`: `HomeUiState`, `HomeViewModel` and `ReminderBanner` gain the throttling state.

**Manifest** — adds `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_SHORT_SERVICE`, `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED` for
`LOCKED_BOOT_COMPLETED`; declares the new service; marks the alarm receiver direct-boot aware.

**Dependencies** — adds `androidx.work:work-runtime-ktx` to the version catalog. It is AndroidX, it
is the only supported way to run a periodic check that survives process death, and writing one by
hand would mean re-implementing its scheduling and backoff.

**Privacy** — unchanged. No new network access, nothing leaves the device, and the new logging
stays at debug level with dose ids only.

**Battery** — a 15-minute periodic worker and per-dose alarm-clock alarms cost more than one idle
alarm. That is the trade the product promise requires, and the worker does nothing when there are
no pending doses.
