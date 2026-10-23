# Proposal

## Why

**GitHub Issue:** #100 (https://github.com/hexmasternl/pillsner/issues/100)

Play release notes ship only in English and Dutch. The app itself speaks six languages, so German, French, Spanish and Portuguese users read "what's new" in another language, and Play Console shows "Enter or paste your release notes" placeholders on every release (issue #100). The release-notes drafting spec names only English and Dutch, so the pipeline is doing what was agreed. The agreement is what has to change.

## What Changes

- `distribution/whatsnew/` gains `whatsnew-de-DE`, `whatsnew-fr-FR`, `whatsnew-es-ES` and `whatsnew-pt-PT` next to the existing `en-US` and `nl-NL` files, so `release.yml` uploads notes in all six app languages to both the phone and the watch track. The release workflow doesn't change; it already uploads every file in that directory.
- The current notes are translated into the four new languages. The outdated "English and Dutch" line becomes accurate in all six files.
- The release-notes drafting workflow drafts in all six app languages instead of two, and posts every draft in the same single, updatable pull request comment.
- A check proves every app language has a whatsnew file within Play's 500-character limit, so a missing or oversized file is caught before a release instead of in Play Console.
- Out of scope: Play listing languages that are not app languages (Czech, Danish, Finnish and Indonesian today). They get no notes of their own.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `release-notes-drafting`: drafting covers the six app languages, not just English and Dutch. The comment and the "nothing is committed" guarantee extend to every whatsnew file. A new requirement says every app language has a whatsnew file within Play's character limit.

## Impact

- `distribution/whatsnew/` (four new files, two edited).
- `.github/workflows/release-notes.yml` and `.github/scripts/collect_release_notes.py`, plus a new unit test for the script.
- `README.md` repository layout row for `distribution/whatsnew/`.
- No app code, no dependencies, no permissions. Nothing changes for app users except the store text they read.
- Before the first release, the Play store listing must include de-DE, fr-FR, es-ES and pt-PT.
