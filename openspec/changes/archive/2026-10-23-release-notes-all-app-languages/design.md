# Design

## Context

- `release.yml` passes `whatsNewDirectory: distribution/whatsnew` to `r0adkll/upload-google-play` for both the phone and the watch upload. The action turns every `whatsnew-<locale>` file in that folder into localized release notes, so a new file needs no workflow change.
- `release-notes.yml` drafts with two hard-coded `actions/ai-inference` steps (English, Dutch). `collect_release_notes.py` takes `--style-en/--style-nl` and `--english-file/--dutch-file`, and builds a comment with two fixed sections. The script has no tests.
- The app's languages are in `values`, `values-nl`, `-de`, `-fr`, `-es`, `-pt`. The website already treats Portuguese as European Portuguese (`pt_PT` in `src/website/hugo.toml`).

## Goals / Non-Goals

**Goals:**
- One table of release-note languages in the script, so adding a seventh language later is one line plus a file and a workflow step.
- A cheap, fast check that runs on every pull request.

**Non-Goals:**
- Committing drafts automatically. The human copy step stays, per the spec.
- Release notes for Play listing languages the app doesn't support.
- Changing `release.yml`.

## Decisions

1. **Play locale per app language: en-US, nl-NL, de-DE, fr-FR, es-ES, pt-PT.** These are Play's codes for the main markets, and pt-PT matches the website. pt-BR was considered and dropped: the app's `values-pt` is unregioned and the site already chose Portugal.
2. **The language table lives in `collect_release_notes.py`.** A `LANGUAGES` list of `(code, play_locale, heading)`. `format-prompt` takes the style reference from every `whatsnew-<locale>` file it finds in a `--whatsnew-dir`. `comment` takes a `--draft <code>=<file>` argument once per language and renders one section per language, in table order, each naming its target file. The old `--style-en/--style-nl` and `--english-file/--dutch-file` flags go away, since the workflow is their only caller.
3. **One explicit `ai-inference` step per language, with an English system prompt that names the target language.** That gives six near-identical steps, against a matrix job that would need artifacts and a fan-in job just to post one comment. The Dutch-language system prompt is replaced with the shared English one. The instruction "respond only in <language>" matters more than the language the instruction is written in, and one template is easier to keep consistent.
4. **The check is a Python `unittest` in `.github/scripts/test_collect_release_notes.py`, run by a new step in `ci.yml`.** It asserts the six whatsnew files exist, are non-empty and are at most 500 characters. It also covers comment rendering and prompt building. `ci.yml` already runs on every pull request and `ubuntu-latest` has Python 3, so there's no new dependency. The step goes before the Gradle setup so it fails fast.
5. **Translations: the current notes, rewritten natively.** They are not machine-literal translations, and each stays under 500 characters. The language bullet becomes "six languages" in all six files.

## Risks / Trade-offs

- [Play rejects release notes for a language missing from the store listing] → Task: confirm de-DE, fr-FR, es-ES and pt-PT are listing languages in Play Console (add them if not) before the next release. It's flagged in the PR description.
- [More AI calls per release PR (six instead of two), so more quota use and more chances for one to fail] → Failures stay isolated by the existing requirement, and drafting only runs on development → main pull requests.
- [Six workflow steps repeat themselves] → Accepted for readability. The differing values (language name, output id) are one line each.
- [Translation quality in languages the maintainers don't read] → Keep the notes short and plain. The PR reviewer can check them with a translator.
