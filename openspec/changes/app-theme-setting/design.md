## Context

`PillsnerTheme` in `ui/theme/Theme.kt` already takes the parameter this change needs:

```kotlin
@Composable
fun PillsnerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```

It picks `DarkColorScheme` or `LightColorScheme`, provides `LocalPillsnerDarkTheme` so tiles and previews agree with it, and runs a `SideEffect` that sets the status and navigation bar icon appearance from the same boolean. Every screen is inside one call to it, in `MainActivity.onCreate`. So the mechanism is in place, and untested only in the sense that nothing has ever passed a value other than the default.

The Settings screen is a `LazyColumn` of self-contained sections — Language, Security, Legal, About — each added by appending an `item`. Language is the closest sibling to what this change adds: an `AppLanguage` enum where `SYSTEM` is a null tag rather than a language, a `LanguageRepository` port, a `DataStoreLanguageRepository` writing one string key into the shared `settings` Preferences DataStore, a `LanguageSectionViewModel` mapping the stored value into a UI state, and a `LanguageSection` rendering an `ExposedDropdownMenuBox`. Following that shape costs nothing and makes the two settings read the same way.

One thing does **not** carry over. The language is resolved once per process in `PillsnerApplication.onCreate` and then frozen, because `MainActivity.attachBaseContext` bakes the locale into the activity's resources before anything composes — which is why the Language section has to show a restart notice. A colour scheme has no such anchor. It is an argument to a composable, so changing it is a recomposition and nothing more.

Constraints from `CLAUDE.md`: the domain layer has no Android dependencies; user-facing text is a string resource with a Dutch translation; every visual decision is a theme token; no new dependency; nothing leaves the device. And `docs/design-system.md` currently states the opposite of what is being built — principle 4 ("The system's theme is the app's theme") and a Don't row ("Add a theme toggle without a proposal"). Changing it is part of the work, not a side effect of it.

## Goals / Non-Goals

**Goals:**

- Three options — follow the phone, always light, always dark — with follow-the-phone as the default, so nothing changes for a user who never opens the setting.
- The choice takes effect the instant it is made, on the screen the user is looking at. No restart notice, because there is nothing to wait for.
- No flash of the wrong scheme on cold start. A user who chose Light never sees Deep Moss, not even for one frame.
- The system bars follow the app's theme, not the phone's.
- The resolution rule — choice plus phone state, to light or dark — lives in the domain layer and is unit-tested without a device.
- The design system document ends the change agreeing with the app.

**Non-Goals:**

- Adding, changing or reweighting a single colour. Both palettes ship as they are.
- Dynamic colour. Still off, still forbidden.
- The watch. `WearTheme` stays the dark palette and `wearable-sync` gains no field.
- Notification appearance. The system themes the shade; Pillsner supplies content and an accent colour, and that does not change.
- Per-screen or time-scheduled themes, an AMOLED black variant, or a contrast setting.

## Decisions

### D1. `AppTheme`, a domain enum with the resolution rule attached

`domain/model/AppTheme.kt`:

```kotlin
enum class AppTheme(val key: String?) {
    SYSTEM(null),
    LIGHT("light"),
    DARK("dark"),
    ;

    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun ofKey(key: String?): AppTheme =
            entries.firstOrNull { it.key != null && it.key == key } ?: SYSTEM
    }
}
```

`SYSTEM` carries a null key for the same reason `AppLanguage.SYSTEM` carries a null tag: it is not a theme, it is an instruction to look elsewhere, and storing it as the absence of a key means an absent, empty or corrupt value all read as "follow the phone" without a special case.

`isDark` is the whole decision in one pure function with no Android import, so the three-by-two truth table is a plain unit test. The alternative — a `when` inlined in `MainActivity` — is three lines in a place that needs an instrumented test to reach, which is how a rule stops being tested.

Named `AppTheme`, matching `AppLanguage`, and placed in `domain/model` beside it.

### D2. One key in the existing `settings` DataStore

`ThemeRepository` in `domain/repository`, mirroring `LanguageRepository`:

```kotlin
interface ThemeRepository {
    fun observeTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
}
```

`DataStoreThemeRepository` writes a string key `theme` into `context.settingsDataStore` — the same general preferences file the language uses, whose KDoc already says later general settings belong there. Writing `SYSTEM` removes the key rather than storing a sentinel.

Alternatives: the `applock` DataStore (rejected — it is excluded from backup because it holds security material, and a theme is harmless enough to travel to a new phone); Room (rejected — one scalar does not earn a table, and a table drags a schema migration into every future preference); `SharedPreferences` (rejected — the app has one preferences mechanism and it is DataStore).

Because both repositories read the same `DataStore` instance, the second read at startup is served from DataStore's in-memory cache. Two repositories, one file read.

### D3. The choice applies immediately, with no restart notice

The Language section's restart notice exists because `attachBaseContext` has already run by the time the user picks. Nothing equivalent holds a colour scheme: `PillsnerTheme` reads `darkTheme` during composition, so a new value recomposes every screen under it, including the Settings screen the user is standing on, and the `SideEffect` re-runs to repaint the system bars.

This is worth stating as a decision because the obvious move is to copy the Language section wholesale, notice and all, and ship a warning about a restart that is not needed. The Theme section has no notice, and the specs say so explicitly so that a later reviewer does not "fix" it.

### D4. The theme is read before the first composition, in `AppContainer`

A view model that starts at a default and catches up when DataStore answers would paint one or more frames in the wrong scheme — a white flash on an OLED phone at night, which is exactly what the setting exists to prevent.

So the container exposes the choice as a `StateFlow` whose initial value is already correct:

```kotlin
val themeRepository: ThemeRepository = DataStoreThemeRepository(applicationContext)

/** The stored theme, correct from the first frame. */
val theme: StateFlow<AppTheme> = themeRepository.observeTheme()
    .stateIn(appScope, SharingStarted.Eagerly, runBlocking { themeRepository.observeTheme().first() })
```

`MainActivity` then collects it and hands it down:

```kotlin
setContent {
    val theme by container.theme.collectAsStateWithLifecycle()
    PillsnerTheme(darkTheme = theme.isDark(isSystemInDarkTheme())) { ... }
}
```

One blocking read of one small file, on the same startup path that already blocks once for the language and reads the same cached DataStore — the precedent, and its justification, are in `PillsnerApplication.applyStoredLanguage`. `AppContainer`'s KDoc warns that construction must stay cheap because a `BroadcastReceiver` may start the process with ten seconds for everything; this is a property initialiser measured in microseconds against that budget, and the container already launches work in `init`.

`isSystemInDarkTheme()` is read inside composition, so when `SYSTEM` is selected a change to the phone's dark mode flows through on its own — the activity is recreated for a `uiMode` configuration change and the value is re-read either way. When `LIGHT` or `DARK` is selected the same call is made and its answer discarded by `isDark`, which is what "the phone is ignored" means in code.

Alternatives: `AppCompatDelegate.setDefaultNightMode` (rejected — it pulls AppCompat into an app that has no AppCompat activity, and it works by recreating the activity, a visible jump for something that should be a repaint); a `ThemeViewModel` owned by `MainActivity` (rejected — a view model cannot be observed before `setContent` without the same flash, and this value is process-wide, not screen-scoped); a `staticCompositionLocalOf` (rejected — it would have to be provided above `PillsnerTheme`, which is the thing being configured).

### D5. The Settings section is a dropdown, like Language

`ui/settings/theme/ThemeSection.kt` and `ThemeSectionViewModel.kt`, built on `ExposedDropdownMenuBox` with an `OutlinedTextField` anchor, a header in `headlineSmall` marked `heading()`, and test tags in a `ThemeSectionTestTags` object — the same shape as `LanguageSection`, minus the restart notice.

`ThemeSectionState` is `selected` plus `options`; there is no third field, because there is no pending state to represent.

Alternatives: a `SingleChoiceSegmentedButtonRow` (rejected — three labels side by side is the control that breaks first at a 2x font scale, and the design system's component list in section 8 does not contain it, so it would need adding and defending); a column of `RadioButton` rows (rejected — legitimate and accessible, but it makes the Settings screen use two different controls for two adjacent single-choice settings). Consistency wins on a screen whose whole structure is "a column of sections that look alike".

Placement: **after** Language and before Security. Language comes first because it decides how everything below it reads; the theme is the other presentation setting and belongs next to it, above the sections about security and documents.

Copy: header "Theme", field label "Theme", options "System default", "Light", "Dark", with Dutch "Thema", "Systeemstandaard", "Licht", "Donker". "System default" repeats the Language section's wording for the same idea but gets its own string resource rather than sharing `settings_language_system_default` — a string shared across two settings makes a translator's job harder and breaks the day one of them needs different phrasing.

### D6. `PillsnerTheme` keeps its signature; only its default caller and its documentation change

`darkTheme: Boolean = isSystemInDarkTheme()` stays. That default is what every `@Preview` and every Compose test relies on to render both schemes through `@PreviewLightDark`, and it is still right for a caller with no user choice to hand — which is every caller except `MainActivity`.

What changes is the KDoc, which currently says the theme "follows the system light/dark setting" and must now say it renders what it is given, defaulting to the system. No behavioural change to `LocalPillsnerDarkTheme`, the system-bar `SideEffect` or `tileContainerColor`: they all key off `darkTheme`, so they follow the user's choice for free. That is the payoff for the parameter having been there from the start.

### D7. The design system document is edited in the same change

Three edits to `docs/design-system.md`, and no others:

1. **Principle 4** — from "*The system's theme is the app's theme.* Light and dark follow the OS setting." to a statement that the app offers System, Light and Dark, that System is the default and follows the OS, and that both schemes remain first-class designs rather than inversions of each other.
2. **The Do/Don't table** — the row "Follow the system light/dark setting | Add a theme toggle without a proposal" becomes "Render the user's stored theme choice, defaulting to the system setting | Add a fourth theme, a schedule or an AMOLED variant without a proposal".
3. **The `Theme.kt` row** in the Compose implementation table — records that `MainActivity` passes the stored choice and that the default remains `isSystemInDarkTheme()`.

Principle 5, the dynamic-colour ban, section 2.2 and every palette row are untouched, and the change must be able to say so.

### D8. What gets tested, and where

- **Unit**, no Android: `AppTheme.isDark` across all three options against both phone states; `AppTheme.ofKey` for each key, for null, for empty and for an unrecognised value; `ThemeSectionViewModel` mapping a stored value into state and writing a selection through a fake repository.
- **Instrumented**, `androidTest`: `DataStoreThemeRepository` round-trips each option and reads `SYSTEM` from a fresh store and from a corrupt value — mirroring `DataStoreLanguageRepositoryTest`, and instrumented for the same reason, that DataStore wants a real `Context`.
- **Compose**: `ThemeSectionTest` — the dropdown shows the stored option, opening it offers three, choosing one reports it, and no restart notice appears. Plus one test for what the change is actually for: a composable under `PillsnerTheme` with `DARK` selected while the system is light resolves to the dark scheme, which `LocalPillsnerDarkTheme` makes directly assertable.
- **Existing tests**: the `SettingsScreen` preview and any test constructing `SettingsScreen` gain the two new parameters. Nothing else should need touching; if a screenshot test does, that is a signal the section landed in the wrong place.

## Risks / Trade-offs

**A blocking read on the startup path** → One key from a Preferences DataStore file that the language read has already pulled into memory in the same process. The same trade was made, and justified in a comment, for the language; the alternative is a visible flash of the scheme the user asked not to see. If the read ever becomes measurable, the fix is one `first()` shared between the two repositories, not a different architecture.

**`AppContainer` construction is on a ten-second receiver budget** → The read is a property initialiser, not work launched in `init`, and it is bounded by the size of one small file. Worth keeping in mind only because every future preference added to this container makes the same argument, and at some point the answer is a single combined read.

**A Light theme on a dark phone gets light system bars in recents and the task switcher** → The `SideEffect` sets bar icon appearance from `darkTheme`, so the bars are right while the app is in front. The system's own chrome around it is not the app's to theme, and that is the accepted cost.

**The watch diverges from the phone** → Deliberate, documented in the proposal and in `WearTheme`'s KDoc, which already says the watch is dark because a watch face is always dark. The risk is a user expecting the choice to travel; nothing in the phone UI suggests it does.

**The design system and the app drift apart** → Mitigated by D7 being a task in this change rather than a follow-up, and by `pillsner-ui-review` running before the UI task is called done.

**A future setting copies the restart notice by pattern-matching on Language** → The specs state that the theme applies immediately and that no restart notice is shown, so it is a requirement a reviewer can point at rather than a preference.

## Open Questions

None blocking. One to hand on: `app-settings-reset` is in flight and its dialog names what a reset keeps — the language, the app lock and the accepted documents. The theme sits in the same settings store and is kept for the same reason, so that change may want to name it too. It is that change's copy to decide, and this one does not edit it.
