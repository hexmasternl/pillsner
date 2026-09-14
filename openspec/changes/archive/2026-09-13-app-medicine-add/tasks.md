## 1. Preconditions and build setup

- [x] 1.1 Verify `app-welcome-screen` and `app-medicine-overview` are applied: `PillsnerApp`, `Medicines` route, `AddMedication` placeholder route, `Medication`, `Schedule`, `ScheduleSummary`, `MedicationRepository`, `InMemoryMedicationRepository`, `AppContainer` all exist; stop and report if not
- [x] 1.2 Add to the version catalog, after re-checking the release pages for anything newer and stable: Room 2.8.5 (runtime, ktx, compiler, testing, Gradle plugin) and the KSP Gradle plugin 2.3.12; record the versions actually used in design D9 if they differ
- [x] 1.3 Apply the KSP and Room Gradle plugins in the app module, add Room dependencies, configure `room { schemaDirectory("$projectDir/schemas") }`, and add `room-testing` plus the schemas directory as an instrumented test asset source
- [x] 1.4 Confirm `./gradlew assembleDebug` still succeeds from `src/`

## 2. Domain model revision

- [x] 2.1 Create `domain/model/Quantity.kt` with `DoseUnit` enum and `Quantity(value: BigDecimal, unit)` requiring a positive value and equal by numeric value and unit, with KDoc
- [x] 2.2 Create `domain/model/Prescriber.kt` with the five values
- [x] 2.3 Revise `domain/model/Schedule.kt`: remove `AsNeeded`; add `amount` to every shape; rename `FixedTimes` to `OnWeekdays` requiring a strict, non-empty weekday subset and non-empty times; give `EveryNDays` a `times` list; give `EveryNHours` a `firstDoseAt` and a 1..24 interval; add a helper that lists the daily dose times for `EveryNHours`
- [x] 2.4 Revise `domain/model/Medication.kt`: add `defaultDose`, `usedSince`, `useUntil`, `prescribedBy`, `schedules: List<Schedule>`; validate non-blank name and end-not-before-start; add `NewMedication` for unsaved medicines
- [x] 2.5 Revise `domain/model/ScheduleSummary.kt`: replace `EveryOtherDay`/`EveryNDays` with `TimesEveryOtherDay(count)`/`TimesEveryNDays(count, days)`; update `summarize()` rules; add `Medication.summarizeSchedules()` returning `[AsNeeded]` for an empty list
- [x] 2.6 Add `add(NewMedication): MedicationId` to `MedicationRepository`; implement it in `InMemoryMedicationRepository` and update the debug preview repository to the new model
- [x] 2.7 Create `domain/validation/ScheduleDraftValidator.kt` and `domain/validation/AddMedicationValidator.kt` returning typed validation errors (no resources) for every rule in the specs
- [x] 2.8 Unit tests: `Quantity` (positive, equality), `Schedule` construction (each rejection), `EveryNHours` daily times, `Medication` validation, every `summarize()` rule including duplicates and 24-hour collapse, `summarizeSchedules()` for empty and multiple, both validators

## 3. Persistence

- [x] 3.1 Create `data/db/Converters.kt` for `BigDecimal`, `LocalDate`, `LocalTime`, `DayOfWeek` sets and time lists as strings
- [x] 3.2 Create `MedicationEntity`, `ScheduleEntity` (nullable pattern columns, `position`, FK with cascade, index on `medication_id`) and `MedicationWithSchedules` with `@Relation`
- [x] 3.3 Create `MedicationDao` with `observeAllWithSchedules(): Flow`, inserts, and a `@Transaction suspend fun insert(medication, schedules)` that assigns the generated id to schedule rows
- [x] 3.4 Create `PillsnerDatabase` at version 1 with `exportSchema = true`; no destructive fallback in release
- [x] 3.5 Create entity-to-domain mappers that enforce shape invariants and throw a descriptive error naming kind and row id on corrupt rows
- [x] 3.6 Create `data/RoomMedicationRepository.kt` implementing `observeAll()` and `add()`
- [x] 3.7 Build the database in `AppContainer` and wire `RoomMedicationRepository` as the app's `MedicationRepository`
- [x] 3.8 Build once to generate `schemas/…/1.json` and check it in
- [x] 3.9 Instrumented DAO tests: round trip for each schedule shape, decimal exactness, schedule order, null use-until, cascade delete, atomic insert when a schedule insert fails, flow re-emits on add
- [x] 3.10 Instrumented migration harness test with `MigrationTestHelper` validating version 1

## 4. Description formatting

- [x] 4.1 Create `ui/medicines/QuantityFormatter.kt`: locale decimal format up to three fraction digits plus unit plural string resources
- [x] 4.2 Revise `ScheduleDescriptionFormatter` to take a summary and an amount, add resources for "times every other day" and "times every N days" (once, twice, numbered), and prefix the amount via a format string
- [x] 4.3 Update `MedicineTile` and `MedicineTileState` to show one description per schedule with the schedule's amount, and "<default dose> as needed" for none; keep the merged semantics
- [x] 4.4 Update the `MedicinesViewModel` mapping and existing overview tests to the revised model
- [x] 4.5 Unit tests for `QuantityFormatter` and every formatter row in the model spec

## 5. Navigation

- [x] 5.1 Replace `AddMedication` in `Routes.kt` with `AddMedicationGraph`, `AddMedicationForm` and `EditSchedule(index: Int?)`
- [x] 5.2 In `PillsnerApp`, add `navigation<AddMedicationGraph>` with both destinations, obtain `AddMedicationViewModel` scoped to the graph entry, point the overview FAB at the graph, and pop the graph on save
- [x] 5.3 Delete `AddMedicationPlaceholderScreen` and update navigation tests (FAB opens the form, bottom bar hidden on both destinations, back behaviour, save pops the flow)

## 6. Add medicine form

- [x] 6.1 Create `AddMedicationDraft` and a `DraftSaver` that persists it through `SavedStateHandle` as strings; unit test save and restore including schedules
- [x] 6.2 Create `AddMedicationUiState` (raw field strings, parsed values, per-field error resource ids, schedule rows, canSave) and `AddMedicationViewModel` handling field events, `openSchedule`, `removeSchedule`, `save` and a one-shot `Saved` effect; log nothing that contains name or dose
- [x] 6.3 Create `QuantityField` with the `pillsner-ui-build` skill (`OutlinedTextField` per design system 8.11 plus unit `ExposedDropdownMenuBox`, locale-aware parsing) for reuse by the editor
- [x] 6.4 Create `DateField` using Material 3 `DatePickerDialog`, with optional clear affordance and a selectable-dates constraint
- [x] 6.5 Create `ScheduleRow` as a `ListItem` (`bodyLarge` description, tap to edit, `Sizes.minTouchTarget` remove icon button with content description) and the as-needed `bodyMedium` empty text
- [x] 6.6 Create `AddMedicationScreen`: `TopAppBar` per 8.7 with back arrow and `titleLarge` title, scrolling column (`Spacing.screenEdge`, `Spacing.lg` gaps, `Spacing.contentMaxWidth` cap) with all fields, prescriber dropdown, `headlineSmall` schedules header, `FilledTonalButton` "Add schedule", error `supportingText` in the `error` role, save-failure snackbar, and the filled full-width Save button pinned to the bottom above the keyboard; `@PreviewLightDark` and `fontScale = 2f` previews
- [x] 6.7 Create `DiscardDialog` as an `AlertDialog` per 8.12 (`headlineMedium` title, `bodyLarge` body, two `TextButton`s, no `error` colour) and wire back handling: pop immediately when untouched, otherwise confirm
- [x] 6.8 Add all string resources for labels, hints, errors, dialog and units
- [x] 6.9 Unit tests for `AddMedicationViewModel`: defaults, each validation error, schedule add/edit/remove ordering, save calls repository with a `NewMedication` and emits `Saved`, save failure keeps state and surfaces the error

## 7. Schedule editor

- [x] 7.1 Create `ScheduleEditorUiState` and the editor event handling inside `AddMedicationViewModel` (amount, pattern switch preserving times, interval, times list add/remove/duplicate rejection, weekday toggles, first dose time, preview or first error, `commitSchedule`)
- [x] 7.2 Create `TimesList` (sorted `ListItem` rows with `titleLarge` tabular times, `TimePickerDialog`, remove icon buttons, `FilledTonalButton` "Add time") and `WeekdayChips` (seven `FilterChip`s in a wrapping row in locale week order, selected in `secondaryContainer`, with selected-state semantics)
- [x] 7.3 Create `ScheduleEditorScreen`: `TopAppBar` with back arrow and `titleLarge` title, `QuantityField`, segmented pattern selector, per-pattern inputs including the every-N-days interval labels, the every-N-hours interval dropdown with computed daily times, the all-seven-days hint, the preview `Card` in `shapes.large` on `tileContainerColor()` (first error in the `error` role with icon), and the filled full-width Done button pinned to the bottom; `@PreviewLightDark` and `fontScale = 2f` previews
- [x] 7.4 Add string resources for the editor
- [x] 7.5 Unit tests for editor state: pattern switch keeps times, duplicate time rejected, every-N-hours daily times, preview text for each pattern, all-seven rejection, commit appends or replaces at index

## 8. UI tests and manual checks

- [x] 8.1 Compose test for the form: defaults, required-name error, invalid dose error, end-before-start error, prescriber selection, empty schedules text, discard dialog behaviour
- [x] 8.2 Compose test for the editor round trip: open from form, enter 40 mg every 12 hours from 08:00, Done, row reads "40 mg every 12 hours"; edit the row to 20 mg; remove it
- [x] 8.3 Compose end-to-end test: save a medicine with two schedules and assert it appears under Active on the overview with both descriptions
- [x] 8.4 Configuration-change test: rotate with a schedule in the draft and on the editor; draft preserved — covered by `DraftSaverTest` (unit, every schedule shape round-trips through `SavedStateHandle`) plus manual case MT-5. A rotation test through the real activity was not added: the flow test drives `PillsnerApp` from its own `setContent`, which an activity recreation discards, so such a test would exercise the harness rather than the draft
- [x] 8.5 Manual test cases documented in the change (see `manual-tests.md`): 200 percent font scale on form and editor with the keyboard open and the pinned Save and Done buttons reachable; TalkBack announces field labels, errors, day chip states and tile descriptions

## 9. Verification and documentation

- [x] 9.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim — both BUILD SUCCESSFUL. Lint found and we fixed one real defect (`LocalDate.EPOCH` needs API 34, minSdk is 26) and one unused string (the duplicate-time message, now shown in the editor). Nine `PluralsCandidate` warnings remain on the numbered schedule strings; left as designed (design D2 uses explicit strings because the numbered form is only reached from a count of three)
- [x] 9.2 Run `./gradlew connectedAndroidTest` for DAO, migration and Compose tests — 80 instrumented tests on the `pixel_7_-_api_36_0` emulator (API 36), 0 failures, including `MedicationDaoTest` (11), `PillsnerDatabaseMigrationTest` (1) and `AddMedicationFlowTest` (12)
- [x] 9.2a Run the `pillsner-ui-review` skill over `ui/medicines/add` and `ui/medicines/schedule`; resolve every finding or list the remaining ones with a reason — sweep clean: no hex colours, no raw dp, no inline text styles, no truncation, no alpha, no literal user-facing strings, one `displayLarge` in the whole feature, `fontScale = 2f` previews on both screens. Every `colorScheme.error` use is a validation error, which section 2.4 allows. Two files have no preview and need none: the navigation graph builder and the formatter render nothing
- [x] 9.3 Update `README.md` "Features" with adding medicines and flexible multiple schedules; confirm the technology table still matches
- [x] 9.4 Review against `CLAUDE.md`: no `android.*` in domain, strings in resources, single DI mechanism, version catalog, no destructive migration in release, no medication names or doses logged at info or above
- [x] 9.5 Confirm archive order: `app-welcome-screen`, then `app-medicine-overview`, then this change — both are already archived (2026-09-13)
