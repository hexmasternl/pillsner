## Why

Pillsner's promise is "remind reliably, confirm in one tap, keep the data on the device", and after `app-medicine-add` the app can store medicines and schedules but never reminds anyone of anything. This change delivers the core of the product: turning schedules into planned doses, reminding the user at the exact moment on the phone and on a paired wearable, and recording in one tap whether the dose was taken.

## What Changes

- **Dose records.** For every moment a schedule says a medicine is due, the app keeps a `Dose` record with a snapshot of the medicine name and the amount, the time it was scheduled, and its outcome: pending, taken (with the time it was actually taken), skipped, or missed. Snapshots mean the history stays correct if the medicine is later renamed or removed. Doses are generated ahead for a rolling two-day window from the active medicines' schedules and kept up to date when medicines change, the day rolls over, the device reboots or the clock or time zone changes.
- **Reminder notification.** When a dose becomes due, the app posts a high-priority notification reading "Take {{dose}} of your medicine '{{name}}', on {{time-scheduled}}", for example "Take 40 mg of your medicine 'Ibuprofen', on 08:00". It offers three actions: **I took it**, **Not yet**, **Not going to**. The notification stays until the user answers.
- **Wearable.** The same notification, with its actions, appears on the user's paired Wear OS watch when one is connected, through the platform's notification bridging. No companion app and no Google Play services dependency.
- **Responses.**
  - *I took it* records the dose as taken at the moment of the tap and removes the notification.
  - *Not yet* snoozes: the notification is removed and posted again 15 minutes later, repeatedly, until the user answers or the dose lapses. Swiping the notification away counts as *Not yet*, since only *Not going to* is meant to stop reminders.
  - *Not going to* records the dose as skipped and removes the notification. No further reminder for that dose.
- **Missed doses.** A pending dose that is never answered becomes *missed* when the next dose of the same medicine is due, or 24 hours after its scheduled time, whichever comes first. Its notification is removed. A skipped dose is never counted as missed.
- **Exact scheduling.** Reminders fire from an exact platform alarm that wakes the device, not from periodic background work. The app keeps one alarm set for the next thing that has to happen (a dose falling due, a snooze ending, a dose lapsing, or the daily dose refresh). Reboots, app updates, clock changes and time zone changes reschedule it.
- **Permissions.** The app asks for notification permission the first time a medicine with a schedule exists, and declares the exact-alarm permission. When either is denied the Home screen shows a banner that says reminders cannot be delivered reliably, with a link to the system setting.
- **Home screen.** The welcome screen's upcoming doses now come from the real dose records: pending doses ordered by time, including a dose that is overdue but still awaiting an answer. `UpcomingDose.amount` becomes the typed `Quantity`, as `app-medicine-add` left for this change.
- **Persistence.** The Room database moves to schema version 2 with a `doses` table, a tested migration from version 1, and an exported schema.

## Capabilities

### New Capabilities
- `dose-records`: The `Dose` and `Intake` model, how doses are generated from every schedule shape within the rolling window, when the window is refreshed, the missed rule, the snapshot rule, and their storage.
- `medicine-reminders`: The reminder notification: its content, priority, persistence until answered, the three actions and their effects, snooze timing, swipe behaviour, grouping, lock-screen visibility, wearable bridging and notification permission.
- `reminder-scheduling`: Exact alarm scheduling of the next wake moment, processing when the alarm fires, exact-alarm permission handling, and recovery after reboot, app update, clock change, time zone change and daylight-saving transitions.

### Modified Capabilities
- `welcome-screen`: "Upcoming doses list" includes overdue pending doses; "Upcoming doses are provided through a domain contract" becomes the Room-backed dose repository with a typed amount; a new requirement adds the reminder permission prompt and readiness banner on the Home screen. This spec is a delta in the active `app-welcome-screen` change.
- `medication-persistence`: "Schema version 1" is replaced by a schema history requirement that adds version 2 with the `doses` table and its migration; "Migration test harness" covers the 1-to-2 migration. This spec is a delta in the active `app-medicine-add` change.

Archive order MUST be `app-welcome-screen`, `app-medicine-overview`, `app-medicine-add`, then this change.

## Impact

- **Application code (`src/`)**: the domain gains `Dose`, `DoseId` (already declared by the welcome screen), `Intake`, `IntakeOutcome`, a pure `DoseGenerator`, a `DoseRepository` interface, a `Clock` seam and use cases for refreshing planned doses, recording intakes, snoozing, marking missed and computing the next wake time. The data layer gains `DoseEntity`, `DoseDao`, the 1-to-2 migration, `RoomDoseRepository`, `RoomUpcomingDosesRepository` (replacing the empty one), an `AlarmManager`-based `ReminderAlarmScheduler`, a `ReminderNotifier` built on `NotificationCompat`, and a `ReminderCoordinator` that ties them together. New `BroadcastReceiver`s handle the alarm, the notification actions, boot, package replacement and time changes. `AppContainer` wires all of it. The Home screen gains the permission prompt and banner.
- **Manifest**: `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (Android 12 only), `RECEIVE_BOOT_COMPLETED`; receivers for boot, package replaced, time and time zone changes, the alarm and the notification actions. No network permission.
- **Dependencies** (first party): `androidx.core:core-ktx` for `NotificationCompat` (likely already present), `androidx.room` already added by `app-medicine-add`. No Wear OS, Play services or third-party libraries.
- **Depends on**: `app-welcome-screen`, `app-medicine-overview` and `app-medicine-add` applied first. `app-login` is unaffected; its lock gate does not block notification actions because they run in receivers, not the activity.
- **Privacy**: the notification shows the medicine name and amount. On the lock screen the system's "sensitive content" setting decides; the app supplies a public version reading "Time for your medicine" without name or amount. Nothing about doses is logged at info level or above.
- **Store policy**: `USE_EXACT_ALARM` is reserved by Google Play for apps whose core function is alarms or reminders. Pillsner qualifies as a medication reminder, and the README will state the permission and why.
- **Tests**: unit tests for generation over every schedule shape and the edge cases `CLAUDE.md` names (midnight, month end, leap day, daylight-saving gap and overlap, time zone move), the missed rule, snooze bounds and next-wake computation; Room migration 1-to-2 and DAO tests; instrumented tests for the alarm receiver, the action receivers and the boot receiver; Compose tests for the Home banner; documented manual test cases for reboot, clock change, time zone change and the wearable.
- **README**: "Features" gains reminders with one-tap confirmation, snooze and skip, wearable notifications; the permissions and their reasons are disclosed.

## Non-goals

- Answering a dose from inside the app (tapping a tile on the Home screen). The notification is the only intake input in this change; in-app confirmation and an intake history screen are their own change.
- Adherence statistics and refill tracking.
- A Wear OS companion app, watch-face complications or wearables that do not mirror Android notifications.
- Configurable snooze length, quiet hours, or per-medicine notification sounds. Snooze is 15 minutes, full stop.
- Editing doses (changing the time of one occurrence) or taking a dose early from the app.
- Automatic deactivation when "use until" has passed. Doses are simply not generated after that date; the overview still shows the medicine as active until an edit change addresses it.
