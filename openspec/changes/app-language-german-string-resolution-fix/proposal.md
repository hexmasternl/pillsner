## Why

Two instrumented tests in `LanguageSectionTest` fail consistently, both in isolation and inside the
full instrumented suite: `aGermanPhoneReadsGermanWithoutAnyChoice` and
`theRestartNoticeIsTranslatedIntoANewLanguage`. In both, `AppLocale` correctly resolves German
(`AppLocale.inEffect == AppLanguage.GERMAN` passes), but reading a string back through
`AppLocale.wrap(context).getString(...)` still returns the English resource instead of the German
one. The `values-de` resources themselves are present and correct, and the equivalent Dutch path
(`aDutchPhoneReadsDutchWithoutAnyChoice`) passes, so this is not a missing translation — the app is
failing the `app-language` capability's own "Language resolution at startup" requirement, which
already documents German scenarios as required behaviour. Tracked as GitHub issue #8.

## What Changes

- Investigate why `AppLocale.wrap(context)` returns English resources for German specifically,
  while an otherwise-identical Dutch resolution succeeds. Candidate causes to rule in or out:
  Android's `Resources`/`AssetManager` caching a `Configuration`-keyed `Resources` instance across
  calls within one process (instrumented tests share one app process for the whole run and mutate
  the `AppLocale` singleton directly), a difference in how `createConfigurationContext` merges
  `LocaleList` versus the base configuration for `de` specifically, or a test-order dependency
  within `LanguageSectionTest` itself.
- Fix whatever the root cause turns out to be in `AppLocale`, `LanguageSectionTest`, or both (if the
  bug is real production behaviour, fix `AppLocale`/`wrap`; if it is purely a test-process artifact
  of a shared singleton across instrumented test methods, fix the test's isolation instead — this is
  a bug-fix change, so the design step is where that determination gets made and recorded before any
  code changes).
- No new product behaviour: German is already a supported, specified language
  (`app-language`'s "Supported languages" and "Language resolution at startup" requirements). This
  change makes the existing, already-agreed behaviour actually hold for German.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

(none expected — this restores compliance with the existing `app-language` "Language resolution at
startup" requirement rather than changing what it requires. If root-causing the bug reveals that the
existing requirement's scenarios need clarifying to prevent this regression recurring, the design
step will say so explicitly and a delta will be added then.)

## Impact

- **Code**: `ui/locale/AppLocale.kt` and/or
  `app/src/androidTest/java/nl/hexmaster/pillsner/ui/settings/language/LanguageSectionTest.kt`,
  scope to be confirmed once root-caused.
- **Tests**: `LanguageSectionTest` is the regression signal; a fix must make
  `aGermanPhoneReadsGermanWithoutAnyChoice` and `theRestartNoticeIsTranslatedIntoANewLanguage` pass,
  both in isolation and as part of the full instrumented suite, without weakening either assertion.
- **No dependency, permission, or user-facing behaviour change.**
- Related: GitHub issue #8.
