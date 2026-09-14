## 1. Preconditions and build setup

- [x] 1.1 Verify the `app-welcome-screen` shell exists (`PillsnerApplication`, `MainActivity`, `PillsnerApp`, `Settings` route, `AppContainer`, `ui/theme`); stop if it does not, since this change creates no scaffold. Check whether `app-login` has already created the Settings sections list or a DataStore dependency, and adapt the tasks below to what exists
- [x] 1.2 Add `androidx.datastore:datastore-preferences` to the version catalog and app module if absent — already there at 1.2.1, added by `app-login`
- [x] 1.3 Add `resourceConfigurations += listOf("en", "nl")` to the app module's default config
- [x] 1.4 Add `lint.xml` with `MissingTranslation` and `ExtraTranslation` at error severity and reference it from the app module's lint options

## 2. Domain

- [x] 2.1 Create `domain/model/AppLanguage.kt` with `AppLanguage` (SYSTEM, ENGLISH, DUTCH with language tags) and `SupportedLanguages` (ordered list plus English fallback), with KDoc
- [x] 2.2 Create `domain/locale/LocaleResolver.kt` implementing: stored wins; else first system tag whose language subtag matches a supported tag; else English; unknown stored value treated as SYSTEM
- [x] 2.3 Create `domain/repository/LanguageRepository.kt` with `observeLanguage(): Flow<AppLanguage>` and `suspend fun setLanguage(AppLanguage)`
- [x] 2.4 Unit tests for `LocaleResolver`: stored Dutch over English phone, system default with Dutch second in list, `nl-BE` and `en-GB` variants, unsupported list falls back to English, empty list falls back to English, corrupt stored tag treated as SYSTEM

## 3. Data

- [x] 3.1 Create `data/settings/SettingsDataStore.kt` providing the general `settings` Preferences DataStore (distinct from any `applock` file)
- [x] 3.2 Create `data/settings/DataStoreLanguageRepository.kt` mapping an absent or unknown value to SYSTEM and writing the language tag on set
- [x] 3.3 Expose `languageRepository` from `AppContainer`, constructed before anything that needs the locale
- [x] 3.4 Instrumented test for `DataStoreLanguageRepository`: default is SYSTEM, set then observe returns the value, value survives a new repository instance on the same file

## 4. Applying the locale

- [x] 4.1 Create `ui/locale/AppLocale.kt` with `current`, `inEffect: AppLanguage`, `wrap(context)` using `createConfigurationContext`, and `refresh(systemLocales)` that re-resolves and calls `Locale.setDefault`
- [x] 4.2 In `PillsnerApplication.onCreate`, read the stored language with a blocking first read, resolve against the system locale list, and initialise `AppLocale`; override `onConfigurationChanged` to refresh when the stored language is SYSTEM
- [x] 4.3 Override `MainActivity.attachBaseContext` to wrap the base context with `AppLocale.wrap`
- [x] 4.4 Confirm existing formatters (`UpcomingDoseTimeFormatter`, `ScheduleDescriptionFormatter`, `QuantityFormatter`) and the overview `Collator` use `Locale.getDefault()` and accept an explicit locale in tests; fix any that hard-code a locale
- [x] 4.5 Route `ReminderNotifier` through `AppLocale.wrap` — it is applied, so the notifier wraps its own context and its quantity formatter. The action receivers read no strings themselves; everything they show goes through the notifier
- [x] 4.6 Instrumented test: with Dutch stored, a freshly launched activity resolves the Settings title as "Instellingen"; with nothing stored on an English device, as "Settings"

## 5. Settings screen and Language section

- [x] 5.1 Replace the Settings placeholder with `SettingsScreen` using the `pillsner-ui-build` skill: `displayLarge` title with `heading()` semantics, `LazyColumn` (`Spacing.screenEdge`, `Spacing.contentMaxWidth` cap, `Spacing.xl` between sections) of section composables with `headlineSmall` headers, Language section first; if `app-login` already created the screen, insert the Language section above Security
- [x] 5.2 Create `LanguageSectionState` and `LanguageSectionViewModel` combining the stored language with `AppLocale.inEffect` into `selected`, `options` and `restartRequired`; `onLanguageSelected` writes through the repository
- [x] 5.3 Create `LanguageSection` with an `ExposedDropdownMenuBox` on an `OutlinedTextField` labelled "Language", options from the state (System default translated, native names from a non-translatable array), and the restart notice row per design D6 (`info` icon, `bodyMedium`, `secondaryContainer` / `onSecondaryContainer`, `shapes.small`, polite live region; no `error` colour); `@PreviewLightDark` and `fontScale = 2f` previews
- [x] 5.4 Add English strings: settings title, Language section header, dropdown label, "System default", restart notice; add the non-translatable native-name array
- [x] 5.5 Register `LanguageSectionViewModel` in the shared view model factory
- [x] 5.6 Unit tests for the view model: default state, selection persists and updates `selected`, `restartRequired` true when stored differs from in-effect, false after selecting the in-effect language
- [x] 5.7 Compose tests: dropdown shows current selection, options in order, selecting Dutch shows the warning, re-selecting the in-effect language hides it, screen scrolls at maximum font scale

## 6. Dutch translation

- [x] 6.1 Create `values-nl/strings.xml` translating every translatable string present in `values/` at apply time (welcome, navigation, overview, add medicine, schedule editor, settings, and lock or reminder strings if already applied), using glossary terms: medicijn, dosis, inname, herinnering, schema
- [x] 6.2 Translate plurals (units, counts) with `one`/`other` and verify format arguments match the English versions
- [x] 6.3 Mark the app name and native language names `translatable="false"`
- [x] 6.4 Add a unit test or lint verification that `values` and `values-nl` contain the same translatable keys — `TranslationCompletenessTest` checks both directions and, beyond what lint does, that every translation takes the same format arguments as its original: a swapped `%1$s` is a wrong sentence rather than a crash
- [x] 6.5 Run `./gradlew lint` and confirm zero missing-translation and extra-translation errors

## 7. Verification and documentation

- [x] 7.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim — both BUILD SUCCESSFUL, 235 unit tests, 0 failures. Lint found one real defect with the new strictness: `AppBundleLocaleChanges`, because Play would otherwise ship only the phone's own language and the in-app picker offers the other. Fixed by turning off the language split. Zero `MissingTranslation` and `ExtraTranslation` at their new error severity; only the nine known `PluralsCandidate` warnings remain
- [x] 7.2 Run `./gradlew connectedAndroidTest` for the DataStore and locale tests — 159 instrumented tests on the `pixel_7_-_api_36_0` emulator (API 36), 0 failures, including `DataStoreLanguageRepositoryTest` (4) and `LanguageSectionTest` (9, four of which resolve real Dutch and English resources through `AppLocale.wrap`)
- [x] 7.2a Run the `pillsner-ui-review` skill over `ui/settings`; resolve every finding or list the remaining ones with a reason — sweep clean: no hex colours, no raw dp, no inline text styles, no truncation, no alpha, no literal user-facing strings, one `displayLarge`, and `fontScale = 2f` previews on the screen and the section. The restart notice is `secondaryContainer`, never the error role, because a pending restart is information
- [x] 7.3 Manual test cases documented in the change (see `manual-tests.md`): phone in Dutch with nothing stored starts Dutch; phone in German starts English; select Dutch on an English phone shows the notice, restart applies Dutch, notice gone; with System default, change phone language and confirm the app follows; Dutch at 200 percent font scale on the welcome, overview, add and settings screens with no truncated navigation label; startup time not measurably affected by the blocking read on a low-end device
- [x] 7.4 Update `README.md` "Features" with English and Dutch support following the phone language by default
- [x] 7.5 Review against `CLAUDE.md`: no `android.*` in domain, strings in resources, single DI mechanism, version catalog, no new third-party dependencies
- [x] 7.6 Confirm archive order — both are archived (2026-09-13), so the `app-navigation` MODIFIED block has a requirement to modify
