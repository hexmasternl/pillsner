## Context

`app-welcome-screen` lays down the shell this change extends: a single activity, `PillsnerApp` with a `Scaffold`, a bottom `NavigationBar` shown only on the three top-level routes (`Home`, `Medicines`, `Settings`), type-safe `@Serializable` routes, a `PillsnerTheme`, and a manual `AppContainer` for dependency injection. The Medicines destination is a title-only placeholder. This change replaces that placeholder with the medicine overview and adds the first nested route (`AddMedication`).

There is no medication model, no schedule model and no persistence yet. The overview needs all three concepts to exist at least as domain contracts: a medicine has a name and an active flag, a medicine has a schedule, and a schedule can be described in words. These types will later be persisted by Room and consumed by the dose generator and the reminder scheduler, so they must be shaped for the whole product, not only for this screen, while still leaving out fields no current change needs.

Constraints from `CLAUDE.md` that shape this design: domain layer without Android imports, strings in resources, Material 3 accessibility (large fonts, TalkBack, one hand), no new dependencies without a named reason, glossary terms (`Medication`, `Schedule`) in code. UI copy uses "medicine" because that is the word already on the bottom navigation item; code uses `Medication` per the glossary.

## Goals / Non-Goals

**Goals:**
- A Medicines screen with two sections, active then inactive, each medicine as a tile with name and a human-readable schedule description.
- Correct, translatable descriptions for every schedule shape the model can express.
- A large FAB at the bottom right that navigates to the Add medicine destination.
- Domain contracts (`Medication`, `Schedule`, `MedicationRepository`, `ScheduleSummary`) that the add-medicine, Room and scheduling changes can build on without reshaping.
- Unit tests for summarisation, ordering and partitioning; Compose semantics tests for sections, empty states, inactive announcement and FAB navigation.

**Non-Goals:**
- The add-medicine form, editing, deleting, activating or deactivating.
- Persistence. The repository is in-memory and empty in this change.
- Tile actions, detail screens, dose amounts, stock or refill hints on the tile.
- Dose generation from a schedule. `Schedule` carries enough to describe itself and to be generated from later; the generator is not written here.

## Decisions

### D1. Domain model: `Medication` owns exactly one `Schedule`

```kotlin
@JvmInline value class MedicationId(val value: Long)

data class Medication(
    val id: MedicationId,
    val name: String,
    val schedule: Schedule,
    val isActive: Boolean,
)

sealed interface Schedule {
    /** Fixed clock times on the given days of the week. All seven days means daily. */
    data class FixedTimes(val times: List<LocalTime>, val days: Set<DayOfWeek>) : Schedule
    /** One dose every [intervalDays] days at [time]. intervalDays >= 1. */
    data class EveryNDays(val intervalDays: Int, val time: LocalTime) : Schedule
    /** One dose every [intervalHours] hours, clock-driven from the first dose. intervalHours >= 1. */
    data class EveryNHours(val intervalHours: Int) : Schedule
    /** No schedule; the user takes it when needed. */
    data object AsNeeded : Schedule
}
```

*Why one schedule per medication:* it covers every example in the glossary ("fixed times, intervals, weekdays, or none for as-needed use") and every description the proposal asks for. A medication with two independent schedules ("1 tablet in the morning, 2 in the evening") is really a difference in dose amount per time, which belongs to the add-medicine change as a property of each fixed time, not as a second schedule. Splitting into two entities now would force a join on every read for a case nobody has asked for.

*Why `LocalTime`/`DayOfWeek` from `java.time`:* schedules are wall-clock rules ("08:00 every day") that must survive time zone and DST changes as wall-clock times; converting to instants is the dose generator's job. `java.time` is available from `minSdk` 26 and has no Android dependency, so the domain stays unit-testable.

*Why `isActive` on `Medication` rather than a separate status list:* the overview partitions on it, the dose generator will filter on it, and the add-medicine change toggles it. One boolean, one meaning: "this medication's schedule currently produces doses".

*Alternatives considered:* a `Schedule` as a single data class with nullable fields for each shape. Rejected because invalid combinations become representable and every consumer must check nulls. A `frequency: String` stored on the medication for display. Rejected because it cannot be translated, cannot drive dose generation and would drift from the real schedule.

*Deliberately omitted fields:* form, strength, notes, stock. The glossary lists them but no screen in this change shows them. The add-medicine change adds them; adding nullable placeholders now would only invite unused code.

### D2. Repository contract

```kotlin
interface MedicationRepository {
    /** Emits every medication, active and inactive, in no guaranteed order, and re-emits on any change. */
    fun observeAll(): Flow<List<Medication>>
}
```

*Why unsorted and unpartitioned:* sorting by name is a locale-sensitive presentation concern, and the split into active/inactive is a view concern. Keeping both in the view model means the Room implementation is a single query and the behaviour is unit-tested once, in the view model. If the list grows large enough that database-side filtering matters, the interface can gain `observeActive()` without breaking callers.

`InMemoryMedicationRepository` in `data` holds a `MutableStateFlow<List<Medication>>` starting empty. It is the wired implementation until Room lands, and its mutability is what the add-medicine change will use if it is applied before persistence. `PreviewMedicationRepository` in the `debug` source set holds five invented medicines covering each schedule shape, with two inactive, for Compose previews.

### D3. Schedule description: domain summary, UI wording

The domain layer computes a `ScheduleSummary`; the UI layer turns it into text.

```kotlin
sealed interface ScheduleSummary {
    data class TimesPerDay(val count: Int) : ScheduleSummary                      // daily
    data class TimesPerDayOnDays(val count: Int, val days: Set<DayOfWeek>) : ScheduleSummary
    data object EveryOtherDay : ScheduleSummary
    data class EveryNDays(val days: Int) : ScheduleSummary                        // days >= 3
    data class EveryNHours(val hours: Int) : ScheduleSummary
    data object AsNeeded : ScheduleSummary
}

fun Schedule.summarize(): ScheduleSummary
```

Summarisation rules:
- `FixedTimes` with all seven days → `TimesPerDay(distinct times)`; with a strict subset → `TimesPerDayOnDays(distinct times, days)`. Duplicate times are counted once. An empty `times` list or empty `days` set is invalid and is rejected at construction (`require`), so the summary never has to describe it.
- `EveryNDays(1, _)` → `TimesPerDay(1)`; `EveryNDays(2, _)` → `EveryOtherDay`; `EveryNDays(n >= 3, _)` → `EveryNDays(n)`.
- `EveryNHours(24)` → `TimesPerDay(1)`; otherwise `EveryNHours(h)`.
- `AsNeeded` → `AsNeeded`.

`ScheduleDescriptionFormatter` in `ui/medicines` maps the summary to string resources:

| Summary | English text |
| --- | --- |
| `TimesPerDay(1)` | Once a day |
| `TimesPerDay(2)` | Twice a day |
| `TimesPerDay(n)` | %d times a day |
| `TimesPerDayOnDays(1, Mon, Wed, Fri)` | Once a day on Mon, Wed, Fri |
| `TimesPerDayOnDays(2, Mon..Fri)` | Twice a day on weekdays |
| `EveryOtherDay` | Once every other day |
| `EveryNDays(3)` | Once every 3 days |
| `EveryNHours(8)` | Every 8 hours |
| `AsNeeded` | As needed |

"Once" and "Twice" are separate string resources rather than a plural rule because English plurals only distinguish one/other, and "twice" is the wording the product wants. Day names use `DayOfWeek.getDisplayName(TextStyle.SHORT, locale)` ordered from the locale's first day of the week, joined with a list-separator string resource. When the selected days are exactly Monday to Friday the text uses "on weekdays" instead of listing them.

*Why split domain/UI:* the domain cannot touch resources, and the summary is where the logic lives (is every 24 hours "once a day"? is Mon to Fri "weekdays"?). Testing the summary in plain JUnit covers the rules; the formatter test only checks resource selection.

*Alternative considered:* formatting straight from `Schedule` in the UI. Rejected because the collapsing rules (24 hours = daily, 1 day = daily) would be untestable without Android and would be duplicated by the reminder notification text later.

### D4. UI state and view model

```kotlin
data class MedicinesUiState(
    val active: List<MedicineTileState> = emptyList(),
    val inactive: List<MedicineTileState> = emptyList(),
    val isLoading: Boolean = true,
)

data class MedicineTileState(
    val id: MedicationId,
    val name: String,
    val summary: ScheduleSummary,
    val isActive: Boolean,
)
```

`MedicinesViewModel` maps `observeAll()` into a `StateFlow<MedicinesUiState>` with `stateIn(viewModelScope, WhileSubscribed(5_000), MedicinesUiState())`. Partition on `isActive`; sort each partition by name using a `Collator` for the default locale, case-insensitive. The view model exposes `ScheduleSummary`, not text, so it stays free of resources; the composable formats at render time, which also means a locale change re-renders correctly.

The only event flowing up is `onAddMedicine`, which the screen receives as a lambda and `PillsnerApp` binds to `navController.navigate(AddMedication)`. The view model has no part in navigation.

### D5. Screen layout: one lazy list, two sections, a large FAB

`MedicinesScreen` is a `Scaffold` whose `floatingActionButton` is a `LargeFloatingActionButton` with an `Add` icon and the content description "Add medicine". The content is a single `LazyColumn` with:

1. Section header "Active", shown whenever at least one medicine exists in either section.
2. Active tiles, or an active empty state ("You have no active medicines. Tap + to add one.") when `active` is empty but `inactive` is not.
3. Section header "Inactive" and inactive tiles, only when `inactive` is non-empty.
4. When both lists are empty and loading is finished: a single centred empty state with the same hint towards the button, and no headers.

*Why one list rather than two:* two independently scrolling lists on one screen are hard to use with one hand and confusing with TalkBack. One list with section headers scrolls as a whole at large font sizes and gives a natural reading order. Headers use `stickyHeader` so the user always knows which section they are in.

The list has bottom `contentPadding` of the FAB height plus 16 dp so the last tile is never hidden behind the button. The FAB sits inside the Medicines screen's own `Scaffold`, so it stays above the bottom navigation bar.

*Alternative considered:* a `MediumFloatingActionButton` or standard size. The product asked for a big button and it is the screen's only action; large is the Material size for a dominant primary action and the easiest target for users with reduced dexterity.

### D6. Tile design and accessibility

`MedicineTile` is an `ElevatedCard` (matching the welcome screen tiles) with the name as `titleMedium` and the description as `bodyMedium` supporting text. Inactive tiles use `OutlinedCard` with content at `MaterialTheme.colorScheme.onSurfaceVariant` so they read as secondary without relying on colour alone: the section header and the screen-reader text carry the meaning. Each tile uses `Modifier.semantics(mergeDescendants = true)` so TalkBack reads "Ibuprofen, twice a day" as one item; inactive tiles append the "Inactive" word to the merged description. Minimum height 56 dp; tiles are not clickable in this change but keep the standard card shape so tapping can be added without a layout change.

### D7. Navigation: `AddMedication` nested route

`Routes.kt` gains `@Serializable data object AddMedication`. `PillsnerApp` adds a `composable<AddMedication>` destination rendering `AddMedicationPlaceholderScreen` (title "Add medicine" from a string resource, a top app bar with a back arrow that pops the back stack). Because the bottom bar visibility rule already checks for top-level routes only (welcome design D3), the bar hides on this destination without extra work. Back returns to Medicines with its list state intact because the FAB navigation is a plain `navigate(AddMedication)` with no `popUpTo`.

*Why a placeholder rather than no destination:* the FAB requirement must be observable and testable now, and the add-medicine change then only replaces the placeholder composable.

### D8. Dependency injection

`AppContainer` gains `val medicationRepository: MedicationRepository` (wired to `InMemoryMedicationRepository`) and the shared `ViewModelProvider.Factory` learns to create `MedicinesViewModel`. No new mechanism, in line with the welcome-screen and login designs.

### D9. Package layout

```
domain/model/Medication.kt          MedicationId, Medication
domain/model/Schedule.kt            Schedule sealed interface with validation
domain/model/ScheduleSummary.kt     ScheduleSummary + Schedule.summarize()
domain/repository/MedicationRepository.kt
data/InMemoryMedicationRepository.kt
data/PreviewMedicationRepository.kt (debug source set)
ui/medicines/MedicinesUiState.kt
ui/medicines/MedicinesViewModel.kt
ui/medicines/MedicinesScreen.kt
ui/medicines/MedicineTile.kt
ui/medicines/MedicinesSectionHeader.kt
ui/medicines/ScheduleDescriptionFormatter.kt
ui/medicines/add/AddMedicationPlaceholderScreen.kt
ui/navigation/Routes.kt             + AddMedication
```

## Risks / Trade-offs

- [This change assumes the `app-welcome-screen` shell exists] → Stated as a hard dependency in the proposal. Tasks begin by verifying the shell and stop if it is absent rather than recreating it.
- [The `app-navigation` delta uses MODIFIED against a requirement that only exists once `app-welcome-screen` is archived] → Archive order is documented in the proposal and repeated in the tasks. If the order cannot be honoured, the MODIFIED block is rewritten as ADDED with a distinct requirement name before archiving.
- [`Schedule` is defined before the dose generator exists and may need fields the generator wants (start date, end date, first-dose time for hourly intervals)] → The sealed interface is additive: new fields with defaults or new shapes do not break the overview. Anything not needed to describe the schedule is left for the scheduling change, which owns generation semantics.
- [In-memory repository means a real device shows the empty state until add-medicine and Room land] → Accepted, same as the welcome screen. Previews and tests exercise the populated states.
- [Sorting with `Collator` on every emission] → Lists are tens of items at most; cost is negligible. Sorting moves to `Dispatchers.Default` inside the `map` if profiling ever shows otherwise.
- [Sticky headers with a large FAB can crowd a small screen at maximum font scale] → Headers are single-line `labelLarge`, the FAB is anchored to the scaffold rather than the list, and the manual test case covers maximum font scale.
- [Inactive de-emphasis by colour only] → Section header, card style and the spoken "Inactive" carry the distinction independently of colour.

## Migration Plan

Additive. No persisted data exists, so nothing migrates. Rollback is removing the `ui/medicines` content (restoring the placeholder), the `AddMedication` route and the domain/data files. The `Medication` and `Schedule` types are intended to be persisted by the Room change; that change owns the initial schema and its first migration test.

## Open Questions

- Should the description include the dose amount ("1 tablet twice a day") once the add-medicine change introduces a typed quantity? The formatter takes a `ScheduleSummary` today; extending it with an optional quantity is a small change in that proposal.
- Should tapping a tile open a detail or edit screen? Expected yes, in the add/edit medicine change.
- Do users need to hide inactive medicines entirely, or archive them after a period? Not until someone asks; the section is already hidden when empty.
