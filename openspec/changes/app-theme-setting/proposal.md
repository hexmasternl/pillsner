## Why

Pillsner's light and dark schemes are both finished designs, but the user has no say over which one they see: the app follows the phone and nothing else. People who keep their phone in dark mode but want a bright, high-contrast medicine list — or the reverse — have no way to ask for it, and on a device without a system dark mode schedule the only route is to change the whole phone.

A theme choice is also the least risky setting Pillsner can offer: it touches no medication data, no scheduling and no permission. It changes which of two existing colour schemes is handed to `MaterialTheme`.

## What Changes

- The Settings screen gains a **Theme** section offering three options: **System default** (follow the phone), **Light** and **Dark**. System default is what an installed app has until the user chooses otherwise, so behaviour is unchanged for anyone who never opens the setting.
- The choice is stored on the device in the existing general settings store, alongside the language, and survives restart.
- The choice applies **immediately**, with no restart and no flicker: unlike the language, a colour scheme is only a recomposition. The Settings screen the user is looking at repaints as they pick.
- While System default is selected, a change to the phone's dark mode continues to be followed live. While Light or Dark is selected, the phone's setting is ignored.
- Status and navigation bar appearance follow the chosen theme, not the phone, so a light app on a dark phone still gets dark icons in the system bars.
- **BREAKING (design system):** `docs/design-system.md` principle 4 currently reads "The system's theme is the app's theme" and its Do/Don't table forbids adding a theme toggle without a proposal. This is that proposal. Both are reworded: the system theme becomes the *default*, and the user's stored choice becomes what `PillsnerTheme` renders. Dynamic colour stays off and the two palettes are untouched — this change adds no colour.

Out of scope, deliberately:

- **The watch.** `WearTheme` is the dark palette because a watch face is always dark; a phone theme choice does not travel to it, and `wearable-sync` is not touched.
- **Reminder notifications.** The system renders them in its own theme. Nothing in this change alters that, and nothing should.
- **A scheduled or battery-saver-linked theme.** Three fixed options only. Anything time-based is a separate decision.

## Capabilities

### New Capabilities

- `app-theme`: the theme options, how the choice is stored, how it is resolved at startup and at runtime, what the Settings section looks like, and how it interacts with the phone's own dark mode.

### Modified Capabilities

- `welcome-screen`: the "Visual baseline from the design system" requirement states that the light and dark schemes follow the system setting. That becomes: they follow the user's stored theme choice, which defaults to the system setting. The dark-mode scenario is restated in those terms, and the "dynamic colour is off" rule is untouched.

## Impact

Code, all under `src/app/src/main/java/nl/hexmaster/pillsner`:

- `domain/model/AppTheme.kt` — new: the three options and the pure rule that turns a choice plus the phone's dark mode into "dark or light". No Android dependency, unit-tested.
- `domain/repository/ThemeRepository.kt` — new: observe and set, mirroring `LanguageRepository`.
- `data/settings/DataStoreThemeRepository.kt` — new: one key in the existing `settings` DataStore. No Room, no schema, no migration.
- `di/AppContainer.kt` — registers the repository and the new view model.
- `MainActivity.kt` — collects the choice and passes `darkTheme` into `PillsnerTheme`.
- `ui/theme/Theme.kt` — the `darkTheme` parameter already exists; its default and its KDoc change, and the system-bar side effect keeps following it.
- `ui/settings/theme/` — new: `ThemeSection` and `ThemeSectionViewModel`, following `LanguageSection` in shape and in accessibility.
- `ui/settings/SettingsScreen.kt` — one more section, and one more pair of parameters.
- `ui/PillsnerApp.kt` — passes them through.
- `res/values/strings.xml` and `res/values-nl/strings.xml` — section header, field label and three option labels, in both languages, or lint fails.

Documentation: `docs/design-system.md` principle 4, the Do/Don't table and the `Theme.kt` row in the Compose implementation table.

No new dependency, no new permission, no network, no change to the database, the alarm scheduling or the app lock. `app-settings-reset` is in flight and states that a reset keeps the language, the lock and the accepted documents; the theme lives in the same settings store and is likewise untouched by a reset, but that change's dialog copy is its own to update and is not edited here.
