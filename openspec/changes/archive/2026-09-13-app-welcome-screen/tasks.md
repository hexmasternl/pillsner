## 1. Project scaffold (this change is the only one that creates it)

- [x] 1.1 Confirm `src/` holds no Gradle project. If one exists, stop and report: no other change is allowed to have created it, so something is out of order
- [x] 1.2 Re-check every row of design D11 against the official release pages (Gradle, AGP, Kotlin, Compose BOM mapping, AndroidX release notes). Use the newest stable version of each; never an alpha, beta or release candidate. Record any difference from D11 in the design before continuing
- [x] 1.3 Make the Android SDK location available to the build (`ANDROID_HOME` or an untracked `src/local.properties`), install SDK Platform 37 and Build Tools 36.0.0, and confirm JDK 21 is the JDK Gradle runs on
- [x] 1.4 Create the Gradle project in `src/` with Kotlin DSL: `settings.gradle.kts` (plugin management, `google()` and `mavenCentral()` repositories, `:app`), root `build.gradle.kts`, `gradle.properties` (AndroidX, non-transitive R classes, configuration cache on), and `gradle/libs.versions.toml` with every version from D11 pinned exactly
- [x] 1.5 Generate the Gradle wrapper at 9.7.1 with `gradle wrapper --gradle-version 9.7.1` (or Android Studio); commit `gradlew`, `gradlew.bat` and `gradle/wrapper/`
- [x] 1.6 Create the `app` module `build.gradle.kts`: Android application plugin (AGP 9.4.0), Kotlin Android, Kotlin Compose compiler plugin and Kotlin serialization plugin (all 2.4.20); `namespace` and `applicationId` `nl.hexmaster.pillsner`; `minSdk` 26, `compileSdk` and `targetSdk` 37; Java toolchain and `jvmTarget` 21; Compose BOM 2026.08.00 with `material3`, `material3-adaptive-navigation-suite`, `ui-tooling-preview` (debug `ui-tooling`), `navigation-compose`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `activity-compose`, `core`, `kotlinx-coroutines-android`, `kotlinx-serialization-json`; test dependencies JUnit 4, `kotlinx-coroutines-test`, AndroidX Test core/runner/rules/ext-junit, Compose `ui-test-junit4` and debug `ui-test-manifest`
- [x] 1.7 Add `AndroidManifest.xml` declaring `PillsnerApplication` and `MainActivity` as the single launcher activity with the Pillsner theme; no permissions in this change
- [x] 1.8 Add a `.gitignore` covering Android and Gradle build output, `local.properties`, IDE metadata and keystores; confirm no local property file or keystore is tracked
- [x] 1.9 Verify `./gradlew assembleDebug` succeeds from `src/` with an empty activity, and record the exact resolved versions of Gradle, AGP, Kotlin and the Compose BOM in design D11 if any differ from the table

## 2. Theme and assets (design system sections 2 to 7 and 11)

- [x] 2.1 Download the five static font files named in design D10 from the official Raleway and Montserrat repositories into `res/font/` with the exact resource names, and add `res/font/OFL.txt` with the SIL Open Font License text
- [x] 2.2 Create `ui/theme` with the `pillsner-theme` skill: `Color.kt` (only file with hex literals), `Type.kt` (`RalewayFamily`, `MontserratFamily`, `PillsnerTypography`), `Shape.kt`, `Dimens.kt` (`Spacing`, `Sizes`), `IntakeStatusColors.kt` (`IntakeStatus` with all six states), `Theme.kt` (`PillsnerTheme`, `isSystemInDarkTheme()` default, edge-to-edge, no dynamic colour) and a `ThemePreview.kt` with `@PreviewLightDark`
- [x] 2.3 Run the skill's verification: no hex outside `Color.kt`, no `dynamicLightColorScheme`, type sizes exactly the section 3.2 set, no Raleway under 24 sp
- [x] 2.4 Add the placeholder capsule mark `ic_pillsner_logo.xml` (diagonal green over blue capsule on a white circle) and the adaptive launcher icon with the mark on a `primary` background, both built from theme colours where the format allows
- [x] 2.5 Add the bundled Material Symbols Rounded vector drawables this change needs: `home`, `medication`, `settings` (outlined and filled), `schedule` (Due). Do not add `material-icons-extended`
- [x] 2.6 Add string resources: app name and title, navigation labels (Home, Medicines, Settings), placeholder screen titles, empty-state headline "Nothing due right now" and hint "Your next dose will appear here.", logo content description, "Tomorrow" day label, "Due" status label, and the tile content description template

## 3. Domain and data

- [x] 3.1 Create `domain/model/UpcomingDose.kt` (`DoseId`, `medicationName`, `amount`, `scheduledAt: Instant`) with KDoc and no Android imports
- [x] 3.2 Create `domain/repository/UpcomingDosesRepository.kt` with `observeUpcoming(limit: Int): Flow<List<UpcomingDose>>` and KDoc stating the ordering and limit contract
- [x] 3.3 Create `data/EmptyUpcomingDosesRepository.kt` emitting an empty list
- [x] 3.4 Create `PreviewUpcomingDosesRepository` in the `debug` source set with three invented sample doses for Compose previews
- [x] 3.5 Create `AppContainer` exposing the repository and a `ViewModelProvider.Factory`; instantiate it in `PillsnerApplication`. This is the single DI mechanism every later change extends

## 4. Welcome screen (design system sections 8.1, 8.7, 8.10)

- [x] 4.1 Create `ui/home/HomeUiState.kt` and `ui/home/HomeViewModel.kt` mapping `observeUpcoming(5)` into a `StateFlow<HomeUiState>`, enforcing the five-item cap and soonest-first ordering
- [x] 4.2 Create `ui/home/WelcomeHeader.kt`: capsule mark (painter parameter with default, content description) and the app title in `displayLarge`, centred, with `heading()` semantics; no app bar on Home
- [x] 4.3 Create `ui/home/DoseTile.kt` with the `pillsner-ui-build` skill: `Card` in `shapes.large` on `tileContainerColor()`, `Sizes.stateStripeWidth` stripe in the status container colour, status icon, name `titleMedium`, amount `bodyLarge`, `IntakeStatusChip`, time `titleLarge` with tabular figures and the day label when not today, padding `Spacing.lg`, minimum height `Sizes.tileMinHeight`, one merged semantics node with the full description, chip `stateDescription`
- [x] 4.4 Create `ui/home/IntakeStatusChip.kt` per section 8.3: non-interactive, `shapes.full`, `Sizes.statusChipHeight`, `labelMedium`, icon plus label, colours from `intakeStatusColors(status)`
- [x] 4.5 Create a `UpcomingDoseTimeFormatter` in the UI layer that formats an `Instant` for the device time zone and locale through `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)` and decides when to show the day label
- [x] 4.6 Create `ui/home/EmptyState.kt` per section 8.10 (outlined icon at `Sizes.iconEmptyState`, `headlineSmall` line, `bodyLarge` hint, optional button slot) reusable by later screens
- [x] 4.7 Create `ui/home/WelcomeScreen.kt`: header, then a `LazyColumn` of tiles with `Arrangement.spacedBy(Spacing.lg)`, `Spacing.screenEdge` side padding and the `Spacing.contentMaxWidth` cap, or the empty state; previews with `@PreviewLightDark` for empty and five-tile states and a `fontScale = 2f` preview
- [x] 4.8 Add test tags for header, tile list, individual tiles and empty state to support semantics tests

## 5. Navigation shell (design system section 8.6)

- [x] 5.1 Create `ui/navigation/Routes.kt` with `@Serializable` route objects `Home`, `Medicines`, `Settings` and a list of top-level destinations (route, label resource, outlined and filled icon resources)
- [x] 5.2 Create `ui/medicines/MedicinesScreen.kt` and `ui/settings/SettingsScreen.kt` placeholder composables that display their title in `displayLarge` with `heading()` semantics
- [x] 5.3 Create `ui/PillsnerApp.kt`: `NavigationSuiteScaffold` with the three items (selected: filled icon, `secondaryContainer` indicator, `onSurface` label; unselected: outlined icon, `onSurfaceVariant`; labels always visible), shown only on top-level destinations; `NavHost` with `Home` as start destination; switching with `popUpTo(start) { saveState = true }`, `launchSingleTop = true`, `restoreState = true`
- [x] 5.4 Wire `MainActivity` to `enableEdgeToEdge()` and `setContent { PillsnerTheme { PillsnerApp(...) } }`, obtaining `HomeViewModel` through the `AppContainer` factory
- [x] 5.5 Confirm back behaviour: back from Medicines/Settings returns to Home; back from Home finishes the activity
- [x] 5.6 Confirm in a medium-width preview or emulator that the suite switches to a navigation rail and content is capped at `Spacing.contentMaxWidth`

## 6. Tests

- [x] 6.1 Unit test `HomeViewModel`: empty repository yields empty state; three doses yield three in order; eight doses yield the five earliest; updates propagate
- [x] 6.2 Unit test `UpcomingDoseTimeFormatter`: today shows time only; tomorrow shows day label; boundary just before and after midnight in the device time zone
- [x] 6.3 Compose semantics test for `WelcomeScreen`: header visible with heading semantics, empty state shown with no doses, five tiles shown for eight doses, tile content description contains name, amount, status and time
- [x] 6.4 Compose semantics test for `PillsnerApp` navigation in a compact window: Home selected on launch; tapping Medicines/Settings shows the placeholder title and updates selection; tapping Home returns to the welcome screen; re-tapping the selected item does not change the back stack
- [x] 6.5 Manual test case documented in the change: 200 percent system font scale with five tiles scrolls and clips nothing; TalkBack reads each tile as one item and announces navigation item selection state; both light and dark render the brand palette

## 7. Verification and documentation

- [x] 7.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix any failures and report results verbatim
- [x] 7.2 Run `./gradlew connectedAndroidTest` on a device or emulator for the Compose tests
- [x] 7.3 Run the `pillsner-ui-review` skill over `ui/` and resolve every finding, or list the remaining ones with a reason
- [x] 7.4 Update `README.md`: "Features" gains a line about the welcome screen showing upcoming doses; the toolchain table under "Getting started" matches the version catalog exactly; the technology table and repository layout still match
- [x] 7.5 Review the change against `CLAUDE.md` (strings in resources, no Android imports in domain, single DI mechanism, version catalog) and against `docs/design-system.md` section 12 before archiving
