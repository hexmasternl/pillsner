# Tasks

## 1. Failing check first

- [x] 1.1 Add `.github/scripts/test_collect_release_notes.py` with a `unittest` asserting that `distribution/whatsnew/` has `whatsnew-en-US`, `-nl-NL`, `-de-DE`, `-fr-FR`, `-es-ES` and `-pt-PT`, each non-empty and at most 500 characters, naming the language or file on failure. Verify `python3 -m unittest discover -s .github/scripts` fails on `development` because the four new files are missing (issue #100).
- [x] 1.2 Add a "Release notes check" step to `ci.yml` that runs that command from the repository root, before the Gradle setup. Verify the workflow YAML parses (`actionlint`, or by reading it against an existing step).

## 2. Whatsnew files

- [ ] 2.1 Add `whatsnew-de-DE`, `whatsnew-fr-FR`, `whatsnew-es-ES` and `whatsnew-pt-PT` (European Portuguese) as natural translations of the current English notes. Change the language bullet to "six languages" in all six files, `en-US` and `nl-NL` included. Verify the check from 1.1 now passes.

## 3. Drafting in six languages

- [ ] 3.1 In `collect_release_notes.py`, introduce the `LANGUAGES` table. Replace `--style-en/--style-nl` with `--whatsnew-dir`, and `--english-file/--dutch-file` with repeatable `--draft <code>=<file>`. Render one comment section per language in table order, each naming its whatsnew file, with the hidden marker unchanged. Add unit tests for `build_comment_body` (all six headings in order, marker present, the "nothing new" body unchanged) and for prompt building (every style file included). Verify `python3 -m unittest discover -s .github/scripts` passes.
- [ ] 3.2 In `release-notes.yml`, pass `--whatsnew-dir distribution/whatsnew` to `format-prompt`. Replace the English and Dutch drafting steps with six steps (en, nl, de, fr, es, pt) that share one English system prompt naming the target language. Pass every response to `comment` through `--draft`. Update the header comment. Verify the YAML parses and every `steps.draft_<code>.outputs.response` reference matches a step id.

## 4. Documentation and release readiness

- [ ] 4.1 Update the `distribution/whatsnew/` row in `README.md` to "one plain-text file per app language (six)". Check `CHANGELOG.md`'s intro still holds. Verify by reading both.
- [ ] 4.2 Confirm in Play Console that de-DE, fr-FR, es-ES and pt-PT are store-listing languages, or note in the pull request description that a maintainer must add them before the next release. Verify the PR description carries the outcome.
