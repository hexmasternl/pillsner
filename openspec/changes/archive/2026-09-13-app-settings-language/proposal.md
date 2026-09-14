## Why

Pillsner's first users are Dutch and English speakers, and the reminder text, screens and dose descriptions are all English-only string resources so far. People should be able to read a medication reminder in their own language, and the app should pick that language sensibly on its own while still letting the user override it. Doing this now, while the number of strings is small, is far cheaper than retrofitting translations across a finished app.

## What Changes

- The app SHALL be **available in English and Dutch**. Every user-facing string resource gets a Dutch translation, and the project treats a missing translation as a build error so future changes cannot ship English-only strings.
- **Language section on the Settings screen.** A dropdown shows the currently selected language and offers: *System default*, *English*, *Nederlands*. Language names are shown in their own language so a user who cannot read the current one still finds theirs.
- **Restart notice.** Changing the selection saves it immediately and shows a warning beneath the dropdown that Pillsner needs to be restarted for the new language to take effect. The warning stays visible until the app has been restarted and the new language is in effect. The user's request is that the language applies after a restart; the platform can also switch immediately, and that is recorded as an open question rather than done here.
- **Startup resolution.** On every cold start the app reads the stored language setting. If a language is stored, it is used. If nothing is stored (System default), the phone's language list is consulted and the first supported language wins. If none of the phone's languages is supported, the app uses English.
- **Everything follows the app language**, not only screen text: dates and times on tiles, the weekday names in schedule descriptions, name sorting on the medicine overview, and reminder notifications built outside the activity (the `app-medicine-alarm` receivers). The change provides a single way to obtain a correctly localised context for such code.
- Adding a third language later means adding one `values-xx/` folder and one entry in the supported-language list.

## Capabilities

### New Capabilities
- `app-language`: Supported languages, the Language section on Settings, saving and showing the selection, the restart notice, startup resolution rules, the requirement that all text and locale-sensitive formatting follows the app language including notifications, and the translation-completeness rule.

### Modified Capabilities
- `app-navigation`: The "Placeholder Medicines and Settings destinations" requirement changes so that Settings shows the settings screen with its sections (Language first) instead of a title-only placeholder. This requirement is currently a delta inside the active `app-medicine-overview` change; archive order MUST be `app-welcome-screen`, `app-medicine-overview`, then this change. `app-login`'s Security section joins the same screen; whichever of the two is applied second adds its section beneath or above the existing one as this spec states.

## Impact

- **Application code (`src/`)**: a `settings` feature area gains the real Settings screen (a scrolling list of sections), the Language section with its dropdown and warning, a `SettingsViewModel`. The domain gains `AppLanguage`, the supported-language list, and a pure `LocaleResolver` that implements the startup rules. The data layer gains a `LanguageRepository` on a general `settings` Preferences DataStore (separate from `app-login`'s `applock` file, which stays excluded from backup; the language choice is harmless and may be backed up). `PillsnerApplication` resolves the language at startup and sets the process default locale; `MainActivity` wraps its base context; a `localizedContext()` helper is provided for receivers and notification builders.
- **Resources**: `values-nl/strings.xml` for every string in the app at the time this change is applied. `resourceConfigurations` limited to `en` and `nl` so library resources for other locales are stripped. The lint check for missing translations is set to error severity.
- **Dependencies** (first party): `androidx.datastore:datastore-preferences` (already planned by `app-login`; added here if absent). No AppCompat, no third-party libraries, no network.
- **Other changes**: `app-medicine-alarm` must build notification text through the localised context helper introduced here; a note is added to its design when this change is applied first, or a task is added here if the alarm change is already applied. `app-login` places its Security section on the Settings screen introduced here.
- **Tests**: unit tests for `LocaleResolver` (stored language wins, first supported system language wins, unsupported falls back to English, region variants such as `nl-BE` and `en-GB` match their language); DataStore repository tests; Compose tests for the dropdown, the saved selection and the warning's appearance and disappearance; an instrumented test that a freshly started activity resolves resources in the stored language; a resource test that `values` and `values-nl` contain the same keys.
- **README**: "Features" gains a line stating the app is available in English and Dutch and follows the phone language by default.

## Non-goals

- Applying the language change without a restart. Requested behaviour is restart-based; see Open Questions in the design.
- Integrating with the Android 13+ per-app language setting in system Settings. Keeping one source of truth (the in-app setting) is simpler; this can be added later.
- Translating medicine names, units entered by the user, or any user data.
- Right-to-left layout support. Neither supported language needs it; Compose handles it when a RTL language is added.
- Regional variants as separate choices (Belgian Dutch, British English). A single Dutch and a single English translation serve both.
