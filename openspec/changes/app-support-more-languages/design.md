## Context

`app-language` (archived as `app-settings-language`) was deliberately built so that the supported-language list is a single domain-layer object (`SupportedLanguages.all` in `AppLanguage.kt`) that both the Settings dropdown and `LocaleResolver` read from, and so that every piece of user-facing text — including the legal documents and the watch's strings — is a string resource rather than Kotlin text. `LocaleResolver` and the wear module's `LocalizedContent` already operate on arbitrary language tags with no hardcoded list of two. That means this change is close to pure content: four new `AppLanguage` entries, four new native names, and four new `values-xx` resource directories in each of `src/app` and `src/wear`, mirroring the existing `values-nl` sets line for line.

## Goals / Non-Goals

**Goals:**
- Make German, French, Spanish and Portuguese full peers of English and Dutch everywhere the app currently treats English/Dutch as the complete set: the Settings dropdown, `LocaleResolver`, lint's translation-completeness check, the legal documents, and the watch app.
- Keep the "adding a language is: add it to the list, add its resources" contract from `app-language` true after this change — no new indirection, no per-language special-casing in code.

**Non-Goals:**
- No change to the language-resolution algorithm, the restart-warning behaviour, the storage mechanism, or how the watch receives the phone's language tag. All of that already generalises to any number of languages.
- No change to `src/website`'s i18n, which is a separate capability (`marketing-website`) with its own translation files already present in the working tree.
- No native-speaker linguistic review process is being introduced by this change; translation quality assurance is a task-level concern (see tasks.md), not a design decision.

## Decisions

- **Extend the existing enum and list rather than generalising language storage to a dynamic set.** `AppLanguage` stays a fixed enum (now six entries) and `SupportedLanguages.all` a fixed list, matching the current design's stated contract ("adding a language requires adding it there and adding its translated resources, and nothing else"). A data-driven/pluggable language registry would be a bigger architectural change with no present need, and the spec already commits to the enum approach.
- **Translate the legal documents (Disclaimer, Terms of Service) as ordinary string resources**, exactly as `app-legal` already requires for Dutch — no separate translation pipeline or file format for legal text.
- **Mirror `values-nl` and the wear module's `values-nl` as the template for each new locale directory**, so every key that exists in Dutch is accounted for in German, French, Spanish and Portuguese, and nothing in the default (English) tree is missed.
- **Reuse the phone's already-published language tag verbatim for the watch** — no wear-side mapping table. `LocalizedContent` already takes any `Locale`; adding `de`/`fr`/`es`/`pt` string resources on the wear side is sufficient.

## Risks / Trade-offs

- [Machine or non-native translation could read awkwardly or be subtly wrong for a medical-adjacent app] → Translations are produced deliberately and reviewed for tone against the existing English/Dutch strings before the change is archived; flagged in tasks.md as an explicit review step, not skipped as "just" resource files.
- [Longer strings in German in particular can overflow fixed-width UI elements (chips, buttons)] → `pillsner-ui-review` is run after adding the new resources to catch truncation, per the design system's accessibility rules; no layout is assumed to be safe by default.
- [Six full translation sets increase the chance of a missed key going unnoticed] → Lint's missing/extra-translation check, already required by `app-language` and `wearable-app`, is the enforcement mechanism and must pass for all six locales before the change is considered done.

## Open Questions

None — the mechanism this change extends is already fully generalised; the remaining work is producing and verifying the resource content itself.
