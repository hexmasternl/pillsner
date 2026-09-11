## 1. Preconditions

- [ ] 1.1 Verify `app-welcome-screen`, `app-medicine-overview` and `app-medicine-add` are applied: `PillsnerDatabase` at version 1 with exported `1.json`, `Medication` with `schedules`, `usedSince`, `useUntil`, `Quantity`, `QuantityFormatter`, `UpcomingDosesRepository`, `AppContainer`. Stop if any is missing
- [ ] 1.2 Confirm the archive order (`app-welcome-screen`, `app-medicine-overview`, `app-medicine-add`, then this change) so the MODIFIED and REMOVED deltas resolve; if it cannot be honoured, rewrite them as ADDED with distinct names before archiving

## 2. Domain model and clock

- [ ] 2.1 Create `domain/model/Dose.kt` with `IntakeOutcome`, `Intake`, `Dose` (nullable `medicationId`, snapshot `medicationName` and `amount: Quantity`, `scheduledAt`, `intake`, `snoozedUntil`, `firstRemindedAt`, `isPending`) and `PlannedDose`, with KDoc; no Android imports
- [ ] 2.2 Change `UpcomingDose.amount` from `String` to `Quantity` and update `UpcomingDoseTile` to format it with `QuantityFormatter`; update the preview repository and existing welcome tests
- [ ] 2.3 Introduce a `Clock` seam (`java.time.Clock`) in the domain and add it to `AppContainer` as `Clock.systemDefaultZone()`; add a mutable test clock in the test sources
- [ ] 2.4 Create `domain/repository/DoseRepository.kt` with `observePending()`, `observeUpcoming(from, limit)`, `getDue(now)`, `insertPlanned(list)` ignoring duplicates, `deletePlannedNotIn(window, keep)`, `recordIntake(id, outcome, at)`, `setSnooze(id, until?)`, `setFirstReminded(id, at)`, `nextScheduledAtAfter(medicationId, after)`, each with KDoc

## 3. Generation and use cases

- [ ] 3.1 Create `domain/scheduling/DoseGenerator.kt` implementing the rules of design D2 for `EveryNDays` (anchored on `usedSince`), `OnWeekdays` and `EveryNHours` (same-day only), skipping inactive medications and dates outside used-since/use-until, converting through `ZonedDateTime.of(date, time, zone)`, de-duplicating on medication and instant, sorted ascending; KDoc the DST gap and overlap behaviour
- [ ] 3.2 Create `domain/scheduling/RefreshPlannedDoses.kt`: window today..tomorrow in the clock's zone, plan every medication, insert missing, delete pending un-reminded doses in the window not in the planned set, never touch doses with an intake or a first-reminded moment
- [ ] 3.3 Create `domain/scheduling/MarkMissedDoses.kt` computing `lapseAt = min(scheduledAt + 24h, next dose of same medication)` and marking pending doses missed when `lapseAt <= now`, clearing snooze; expose `lapseAt(dose)` for reuse
- [ ] 3.4 Create `domain/scheduling/DueDoses.kt` returning pending doses with `scheduledAt <= now && firstRemindedAt == null` or `snoozedUntil <= now`
- [ ] 3.5 Create `domain/scheduling/ComputeNextWake.kt` returning the minimum of next un-reminded dose, next snooze end, next lapse, and the next 00:05 local refresh (only when any active medicine with schedules exists), or null
- [ ] 3.6 Create `domain/intake/RecordIntake.kt` (taken or skipped at a moment, clears snooze) and `domain/intake/SnoozeDose.kt` (`snoozedUntil = min(now + 15 min, lapseAt)`)

## 4. Persistence: schema version 2

- [ ] 4.1 Create `data/db/DoseEntity.kt` per design D9 with `ForeignKey(onDelete = SET_NULL)`, unique index on `(medication_id, scheduled_at)` and index on `scheduled_at`; add type converters for `Instant` (epoch millis) and `IntakeOutcome`
- [ ] 4.2 Create `data/db/DoseDao.kt` with the queries backing every `DoseRepository` operation, using `OnConflictStrategy.IGNORE` for planned inserts and `@Transaction` where several statements are involved
- [ ] 4.3 Add `MIGRATION_1_2` in `data/db/Migrations.kt`, bump `PillsnerDatabase` to version 2, register the migration in `AppContainer`, build to export `2.json` and check it in
- [ ] 4.4 Extend `PillsnerDatabaseMigrationTest` with `migrate1To2`: seed a version 1 database with one medication and one schedule, migrate, validate against `2.json`, assert old rows unchanged and `doses` empty
- [ ] 4.5 Create `data/RoomDoseRepository.kt` and `data/RoomUpcomingDosesRepository.kt` (pending doses ordered by `scheduled_at`, limited, including overdue ones); replace `EmptyUpcomingDosesRepository` in `AppContainer`; keep an `InMemoryDoseRepository` for unit tests and previews

## 5. Alarm scheduling

- [ ] 5.1 Create `data/reminders/ReminderAlarmScheduler.kt` wrapping `AlarmManager`: one fixed request code, `setExactAndAllowWhileIdle(RTC_WAKEUP)` when `canScheduleExactAlarms()` is true, otherwise `setWindow` with a ten-minute window; `cancel()`; expose `isExact: StateFlow<Boolean>` for the banner
- [ ] 5.2 Create `data/reminders/ReminderCoordinator.kt` with an application-scoped `SupervisorJob` on `Dispatchers.IO` and `onWake(reason)` running mark-missed, refresh, post due notifications (set first-reminded, clear snooze), then compute-next-wake and reschedule inside `finally`, all under a 9-second `withTimeout`; also collect `MedicationRepository.observeAll()` and call `onWake(MEDICATIONS_CHANGED)` on each emission
- [ ] 5.3 Create `data/reminders/ReminderAlarmReceiver.kt` using `goAsync()` to call `onWake(ALARM)` and `finish()` in `finally`
- [ ] 5.4 Create `data/reminders/SystemEventsReceiver.kt` for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` and `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, each calling `onWake` with a matching reason
- [ ] 5.5 Add manifest entries: `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"`, `RECEIVE_BOOT_COMPLETED`; the three receivers with their intent filters; confirm no network permission
- [ ] 5.6 Call `onWake(APP_START)` from `PillsnerApplication.onCreate` after the container is built, keeping `AppContainer` construction cheap so receivers can start the process

## 6. Notifications

- [ ] 6.1 Create `data/reminders/ReminderChannels.kt` creating the `reminders` channel (high importance, default sound and vibration, lights, badge) at app start
- [ ] 6.2 Add string resources: channel name and description, notification title "Time for your medicine", `reminder_text` "Take %1$s of your medicine '%2$s', on %3$s", the three action labels, public-version text, group summary plural "%d medicines due", permission rationale, banner texts and button labels
- [ ] 6.3 Create `data/reminders/ReminderNotifier.kt` building the notification per design D7: category reminder, high priority, not auto-cancel, alert on re-post, `BigTextStyle`, three action `PendingIntent`s with distinct request codes, delete intent mapped to snooze, content intent to `MainActivity`, private visibility with a public version carrying the same actions, group with summary when more than one dose is due, `WearableExtender` with "I took it" as content action; `cancel(doseId)`
- [ ] 6.4 Reuse `UpcomingDoseTimeFormatter` for the time part, including the short day indication when the scheduled date is not today
- [ ] 6.5 Create `data/reminders/ReminderActionReceiver.kt` handling taken, snooze, skip and the delete intent with `goAsync()`: record or snooze, cancel the notification, then `onWake(ACTION)`
- [ ] 6.6 Ensure no log statement in `data/reminders` includes a name or amount; add a lint rule or a unit test that scans the package for `Log.[iwe]` calls with string templates

## 7. Home screen

- [ ] 7.1 Create `ui/home/NotificationPermissionRequest.kt`: on Android 13+, request `POST_NOTIFICATIONS` once when an active medicine with a schedule exists and permission is not granted, remembering the request in a `DataStore` flag
- [ ] 7.2 Create `ui/home/ReminderReadinessBanner.kt` shown when notifications are denied or exact alarms are unavailable, with a button opening `ACTION_APP_NOTIFICATION_SETTINGS` or `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; expose the readiness state through `HomeViewModel`
- [ ] 7.3 Re-check readiness when the activity resumes so the banner disappears after the user returns from settings

## 8. Tests

- [ ] 8.1 Unit test `DoseGenerator` for every scenario in `dose-records`: daily two times, every other day anchored, weekdays, every 8 hours, every 12 hours from 20:00, inactive, used-since/use-until bounds, coinciding schedules, month boundary, leap day, midnight, spring-forward gap, autumn overlap, zone move
- [ ] 8.2 Unit test `RefreshPlannedDoses`: new medicine, idempotence, schedule removed, medicine deactivated, day rollover, reminded and answered doses untouched
- [ ] 8.3 Unit test `MarkMissedDoses` and `SnoozeDose`: superseded by next dose, 24-hour cap, skipped stays skipped, snoozed dose lapses, snooze bounded by lapse
- [ ] 8.4 Unit test `ComputeNextWake` and `DueDoses` for every scenario in `reminder-scheduling`, including "nothing to remind", clock moved forward and clock moved backward with a mutable test clock
- [ ] 8.5 Unit test `HomeViewModel` with the new repository: overdue pending dose first, answered dose disappears, cap of five
- [ ] 8.6 Room tests: `DoseDao` round trip, duplicate planned insert ignored, `SET NULL` on medication delete, `observePending` re-emits; migration test from 4.4
- [ ] 8.7 Instrumented tests: `ReminderActionReceiver` records taken, skipped and snooze and cancels the notification; `ReminderAlarmReceiver` posts a notification for a due dose and sets the next alarm (verify through `AlarmManager.getNextAlarmClock` or a scheduler fake); `SystemEventsReceiver` on `BOOT_COMPLETED` schedules the alarm
- [ ] 8.8 Instrumented test for notification content: text equals "Take 40 mg of your medicine 'Ibuprofen', on 08:00" in an English locale with a fixed clock, three actions in order, public version has no name, not local-only
- [ ] 8.9 Compose semantics test for `ReminderReadinessBanner`: shown for denied notifications, shown for inexact alarms, hidden when both permitted, button and message readable by a screen reader
- [ ] 8.10 Document manual test cases in the change: reboot across a dose and across a lapse; clock forward and backward; time zone change; the daylight-saving night; reminder on an idle device with the app killed; answering from a Wear OS watch; lock screen with sensitive content hidden and shown; largest font scale on the notification; one aggressive OEM battery optimiser

## 9. Verification and documentation

- [ ] 9.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim
- [ ] 9.2 Run `./gradlew connectedAndroidTest` on a device or emulator for the Room, receiver, notification and Compose tests
- [ ] 9.3 Update `README.md`: "Features" gains reminders with one-tap confirm, 15-minute snooze, skip, and wearable delivery; add a permissions section disclosing notifications, exact alarms (and why), and boot completed
- [ ] 9.4 Review against `CLAUDE.md`: exact alarms only, distinct snooze/skip/confirm outcomes, reboot and time-change coverage, migration with test, no Android imports in domain, no sensitive logging, strings in resources, no new third-party dependencies
