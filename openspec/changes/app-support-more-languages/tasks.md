## 1. Domain layer

- [ ] 1.1 Add `GERMAN("de")`, `FRENCH("fr")`, `SPANISH("es")` and `PORTUGUESE("pt")` entries to `AppLanguage` in `src/app/src/main/java/nl/hexmaster/pillsner/domain/model/AppLanguage.kt`.
- [ ] 1.2 Add the four new entries to `SupportedLanguages.all`, in the order English, Dutch, German, French, Spanish, Portuguese.
- [ ] 1.3 Update or add unit tests for `LocaleResolver` covering: a supported new-language phone tag being picked under System default, a regional variant of a new language (e.g. `pt-BR`) resolving to the base language, and an unsupported language still falling back to English with the larger list in place.
- [ ] 1.4 Update any existing test that asserts the supported-language count or exact list (e.g. Settings/dropdown option tests) to expect six languages.

## 2. Phone app resources — native names

- [ ] 2.1 Add "Deutsch", "Français", "Español", "Português" to the `language_names` string-array in `src/app/src/main/res/values/arrays.xml`, in the same order as `SupportedLanguages.all`.

## 3. Phone app resources — German

- [ ] 3.1 Create `src/app/src/main/res/values-de/strings.xml` translating every key in `values/strings.xml` (mirroring `values-nl/strings.xml` for coverage), including the Disclaimer and Terms of Service text.
- [ ] 3.2 Review the German translation for tone and correctness against the English and Dutch versions.

## 4. Phone app resources — French

- [ ] 4.1 Create `src/app/src/main/res/values-fr/strings.xml` translating every key in `values/strings.xml`, including the Disclaimer and Terms of Service text.
- [ ] 4.2 Review the French translation for tone and correctness against the English and Dutch versions.

## 5. Phone app resources — Spanish

- [ ] 5.1 Create `src/app/src/main/res/values-es/strings.xml` translating every key in `values/strings.xml`, including the Disclaimer and Terms of Service text.
- [ ] 5.2 Review the Spanish translation for tone and correctness against the English and Dutch versions.

## 6. Phone app resources — Portuguese

- [ ] 6.1 Create `src/app/src/main/res/values-pt/strings.xml` translating every key in `values/strings.xml`, including the Disclaimer and Terms of Service text.
- [ ] 6.2 Review the Portuguese translation for tone and correctness against the English and Dutch versions.

## 7. Wear app resources

- [ ] 7.1 Create `src/wear/src/main/res/values-de/strings.xml`, `values-fr/strings.xml`, `values-es/strings.xml` and `values-pt/strings.xml`, each translating every key in `src/wear/src/main/res/values/strings.xml` (mirroring `values-nl/strings.xml`).
- [ ] 7.2 Review the four wear translations for tone and correctness.

## 8. UI verification

- [ ] 8.1 Run the `pillsner-ui-review` skill (or agent) against the Settings language section and any screen with fixed-width text elements (chips, buttons, bottom navigation) to check for truncation in the new languages, especially German.
- [ ] 8.2 Manually verify (or add a semantics test for) the Settings dropdown listing all six native names in the correct order, and the restart warning text in at least one new language.

## 9. Build and verification

- [ ] 9.1 Run the phone app's unit test task and confirm the `LocaleResolver` and language-list tests pass.
- [ ] 9.2 Run lint for the phone app and confirm no missing/extra-translation errors for `de`, `fr`, `es`, `pt` (or any existing language).
- [ ] 9.3 Run lint/build for the wear app and confirm no missing/extra-translation errors for the four new languages.
- [ ] 9.4 Run instrumented tests if any exercise the language dropdown or notification text, per the project's instrumented-test guidance for UI changes.

## 10. Spec and documentation sync

- [ ] 10.1 Confirm the delta specs for `app-language`, `app-legal` and `wearable-app` in this change accurately describe the shipped behaviour before archiving.
- [ ] 10.2 Check whether `README.md` or `docs/design-system.md` mention the supported-language set and update them if so.
