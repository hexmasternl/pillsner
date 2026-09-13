## Context

`src/` is empty. This change lays down the first application code: the Gradle project, the single activity, the Pillsner theme, the navigation shell and the welcome screen. It is the **only** change that creates the project scaffold. Everything later (medications, schedules, reminders, the app lock from `app-login`) plugs into the shell defined here, so the structure has to match the conventions in `CLAUDE.md` from the start: Kotlin only, Compose with Material 3, MVVM with unidirectional data flow, a domain layer with no Android dependencies, and a single lightweight DI mechanism.

There is no medication or schedule data yet. The welcome screen must be built against a domain contract that a later Room-backed implementation can satisfy without touching the UI.

`docs/design-system.md` (confirmed 11 September 2026) is the source of truth for everything the user sees: colour roles, type scale, shape, spacing, component recipes and accessibility rules. Because this change builds the theme layer and the first screen, it sets the visual baseline every later screen inherits.

Constraints from `CLAUDE.md` that shape this design: user-facing strings in resources, accessibility for large fonts and TalkBack, dependencies in a version catalog, no third-party libraries without a named reason, every visual decision from design-system tokens, UI work through the `pillsner-theme`, `pillsner-ui-build` and `pillsner-ui-review` skills.

## Goals / Non-Goals

**Goals:**
- A runnable debug build with a welcome screen that shows logo, title, up to five upcoming dose tiles, and an empty state.
- A bottom navigation bar with Home, Medicines and Settings that behaves like a standard Material 3 top-level navigation, and becomes a navigation rail at medium width as the design system requires.
- A project structure (packages, layers, DI seam, version catalog, pinned toolchain) that later changes extend rather than rework, and that no other change recreates.
- A complete `ui/theme` layer implementing design-system sections 2 to 6 and 11, so no later screen needs a colour, size or font that is not a token.
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

`androidx.navigation:navigation-compose` with `@Serializable` route objects `Home`, `Medicines`, `Settings`. A `PillsnerApp` root composable hosts a `NavigationSuiteScaffold` (Material 3 adaptive navigation suite, in the Compose BOM) whose `NavHost` fills the content area. On compact widths the suite renders a `NavigationBar` on `surfaceContainer`; at medium width and above it renders a `NavigationRail`, which is what design system sections 5 and 8.6 require. Switching destinations uses `popUpTo(startDestination) { saveState = true }`, `launchSingleTop = true`, `restoreState = true`, which is the Material-recommended pattern for top-level navigation.

Items follow section 8.6: filled icon, `secondaryContainer` indicator and `onSurface` label when selected; outlined icon and `onSurfaceVariant` when not; labels always visible and never truncated. Icons are Material Symbols Rounded (`home`, `medication`, `settings`) bundled as vector drawables, because the pre-generated `material-icons-extended` artifact is deprecated and pulls in thousands of unused icons.

*Why:* it is the first-party Compose navigation library, already the README's stated choice, and type-safe routes remove string-route typos. Back from Medicines or Settings returns to Home; back from Home leaves the app, which matches platform expectations for a top-level start destination. The adaptive suite costs one dependency from the BOM and saves every later change from handling wide screens.

*Alternative considered:* a hand-rolled `when(selectedTab)` without a nav library. Simpler today, but every later screen (medication detail, add medication) would need real navigation anyway, and `app-login` expects a navigation host to wrap. *Also considered:* Navigation 3 (`androidx.navigation3`), stable since early 2026. Its back-stack-as-state model is attractive for a greenfield app, but every other proposal in `openspec/changes/` is written against `NavHost`, nested graphs and `toRoute`; adopting Navigation 3 is a separate proposal that would touch all of them.

### D3. Navigation visibility

The navigation bar or rail is only shown when the current back-stack entry is one of the three top-level routes. This is decided at the root by inspecting the current destination, so later nested screens (for example a medication detail) hide it without changes to those screens.

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

### D8. Dose tile, header and empty state per the design system

**Header (sections 3.2, 8.7).** Home has no app bar. The header is the capsule mark followed by the app title in `displayLarge` (Raleway 48 / 200), centred, with `heading()` semantics. It is the single `displayLarge` on the screen.

**Dose tile (section 8.1).** `DoseTile` is a `Card` with `MaterialTheme.shapes.large`, container `surfaceContainerLowest` in light and `surfaceContainerHigh` in dark (`tileContainerColor()` from the theme layer), no border, no shadow. Its left edge carries a `Sizes.stateStripeWidth` vertical stripe in the status container colour. Content, padded `Spacing.lg`: the status icon, the medication name in `titleMedium`, the amount in `bodyLarge`, an `IntakeStatusChip` ("Due" in this change), and the scheduled time right-aligned in `titleLarge` with tabular figures. Time shows as a locale-formatted time for today; if the dose falls on a later day, the label also shows a short relative day ("Tomorrow", or the weekday). Minimum height `Sizes.tileMinHeight`. Each tile is one merged semantics node so TalkBack reads "Ibuprofen, 1 tablet, due at 8:00 tomorrow" as one item; the chip carries `stateDescription`. The list is a `LazyColumn` with `Arrangement.spacedBy(Spacing.lg)`, `Spacing.screenEdge` side padding and a `Spacing.contentMaxWidth` cap on wide screens, so it scrolls at large font sizes even with five tiles. Status colours come from `intakeStatusColors(status)`; the full six-state mapping from section 2.3 is implemented in the theme layer now so the reminders change only has to supply the status.

**Empty state (section 8.10).** Centred in the remaining space: the outlined `medication` icon at `Sizes.iconEmptyState` in `onSurfaceVariant`, "Nothing due right now" in `headlineSmall`, "Your next dose will appear here." in `bodyLarge` `onSurfaceVariant`. No button in this change.

Touch targets meet `Sizes.minTouchTarget` even though tiles are not yet clickable, so adding the 56 dp "Taken" button later does not change layout.

### D9. Logo placeholder

A vector drawable `ic_pillsner_logo.xml` implementing the placeholder mark from design system section 7: a rounded capsule split diagonally, green upper half and blue lower half, on a white circle. The adaptive launcher icon uses the same mark on a `primary` background. The header composable takes the painter as a parameter with this drawable as default, so replacing the artwork means swapping the XML file only.

### D10. Theme

The `ui/theme` package implements design system section 11 exactly, through the `pillsner-theme` skill:

- `Color.kt`: every hex from section 2.2 and `LightColorScheme` / `DarkColorScheme`. The only file with a hex literal.
- `Type.kt`: `RalewayFamily`, `MontserratFamily` from the five bundled static TTFs in `res/font/` (SIL Open Font License text in `assets/font/OFL.txt`; discovered at apply time that AGP's resource merger rejects any file in `res/font/` other than `.xml`, `.ttf`, `.ttc` or `.otf`, so the licence text ships from `assets/` instead, which still packages it with the app) and `PillsnerTypography` with every row of section 3.2.
- `Shape.kt`: `PillsnerShapes` from section 4.
- `Dimens.kt`: `Spacing` (section 5) plus `Sizes` for touch targets, tile height, stripe width, chip height and icon sizes.
- `IntakeStatusColors.kt`: `IntakeStatus` (Due, Taken, Snoozed, Skipped, Overdue, Missed) mapped to container, on-container and icon per section 2.3.
- `Theme.kt`: `PillsnerTheme(darkTheme = isSystemInDarkTheme(), content)`, edge-to-edge system bars, no dynamic colour parameter.

Light and dark follow the system setting (principle 4). Dynamic colour is off (principle 5). Every preview uses `@PreviewLightDark`. Downloadable fonts are not used: they need a network round trip and a Play dependency, which conflict with the privacy promise; the five static files cost about 1 MB.

### D11. Build configuration and version baseline

Gradle Kotlin DSL, version catalog in `src/gradle/libs.versions.toml`, Compose BOM, Kotlin Compose compiler plugin. Application id `nl.hexmaster.pillsner` (the wearable change relies on it being fixed). `minSdk` 26: it gives `java.time` natively, `NotificationChannel` (needed by the reminders change) and covers the overwhelming majority of active devices. Lowering it later requires a proposal per `CLAUDE.md`.

The baseline below was checked against the official release pages on 11 September 2026. Every version is the newest stable release on that date, or the newest long-term-support release where the tool has one. Pre-release versions are not used. The apply task re-checks each line before writing the catalog and records any change here.

| Component | Version | Notes |
| --- | --- | --- |
| JDK for running Gradle and as the JVM toolchain | 21 (LTS) | Bundled with current Android Studio and installed locally. JDK 25 is the newer LTS, but AGP documents 17 as its tested default and Studio ships 21, so 21 is the newest LTS both agree on. `compileOptions` and `jvmTarget` are 21 as well. |
| Gradle wrapper | 9.7.1 | Released 19 August 2026. AGP 9.4 requires at least 9.6.0. |
| Android Gradle Plugin | 9.4.0 | Released 3 September 2026. Requires JDK 17+, Build Tools 36.0.0, supports API 37. |
| Kotlin (`org.jetbrains.kotlin.android`, `plugin.compose`, `plugin.serialization`) | 2.4.20 | Released 7 September 2026. The Compose compiler ships inside Kotlin. Kotlin 2.4 needs AGP 8.5.2 or newer. |
| KSP (`com.google.devtools.ksp`) | 2.3.12 | Not needed by this change; recorded so `app-medicine-add` adds Room against a matching version. KSP is Kotlin-version independent since 2.3.0. |
| `compileSdk` / `targetSdk` | 37 (Android 17) | The August 2026 Compose BOM requires `compileSdk` 37. |
| `minSdk` | 26 | See above. |
| SDK Build Tools | 36.0.0 | AGP 9.4 default. |
| Compose BOM (`androidx.compose:compose-bom`) | 2026.09.00 | Re-checked at apply time: 2026.09.00 had just been published to the Google Maven repository, superseding 2026.08.00 recorded above. Material 3 stays at 1.4.0 (the newest stable; 1.5.0 is alpha-only as of 11 September 2026), so this bump only picks up newer point releases of the libraries the BOM already pins. Includes `material3-adaptive-navigation-suite`. Requires AGP 9.1.1 or newer. |
| `androidx.activity:activity-compose` | 1.13.0 | Released 11 March 2026. |
| `androidx.lifecycle` (`lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`) | 2.11.0 | Released 17 June 2026. |
| `androidx.navigation:navigation-compose` | 2.10.1 | Released 9 September 2026. `minSdk` 24 or higher. |
| `androidx.navigation:navigation-testing` | 2.10.1 | Same version as `navigation-compose`. Added at apply time, `androidTestImplementation` only: the navigation instrumented test drives a `TestNavHostController` to assert on the back stack without a real `NavHostController`. Not listed in the original catalog table; recorded here per the apply-time re-check rule. |
| `androidx.core:core` | 1.19.0 | Released 3 June 2026. `core-ktx` is merged into `core` since this version; do not add `core-ktx`. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` and `-test` | 1.11.0 | Released May 2026. |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.11.0 | Stable; 1.12.0 is still a release candidate. |
| JUnit 4 | 4.13.2 | Compose UI Test rules are JUnit 4 based. |
| `androidx.test:core`, `runner`, `rules` / `androidx.test.ext:junit` / `espresso-core` | 1.7.0 / 1.3.0 / 3.7.0 | Latest stable AndroidX Test. |

Versions that later changes add (Room, DataStore, Biometric, Wear Compose, Play services Wearable) are pinned in those changes' designs and follow the same rule: newest stable on the day they are applied, re-checked at apply time.

## Risks / Trade-offs

- [Another change creates part of the scaffold on its own] → This change is the sole owner of the scaffold. `app-login` and every other change begin with a precondition that verifies the shell exists and stop otherwise; none of them contains a scaffold task any more.
- [A pinned version is superseded or turns out incompatible at apply time] → The apply task re-checks each row of D11 against the official release pages, prefers the newest stable, and records the actual versions in this design before writing the catalog. A version that only exists as alpha, beta or release candidate is never chosen.
- [Bundled fonts add about 1 MB] → Accepted; the design system chose static files over downloadable fonts for privacy. Variable fonts can replace them later without touching call sites.
- [The adaptive navigation suite behaves differently from a plain `NavigationBar` in tests] → The Compose tests assert on item labels and selected state, which the suite exposes identically; a compact window is forced in the test rule.
- [`amount` as a pre-formatted string leaks presentation into the domain] → Accepted for now to avoid inventing the medication model early. Tracked as an open question; the Room/medication change will replace it with a typed quantity.
- [Manual DI grows unwieldy as features are added] → The single `AppContainer` keeps the swap cost to one file. Revisit when a change needs scoped or lazily created dependencies.
- [Placeholder Medicines and Settings screens ship in a build] → Acceptable in early development; both display a title so the navigation is testable. They are not user-visible until a release is tagged.
- [Brand colours look wrong on some devices] → Dynamic colour is off, so the palette is identical everywhere. Screenshot tests, if added, run against both static schemes.

## Migration Plan

Not applicable: there is no existing app, database or persisted state. The change is additive and can be reverted by deleting `src/` contents.

## Manual test cases

These need a physical device or emulator and are not automated. Record the result against each when run.

1. **200% system font scale.** Set the system font size to its maximum (Settings → Display → Font size, or `adb shell settings put system font_scale 2.0` combined with the largest display "Font size" slider). Load the welcome screen with five upcoming doses. Expect: the header and every tile's text wraps rather than truncating, the tile list scrolls to reveal all five tiles, and no text is clipped top or bottom.
2. **TalkBack.** Enable TalkBack. Swipe through the welcome screen. Expect: the header reads as a heading with the app title; each dose tile is announced as a single item combining medication name, amount, status and time (for example "Ibuprofen, 1 tablet, Due at 8:00 Tomorrow"); focusing the bottom navigation announces each item's label and whether it is selected.
3. **Light and dark.** Toggle the system theme between light and dark (Settings → Display → Dark theme, or `adb shell "cmd uimode night yes"` / `night no`). Expect: both renders use only Pillsner green, blue and white tones from `docs/design-system.md` section 2, never a dynamically-derived wallpaper colour, and the layout is otherwise identical.
4. **Medium-width layout.** Run the app in a resizable or tablet emulator, or drag a freeform window past 600 dp wide. Expect: the bottom navigation bar becomes a navigation rail on the leading edge, and screen content is capped at `Spacing.contentMaxWidth` (600 dp) rather than stretching edge to edge.

## Open Questions

- When the `Medication` and `Dose` models arrive, should `UpcomingDose.amount` become a typed `Quantity(value, unit)` formatted in the UI layer? (Recommended: yes, in that change.)
- Should the welcome screen also show doses that are already overdue today, or strictly future ones? This design shows doses with `scheduledAt` at or after now; overdue handling belongs with intake tracking, where "missed" is defined.
- Final logo artwork is pending design input; the placeholder capsule mark from design system section 7 is used until then. App name styling is settled by the design system (`displayLarge`).
