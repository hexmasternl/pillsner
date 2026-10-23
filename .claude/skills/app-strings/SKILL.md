---
name: app-strings
description: Add or change user-facing text in the Pillsner app (phone and watch) in all six locales - English source in values/strings.xml, translations in values-nl, -fr, -es, -pt and -de - with correct plurals, positional placeholders, glossary terms, escaping and length, so the MissingTranslation/ExtraTranslation lint errors never fire and no Dutch user reads English. Use whenever a change adds, renames, rewords or removes a string, plural or content description in src/app or src/wear, and when reviewing string changes.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

Every user-facing string lives in a string resource (`CLAUDE.md`), and every string exists in all six
locales: `app/lint.xml` makes `MissingTranslation` and `ExtraTranslation` build errors, because
someone reading a medication reminder should read it in their own language.

## Where strings live

| Module | Files |
| --- | --- |
| Phone | `src/app/src/main/res/values/strings.xml` (English, source of truth) and `values-{nl,fr,es,pt,de}/strings.xml` |
| Watch | `src/wear/src/main/res/values/strings.xml` and the same five locale folders |

Strings aren't shared between the modules. A text both show is defined in both, worded the same.

## Adding a string

1. **Name the key** after the screen or feature and the role, matching the existing prefixes in the
   file (e.g. `applock_…`, `dose_…`) — read the neighbouring keys first. Snake case.
2. **Write the English** in the app's voice: short, plain, second person, using the glossary terms
   from `CLAUDE.md` (medicine, schedule, dose, reminder, history — not "pill", "alarm", "event"
   unless that's the existing UI word). Reuse the established answers verbatim ("I took it",
   "Not yet", "Not going to").
3. **Placeholders** are positional (`%1$s`, `%2$d`) whenever there's more than one, so translators
   can reorder them. Never build a sentence by concatenating strings in code.
4. **Counts are plurals**, never `"%d item(s)"`: a `<plurals>` with `one` and `other` in English,
   and in each locale the quantities lint asks for (French and Portuguese treat 0 and 1 differently
   from English; let `./gradlew lint` tell you which quantities each locale needs). In Compose:
   `pluralStringResource(R.plurals.key, count, count)`.
5. **Escape** apostrophes (`\'`) and quotes, which French, Portuguese and Dutch use often. Use real
   typographic characters where the existing strings do (`…`, `–`).
6. **Translate into all five locales in the same commit.** Before translating, read that locale's
   file for the terms it already uses for the same concepts and reuse them — the Dutch file's word
   for "dose" must stay the same word everywhere. Translate the meaning, not word for word.
7. **Content descriptions** for icons and images are strings too, in every locale, and describe the
   action or meaning ("Delete medicine"), not the picture ("trash can").

## Changing or removing a string

- **Rewording** the English means rewording every translation in the same commit. A changed English
  string with old translations is a silent mistranslation that lint can't catch.
- **Changing the meaning** (not just the wording) gets a new key, so no stale translation can survive
  under the old one.
- **Removing** a string removes it from all six files (else `ExtraTranslation` fails), and from the
  website if the website quotes it (`website-feature-sync`).

## Length

German and French are often 30% longer than English. The design system forbids truncating text, so
check the longest locale fits: run the composable's preview in that locale
(`@Preview(locale = "de")`), or a semantics test. A string that doesn't fit is a layout problem to
fix (wrap, reflow), not a reason to shorten one locale's text.

## Verify

- `./gradlew lint` from `src/` passes (proves every key in every locale, and plural quantities).
- `grep -c "<string " src/app/src/main/res/values*/strings.xml` — the counts match across locales
  (a quick check before lint).
- Any test that asserts on the text uses the resource (`context.getString(R.string.key)`), not a
  literal, so it passes in every locale.

## Guardrails

- Never put a medicine name, dose or other user data into a string resource or a log message.
- Never add a locale's `values-xx` folder without a change proposing the language (it needs the
  in-app language picker, the website locale and the Play listing).
- Never leave an English string in a non-English file to satisfy lint. If a translation is
  uncertain, translate it as well as possible and flag it in the pull request summary for a native
  speaker to check.
