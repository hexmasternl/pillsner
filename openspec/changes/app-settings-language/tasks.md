## 1. Preconditions and build setup

- [ ] 1.1 Verify the `app-welcome-screen` shell exists (`PillsnerApplication`, `MainActivity`, `PillsnerApp`, `Settings` route, `AppContainer`); check whether `app-login` has already created a Settings screen or a DataStore dependency, and adapt the tasks below to what exists
- [ ] 1.2 Add `androidx.datastore:datastore-preferences` to the version catalog and app module if absent
- [ ] 1.3 Add `resourceConfigurations += listOf("en", "nl")` to the app module's default config
- [ ] 1.4 Add `lint.xml` with `MissingTranslation` and `ExtraTranslation` at error severity and reference it from the app module's lint options

## 2. Domain

- [ ] 2.1 Create `domain/model/AppLanguage.kt` with `AppLanguage` (SYSTEM, ENGLISH, DUTCH with language tags) and `SupportedLanguages` (ordered list plus English fallback), with KDoc
- [ ] 2.2 Create `domain/locale/LocaleResolver.kt` implementing: stored wins; else first system tag whose language subtag matches a supported tag; else English; unknown stored value treated as SYSTEM
- [ ] 2.3 Create `domain/repository/LanguageRepository.kt` with `observeLanguage(): Flow<AppLanguage>` and `suspend fun setLanguage(AppLanguage)`
- [ ] 2.4 Unit tests for `LocaleResolver`: stored Dutch over English phone, system default with Dutch second in list, `nl-BE` and `en-GB` variants, unsupported list falls back to English, empty list falls back to English, corrupt stored tag treated as SYSTEM

## 3. Data

- [ ] 3.1 Create `data/settings/SettingsDataStore.kt` providing the general `settings` Preferences DataStore (distinct from any `applock` file)
- [ ] 3.2 Create `data/settings/DataStoreLanguageRepository.kt` mapping an absent or unknown value to SYSTEM and writing the language tag on set
- [ ] 3.3 Expose `languageRepository` from `AppContainer`, constructed before anything that needs the locale
- [ ] 3.4 Instrumented test for `DataStoreLanguageRepository`: default is SYSTEM, set then observe returns the value, value survives a new repository instance on the same file

## 4. Applying the locale

- [ ] 4.1 Create `ui/locale/AppLocale.kt` with `current`, `inEffect: AppLanguage`, `wrap(context)` using `createConfigurationContext`, and `refresh(systemLocales)` that re-resolves and calls `Locale.setDefault`
- [ ] 4.2 In `PillsnerApplication.onCreate`, read the stored language with a blocking first read, resolve against the system locale list, and initialise `AppLocale`; override `onConfigurationChanged` to refresh when the stored language is SYSTEM
- [ ] 4.3 Override `MainActivity.attachBaseContext` to wrap the base context with `AppLocale.wrap`
- [ ] 4.4 Confirm existing formatters (`UpcomingDoseTimeFormatter`, `ScheduleDescriptionFormatter`, `QuantityFormatter`) and the overview `Collator` use `Locale.getDefault()` and accept an explicit locale in tests; fix any that hard-code a locale
- [ ] 4.5 If `app-medicine-alarm` is already applied, route `ReminderNotifier` and the action receivers through `AppLocale.wrap`; otherwise add a note to that change's design that notification contexts must use the helper
- [ ] 4.6 Instrumented test: with Dutch stored, a freshly launched activity resolves the Settings title as "Instellingen"; with nothing stored on an English device, as "Settings"

## 5. Settings screen and Language section

- [ ] 5.1 Replace the Settings placeholder with `SettingsScreen`: title from resources, `LazyColumn` of section composables, Language section first; if `app-login` already created the screen, insert the Language section above Security
- [ ] 5.2 Create `LanguageSectionState` and `LanguageSectionViewModel` combining the stored language with `AppLocale.inEffect` into `selected`, `options` and `restartRequired`; `onLanguageSelected` writes through the repository
- [ ] 5.3 Create `LanguageSection` with an `ExposedDropdownMenuBox` labelled "Language", options from the state (System default translated, native names from a non-translatable array), and the warning row with icon, error colour and polite live region
- [ ] 5.4 Add English strings: settings title, Language section header, dropdown label, "System default", restart warning; add the non-translatable native-name array
- [ ] 5.5 Register `LanguageSectionViewModel` in the shared view model factory
- [ ] 5.6 Unit tests for the view model: default state, selection persists and updates `selected`, `restartRequired` true when stored differs from in-effect, false after selecting the in-effect language
- [ ] 5.7 Compose tests: dropdown shows current selection, options in order, selecting Dutch shows the warning, re-selecting the in-effect language hides it, screen scrolls at maximum font scale

## 6. Dutch translation

- [ ] 6.1 Create `values-nl/strings.xml` translating every translatable string present in `values/` at apply time (welcome, navigation, overview, add medicine, schedule editor, settings, and lock or reminder strings if already applied), using glossary terms: medicijn, dosis, inname, herinnering, schema
- [ ] 6.2 Translate plurals (units, counts) with `one`/`other` and verify format arguments match the English versions
- [ ] 6.3 Mark the app name and native language names `translatable="false"`
- [ ] 6.4 Add a unit test or lint verification that `values` and `values-nl` contain the same translatable keys
- [ ] 6.5 Run `./gradlew lint` and confirm zero missing-translation and extra-translation errors

## 7. Verification and documentation

- [ ] 7.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim
- [ ] 7.2 Run `./gradlew connectedAndroidTest` for the DataStore and locale tests
- [ ] 7.3 Manual test cases documented in the change: phone in Dutch with nothing stored starts Dutch; phone in German starts English; select Dutch on an English phone shows the warning, restart applies Dutch, warning gone; with System default, change phone language and confirm the app follows; Dutch at maximum font scale on the welcome, overview, add and settings screens; startup time not measurably affected by the blocking read on a low-end device
- [ ] 7.4 Update `README.md` "Features" with English and Dutch support following the phone language by default
- [ ] 7.5 Review against `CLAUDE.md`: no `android.*` in domain, strings in resources, single DI mechanism, version catalog, no new third-party dependencies
- [ ] 7.6 Confirm archive order: `app-welcome-screen`, `app-medicine-overview`, then this change
