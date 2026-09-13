## Context

The shell from `app-welcome-screen` has a title-only Settings destination. `app-login` plans to add a Security section to a Settings screen and a Preferences DataStore for lock settings. `app-medicine-alarm` will build notification text in `BroadcastReceiver`s, outside any activity. All strings so far are English `values/` resources, and several places already depend on the default locale: `UpcomingDoseTimeFormatter`, `ScheduleDescriptionFormatter` (short weekday names), `QuantityFormatter` (decimal format) and the overview's `Collator` sort.

The product owner's rules: English and Dutch now, more later; a dropdown on Settings; a restart warning on change; stored language wins, else the phone's default if supported, else English.

Constraints from `CLAUDE.md`: strings in resources and translation from the start, single DI mechanism, first-party dependencies, domain layer free of Android classes, no network, every visual decision from `docs/design-system.md` tokens checked with `pillsner-ui-review`. The project scaffold and version catalog come from `app-welcome-screen`; this change only adds DataStore to the catalog if `app-login` has not already.

## Goals / Non-Goals

**Goals:**
- One source of truth for the app language: the stored setting.
- Deterministic startup resolution implemented in pure Kotlin and unit-tested.
- Every string, date, time, weekday name, number and sort order follows the app language, including in receivers and notifications.
- A Settings screen structure that `app-login` and later changes extend by adding sections.
- Complete Dutch translations with a build-time guard against gaps.

**Non-Goals:**
- Immediate language switching, system per-app language integration, RTL, regional variants, translating user data.

## Decisions

### D1. Language model and resolution in the domain layer

```kotlin
enum class AppLanguage(val tag: String?) { SYSTEM(null), ENGLISH("en"), DUTCH("nl") }

object SupportedLanguages { val all = listOf(AppLanguage.ENGLISH, AppLanguage.DUTCH); val fallback = AppLanguage.ENGLISH }

object LocaleResolver {
    /** systemLanguageTags is the phone's preferred language list, most preferred first, e.g. ["nl-BE", "en-GB"]. */
    fun resolve(stored: AppLanguage, systemLanguageTags: List<String>): AppLanguage
}
```

Rules: `stored != SYSTEM` → `stored`. Otherwise the first tag whose language subtag (the part before `-`) equals a supported language's tag wins, so `nl-BE` resolves to Dutch and `en-GB` to English. If none matches → `ENGLISH`. The resolver takes plain strings so it has no dependency on `java.util.Locale` behaviour differences and is trivially unit-tested.

Adding a language is: add an enum value, add it to `SupportedLanguages.all`, add `values-xx/strings.xml`, add its native name to the dropdown list. The dropdown iterates `SupportedLanguages.all`, so nothing else changes.

### D2. Storage: a general `settings` Preferences DataStore

`LanguageRepository` (domain interface): `observeLanguage(): Flow<AppLanguage>`, `suspend fun setLanguage(AppLanguage)`. `DataStoreLanguageRepository` stores the tag string under key `language` in a `settings` DataStore file. Absent key means `SYSTEM`.

*Why a new `settings` file rather than `app-login`'s `applock` file:* the lock file is excluded from backup by design and holds security material; general preferences should be backed up and kept apart. Future general settings (snooze length, quiet hours) go in the same `settings` file. *Why not Room:* it is one scalar, not a domain record.

### D3. Applying the language: resolve once at process start, wrap contexts

```kotlin
object AppLocale {                       // ui/locale or data/locale, Android-aware
    lateinit var current: Locale        // set once in PillsnerApplication.onCreate
    fun wrap(context: Context): Context // createConfigurationContext with `current`
}
```

- `PillsnerApplication.onCreate` reads the stored language with `runBlocking { repository.observeLanguage().first() }`, resolves it against `LocaleList.getDefault()` (or `Resources.getSystem().configuration.locales`), sets `AppLocale.current`, and calls `Locale.setDefault(current)` so `DateTimeFormatter`, `NumberFormat`, `Collator` and `DayOfWeek.getDisplayName` all follow the app language without each call site knowing about it.
- `MainActivity.attachBaseContext(base)` calls `super.attachBaseContext(AppLocale.wrap(base))`, so every Compose `stringResource` resolves in the app language.
- Receivers and notification builders call `AppLocale.wrap(context)` before reading resources. `ReminderNotifier` in `app-medicine-alarm` takes its context from this helper; the task list covers coordinating that.
- The application context itself is not mutated (`updateConfiguration` is deprecated); code that needs localised resources uses `wrap`.

*Why `runBlocking` at startup is acceptable:* the file holds one key and is read once per process, before any UI exists. AppCompat's own `autoStoreLocales` backport does the same with a synchronous `SharedPreferences` read. If profiling ever shows it matters, the read can move to a `SharedPreferences` mirror written alongside the DataStore.

*Why the language is applied only at cold start:* that is the requested behaviour and makes the restart warning truthful. A language change writes the setting and nothing else; no activity recreation, no `Configuration` juggling mid-session.

*Alternative considered: AppCompat `setApplicationLocales` with `autoStoreLocales`.* It is the platform-recommended path and handles storage and activity contexts, but it applies immediately (contradicting the requested restart model), requires the activity to become an `AppCompatActivity` plus the AppCompat dependency and theme, and still does not cover receiver contexts below Android 13. Rejected for this change; the open question records when to revisit.

*Alternative considered: framework `LocaleManager` on Android 13+.* Introduces a second persisted source of truth (the system's per-app locale) that the system Settings page can change behind the app's back. Rejected in favour of one setting.

### D4. System language changes while "System default" is selected

When the phone language changes, Android recreates the activity with a new configuration and resets `Locale.getDefault()`. `MainActivity.attachBaseContext` runs again; if the stored setting is `SYSTEM` the resolver re-runs against the new system list and `AppLocale.current` and `Locale.setDefault` are refreshed. If a fixed language is stored, the app keeps it. `PillsnerApplication.onConfigurationChanged` performs the same refresh so receivers that run without an activity also see the right locale.

### D5. Settings screen structure

`SettingsScreen` becomes a `LazyColumn` (`Spacing.screenEdge` side padding, `Spacing.contentMaxWidth` cap) with the title "Settings" in `displayLarge` with `heading()` semantics (a top-level destination has no app bar, design system 8.7) followed by section composables, each with a `headlineSmall` header (group headers, section 3.2) and `Spacing.xl` between sections. This change adds `LanguageSection` as the first section. `app-login`'s `SecuritySection` follows it; if `app-login` was applied first and already created this structure, the Language section is inserted above Security. Each section is a self-contained composable with its own view model or state so sections can be added by future changes without touching each other.

### D6. Language section UI and the restart warning

- An `ExposedDropdownMenuBox` (read-only `OutlinedTextField` per design system 8.11 with menu) labelled "Language". Options, in order: "System default" (translated), then each supported language by its native name: "English", "Nederlands". Native names are literals in a non-translatable array resource, not translated strings.
- Selecting an option calls `viewModel.onLanguageSelected` which writes through `LanguageRepository` immediately.
- Beneath the dropdown, when `stored != inEffect`, a restart notice row: the `info` icon and the text "Restart Pillsner to apply the new language" in `bodyMedium`, on a `secondaryContainer` surface in `shapes.small` with `onSecondaryContainer` content. Design system section 2.4 reserves the `error` role for things that are wrong (overdue doses, undeliverable reminders, destructive confirmations, empty stock, validation errors); a pending restart is information, and blue means information. `inEffect` is the language resolved at process start, exposed by `AppLocale`. After a restart the two match and the notice disappears. Choosing back the language that is in effect also hides it. The notice row has `liveRegion = LiveRegionMode.Polite` so TalkBack announces it when it appears. The spec keeps calling it a "warning" for its behaviour; its colour is informational.

```kotlin
data class LanguageSectionState(
    val selected: AppLanguage,
    val options: List<AppLanguage>,        // SYSTEM + SupportedLanguages.all
    val restartRequired: Boolean,
)
```

*Why not a "Restart now" button:* not requested; a process kill from inside the app is abrupt and the user might be mid-flow elsewhere. Listed as an open question.

### D7. Translations and completeness guard

- `values-nl/strings.xml` and `values-nl/plurals` mirror every key in `values/`. Strings that must not be translated (native language names, the app name) get `translatable="false"`.
- Lint: `MissingTranslation` and `ExtraTranslation` set to `error` in `lint.xml`; `lint` already runs before a change is declared done per `CLAUDE.md`, so a change adding an English string without Dutch fails.
- `android.defaultConfig.resourceConfigurations += listOf("en", "nl")` strips other locales from AndroidX resources, which also guarantees the fallback chain is `values-nl` → `values` (English).
- Plurals and format arguments: Dutch plurals use `one`/`other` like English; `QuantityFormatter` and the weekday/time formatters take their locale from `Locale.getDefault()`, which D3 sets, so their tests run with an explicit locale parameter.

### D8. Dependency injection

`AppContainer` gains `languageRepository`. Because `PillsnerApplication.onCreate` needs it before anything else, the container is constructed first and the language resolved immediately after. `SettingsViewModel` (or a dedicated `LanguageSectionViewModel`) is created by the shared factory.

### D9. Package layout

```
domain/model/AppLanguage.kt              AppLanguage, SupportedLanguages
domain/locale/LocaleResolver.kt
domain/repository/LanguageRepository.kt
data/settings/SettingsDataStore.kt       the `settings` Preferences DataStore
data/settings/DataStoreLanguageRepository.kt
ui/locale/AppLocale.kt                   current, wrap(), refresh()
ui/settings/SettingsScreen.kt            LazyColumn of sections
ui/settings/language/LanguageSection.kt, LanguageSectionState.kt, LanguageSectionViewModel.kt
res/values/strings.xml                   + language strings
res/values-nl/strings.xml                full Dutch translation
res/values/arrays.xml                    language native names (non-translatable)
lint.xml                                 MissingTranslation, ExtraTranslation = error
```

## Risks / Trade-offs

- [Blocking read at startup] → One tiny file, read once; measured on a low-end device in the manual test. Mirror to `SharedPreferences` if it ever exceeds a few milliseconds.
- [A receiver forgets to wrap its context and posts an English notification on a Dutch phone] → `ReminderNotifier` receives its context through `AppLocale.wrap` in one place; the instrumented notification test asserts the Dutch title when Dutch is stored. A lint rule is not attempted.
- [Translations drift or are machine-quality] → Lint enforces completeness, not quality. Dutch copy in this change is written by hand and reviewed by the product owner, who is a native speaker. Wording follows the glossary: medicijn, dosis, inname, herinnering, schema.
- [Three unarchived changes with chained navigation deltas] → Archive order documented in the proposal and tasks; if `app-login` lands first with its own Settings screen, task 4.1 adapts by inserting the Language section rather than creating the screen.
- [Warning hides if the user toggles away and back before restart] → Correct by design: the setting equals the language in effect, so no restart is needed.
- [Dutch strings are longer than English] → Tiles, the navigation labels and the notification text are checked at 200 percent font scale in Dutch in the manual test. Design system section 10 forbids truncation anywhere but the app bar title, so navigation labels are kept short (Home, Medicijnen, Instellingen) and verified to fit without `maxLines = 1` or ellipsis; a label that does not fit is reworded, not clipped.

## Migration Plan

Additive. No stored setting exists before this change, which the resolver treats as System default. No Room change. Rollback is removing the Language section, the `settings` DataStore and the context wrapping; `values-nl/` can stay harmlessly.

## Open Questions

- Should a later change apply the language immediately (activity recreation) instead of on restart, or add a "Restart now" button under the warning? Recommended: revisit after the first usability round; the platform supports both.
- Should the app declare `android:localeConfig` so Android 13+ users can also pick the language in system Settings? Only sensible if the in-app setting and the system setting are reconciled; deferred.
- Which Dutch wording for "dose": "dosis" is used throughout; confirm with the product owner during review.
