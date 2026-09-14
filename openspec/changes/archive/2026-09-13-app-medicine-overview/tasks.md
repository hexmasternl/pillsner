## 1. Preconditions

- [x] 1.1 Verify the `app-welcome-screen` shell exists in `src/`: `PillsnerApp`, `Routes.kt` with `Medicines`, the placeholder `MedicinesScreen`, `AppContainer` and the version catalog. If it does not, stop and apply `app-welcome-screen` first; this change does not create the scaffold
- [x] 1.2 Confirm `app-welcome-screen` is archived (or will be archived before this change) so the `app-navigation` MODIFIED delta has a requirement to modify; if it cannot be, rewrite that delta block as ADDED with a distinct requirement name

## 2. Domain model

- [x] 2.1 Create `domain/model/Medication.kt` with `MedicationId` (inline value class over `Long`) and `Medication(id, name, schedule, isActive)`, with KDoc explaining the active flag; no Android imports
- [x] 2.2 Create `domain/model/Schedule.kt` as a sealed interface with `FixedTimes(times, days)`, `EveryNDays(intervalDays, time)`, `EveryNHours(intervalHours)` and `AsNeeded`, using `java.time.LocalTime` and `DayOfWeek`; add `require` checks rejecting empty times, empty days and intervals below one; KDoc each shape as wall-clock rules
- [x] 2.3 Create `domain/model/ScheduleSummary.kt` with the sealed `ScheduleSummary` (`TimesPerDay`, `TimesPerDayOnDays`, `EveryOtherDay`, `EveryNDays`, `EveryNHours`, `AsNeeded`) and `Schedule.summarize()` implementing the collapsing rules from design D3 (distinct times, all-seven-days is daily, every 1 day and every 24 hours are once a day, every 2 days is every other day)
- [x] 2.4 Create `domain/repository/MedicationRepository.kt` with `observeAll(): Flow<List<Medication>>` and KDoc stating it emits all medications unordered and re-emits on change

## 3. Data layer and DI

- [x] 3.1 Create `data/InMemoryMedicationRepository.kt` backed by a `MutableStateFlow<List<Medication>>` starting empty, with an internal `replaceAll`/`upsert` for tests and later changes
- [x] 3.2 Create `PreviewMedicationRepository` in the `debug` source set with five invented medicines covering every schedule shape, two of them inactive
- [x] 3.3 Extend `AppContainer` with `medicationRepository` wired to `InMemoryMedicationRepository`, and extend the `ViewModelProvider.Factory` to create `MedicinesViewModel`

## 4. String resources

- [x] 4.1 Add strings: screen title "Medicines" (already present from the shell), section headers "Active" and "Inactive", empty-state headline and hint for no medicines, empty-state headline and hint for no active medicines, FAB content description "Add medicine", the `Inactive` chip label and state description, Add medicine screen title, back content description
- [x] 4.2 Add schedule description strings: "Once a day", "Twice a day", "%d times a day", "Once a day on %s", "Twice a day on %s", "%1$d times a day on %2$s", "on weekdays" variants, "Once every other day", "Once every %d days", "Every %d hours", "As needed", and a list separator string

## 5. Medicines screen

- [x] 5.1 Create `ui/medicines/MedicinesUiState.kt` with `MedicinesUiState(active, inactive, isLoading)` and `MedicineTileState(id, name, summary, isActive)`
- [x] 5.2 Create `ui/medicines/MedicinesViewModel.kt` mapping `observeAll()` into a `StateFlow<MedicinesUiState>`: partition on `isActive`, sort each partition by name with a locale `Collator`, case-insensitive; `stateIn` with `WhileSubscribed(5_000)`
- [x] 5.3 Create `ui/medicines/ScheduleDescriptionFormatter.kt` mapping each `ScheduleSummary` to the string resources from 4.2, ordering day names from the locale's first day of the week using short display names, and substituting "weekdays" when the days are exactly Monday to Friday
- [x] 5.4 Create `ui/medicines/MedicineTile.kt` with the `pillsner-ui-build` skill per design system 8.2: `Card` in `shapes.large` on `tileContainerColor()` for active; `surfaceContainerLow` container, `onSurfaceVariant` text, `Inactive` chip and `stateDescription` for inactive (no alpha); name `titleMedium`, one `bodyMedium` line per description; `semantics(mergeDescendants = true)`; minimum height `Sizes.tileMinHeight`; `@PreviewLightDark` for active and inactive
- [x] 5.5 Create `ui/medicines/MedicinesSectionHeader.kt` as a `headlineSmall` header with `heading()` semantics on the `surface` colour, usable as a `stickyHeader`
- [x] 5.6 Replace the placeholder in `ui/medicines/MedicinesScreen.kt` with a `Scaffold` holding a `LargeFloatingActionButton` per 8.5 (`primaryContainer`, add icon, content description, `onAddMedicine` callback) and a single `LazyColumn` (`Spacing.screenEdge` padding, `Arrangement.spacedBy(Spacing.lg)`, `Spacing.contentMaxWidth` cap) with the `displayLarge` title and the section logic from design D5: headers only when medicines exist, active empty state when only inactive exist, inactive section only when non-empty, the shared `EmptyState` when there are none; bottom content padding of FAB height plus `Spacing.lg`
- [x] 5.7 Add test tags for the FAB, both headers, individual tiles, the active empty state and the overall empty state; add `@PreviewLightDark` previews for empty, active-only, and mixed states using `PreviewMedicationRepository`, plus a `fontScale = 2f` preview

## 6. Navigation

- [x] 6.1 Add `@Serializable data object AddMedication` to `ui/navigation/Routes.kt`
- [x] 6.2 Create `ui/medicines/add/AddMedicationPlaceholderScreen.kt` with a `TopAppBar` per design system 8.7 (`titleLarge` title "Add medicine", back arrow with content description, `surface` container) that invokes an `onBack` callback
- [x] 6.3 In `PillsnerApp`, wire `MedicinesScreen` to the `MedicinesViewModel` from the `AppContainer` factory, bind `onAddMedicine` to `navController.navigate(AddMedication)`, add the `composable<AddMedication>` destination with `onBack = { navController.popBackStack() }`, and confirm the bottom bar hides because `AddMedication` is not a top-level route

## 7. Tests

- [x] 7.1 Unit test `Schedule` construction: empty times, empty days and zero intervals throw; valid shapes construct
- [x] 7.2 Unit test `Schedule.summarize()` for every scenario in the `medication-schedule-model` spec: two distinct times daily, duplicate times, subset of days, every 1 day, every 2 days, every 3 days, every 24 hours, every 8 hours, as needed
- [x] 7.3 Unit test `MedicinesViewModel`: empty repository yields empty lists; mixed emission partitions correctly; names sort case-insensitively (paracetamol, Ibuprofen, Amoxicillin becomes Amoxicillin, Ibuprofen, paracetamol); a medication whose active flag flips moves between lists on the next emission
- [x] 7.4 Unit test `InMemoryMedicationRepository`: first emission is empty; adding a medication re-emits to an active collector
- [x] 7.5 Instrumented or Robolectric test for `ScheduleDescriptionFormatter` in an English locale covering every description scenario in the spec, including "Once a day on Mon, Wed, Fri" and "Twice a day on weekdays"
- [x] 7.6 Compose semantics test for `MedicinesScreen`: empty state shown with no headers; active-only shows one header and no inactive header; mixed shows both headers in order; only-inactive shows the active empty state above the inactive header; inactive tile carries the "Inactive" state description and chip; active tile merges name and description into one node; FAB present with content description
- [x] 7.7 Compose semantics test for `PillsnerApp` navigation: tapping the FAB on Medicines shows the "Add medicine" title and hides the bottom bar; back returns to Medicines with the Medicines item selected and the bottom bar visible
- [x] 7.8 Document the manual test case in the change (see `manual-tests.md`): maximum system font scale with five active and three inactive medicines scrolls as one list, clips nothing, keeps the FAB visible and lets the last tile be fully revealed; TalkBack reads each tile as one item and says "Inactive" for inactive tiles

## 8. Verification and documentation

- [x] 8.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim — `testDebugUnitTest` and `lintDebug` both BUILD SUCCESSFUL. Lint reports one warning, `PluralsCandidate` on `schedule_n_times_a_day` and `schedule_every_n_days`; left as designed (design D3 chooses explicit strings over plurals because the numbered form is only reached from a count of three)
- [x] 8.2 Run `./gradlew connectedAndroidTest` on a device or emulator for the Compose and formatter tests — 51 instrumented tests on the `pixel_7_-_api_36_0` emulator (API 36), 0 failures, including `MedicinesScreenTest` (10) and `ScheduleDescriptionFormatterTest` (10)
- [x] 8.2a Run the `pillsner-ui-review` skill over `ui/medicines`; resolve every finding or list the remaining ones with a reason — sweep clean: no hex colours, no raw dp, no inline text styles, no alpha de-emphasis, no literal user-facing strings, one `displayLarge` on the screen, previews on every visual composable and a `fontScale = 2f` preview on the screen and the tile. One deliberate remaining item: the numbered schedule strings are not `<plurals>` (see 8.1)
- [x] 8.3 Update `README.md` "Features" with a line about the medicine overview (active and inactive medicines with schedule descriptions, add button)
- [x] 8.4 Review against `CLAUDE.md`: no Android imports in `domain`, all strings in resources, single DI mechanism, no new dependencies, glossary terms in code, no medication names logged
