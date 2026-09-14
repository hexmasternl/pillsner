## Context

`app-welcome-screen` provides the shell (single activity, `PillsnerApp`, type-safe routes, `AppContainer`). `app-medicine-overview` provides the Medicines screen with its + button, the `AddMedication` placeholder route, and the first domain types: `Medication`, `Schedule`, `ScheduleSummary`, `MedicationRepository` with an in-memory implementation. Nothing is persisted yet.

This change is the first one that writes data, so it owns three things at once: the add-medicine form, the schedule editor, and the Room database. It also revises the domain model. The overview design chose one schedule per medication and argued the case; the product owner has since asked for multiple schedules per medicine, each with its own amount, and for a flexible "x times per N days" pattern. The revision is done here, before anything is persisted, so no migration is needed for it.

Constraints from `CLAUDE.md`: domain layer without Android imports, Room with exported schema and a migration test for every version, no destructive migration fallback in release, strings in resources, Material 3 accessibility, single DI mechanism, glossary terms in code, never log medication names or dosages at info level or above, every visual decision from `docs/design-system.md` (forms per section 8.11, dialogs per 8.12, buttons per 8.4) through the `pillsner-ui-build` and `pillsner-ui-review` skills. The project scaffold and version catalog come from `app-welcome-screen`; this change only adds its own libraries to the catalog.

## Goals / Non-Goals

**Goals:**
- A form that captures name, default dose, used since, use until, prescriber and zero or more schedules, validates, and saves.
- A schedule editor that expresses "x times per N days", "specific weekdays" and "every N hours from a first dose", each with an amount, with a live preview of the resulting description.
- A revised domain model that the dose generator can consume later without another reshaping: amount per schedule, anchor date, first-dose time.
- Room persistence with a version 1 schema, exported, with the migration harness in place.
- Saved medicines appear on the overview immediately.

**Non-Goals:**
- Editing, deleting, activating or deactivating medicines.
- Dose generation, reminders, alarms.
- Strength, notes, stock, form as separate fields.
- Cloud backup decisions beyond noting the default.

## Decisions

### D1. Revised domain model

```kotlin
enum class DoseUnit { MILLIGRAM, GRAM, MICROGRAM, MILLILITRE, TABLET, CAPSULE, DROP, PUFF, UNIT }

/** A positive amount of medication in a unit. value is stored in the smallest sensible precision: 3 decimals. */
data class Quantity(val value: BigDecimal, val unit: DoseUnit) { init { require(value > BigDecimal.ZERO) } }

enum class Prescriber { GENERAL_PRACTITIONER, SPECIALIST, PHARMACIST, SELF, OTHER }

data class Medication(
    val id: MedicationId,
    val name: String,
    val defaultDose: Quantity,
    val usedSince: LocalDate,
    val useUntil: LocalDate?,          // null = open-ended
    val prescribedBy: Prescriber,
    val schedules: List<Schedule>,     // empty = as needed
    val isActive: Boolean,
) { init { require(name.isNotBlank()); require(useUntil == null || !useUntil.isBefore(usedSince)) } }

sealed interface Schedule {
    val amount: Quantity
    /** [times] on each day that is a whole multiple of [intervalDays] after the medication's usedSince. intervalDays >= 1. */
    data class EveryNDays(override val amount: Quantity, val intervalDays: Int, val times: List<LocalTime>) : Schedule
    /** [times] on each of [days]. */
    data class OnWeekdays(override val amount: Quantity, val days: Set<DayOfWeek>, val times: List<LocalTime>) : Schedule
    /** One dose at [firstDoseAt] and then every [intervalHours] hours, restarting each day. 1 <= intervalHours <= 24, 24 % intervalHours == 0 recommended. */
    data class EveryNHours(override val amount: Quantity, val intervalHours: Int, val firstDoseAt: LocalTime) : Schedule
}
```

*Changes from the overview model and why:*
- **List of schedules** on `Medication`, requested by the product owner. The overview's argument (dose differences belong to times, not schedules) does not cover "40 mg every 12 hours on weekdays, 20 mg once a day at weekends", which needs two rules with two amounts. Ordering of `schedules` is the order the user added them.
- **`AsNeeded` removed** from `Schedule`. With a list, "no schedules" already means as needed, and keeping both would allow a medication with `[AsNeeded, EveryNDays]`, which is meaningless. `ScheduleSummary.AsNeeded` stays as the summary of an empty list.
- **`FixedTimes` renamed `OnWeekdays`**, and **`EveryNDays` gains `times: List<LocalTime>`**. This gives the requested "x times per N days" directly: twice a day is `EveryNDays(1, [08:00, 20:00])`. `OnWeekdays` with all seven days is equivalent to `EveryNDays(1, ...)`; construction of `OnWeekdays` requires a strict subset of days so the two never overlap and the editor never has to pick between two representations of "daily".
- **`EveryNHours` gains `firstDoseAt`**, so the generator has an anchor and the preview can show actual clock times. Restarting each day (24 must be divisible by the interval for evenly spaced doses) keeps the rule a wall-clock rule that survives DST like the others; the editor restricts choices to 1, 2, 3, 4, 6, 8, 12, 24.
- **Amount on every schedule**, prefilled from `defaultDose` in the editor. `BigDecimal` rather than `Double` so "0.5 tablet" and "2.5 ml" round-trip exactly through the database and never display as 2.4999.
- `usedSince` is the anchor for `EveryNDays` counting, which is why it is a required field with a default of today.
- Validation is in `init` blocks (`require`), so an invalid schedule cannot exist; the editor and form perform the same checks earlier to show messages.

*Alternatives considered:* a single generic `Schedule(intervalDays, days, times, intervalHours)` with nullables. Rejected for the same reason as before: invalid combinations become representable. A separate `AsNeeded` medication type. Rejected: it would split every consumer.

### D2. Revised summary and description

```kotlin
sealed interface ScheduleSummary {
    data class TimesPerDay(val count: Int) : ScheduleSummary
    data class TimesPerDayOnDays(val count: Int, val days: Set<DayOfWeek>) : ScheduleSummary
    data class TimesEveryOtherDay(val count: Int) : ScheduleSummary
    data class TimesEveryNDays(val count: Int, val days: Int) : ScheduleSummary   // days >= 3
    data class EveryNHours(val hours: Int) : ScheduleSummary
    data object AsNeeded : ScheduleSummary
}

fun Schedule.summarize(): ScheduleSummary
fun Medication.summarizeSchedules(): List<ScheduleSummary>   // [AsNeeded] when schedules is empty
```

Rules: `EveryNDays(1, t)` → `TimesPerDay(distinct t)`; `EveryNDays(2, t)` → `TimesEveryOtherDay(distinct t)`; `EveryNDays(n>=3, t)` → `TimesEveryNDays(distinct t, n)`; `OnWeekdays(d, t)` → `TimesPerDayOnDays(distinct t, d)`; `EveryNHours(24, _)` → `TimesPerDay(1)`; `EveryNHours(h, _)` → `EveryNHours(h)`.

`ScheduleDescriptionFormatter.describe(summary, amount: Quantity?)` produces "40 mg twice a day", "1 tablet once every other day", "20 mg 3 times every 3 days", "5 ml every 8 hours", "40 mg as needed". The amount is formatted by a `QuantityFormatter` using the locale's decimal format with up to three fraction digits and a unit string resource with plural support ("1 tablet", "2 tablets", "40 mg"). Amount precedes the frequency because that reads naturally in English and matches how prescriptions are written; translators can reorder via the format string.

The overview tile shows `medication.summarizeSchedules()` mapped one line per schedule, paired with each schedule's amount; the as-needed line uses `defaultDose`.

### D3. Persistence with Room

Two tables, one-to-many:

```
medications(id PK autoincrement, name TEXT, default_dose_value TEXT, default_dose_unit TEXT,
            used_since TEXT (ISO date), use_until TEXT NULL, prescribed_by TEXT, is_active INTEGER)
schedules(id PK autoincrement, medication_id FK → medications.id ON DELETE CASCADE, position INTEGER,
          kind TEXT ('EVERY_N_DAYS' | 'ON_WEEKDAYS' | 'EVERY_N_HOURS'),
          amount_value TEXT, amount_unit TEXT,
          interval_days INTEGER NULL, interval_hours INTEGER NULL,
          days TEXT NULL (comma-separated DayOfWeek names), times TEXT NULL (comma-separated HH:mm),
          first_dose_at TEXT NULL)
index on schedules(medication_id)
```

*Why a flat schedule row with nullable columns rather than one table per shape:* one join, one DAO query with `@Relation`, and the sealed-interface mapping lives in a single `ScheduleEntity.toDomain()` with a `when(kind)`. Nullable columns are acceptable in the storage layer because the mapper enforces the invariants on the way out and throws on a corrupt row rather than producing a half-valid schedule. `BigDecimal` is stored as its plain string to keep exactness; `LocalDate`, `LocalTime` and enums as ISO/name strings via `TypeConverter`s, which keeps the schema readable in migration tests.

`MedicationDao`: `@Transaction @Query observeAllWithSchedules(): Flow<List<MedicationWithSchedules>>`, `@Insert insertMedication`, `@Insert insertSchedules`, and a `@Transaction suspend fun insert(medication, schedules)` that assigns the medication id to each schedule row.

`PillsnerDatabase` at `version = 1`, `exportSchema = true`, schema JSON checked in under `src/app/schemas/`. `fallbackToDestructiveMigration` is not called; a debug-only `fallbackToDestructiveMigrationOnDowngrade` is acceptable per `CLAUDE.md`. `room-testing`'s `MigrationTestHelper` is set up with a `PillsnerDatabaseMigrationTest` that creates version 1 and validates it, so version 2 only has to add its migration and one more test.

Repository contract gains:

```kotlin
interface MedicationRepository {
    fun observeAll(): Flow<List<Medication>>
    suspend fun add(medication: NewMedication): MedicationId
}
data class NewMedication(name, defaultDose, usedSince, useUntil, prescribedBy, schedules)
```

`NewMedication` avoids a fake id on an unsaved medication. `RoomMedicationRepository` replaces `InMemoryMedicationRepository` in `AppContainer`; the in-memory one stays for tests and previews.

*Alternative considered:* serialising `schedules` as JSON into a single column on `medications`. Simpler schema, but every future query on schedules (the dose generator wants "all schedules of active medications") would deserialise everything, and migration tests become string manipulation. Rejected.

### D4. Navigation: a nested graph with a shared draft

```kotlin
@Serializable data object AddMedicationGraph
@Serializable data object AddMedicationForm
@Serializable data class EditSchedule(val index: Int? = null)   // null = new schedule
```

`PillsnerApp` replaces `composable<AddMedication>` with `navigation<AddMedicationGraph>(startDestination = AddMedicationForm) { composable<AddMedicationForm>; composable<EditSchedule> }`. The overview FAB navigates to `AddMedicationGraph`. Both destinations obtain `AddMedicationViewModel` scoped to the graph's back-stack entry (`navController.getBackStackEntry<AddMedicationGraph>()`), so the draft, including its schedule list, survives navigating to the editor and back, rotation and process death (via `SavedStateHandle`).

*Why a shared, graph-scoped view model rather than a navigation result:* the medicine is not saved until the form's Save, so schedules must live in the draft, not in the database. Passing a `Schedule` back through a nav result means serialising it into a bundle and reconciling edits with indices anyway. One view model owning the draft is the simplest correct model, and it is the officially documented pattern for multi-step flows.

Bottom bar visibility already hides on non-top-level routes (welcome D3), so both screens have no bottom bar. Back from the editor pops to the form without committing; back from the form with a non-empty draft asks to discard (see D7).

### D5. Form screen

`AddMedicationScreen` is a `Scaffold` with a `TopAppBar` per design system 8.7 (title "Add medicine" in `titleLarge`, back arrow, `surface` container shifting to `surfaceContainer` on scroll, no actions) and a vertically scrolling `Column` with `Spacing.screenEdge` side padding and `Spacing.lg` between fields:

1. **Name**: `OutlinedTextField` per 8.11 (`bodyLarge` value, `bodyMedium` label, `bodySmall` supporting text), single line, capitalised words, required.
2. **Default dose**: a `QuantityField` composable: numeric `OutlinedTextField` (decimal keyboard, locale-aware parsing) plus an `ExposedDropdownMenuBox` for the unit, as 8.11 prescribes for quantity fields. Reused by the schedule editor.
3. **Used since**: read-only `OutlinedTextField` that opens a Material 3 `DatePickerDialog`; default today.
4. **Use until**: same, optional, with a clear affordance; picker disables dates before "used since".
5. **Prescribed by**: `ExposedDropdownMenuBox` with the five options; default General practitioner.
6. **Schedules** section header in `headlineSmall`, then one `ScheduleRow` per draft schedule (a `ListItem`: description in `bodyLarge` via the formatter, tap to edit, trailing remove icon button of `Sizes.minTouchTarget` with content description "Remove schedule"), then a `FilledTonalButton` "Add schedule" (second-rank action per 8.4). When empty, a `bodyMedium` `onSurfaceVariant` line "No schedules yet. This medicine will be listed as taken when needed."

**Save** is the screen's one positive action, so per 8.4 and 8.11 it is a filled `Button` of at least `Sizes.minTouchTarget` height, full width, pinned to the bottom of the form above the keyboard (in the `Scaffold` bottom bar, with `imePadding`), not a text button in the app bar. It is enabled only when the draft is valid; invalid fields show `supportingText` errors in the `error` role (a validation error, allowed by 2.4) that say how to fix the problem, on blur or on a Save attempt. On Save, the view model calls `repository.add`, then emits a one-shot `Saved` effect; `PillsnerApp` pops back to `Medicines`. The overview updates through its `Flow`. The form column is capped at `Spacing.contentMaxWidth` on wide screens.

`AddMedicationUiState` holds raw field strings plus parsed values and per-field error resource ids, so the text fields never fight the user's typing (for example "2." while typing "2.5").

### D6. Schedule editor screen

`ScheduleEditorScreen` is a `Scaffold` with a `TopAppBar` per 8.7 (title "Add schedule" or "Edit schedule" in `titleLarge`, back arrow, no actions) and a scrolling `Column` with the same spacing as the form:

1. **Amount**: `QuantityField`, prefilled with the medication's default dose (or the schedule's amount when editing).
2. **Pattern**: `SingleChoiceSegmentedButtonRow` with three options: "Every N days", "Weekdays", "Every N hours". Changing the pattern keeps the amount and any times already entered where they still apply.
3. Pattern inputs:
   - *Every N days*: an interval stepper (1 to 30, label reads "Every day" for 1, "Every other day" for 2, "Every N days" otherwise) with `Sizes.minTouchTarget` buttons, and a **times list**: one `ListItem` row per `LocalTime` (time in `titleLarge` with tabular figures) with a `TimePickerDialog` on tap and a remove icon button, plus a `FilledTonalButton` "Add time". At least one time; duplicates are rejected with a message.
   - *Weekdays*: seven `FilterChip`s in a wrapping row in the locale's week order, selected chips in `secondaryContainer` (8.11), plus the same times list. At least one day; selecting all seven shows a hint to use "Every N days" and is rejected on Done so the model invariant holds.
   - *Every N hours*: an interval `ExposedDropdownMenuBox` limited to 1, 2, 3, 4, 6, 8, 12, 24, and a first-dose time picker (`TimePickerDialog`, never free text). Below it, the computed dose times for a day ("08:00, 20:00") as read-only `bodyMedium` text.
4. **Preview**: a `Card` in `shapes.large` on `tileContainerColor()` showing the description the overview will display ("40 mg every 12 hours") in `bodyLarge`, updated on every change; shows the first validation error in the `error` role with the `error` icon instead when the draft is invalid.

**Done** is the editor's one positive action: a filled `Button`, full width, pinned to the bottom above the keyboard, like Save on the form. It validates, builds the `Schedule`, and either appends to or replaces at `index` in the draft, then pops back. Back discards editor edits without prompting because the editor is small and the form still holds the previous version.

### D7. Discarding a draft

Pressing back or the back arrow on the form with any field changed shows an `AlertDialog` per 8.12: title "Discard this medicine?" in `headlineMedium`, one `bodyLarge` line, and exactly two `TextButton` actions, "Discard" and "Keep editing". Discarding an unsaved draft is not one of the destructive cases in section 2.4, so neither action uses the `error` role. An untouched form pops immediately. The check compares the draft with its initial value.

### D8. View models and state

`AddMedicationViewModel(repository, savedStateHandle)` owns `AddMedicationDraft` (all form fields and `List<Schedule>`), exposes `StateFlow<AddMedicationUiState>` and `StateFlow<ScheduleEditorUiState>` (the editor state for the schedule at the current `index`, or a new one), and handles events: field edits, `openSchedule(index?)`, editor field edits, `commitSchedule()`, `removeSchedule(index)`, `save()`. One view model for both screens because they edit one draft. Validation logic lives in pure Kotlin `AddMedicationValidator` and `ScheduleDraftValidator` in the domain layer so it is unit-tested without Android.

### D9. Dependencies and build

Version catalog additions, at the newest stable versions on 11 September 2026 (re-checked at apply time like every row of the `app-welcome-screen` baseline): `androidx.room` 2.8.5 (`room-runtime`, `room-ktx`, `room-compiler`, `room-testing`, `room-gradle-plugin`; released 9 September 2026; Room 3.0 is still pre-release and is not used), and the KSP Gradle plugin `com.google.devtools.ksp` 2.3.12 (KSP is Kotlin-version independent since 2.3.0, so it does not need to match Kotlin 2.4.20 exactly, only to be 2.3.0 or newer). The Room Gradle plugin provides `room { schemaDirectory("$projectDir/schemas") }` in the app module. `androidx.compose.material3` date and time pickers come from the existing BOM. KSP is Google-maintained and is the only supported processor for Room with Kotlin; this is the justification `CLAUDE.md` asks for.

### D10. Logging and privacy

No log statements include the draft's name or quantities. Repository errors are surfaced to the user as a generic "Could not save" snackbar and logged at debug level with the exception type only.

### D11. Package layout

```
domain/model/Quantity.kt              Quantity, DoseUnit
domain/model/Prescriber.kt
domain/model/Medication.kt            revised, + NewMedication
domain/model/Schedule.kt              revised shapes
domain/model/ScheduleSummary.kt       revised summary + summarizeSchedules()
domain/validation/AddMedicationValidator.kt, ScheduleDraftValidator.kt
domain/repository/MedicationRepository.kt   + add()
data/db/PillsnerDatabase.kt, Converters.kt
data/db/MedicationEntity.kt, ScheduleEntity.kt, MedicationWithSchedules.kt
data/db/MedicationDao.kt
data/RoomMedicationRepository.kt
data/InMemoryMedicationRepository.kt  kept for tests/previews, gains add()
ui/medicines/ScheduleDescriptionFormatter.kt  revised, + QuantityFormatter
ui/medicines/add/AddMedicationScreen.kt, AddMedicationViewModel.kt, AddMedicationUiState.kt, QuantityField.kt, DateField.kt, ScheduleRow.kt, DiscardDialog.kt
ui/medicines/schedule/ScheduleEditorScreen.kt, ScheduleEditorUiState.kt, TimesList.kt, WeekdayChips.kt
ui/navigation/Routes.kt               AddMedication replaced by AddMedicationGraph, AddMedicationForm, EditSchedule
app/schemas/…/1.json
```

## Risks / Trade-offs

- [Reversing the one-schedule decision after the overview was designed] → Done before persistence exists, so no data migration. The overview's spec and formatter are modified in this change rather than left out of sync; the tasks list touches `MedicinesScreen` and its tests explicitly.
- [Three unarchived changes with chained MODIFIED deltas] → Archive order is `app-welcome-screen`, `app-medicine-overview`, then this one. If it cannot be honoured, MODIFIED blocks are rewritten as ADDED with distinct names before archiving.
- [`EveryNHours` restarting daily cannot express "every 36 hours"] → Out of scope; every-N-days plus times covers most multi-day rhythms, and 36-hour regimens are rare. Revisit if asked.
- [`OnWeekdays` must be a strict subset, which the editor has to explain] → The editor shows a hint and rejects all-seven on Done. The alternative (allow all seven and collapse) creates two representations of daily and complicates equality and editing.
- [Nullable columns in `schedules` allow corrupt rows] → The mapper validates and throws a descriptive `IllegalStateException`; DAO tests cover each shape's round trip.
- [Graph-scoped view model with `SavedStateHandle` must serialise `BigDecimal`, `LocalTime`, `LocalDate`] → Stored as strings in the handle through a small `DraftSaver`; covered by a unit test that saves and restores a draft.
- [Room adds KSP and lengthens the build] → Mandated by `CLAUDE.md`; unavoidable for persistence.
- [Date and time picker dialogs at large font on small screens] → Material 3 pickers handle scaling; the manual test case covers 200 percent font scale for both screens.
- [A pinned Save button hides the last field behind the keyboard] → The form column pads its bottom by the button height plus `Spacing.lg` and uses `imePadding`, so the focused field scrolls into view; covered by the Compose test that fills every field.
- [Default backup behaviour would include the database in Android auto backup] → Medication data is health data, and `app-login` already excludes lock settings from backup. Left as an open question because the decision affects data loss on device change and belongs to the product owner.

## Migration Plan

This change introduces schema version 1. No prior data exists, so there is no migration; the version-1 schema is exported and the migration harness test validates it. Rollback is removing the `data/db` package, the Room dependencies and restoring the in-memory wiring; any database file left on a debug device is harmless and is deleted on uninstall.

## Open Questions

- Should the database be excluded from Android auto backup and device-to-device transfer, like the lock settings in `app-login`? Trade-off: privacy versus losing all medicines when switching phones. Recommended: exclude from cloud backup, allow device-to-device transfer, in the change that adds backup rules.
- Should a passed "use until" date automatically make a medicine inactive on the overview? Recommended: yes, decided in the scheduling change, which owns the notion of "today".
- Should "Other" for prescriber allow a free-text name? Not until asked.
- Should the form offer quick presets ("Once a day at 08:00", "Twice a day") to skip the editor for common cases? Worth considering after the first usability pass.
