## Context

`src/` is empty. This change lays down the first application code: the Gradle project, the single activity, the Material 3 theme, the navigation shell and the welcome screen. Everything later (medications, schedules, reminders, the app lock from `app-login`) plugs into the shell defined here, so the structure has to match the conventions in `CLAUDE.md` from the start: Kotlin only, Compose with Material 3, MVVM with unidirectional data flow, a domain layer with no Android dependencies, and a single lightweight DI mechanism.

There is no medication or schedule data yet. The welcome screen must be built against a domain contract that a later Room-backed implementation can satisfy without touching the UI.

Constraints from `CLAUDE.md` that shape this design: user-facing strings in resources, accessibility for large fonts and TalkBack, dependencies in a version catalog, no third-party libraries without a named reason.

## Goals / Non-Goals

**Goals:**
- A runnable debug build with a welcome screen that shows logo, title, up to five upcoming dose tiles, and an empty state.
- A bottom navigation bar with Home, Medicines and Settings that behaves like a standard Material 3 top-level navigation.
- A project structure (packages, layers, DI seam, version catalog) that later changes extend rather than rework.
- A domain contract (`UpcomingDose`, `UpcomingDosesRepository`) that isolates the UI from how doses are produced.
- Tests: view model unit tests, Compose semantics tests for the welcome screen and navigation bar.

**Non-Goals:**
- Real dose generation, medication storage, Room, alarms or notifications.
- Intake actions on tiles.
- Content for Medicines and Settings beyond a titled placeholder.
- Final branding artwork.
- Choosing a DI framework beyond what this change needs (see Decisions).

## Decisions

### D1. Project layout: single Gradle module, layered by package

One `app` module under `src/` with packages `ui`, `domain` and `data`, each split by feature where it helps (`ui/home`, `ui/medicines`, `ui/settings`, `ui/navigation`, `ui/theme`).

*Why:* the app is small and a multi-module split now would be structure without benefit. The layering rule ("domain has no Android dependencies") is enforced by convention and by keeping domain types free of `android.*` imports; a lint check or module split can be added later if it is ever violated.

*Alternative considered:* separate `domain` and `data` Gradle modules to enforce the dependency direction. Rejected for now as premature; revisit when a second feature area lands.

### D2. Navigation: Navigation Compose with a type-safe route object per top-level destination

`androidx.navigation:navigation-compose` with `@Serializable` route objects `Home`, `Medicines`, `Settings`. A `NavigationBar` sits in the `Scaffold` bottom bar of a `PillsnerApp` root composable; the `NavHost` fills the content area. Switching tabs uses `popUpTo(startDestination) { saveState = true }`, `launchSingleTop = true`, `restoreState = true`, which is the Material-recommended pattern for bottom navigation.

*Why:* it is the first-party Compose navigation library, already the README's stated choice, and type-safe routes remove string-route typos. Back from Medicines or Settings returns to Home; back from Home leaves the app, which matches platform expectations for a top-level start destination.

*Alternative considered:* a hand-rolled `when(selectedTab)` without a nav library. Simpler today, but every later screen (medication detail, add medication) would need real navigation anyway, and `app-login` expects a navigation host to wrap.

### D3. Bottom bar visibility

The bottom bar is only shown when the current back-stack entry is one of the three top-level routes. This is decided at the `Scaffold` level by inspecting the current destination, so later nested screens (for example a medication detail) hide it without changes to those screens.

### D4. Domain contract for upcoming doses

```kotlin
data class UpcomingDose(
    val doseId: DoseId,
    val medicationName: String,
    val amount: String,          // already formatted, e.g. "1 tablet", "5 ml"
    val scheduledAt: Instant,
)

interface UpcomingDosesRepository {
    fun observeUpcoming(limit: Int): Flow<List<UpcomingDose>>
}
```

The repository returns doses sorted by `scheduledAt` ascending and already limited. The view model additionally enforces the cap of five so a misbehaving implementation cannot overflow the screen. Formatting the amount is deliberately a string in this change because the `Medication`/`Dose` model does not exist yet; when it does, `amount` becomes a typed quantity and formatting moves to the UI layer. This is noted as a follow-up in Open Questions.

`java.time.Instant` is used for `scheduledAt`. Time formatting for display happens in the UI layer with the device locale and time zone; the domain model stays in `Instant`.

*Alternative considered:* letting the welcome screen read a `Dose` entity directly. Rejected because it couples the screen to a schema that does not exist yet and would force the Room change to also touch the UI.

### D5. Placeholder implementation

`EmptyUpcomingDosesRepository` in `data` returns `flowOf(emptyList())`. A `PreviewUpcomingDosesRepository` with three sample doses, kept in the debug source set, backs Compose previews so the tile design can be seen without real data. The preview data uses invented medication names only.

### D6. Dependency injection: manual constructor injection through a single `AppContainer`

A plain Kotlin `AppContainer` class created in `PillsnerApplication`, exposing the repository and a `ViewModelProvider.Factory`. View models receive their dependencies through constructors.

*Why:* the proposal for a DI mechanism must be agreed once and not mixed. Manual DI is the lightest option that still gives constructor injection and testability. If the team later prefers Hilt or Koin, the container is the single place to replace, and that swap gets its own proposal as `CLAUDE.md` requires.

*Alternative considered:* Hilt. Adds an annotation processor, KSP setup and a learning curve for a screen with one dependency. Not justified yet.

### D7. Welcome screen state and view model

```kotlin
data class HomeUiState(
    val upcomingDoses: List<UpcomingDose> = emptyList(),
    val isLoading: Boolean = true,
)
```

`HomeViewModel` maps `observeUpcoming(limit = 5)` into a `StateFlow<HomeUiState>` using `stateIn(viewModelScope, WhileSubscribed(5_000), HomeUiState())`. The screen renders: header (logo + title), then either the tile list or the empty state. No events flow up yet because tiles are not actionable in this change.

### D8. Tile design and accessibility

Each tile is an `ElevatedCard` with the medication name as the title (`titleMedium`), the amount as supporting text and the scheduled time as a trailing label. Time shows as a locale-formatted time for today; if the dose falls on a later day, the label also shows a short relative day ("Tomorrow", or the weekday). Each tile has a single merged content description so TalkBack reads "Ibuprofen, 1 tablet, at 8:00 tomorrow" as one item. Tiles use `Modifier.semantics(mergeDescendants = true)`. The list is a `LazyColumn` so it scrolls at large font sizes even with five tiles. Touch targets meet 48 dp even though tiles are not yet clickable, so adding actions later does not change layout.

### D9. Logo placeholder

A vector drawable `ic_pillsner_logo.xml` (simple pill shape in the primary colour) and an adaptive launcher icon generated from it. The header composable takes the painter as a parameter with this drawable as default, so replacing the artwork means swapping the XML file only.

### D10. Theme

Material 3 with the static light and dark colour schemes from `docs/design-system.md` section 2.2, following the system setting. Dynamic colour is off (design system principle 5). Typography is `PillsnerTypography` with bundled Montserrat and Raleway (section 3); the theme layer is built with the `pillsner-theme` skill.

### D11. Build configuration

Gradle Kotlin DSL, version catalog in `src/gradle/libs.versions.toml`, Compose BOM, Kotlin Compose compiler plugin. `compileSdk` and `targetSdk` at the current stable API level; `minSdk` 26. Rationale for 26: it gives `java.time` natively, `NotificationChannel` (needed by the reminders change) and covers the overwhelming majority of active devices. Lowering it later requires a proposal per `CLAUDE.md`.

## Risks / Trade-offs

- [Two changes both claim to create the scaffold (`app-login` and this one)] → This change is the intended owner of the shell because it defines navigation and the Settings destination `app-login` needs. When `app-login` is applied afterwards, its scaffold task becomes a no-op. If `app-login` is applied first, this change's scaffold tasks adapt to what exists instead of duplicating it.
- [`amount` as a pre-formatted string leaks presentation into the domain] → Accepted for now to avoid inventing the medication model early. Tracked as an open question; the Room/medication change will replace it with a typed quantity.
- [Manual DI grows unwieldy as features are added] → The single `AppContainer` keeps the swap cost to one file. Revisit when a change needs scoped or lazily created dependencies.
- [Placeholder Medicines and Settings screens ship in a build] → Acceptable in early development; both display a title so the navigation is testable. They are not user-visible until a release is tagged.
- [Brand colours look wrong on some devices] → Dynamic colour is off, so the palette is identical everywhere. Screenshot tests, if added, run against both static schemes.

## Migration Plan

Not applicable: there is no existing app, database or persisted state. The change is additive and can be reverted by deleting `src/` contents.

## Open Questions

- When the `Medication` and `Dose` models arrive, should `UpcomingDose.amount` become a typed `Quantity(value, unit)` formatted in the UI layer? (Recommended: yes, in that change.)
- Should the welcome screen also show doses that are already overdue today, or strictly future ones? This design shows doses with `scheduledAt` at or after now; overdue handling belongs with intake tracking, where "missed" is defined.
- Final logo artwork and app name styling are pending design input.
