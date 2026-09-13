## Context

After `app-medicine-add` the app has a single activity, a Home screen fed by an `UpcomingDosesRepository` whose only implementation returns nothing, a Room database at version 1 with `medications` and `schedules`, and a domain model in which a `Medication` has `defaultDose`, `usedSince`, `useUntil`, `isActive` and a list of `Schedule`s of three shapes: `EveryNDays(amount, intervalDays, times)`, `OnWeekdays(amount, days, times)` and `EveryNHours(amount, intervalHours, firstDoseAt)`. Every schedule is a wall-clock rule anchored on `usedSince`.

This change adds the layer that turns those rules into moments and moments into notifications. It is the most fragile part of the product: it runs when the app is not open, it depends on platform alarm behaviour that has tightened in every Android release since 6, and any mistake means a person does not take their medicine. The design therefore favours one simple mechanism over several clever ones, and puts every time calculation into pure Kotlin where it can be tested against a fake clock.

Constraints from `CLAUDE.md`: exact user-facing alarms, never periodic work for the reminder moment; reboot, time zone, daylight-saving and clock changes each covered by a test or a documented manual test; snooze, skip and confirm are distinct outcomes and a skipped dose is not a missed dose; domain layer without Android imports; Room migration with test; no logging of names or doses at info level or above; strings in resources; no new third-party dependencies; the notification and every Home screen change follow `docs/design-system.md` (sections 2.3, 8.1, 8.8 and 8.9) and pass `pillsner-ui-review`.

## Goals / Non-Goals

**Goals:**
- Dose records that faithfully represent every planned intake, with snapshot name and amount, and survive medicine edits and removal.
- A reminder that fires at the scheduled minute, on the phone and on a bridged wearable, stays until answered, and offers exactly the three requested answers.
- Correct behaviour across reboot, app update, clock change, time zone change and daylight-saving transitions, each tested.
- A single, inspectable scheduling mechanism: one exact alarm for the next thing that has to happen.
- The Home screen shows real upcoming doses.

**Non-Goals:**
- In-app intake actions, history, adherence, refill.
- Wear OS companion app or data-layer sync.
- Configurable snooze, quiet hours, sounds.
- Deactivating medicines whose "use until" has passed.

## Decisions

### D1. Domain model: `Dose` with an optional `Intake`

```kotlin
enum class IntakeOutcome { TAKEN, SKIPPED, MISSED }

/** The recorded outcome of a dose. For TAKEN, [recordedAt] is the moment the user took it. */
data class Intake(val outcome: IntakeOutcome, val recordedAt: Instant)

data class Dose(
    val id: DoseId,
    val medicationId: MedicationId?,      // null once the medication has been deleted
    val medicationName: String,           // snapshot at generation time
    val amount: Quantity,                 // snapshot at generation time
    val scheduledAt: Instant,
    val intake: Intake?,                  // null = pending
    val snoozedUntil: Instant?,           // pending doses only
    val firstRemindedAt: Instant?,        // null until the first notification was posted
) {
    val isPending get() = intake == null
}
```

*Why one record with an optional intake rather than a `doses` and an `intakes` table:* a dose has at most one outcome, and every query the product needs ("what is pending", "what was taken today") is a filter on one row. Two tables would add a join and an invariant (at most one intake per dose) that SQLite cannot express without a unique index anyway.

*Why snapshots:* the glossary's `Intake` is a historical fact. If the user renames "Ibuprofen 400" to "Ibuprofen" or changes the amount next week, what they took last Tuesday must not change. Snapshots also let a dose outlive its medicine (`medicationId` is nullable, see D8) so history survives deletion in a later change.

*Why `firstRemindedAt`:* it separates doses the user has been told about from doses that are still only planned. Planned, un-reminded doses may be regenerated freely when a schedule or the time zone changes; reminded doses are facts the user saw and are never silently rewritten.

`DoseId` already exists from `app-welcome-screen` and is reused.

### D2. Generation: pure `DoseGenerator` over a local-date window

```kotlin
class DoseGenerator {
    /** Planned doses for [medication] on every date in [window], as wall-clock moments in [zone]. */
    fun plan(medication: Medication, window: ClosedRange<LocalDate>, zone: ZoneId): List<PlannedDose>
}
data class PlannedDose(val medicationId: MedicationId, val medicationName: String, val amount: Quantity, val scheduledAt: Instant)
```

Rules, per date `d` in the window, skipped entirely when the medication is inactive, `d < usedSince`, or `useUntil != null && d > useUntil`:
- `EveryNDays(n, times)`: dose at each time when `ChronoUnit.DAYS.between(usedSince, d) % n == 0`.
- `OnWeekdays(days, times)`: dose at each time when `d.dayOfWeek in days`.
- `EveryNHours(h, first)`: doses at `first + k*h` for `k >= 0` while the result is on the same day (`first.toSecondOfDay() + k*h*3600 < 86400`).
- Local to instant: `ZonedDateTime.of(d, time, zone).toInstant()`. On a spring-forward gap `java.time` moves the time forward by the gap (02:30 becomes 03:30); on an autumn overlap it takes the earlier offset (first occurrence). Both are tested and documented in KDoc; they match what a bedside alarm clock does.
- Results are de-duplicated on `(medicationId, scheduledAt)` so two schedules that coincide produce one dose, and sorted ascending.

*Why a local-date window rather than "next N doses":* the window makes refresh idempotent (see D3) and is the natural unit for DST and time zone reasoning: every date is enumerated in the device zone and each moment is derived independently, so a zone change simply regenerates the window.

*Alternative considered:* computing only the next dose on demand, never storing planned doses. Rejected: the Home screen needs a list, the missed rule needs the next dose, and a stored record is what the proposal asks for.

### D3. Materialisation: a rolling two-day window, refreshed idempotently

`RefreshPlannedDoses(medicationRepository, doseRepository, generator, clock)`:
1. `today = LocalDate.now(clock)`, `window = today..today.plusDays(1)`.
2. For every medication, `plan(medication, window, zone)`.
3. Insert every planned dose not already present (`(medication_id, scheduled_at)` is a unique index; inserts use `IGNORE`).
4. Delete doses in the window that are pending, have `firstRemindedAt == null`, and are not in the planned set (schedule edited, medication deactivated, zone changed).
5. Never touch doses with an intake or with `firstRemindedAt != null`.

Two days is enough for the Home screen ("today and tomorrow"), for the missed rule (which needs the next dose of the same medicine, always within 24 hours for daily rhythms, and otherwise falls back to the 24-hour cap), and keeps the table small. The refresh runs on every wake (D5), on app start, whenever `MedicationRepository.observeAll()` emits (collected by an application-scoped coroutine in `ReminderCoordinator`), and at the daily refresh moment 00:05 local, which is one of the candidates for the next alarm.

*Why not generate further ahead:* nothing reads it, and a longer horizon means more rows to reconcile on every edit.

### D4. Missed rule and snooze bound

`MarkMissedDoses(doseRepository, clock)` marks a pending dose `MISSED` at `lapseAt = min(scheduledAt + 24h, nextScheduledAtOfSameMedication)`, where the next dose is the earliest dose with the same `medicationId` and a later `scheduledAt` present in the table. Marking cancels its notification and clears `snoozedUntil`.

`SnoozeDose` sets `snoozedUntil = min(now + 15 min, lapseAt)`. If the bound is hit, the dose lapses at that moment without a further reminder. This is the "short, bounded period" the glossary asks for: a snooze can never carry a dose past the point where it is superseded by the next one.

*Why 24 hours as the cap:* for every-other-day or weekly medicines the next dose is days away; asking about Tuesday's dose on Thursday is noise, and adherence would otherwise count a dose as pending for a week.

### D5. One exact alarm for the next wake

`ComputeNextWake(doseRepository, clock)` returns the minimum of:
- `scheduledAt` of every pending dose with `firstRemindedAt == null` and `scheduledAt > now`,
- `snoozedUntil` of every snoozed pending dose,
- `lapseAt` of every pending dose,
- the next daily refresh moment (00:05 local),

or `null` when there are no doses at all (then only the daily refresh is set if any active medication exists).

`ReminderAlarmScheduler` (data layer) sets exactly one alarm with a fixed `PendingIntent` request code, so setting a new one replaces the previous. It uses `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` when `canScheduleExactAlarms()` is true and `setWindow(RTC_WAKEUP, at, 10 min)` otherwise, and records which mode was used so the Home banner can say so.

On fire, `ReminderAlarmReceiver` calls `goAsync()` and hands off to `ReminderCoordinator.onWake(reason)`, which runs, in order and inside a 9-second timeout:
1. `MarkMissedDoses` (cancel their notifications).
2. `RefreshPlannedDoses`.
3. For every pending dose that is due (`scheduledAt <= now` and `firstRemindedAt == null`, or `snoozedUntil <= now`): post or re-post its notification, set `firstRemindedAt` if null, clear `snoozedUntil`.
4. `ComputeNextWake` and reschedule.

Step 4 also runs in a `finally` so a failure in 1 to 3 never leaves the app without a next alarm.

*Why one alarm rather than one per dose:* Android throttles `allowWhileIdle` alarms to one per nine minutes per app in Doze, and a per-dose scheme needs cancellation bookkeeping on every edit. One alarm is trivially correct: whatever changed, recompute the minimum and set it. The cost is that two doses due a minute apart are both handled on the first fire's follow-up alarm, which is exactly the desired behaviour.

*Why `setExactAndAllowWhileIdle` rather than `setAlarmClock`:* `setAlarmClock` is for user-visible alarm clocks and shows an alarm icon in the status bar permanently; the exact-and-idle variant delivers within the same second in practice and does not misrepresent the app.

### D6. Exact-alarm permission

Manifest declares `USE_EXACT_ALARM` (Android 13+, granted automatically, reserved for alarm and reminder apps, which Pillsner is) and `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"` (Android 12, granted by default there). `canScheduleExactAlarms()` is still checked on every schedule call and the app listens for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` to reschedule when the state flips. If exact alarms are unavailable, reminders are scheduled with a ten-minute window and the Home banner says reminders may be up to ten minutes late, with a button to the system setting. This is a degraded fallback, not a design choice, and the spec says so.

### D7. Notification design

Channel `reminders`: `IMPORTANCE_HIGH`, default sound and vibration, lights on, `setShowBadge(true)`. Created at app start.

Per dose, notification id = `dose.id.value.toInt()`. The design follows design system section 8.8, which defines the reminder notification:
- `setCategory(CATEGORY_REMINDER)`, `setPriority(PRIORITY_HIGH)`, `setOngoing(false)`, `setAutoCancel(false)`, `setOnlyAlertOnce(false)` so a re-post after snooze alerts again.
- Small icon: the monochrome capsule mark (`ic_notification_pillsner`, a white-on-transparent variant of the placeholder logo). Accent colour: `setColor` with Pillsner Green, read from the theme's `Color.kt` constant, the one place a hex value lives.
- Title: the snapshot medicine name. Text: `reminder_text` = "Take %1$s of your medicine '%2$s', on %3$s" with the amount from `QuantityFormatter`, the snapshot name and the scheduled time formatted for the device locale and zone; when the scheduled date is not today the time is preceded by a short relative day using the welcome screen's `UpcomingDoseTimeFormatter`. `BigTextStyle` so nothing truncates at large font.
- Actions in order, fixed so muscle memory works: `I took it` → `ACTION_TAKEN`; `Not yet` → `ACTION_SNOOZE`; `Not going to` → `ACTION_SKIP`. Each is a `PendingIntent.getBroadcast` to `ReminderActionReceiver` with the dose id as an extra and a request code derived from dose id and action so they never collide.
- `setDeleteIntent` → `ACTION_SNOOZE`. Swiping the notification away is "Not yet". Rationale: the request says only "Not going to" ends reminding, and a reminder that can be swiped into oblivion breaks the product promise. The spec states this explicitly so it can be revisited.
- Content intent opens `MainActivity` on Home.
- `setVisibility(VISIBILITY_PRIVATE)` with a `publicVersion` carrying the title "Time for your medicine", no name or amount, and the same three actions. The system's lock-screen setting decides which one shows.
- When more than one dose is due, each dose keeps its own notification and a group summary "N medicines due" is posted with `setGroup("reminders")`.

`ReminderActionReceiver` runs `RecordIntake(TAKEN | SKIPPED)` or `SnoozeDose`, cancels the notification (or lets the snooze re-post later), then `ReminderCoordinator.onWake` to recompute the next alarm. All inside `goAsync()`.

### D8. Wearable

Wear OS mirrors phone notifications, including their actions, when the notification is not marked local-only. The design relies on this and adds `NotificationCompat.WearableExtender` only to set `setContentAction` to "I took it" so the primary action is one tap on the watch. No Wear module, no Play services, no data-layer sync. Third-party wearables that mirror notifications get the text; whether they offer the actions depends on their bridge, and the spec limits the guarantee to Wear OS.

*Alternative considered:* a Wear OS app with a data-layer channel. It needs a second module, Play services on both ends and its own reliability story, for a benefit the bridge already provides. Not justified.

### D9. Persistence: schema version 2

```
doses(id INTEGER PK AUTOINCREMENT,
      medication_id INTEGER NULL REFERENCES medications(id) ON DELETE SET NULL,
      medication_name TEXT NOT NULL, amount_value TEXT NOT NULL, amount_unit TEXT NOT NULL,
      scheduled_at INTEGER NOT NULL,          -- epoch millis
      outcome TEXT NULL,                      -- TAKEN | SKIPPED | MISSED
      recorded_at INTEGER NULL,
      snoozed_until INTEGER NULL,
      first_reminded_at INTEGER NULL)
UNIQUE INDEX doses_medication_scheduled ON doses(medication_id, scheduled_at)
INDEX doses_scheduled_at ON doses(scheduled_at)
```

`MIGRATION_1_2` creates the table and indexes. `PillsnerDatabase` goes to `version = 2`, `2.json` is exported, and `PillsnerDatabaseMigrationTest` gains `migrate1To2` using `MigrationTestHelper` on the exported version 1 schema with a seeded medication row, asserting the table exists and the old rows survive.

*Why `ON DELETE SET NULL` rather than cascade:* dose history is the user's record of what they took; deleting a medicine must not erase it. The name and amount snapshots make the row self-describing.

`DoseDao`: `observePending()`, `observeUpcoming(fromInclusive, limit)`, `getDue(now)`, `insertIgnore(list)`, `deletePlannedNotIn(window, keepIds)`, `setIntake(id, outcome, at)`, `setSnooze(id, until)`, `setFirstReminded(id, at)`, `nextScheduledAtAfter(medicationId, after)`. `RoomDoseRepository` implements the domain `DoseRepository`; `RoomUpcomingDosesRepository` implements the welcome screen's `UpcomingDosesRepository` as "pending doses ordered by `scheduled_at`, limited", which includes overdue pending doses (see D11).

### D10. Receivers, wiring and process model

Manifest receivers (all `exported="false"` except the system-broadcast ones which are protected by their actions):
- `ReminderAlarmReceiver`: the single alarm.
- `ReminderActionReceiver`: the three actions and the delete intent.
- `SystemEventsReceiver`: `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`. Each calls `onWake(reason)`; for `TIMEZONE_CHANGED` and `TIME_SET` the refresh step's regeneration of un-reminded planned doses is what re-anchors everything to the new wall clock.

`ReminderCoordinator` lives in `AppContainer` and holds a `SupervisorJob` scope on `Dispatchers.IO`. Receivers obtain it through `(context.applicationContext as PillsnerApplication).container`. Because a receiver may be the first thing that starts the process, `AppContainer` construction must stay cheap and side-effect free apart from creating the Room instance lazily.

The domain gets a `Clock` (`java.time.Clock`) injected into every use case; production uses `Clock.systemDefaultZone()`, tests a mutable fixed clock.

### D11. Home screen changes

`UpcomingDose.amount: String` becomes `Quantity`, formatted by the existing `QuantityFormatter` in `DoseTile`. `UpcomingDose` also gains the information the tile needs to pick its `IntakeStatus` (design system section 2.3): `isOverdue` (scheduled moment passed, unanswered) and `snoozedUntil`. The tile maps a pending dose to `Due`, a pending dose past its time to `Overdue` (`errorContainer`, `error` icon, one of the allowed uses of red), and a snoozed dose to `Snoozed` (`tertiaryContainer`, `snooze` icon); stripe, icon, chip and spoken description all change together. "Upcoming" is now defined as pending doses ordered by time, including those already due and awaiting an answer, because "what do I need to take" includes the dose the user has not answered yet; a missed or skipped dose is not upcoming.

`HomeScreen` gains a `ReminderBanner`, the attention banner from design system section 8.9: `errorContainer` / `onErrorContainer`, `shapes.large`, the `notifications_off` icon, one `bodyLarge` sentence explaining the problem, and one `TextButton` in `onErrorContainer` that opens the relevant system settings page. It sits at the top of Home above the dose list and is the only red surface that appears without user action; it is shown when notification permission is denied or exact alarms are unavailable. The `POST_NOTIFICATIONS` request is launched from Home the first time the medication list contains an active medication with at least one schedule and the permission is not granted; a `DataStore` flag prevents re-asking, after which the banner takes over.

### D12. Logging

Receivers and the coordinator log at debug level only, with dose ids and outcomes, never names or amounts. Release builds strip debug logs through the existing lint or ProGuard configuration; a lint check for `Log.i`/`Log.w`/`Log.e` calls with string templates in the reminders package is added as a task.

### D13. Package layout

```
domain/model/Dose.kt                    Dose, Intake, IntakeOutcome, PlannedDose
domain/model/UpcomingDose.kt            amount: Quantity
domain/repository/DoseRepository.kt
domain/scheduling/DoseGenerator.kt
domain/scheduling/RefreshPlannedDoses.kt, MarkMissedDoses.kt, ComputeNextWake.kt, DueDoses.kt
domain/intake/RecordIntake.kt, SnoozeDose.kt
data/db/DoseEntity.kt, DoseDao.kt, Migrations.kt, PillsnerDatabase.kt (v2)
data/RoomDoseRepository.kt, RoomUpcomingDosesRepository.kt
data/reminders/ReminderCoordinator.kt, ReminderAlarmScheduler.kt, ReminderNotifier.kt, ReminderChannels.kt
data/reminders/ReminderAlarmReceiver.kt, ReminderActionReceiver.kt, SystemEventsReceiver.kt
ui/home/ReminderBanner.kt, NotificationPermissionRequest.kt
res/drawable/ic_notification_pillsner.xml   monochrome capsule mark for the status bar
app/schemas/…/2.json
```

## Risks / Trade-offs

- [Google Play may reject `USE_EXACT_ALARM` for a medication app] → Pillsner's core function is reminding at exact times, which the policy names. The README states the permission and purpose. If rejected, the fallback is `SCHEDULE_EXACT_ALARM` on all versions with an in-app prompt; the code path already exists (D6).
- [Manufacturer battery optimisers kill alarms or receivers] → Out of the app's control. The Home banner can later gain a "battery settings" hint; documented as a manual test on at least one aggressive OEM device.
- [Swipe-to-dismiss counts as snooze; some users will find it pushy] → Bounded by the lapse rule (at most until the next dose or 24 hours). Called out in the spec so it can be changed by a one-line proposal.
- [Receiver work exceeds the 10-second budget on a slow device with a large table] → Work is a handful of indexed queries on a two-day window; timeout at 9 seconds with the alarm rescheduled in `finally`. If it ever grows, the coordinator moves into an expedited `WorkManager` job triggered by the receiver, without changing the alarm.
- [Clock set backwards re-fires reminders for already reminded doses] → Due-check requires `firstRemindedAt == null` or an elapsed snooze, so a reminded dose is not re-posted; the next alarm is simply recomputed.
- [Two schedules of one medicine coinciding produce one dose with one amount] → De-duplication keeps the first schedule's amount and the case is flagged in KDoc; the schedule editor already discourages it. Revisit if a user hits it.
- [`ON DELETE SET NULL` leaves orphan doses] → Intended: they are history. The unique index on `(medication_id, scheduled_at)` tolerates nulls because SQLite treats each null as distinct.
- [Notification actions on a locked device] → Android may require unlock for actions depending on the user's lock-screen settings; that is the platform's privacy decision and the public version keeps the name hidden. Documented in the manual test.

## Migration Plan

Schema 1 to 2 adds one table and two indexes; no data transformation. On first launch after the update the coordinator runs `onWake(APP_UPDATED)` from `MY_PACKAGE_REPLACED`, which generates the two-day window for existing medicines and sets the alarm, so a user who added medicines under version 1 gets reminders without opening the app. Rollback is a new version that drops the table, which would lose intake history; there is no user-visible reason to roll back other than a defect, and defects are fixed forward.

## Open Questions

- Should swipe-to-dismiss remain "Not yet", or become "no answer" with no re-reminder? Recommended: keep as designed and watch feedback.
- Should the lock-screen public version hide the name by default, or should that be a user setting under Security in `app-login`? Recommended: a setting later; platform default now.
- Should the 24-hour lapse cap be shorter for every-N-hours medicines (for example half the interval)? The next-dose rule already handles it whenever the next dose is within 24 hours.
- Should the app request exemption from battery optimisation (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) on devices known to drop alarms? Play policy limits this; revisit after field experience.
