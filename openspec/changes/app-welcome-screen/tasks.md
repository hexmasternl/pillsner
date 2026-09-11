## 1. Project scaffold

- [ ] 1.1 Check whether `src/` already contains a Gradle project (from `app-login`); if so, adapt the following tasks to what exists instead of recreating it
- [ ] 1.2 Create the Gradle project in `src/` with Kotlin DSL: `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, Gradle wrapper, and `gradle/libs.versions.toml` version catalog
- [ ] 1.3 Create the `app` module with `build.gradle.kts`: Android application plugin, Kotlin Android plugin, Kotlin Compose compiler plugin, `minSdk` 26, `compileSdk`/`targetSdk` at current stable, Compose BOM, Material 3, `navigation-compose`, `lifecycle-viewmodel-compose`, `activity-compose`, `kotlinx-serialization` (for type-safe routes), and test dependencies (JUnit, coroutines-test, Compose UI test)
- [ ] 1.4 Add `AndroidManifest.xml` declaring `PillsnerApplication` and `MainActivity` as the single launcher activity; no permissions in this change
- [ ] 1.5 Extend `.gitignore` for Android/Gradle build output, `local.properties` and IDE metadata
- [ ] 1.6 Verify `./gradlew assembleDebug` succeeds from `src/` with an empty activity

## 2. Theme and assets

- [ ] 2.1 Create `ui/theme` with `PillsnerTheme` using the `pillsner-theme` skill: static light and dark schemes from `docs/design-system.md`, no dynamic colour, bundled Montserrat and Raleway fonts, `PillsnerTypography`, `PillsnerShapes`, `Spacing`
- [ ] 2.2 Add the placeholder vector logo `ic_pillsner_logo.xml` and an adaptive launcher icon derived from it
- [ ] 2.3 Add string resources: app name/title, bottom navigation labels (Home, Medicines, Settings), placeholder screen titles, empty-state message, logo content description, and "Tomorrow" day label

## 3. Domain and data

- [ ] 3.1 Create `domain/model/UpcomingDose.kt` (`DoseId`, `medicationName`, `amount`, `scheduledAt: Instant`) with KDoc and no Android imports
- [ ] 3.2 Create `domain/repository/UpcomingDosesRepository.kt` with `observeUpcoming(limit: Int): Flow<List<UpcomingDose>>` and KDoc stating the ordering and limit contract
- [ ] 3.3 Create `data/EmptyUpcomingDosesRepository.kt` emitting an empty list
- [ ] 3.4 Create `PreviewUpcomingDosesRepository` in the `debug` source set with three invented sample doses for Compose previews
- [ ] 3.5 Create `AppContainer` exposing the repository and a `ViewModelProvider.Factory`; instantiate it in `PillsnerApplication`

## 4. Welcome screen

- [ ] 4.1 Create `ui/home/HomeUiState.kt` and `ui/home/HomeViewModel.kt` mapping `observeUpcoming(5)` into a `StateFlow<HomeUiState>`, enforcing the five-item cap and soonest-first ordering
- [ ] 4.2 Create `ui/home/WelcomeHeader.kt` composable showing the logo (painter parameter with default) and the app title, with a content description on the logo
- [ ] 4.3 Create `ui/home/UpcomingDoseTile.kt`: `ElevatedCard` with medication name, amount and locale-formatted time; show a day indication when the dose is not today; merge semantics so a screen reader reads the tile as one item
- [ ] 4.4 Create a `UpcomingDoseTimeFormatter` in the UI layer that formats an `Instant` for the device time zone and locale and decides when to show the day label
- [ ] 4.5 Create `ui/home/WelcomeScreen.kt`: header, then a `LazyColumn` of tiles or the empty state; add Compose previews for the empty state and the five-tile state
- [ ] 4.6 Add test tags for header, tile list, individual tiles and empty state to support semantics tests

## 5. Navigation shell

- [ ] 5.1 Create `ui/navigation/Routes.kt` with `@Serializable` route objects `Home`, `Medicines`, `Settings` and a list of top-level destinations (route, label resource, icon)
- [ ] 5.2 Create `ui/medicines/MedicinesScreen.kt` and `ui/settings/SettingsScreen.kt` placeholder composables that display their title
- [ ] 5.3 Create `ui/PillsnerApp.kt`: `Scaffold` with `NavigationBar` in the bottom bar (shown only on top-level destinations), `NavHost` with `Home` as start destination, tab switching using `popUpTo(start) { saveState = true }`, `launchSingleTop = true`, `restoreState = true`
- [ ] 5.4 Wire `MainActivity` to `setContent { PillsnerTheme { PillsnerApp(...) } }` obtaining `HomeViewModel` through the `AppContainer` factory
- [ ] 5.5 Confirm back behaviour: back from Medicines/Settings returns to Home; back from Home finishes the activity

## 6. Tests

- [ ] 6.1 Unit test `HomeViewModel`: empty repository yields empty state; three doses yield three in order; eight doses yield the five earliest; updates propagate
- [ ] 6.2 Unit test `UpcomingDoseTimeFormatter`: today shows time only; tomorrow shows day label; boundary just before and after midnight in the device time zone
- [ ] 6.3 Compose semantics test for `WelcomeScreen`: header visible, empty state shown with no doses, five tiles shown for eight doses, tile content description contains name, amount and time
- [ ] 6.4 Compose semantics test for `PillsnerApp` navigation: Home selected on launch; tapping Medicines/Settings shows the placeholder title and updates selection; tapping Home returns to the welcome screen; re-tapping the selected item does not change the back stack
- [ ] 6.5 Manual test case documented in the change: largest system font scale with five tiles scrolls and clips nothing; TalkBack reads each tile as one item and announces navigation item selection state

## 7. Verification and documentation

- [ ] 7.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix any failures and report results verbatim
- [ ] 7.2 Run `./gradlew connectedAndroidTest` on a device or emulator for the Compose tests
- [ ] 7.3 Update `README.md` "Features" with a line about the welcome screen showing upcoming doses, and confirm the technology table and repository layout still match
- [ ] 7.4 Review the change against `CLAUDE.md` conventions (strings in resources, no Android imports in domain, single DI mechanism, version catalog) before archiving
