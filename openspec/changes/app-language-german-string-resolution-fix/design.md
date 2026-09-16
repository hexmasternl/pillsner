## Context

`LanguageSectionTest` has two consistently failing instrumented tests:
`aGermanPhoneReadsGermanWithoutAnyChoice` and `theRestartNoticeIsTranslatedIntoANewLanguage`. Both
force German through `AppLocale.apply(...)`, correctly get `AppLocale.inEffect == AppLanguage.GERMAN`
back, and then read a string through `AppLocale.wrap(context).getString(...)` — which returns the
English resource instead of the German one. `values-de/strings.xml` has the correct entries.

Diagnostics already run while investigating this (see GitHub issue #8), so the tasks below start
from here rather than re-deriving it:

- **Not full-suite test-order leakage.** Reproduces identically running only `LanguageSectionTest`
  in isolation (`-Pandroid.testInstrumentationRunnerArguments.class=...LanguageSectionTest`).
- **Not intra-class test-order leakage either.** Reproduces running only the single failing method
  (`...LanguageSectionTest#aGermanPhoneReadsGermanWithoutAnyChoice`) as the *only* test executed in
  the whole instrumented run — a cold app process, one test method, still fails the same way.
- **Not a missing/wrong translation.** `values-de/strings.xml` has both strings, correct.
- **Not the resolver.** `AppLanguage.GERMAN.tag == "de"`, `LocaleResolver.resolve` and
  `AppLocale.apply` are structurally identical to the Dutch path, which passes
  (`aDutchPhoneReadsDutchWithoutAnyChoice` is green), and the test's own assertion that
  `AppLocale.inEffect == AppLanguage.GERMAN` passes — only the *string lookup* is wrong, not the
  language resolution.

This leaves the remaining suspects narrowly scoped to `AppLocale.wrap(context)` itself
(`ui/locale/AppLocale.kt:48-52`) and how `Context.createConfigurationContext` /
`Resources`/`AssetManager` resolve a `de`-only (no region) `Configuration` on this device/API level
(reproduced on a `Pixel_9`, API 35, AVD) — something specific to requesting `"de"` rather than
`"nl"`, given the two code paths are otherwise identical.

## Goals / Non-Goals

**Goals:**
- Root-cause why `AppLocale.wrap(context)` resolves English resources for a German configuration
  specifically, with a reproducible, isolated minimal case (not just "the test passes now").
- Fix it so both failing tests pass, without weakening either assertion, and without regressing the
  Dutch/other-language paths that already pass.
- Determine whether the bug is in production code (`AppLocale`) or is an artifact of how the test
  harness exercises a process-wide singleton, and fix whichever it actually is.

**Non-Goals:**
- Adding new languages or changing which languages are supported (`app-language`'s "Supported
  languages" requirement is unaffected).
- Changing the "Language resolution at startup" requirement's documented behaviour — this restores
  compliance with it, not changes it.

## Decisions

Root cause not yet confirmed as of writing this design — the diagnostics above narrow it but do not
pin it down. Rather than guess, the first task in `tasks.md` is a further, targeted investigation
step before any fix is written, with concrete next probes to run (in order, cheapest first):

1. **Inspect the resulting `Resources` directly.** In a throwaway instrumented test (or temporarily
   in the failing test), after calling `AppLocale.wrap(context)`, log/assert
   `wrapped.resources.configuration.locales` to see what locale the created context actually reports
   holding — if it already reports `de` but still serves the English string, the bug is in resource
   lookup itself (an `AssetManager`/APK-splitting issue), not in configuration construction.
2. **Try building the `Configuration` from scratch** instead of copying `context.resources.configuration`
   (`Configuration().apply { setLocales(LocaleList(current)) }`) to rule out an inherited flag or
   qualifier from the ambient configuration interfering only for `de`.
3. **Try `LocaleList.forLanguageTags("de")` directly**, bypassing the `java.util.Locale` round-trip
   (`Locale.forLanguageTag(tag)` then wrapped in `LocaleList(current)`), in case something in that
   round-trip produces a `Locale` that is `equals`-identical to `Locale.GERMAN` but not resolved the
   same way internally by `AssetManager`.
4. **Check whether App Bundle / language-split packaging is involved** for the debug/androidTest
   build variant used on the emulator — if resource splitting by language is enabled for any
   variant, a split APK missing from the installed test app would produce exactly this symptom
   (falls back to the base/English resources) and would need a build-config fix, not an `AppLocale`
   code change.

Once one of these isolates the actual cause, record the finding here (updating this section, per
`CLAUDE.md`'s rule to keep the design in sync with what implementation finds) before writing the
fix, so the fix in `tasks.md` targets the real cause rather than the closest plausible one.

### Confirmed root cause

Probe 4 (build/packaging config) found it directly, which made probes 1–3 (all about how `AppLocale`
constructs the `Configuration`/`LocaleList` it hands to `createConfigurationContext`) moot — the bug
is not in `AppLocale` at all.

`app/build.gradle.kts`'s `defaultConfig` sets:

```kotlin
resourceConfigurations += listOf("en", "nl")
```

with the comment "The two languages the app ships. Strips every other locale from library resources,
and makes the fallback chain exactly values-nl to values (English)." That comment is stale: the app
now ships six languages (`SupportedLanguages.all` — English, Dutch, German, French, Spanish,
Portuguese — and the dropdown already offers all six), but `resourceConfigurations` was never updated
past the original two. `resourceConfigurations` is an `aapt`-level filter applied to every variant,
debug and androidTest included, not just release/bundle splitting — it strips the *entire*
`values-de` (and `values-fr`, `values-es`, `values-pt`) directory out of the packaged APK at build
time, regardless of what device or configuration reads it afterwards.

Confirmed empirically, not just inferred from the config: `./gradlew :app:assembleDebug` followed by
`aapt2 dump configurations app/build/outputs/apk/debug/app-debug.apk` lists only `nl` as a packaged
language qualifier — no `de`, `fr`, `es` or `pt` — alongside the unqualified (English) default. And
running `LanguageSectionTest#aGermanPhoneReadsGermanWithoutAnyChoice` alone against that build
reproduces exactly the reported symptom: `AppLocale.inEffect == AppLanguage.GERMAN` (resolution is
correct) but `AppLocale.wrap(context).getString(...)` returns `"Settings"` instead of
`"Einstellungen"` (`org.junit.ComparisonFailure: expected:<[Einstellungen]> but was:<[Settings]>`) —
because `values-de/strings.xml` was never installed on the device to begin with. `AssetManager` falls
back to the default (English) resources exactly as it would for any locale whose resources are
absent, which is also why `AppLocale.inEffect` is unaffected: language *resolution* has nothing to do
with which resource directories made it into the APK.

This is real production behaviour, not a test-process artifact: the same `resourceConfigurations`
filter applies to `assembleRelease`/`bundleRelease`. Today's install package for every user is
missing German, French, Spanish and Portuguese strings entirely, regardless of what the language
picker offers or what the phone's own language is — a phone whose language is German
(`aGermanPhoneReadsGermanWithoutAnyChoice`'s exact scenario) silently reads the app in English. Dutch
is unaffected only because it happens to be the one extra language the list was already updated for.

The fix is therefore in build configuration, not in `AppLocale`: add the remaining supported
languages' tags to `resourceConfigurations` so their resources are actually packaged.

## Risks / Trade-offs

- **[Risk] Root cause could be environment-specific** (this exact AVD image/API level) rather than a
  real device bug. → Mitigation: if diagnosis 4 above or similar points to an emulator/packaging
  quirk rather than app code, say so plainly in this design and in the GitHub issue, and scope the
  fix (or explicit non-fix, if it turns out to only affect this specific test harness setup and not
  real users) accordingly, rather than changing production code to work around a test artifact.
- **[Risk] Touching `AppLocale`** is touching the mechanism every reminder notification, and every
  other locale-sensitive string in the app, reads through. → Mitigation: whatever fix is chosen,
  keep the existing `wrap`/`apply` contract (same signatures, same callers) and run the full
  instrumented suite, not just `LanguageSectionTest`, before considering this change done.

## Migration Plan

Bug fix on a short-lived branch, no feature flag, no data migration. Rollback is a normal revert.
