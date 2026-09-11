## 1. Preconditions

- [ ] 1.1 Verify the `app-welcome-screen` shell exists in `src/`: `PillsnerApp`, `Routes.kt` with `Medicines`, the placeholder `MedicinesScreen`, `AppContainer` and the version catalog. If it does not, stop and apply `app-welcome-screen` first; this change does not create the scaffold
- [ ] 1.2 Confirm `app-welcome-screen` is archived (or will be archived before this change) so the `app-navigation` MODIFIED delta has a requirement to modify; if it cannot be, rewrite that delta block as ADDED with a distinct requirement name

## 2. Domain model

- [ ] 2.1 Create `domain/model/Medication.kt` with `MedicationId` (inline value class over `Long`) and `Medication(id, name, schedule, isActive)`, with KDoc explaining the active flag; no Android imports
- [ ] 2.2 Create `domain/model/Schedule.kt` as a sealed interface with `FixedTimes(times, days)`, `EveryNDays(intervalDays, time)`, `EveryNHours(intervalHours)` and `AsNeeded`, using `java.time.LocalTime` and `DayOfWeek`; add `require` checks rejecting empty times, empty days and intervals below one; KDoc each shape as wall-clock rules
- [ ] 2.3 Create `domain/model/ScheduleSummary.kt` with the sealed `ScheduleSummary` (`TimesPerDay`, `TimesPerDayOnDays`, `EveryOtherDay`, `EveryNDays`, `EveryNHours`, `AsNeeded`) and `Schedule.summarize()` implementing the collapsing rules from design D3 (distinct times, all-seven-days is daily, every 1 day and every 24 hours are once a day, every 2 days is every other day)
- [ ] 2.4 Create `domain/repository/MedicationRepository.kt` with `observeAll(): Flow<List<Medication>>` and KDoc stating it emits all medications unordered and re-emits on change

## 3. Data layer and DI

- [ ] 3.1 Create `data/InMemoryMedicationRepository.kt` backed by a `MutableStateFlow<List<Medication>>` starting empty, with an internal `replaceAll`/`upsert` for tests and later changes
- [ ] 3.2 Create `PreviewMedicationRepository` in the `debug` source set with five invented medicines covering every schedule shape, two of them inactive
- [ ] 3.3 Extend `AppContainer` with `medicationRepository` wired to `InMemoryMedicationRepository`, and extend the `ViewModelProvider.Factory` to create `MedicinesViewModel`

## 4. String resources

- [ ] 4.1 Add strings: section headers "Active" and "Inactive", empty-state message for no medicines, empty-state message for no active medicines, FAB content description "Add medicine", spoken "Inactive" suffix, Add medicine screen title, back content description
- [ ] 4.2 Add schedule description strings: "Once a day", "Twice a day", "%d times a day", "Once a day on %s", "Twice a day on %s", "%1$d times a day on %2$s", "on weekdays" variants, "Once every other day", "Once every %d days", "Every %d hours", "As needed", and a list separator string

## 5. Medicines screen

- [ ] 5.1 Create `ui/medicines/MedicinesUiState.kt` with `MedicinesUiState(active, inactive, isLoading)` and `MedicineTileState(id, name, summary, isActive)`
- [ ] 5.2 Create `ui/medicines/MedicinesViewModel.kt` mapping `observeAll()` into a `StateFlow<MedicinesUiState>`: partition on `isActive`, sort each partition by name with a locale `Collator`, case-insensitive; `stateIn` with `WhileSubscribed(5_000)`
- [ ] 5.3 Create `ui/medicines/ScheduleDescriptionFormatter.kt` mapping each `ScheduleSummary` to the string resources from 4.2, ordering day names from the locale's first day of the week using short display names, and substituting "weekdays" when the days are exactly Monday to Friday
- [ ] 5.4 Create `ui/medicines/MedicineTile.kt`: `ElevatedCard` for active, `OutlinedCard` with `onSurfaceVariant` content for inactive; name as `titleMedium`, description as `bodyMedium`; `semantics(mergeDescendants = true)`; inactive tiles append the spoken "Inactive" suffix to the merged description; minimum height 56 dp
- [ ] 5.5 Create `ui/medicines/MedicinesSectionHeader.kt` as a single-line `labelLarge` header usable as a `stickyHeader`
- [ ] 5.6 Replace the placeholder in `ui/medicines/MedicinesScreen.kt` with a `Scaffold` holding a `LargeFloatingActionButton` (add icon, content description, `onAddMedicine` callback) and a single `LazyColumn` with the section logic from design D5: headers only when medicines exist, active empty state when only inactive exist, inactive section only when non-empty, single centred empty state when there are none; bottom content padding of FAB height plus 16 dp
- [ ] 5.7 Add test tags for the FAB, both headers, individual tiles, the active empty state and the overall empty state; add Compose previews for empty, active-only, and mixed states using `PreviewMedicationRepository`

## 6. Navigation

- [ ] 6.1 Add `@Serializable data object AddMedication` to `ui/navigation/Routes.kt`
- [ ] 6.2 Create `ui/medicines/add/AddMedicationPlaceholderScreen.kt` with a top app bar showing the "Add medicine" title and a back arrow that invokes an `onBack` callback
- [ ] 6.3 In `PillsnerApp`, wire `MedicinesScreen` to the `MedicinesViewModel` from the `AppContainer` factory, bind `onAddMedicine` to `navController.navigate(AddMedication)`, add the `composable<AddMedication>` destination with `onBack = { navController.popBackStack() }`, and confirm the bottom bar hides because `AddMedication` is not a top-level route

## 7. Tests

- [ ] 7.1 Unit test `Schedule` construction: empty times, empty days and zero intervals throw; valid shapes construct
- [ ] 7.2 Unit test `Schedule.summarize()` for every scenario in the `medication-schedule-model` spec: two distinct times daily, duplicate times, subset of days, every 1 day, every 2 days, every 3 days, every 24 hours, every 8 hours, as needed
- [ ] 7.3 Unit test `MedicinesViewModel`: empty repository yields empty lists; mixed emission partitions correctly; names sort case-insensitively (paracetamol, Ibuprofen, Amoxicillin becomes Amoxicillin, Ibuprofen, paracetamol); a medication whose active flag flips moves between lists on the next emission
- [ ] 7.4 Unit test `InMemoryMedicationRepository`: first emission is empty; adding a medication re-emits to an active collector
- [ ] 7.5 Instrumented or Robolectric test for `ScheduleDescriptionFormatter` in an English locale covering every description scenario in the spec, including "Once a day on Mon, Wed, Fri" and "Twice a day on weekdays"
- [ ] 7.6 Compose semantics test for `MedicinesScreen`: empty state shown with no headers; active-only shows one header and no inactive header; mixed shows both headers in order; only-inactive shows the active empty state above the inactive header; inactive tile content description contains "Inactive"; active tile merges name and description into one node; FAB present with content description
- [ ] 7.7 Compose semantics test for `PillsnerApp` navigation: tapping the FAB on Medicines shows the "Add medicine" title and hides the bottom bar; back returns to Medicines with the Medicines item selected and the bottom bar visible
- [ ] 7.8 Document the manual test case in the change: maximum system font scale with five active and three inactive medicines scrolls as one list, clips nothing, keeps the FAB visible and lets the last tile be fully revealed; TalkBack reads each tile as one item and says "Inactive" for inactive tiles

## 8. Verification and documentation

- [ ] 8.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim
- [ ] 8.2 Run `./gradlew connectedAndroidTest` on a device or emulator for the Compose and formatter tests
- [ ] 8.3 Update `README.md` "Features" with a line about the medicine overview (active and inactive medicines with schedule descriptions, add button)
- [ ] 8.4 Review against `CLAUDE.md`: no Android imports in `domain`, all strings in resources, single DI mechanism, no new dependencies, glossary terms in code, no medication names logged
