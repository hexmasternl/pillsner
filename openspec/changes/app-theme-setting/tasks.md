## 1. Check the ground

- [x] 1.1 Confirm the scaffold exists: `src/app`, `di/AppContainer.kt`, `MainActivity.kt`, `ui/theme/Theme.kt` and `ui/settings/SettingsScreen.kt`. Stop and report if any is missing — this change does not create the scaffold.
- [x] 1.2 Confirm `PillsnerTheme` still takes `darkTheme: Boolean = isSystemInDarkTheme()` and that `MainActivity` is its only production caller. If either has changed, revisit design D4 and D6 before writing code.

## 2. Domain

- [x] 2.1 Add `domain/model/AppTheme.kt`: the `SYSTEM` / `LIGHT` / `DARK` enum with a nullable `key`, `isDark(systemInDarkTheme: Boolean)` and `ofKey(key: String?)`, with KDoc explaining why `SYSTEM` has a null key (design D1). No Android imports.
- [x] 2.2 Add `domain/repository/ThemeRepository.kt` with `observeTheme(): Flow<AppTheme>` and `suspend fun setTheme(theme: AppTheme)`, mirroring `LanguageRepository`.
- [x] 2.3 Add `AppThemeTest` in `src/test`: `isDark` for all three options against both phone states, and `ofKey` for each key, for null, for empty and for an unrecognised value.

## 3. Persistence

- [x] 3.1 Add `data/settings/DataStoreThemeRepository.kt` writing the string key `theme` into the existing `settingsDataStore`, removing the key for `SYSTEM` (design D2).
- [x] 3.2 Add `DataStoreThemeRepositoryTest` in `src/androidTest`, modelled on `DataStoreLanguageRepositoryTest`: round-trip each option, read `SYSTEM` from a fresh store, and read `SYSTEM` from a corrupt value.

## 4. Wiring

- [x] 4.1 In `di/AppContainer.kt`, construct `DataStoreThemeRepository` and expose the choice as a `StateFlow<AppTheme>` whose initial value is read before first composition (design D4). Comment the blocking read the way `PillsnerApplication.applyStoredLanguage` is commented.
- [x] 4.2 In `MainActivity.onCreate`, collect the flow with `collectAsStateWithLifecycle()` and pass `theme.isDark(isSystemInDarkTheme())` into `PillsnerTheme`.
- [x] 4.3 Update the KDoc on `PillsnerTheme` in `ui/theme/Theme.kt`: it renders the theme it is given, defaulting to the system setting. Leave the signature, the `SideEffect` and `LocalPillsnerDarkTheme` untouched (design D6).
- [x] 4.4 Register `ThemeSectionViewModel` in the container's `viewModelFactory`.

## 5. Strings

- [x] 5.1 Add to `res/values/strings.xml`: `settings_theme_header`, `settings_theme_label`, `settings_theme_system_default`, `settings_theme_light`, `settings_theme_dark`. Do not reuse `settings_language_system_default` (design D5).
- [x] 5.2 Add the Dutch counterparts to `res/values-nl/strings.xml`: "Thema", "Thema", "Systeemstandaard", "Licht", "Donker".

## 6. Settings UI

- [x] 6.1 Add `ui/settings/theme/ThemeSectionViewModel.kt` with `ThemeSectionState(selected, options)` and `onThemeSelected`, following `LanguageSectionViewModel` but with no restart state (design D3).
- [x] 6.2 Add `ui/settings/theme/ThemeSection.kt`: header in `headlineSmall` marked `heading()`, `ExposedDropdownMenuBox` with a read-only `OutlinedTextField` anchor, three options, a `ThemeSectionTestTags` object, and `@PreviewLightDark` plus a `fontScale = 2f` preview. No restart notice.
- [x] 6.3 Add the section to `ui/settings/SettingsScreen.kt` as an `item(key = "theme")` between `"language"` and `"security"`, with its two new parameters, and update the screen's preview.
- [x] 6.4 Pass the new parameters through `ui/PillsnerApp.kt`.
- [x] 6.5 Add `ThemeSectionViewModelTest` in `src/test`: state reflects the stored value, and selecting an option writes it through a fake repository.
- [x] 6.6 Add `ThemeSectionTest` in `src/androidTest`: the anchor shows the stored option, opening the dropdown offers exactly three, choosing one reports it, and no restart notice is present.
- [x] 6.7 Add a Compose test asserting the point of the change: under `PillsnerTheme(darkTheme = AppTheme.DARK.isDark(systemInDarkTheme = false))`, `LocalPillsnerDarkTheme` is true and the dark scheme is in effect.
- [x] 6.8 Run the `pillsner-ui-review` skill over `ui/settings/theme/` and the edited `SettingsScreen.kt`, and fix what it reports.

## 7. Design system document

- [x] 7.1 Reword principle 4 in `docs/design-system.md`: System, Light and Dark are offered, System is the default and follows the OS, both schemes stay first-class designs (design D7).
- [x] 7.2 Update the Do/Don't row to "Render the user's stored theme choice, defaulting to the system setting" / "Add a fourth theme, a schedule or an AMOLED variant without a proposal".
- [x] 7.3 Update the `Theme.kt` row in the Compose implementation table to record that `MainActivity` passes the stored choice.
- [x] 7.4 Verify nothing else changed: principle 5, the dynamic-colour rule, section 2.2 and every palette row are untouched. Mirror the same three edits into `docs/design-system.html` so the two stay in sync.

## 8. Verify

- [x] 8.1 Run the unit test task from `src` and report any failure verbatim.
- [x] 8.2 Run the lint task from `src`; confirm no missing-translation or extra-translation error.
- [ ] 8.3 Run the instrumented tests from `src` (DataStore and Compose tests are instrumented) and report any failure verbatim.
- [ ] 8.4 Manual check on a device or emulator: choose Dark while the phone is light and confirm the Settings screen repaints at once with no restart notice; kill and cold start the app and confirm the first frame is dark; choose System default and toggle the phone's dark mode to confirm the app follows.
- [ ] 8.5 Manual check: with Light chosen on a dark phone, confirm the status and navigation bar icons are dark and readable.
- [ ] 8.6 Confirm the app lock unlock screen renders in the chosen theme when the app starts locked.
