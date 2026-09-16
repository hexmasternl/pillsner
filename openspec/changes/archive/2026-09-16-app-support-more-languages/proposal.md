## Why

Pillsner currently reads in English and Dutch only. Reminders are the core promise of the app, and a person who cannot comfortably read the app's language is more likely to miss or mis-record a dose. German, French, Spanish and Portuguese are widely spoken in markets Pillsner is reasonable to reach next, so extending the existing language mechanism to cover them removes a real adoption barrier without adding any new architecture — `app-language` was already built so that adding a language is "add resources, add it to the list."

## What Changes

- Add German (`de`), French (`fr`), Spanish (`es`) and Portuguese (`pt`) to the phone app's supported-language list (`SupportedLanguages.all` in the domain layer), each with its own native name ("Deutsch", "Français", "Español", "Português") shown untranslated in the Settings language dropdown.
- Add a complete `values-de`, `values-fr`, `values-es` and `values-pt` translation of every string resource in the phone app (`src/app/src/main/res`), including the Disclaimer and Terms of Service text, so lint's missing-translation check covers all six languages.
- Add the same four translations to the wear app's string resources (`src/wear/src/main/res`), so the watch can render in whichever of the six languages the phone publishes.
- Extend `LocaleResolver`'s system-language matching and the phone-to-watch language tag handling to the four new tags; no change to the resolution algorithm itself, since it already iterates `SupportedLanguages.all`.
- Update the `app-language`, `app-legal` and `wearable-app` specs' "Supported languages" / "translation completeness" requirements to name six languages (English, Dutch, German, French, Spanish, Portuguese) instead of two.

Out of scope: the marketing website's own i18n (`src/website`), which is a separate capability already tracked outside this change.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `app-language`: "Supported languages" grows from {English, Dutch} to {English, Dutch, German, French, Spanish, Portuguese}; the dropdown's native-name list and ordering, and the "Translation completeness" requirement, extend to the four new languages.
- `app-legal`: "The legal text is translated" requirement extends from English-and-Dutch to all six supported languages.
- `wearable-app`: "language published by the phone" and its translation-completeness requirement extend to all six languages; watch strings need German, French, Spanish and Portuguese translations alongside English and Dutch.

## Impact

- `src/app/src/main/java/nl/hexmaster/pillsner/domain/model/AppLanguage.kt` — four new enum entries and an extended `SupportedLanguages.all`.
- `src/app/src/main/res/values/arrays.xml` — four new native names in `language_names`.
- `src/app/src/main/res/values-de`, `values-fr`, `values-es`, `values-pt` (new) — full string and legal-text translations, mirroring `values-nl`.
- `src/wear/src/main/res/values-de`, `values-fr`, `values-es`, `values-pt` (new) — full string translations, mirroring `values-nl`.
- Unit tests for `LocaleResolver` and any tests that assert the supported-language count or list.
- `openspec/specs/app-language/spec.md`, `openspec/specs/app-legal/spec.md`, `openspec/specs/wearable-app/spec.md` — delta specs for the requirement changes above.
- No dependency, schema, permission or network changes.
