## 1. Root-cause the German string resolution failure

- [ ] 1.1 Reproduce `aGermanPhoneReadsGermanWithoutAnyChoice` as the sole test in the instrumented run and confirm it still fails (already done once; re-confirm on the branch this change is implemented on).
- [ ] 1.2 Log or assert `AppLocale.wrap(context).resources.configuration.locales` right after `wrap` runs, to see what locale the created context actually reports holding.
- [ ] 1.3 Try building the `Configuration` passed to `createConfigurationContext` from scratch (`Configuration()`) instead of copying `context.resources.configuration`, to rule out an inherited qualifier.
- [ ] 1.4 Try `LocaleList.forLanguageTags("de")` directly instead of round-tripping through `Locale.forLanguageTag(tag)`, to rule out a `Locale` construction difference.
- [ ] 1.5 Check whether the debug/androidTest build variant has any language-based resource/APK splitting enabled that could leave German resources out of the installed test APK.
- [ ] 1.6 Once the cause is found, update `design.md`'s Decisions section with the confirmed root cause before writing the fix, per `CLAUDE.md`'s rule to keep the design in sync with what implementation finds.

## 2. Fix

- [ ] 2.1 Apply the fix in whichever place task 1 identifies as the actual cause (`AppLocale.wrap`, `AppLocale.apply`, the test itself, or build configuration).
- [ ] 2.2 If the fix changes `AppLocale`'s public behaviour in any way, check every other caller of `AppLocale.wrap`/`apply` (reminder notifications, other locale-sensitive code named in `app-language`'s "Everything follows the app language" requirement) for correctness, not just the failing tests.

## 3. Verification

- [ ] 3.1 Run `LanguageSectionTest` in isolation and confirm all tests pass, including `aGermanPhoneReadsGermanWithoutAnyChoice` and `theRestartNoticeIsTranslatedIntoANewLanguage`.
- [ ] 3.2 Run the full instrumented suite (`./gradlew :app:connectedDebugAndroidTest`) and confirm no other test regresses, especially other locale-sensitive tests (Dutch, French, Spanish, Portuguese paths).
- [ ] 3.3 Run the full unit test suite and lint.
- [ ] 3.4 Update the GitHub issue (#8) with the confirmed root cause and close it once the fix is verified.
