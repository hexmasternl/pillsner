---
name: release-prep
description: Get development ready for a Pillsner release and prepare (not open, not merge) the development → main pull request - check every merged change is archived so the release notes include it, CI is green on development, the website describes what ships, nothing half-done is on development, and the version GitVersion will assign is the intended one. Use only when the user asks to release, ship or cut a release, or asks "are we ready to release?".
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

A push to `main` is a release: `release.yml` builds, signs and uploads both bundles to Google Play,
tags the commit and creates the GitHub release, and `website.yml` deploys the site. Releasing is the
user's decision; this skill only makes sure that what's on `development` is what they think it is,
then hands them the pull request to open.

## Checks

Run from the main checkout, on an up-to-date `development` (`git pull --ff-only`).

1. **What would ship**: `git log --oneline origin/main..origin/development` and the merged PRs in that
   range (`gh pr list --state merged --base development --search "merged:>=<last release date>"`).
   Last release: `git describe --tags --abbrev=0 origin/main`.
2. **Every shipped change is archived.** The `release-notes.yml` workflow drafts the Play "what's new"
   text from changes newly added under `openspec/changes/archive/` (via
   `.github/scripts/collect_release_notes.py`). A change merged into `development` but still in
   `openspec/changes/` is missing from the notes. Use `changes-status` to find them; archive each
   (`/opsx:archive`) before the release PR.
3. **Nothing half-done is on development.** No change folder on `development` whose code merged only
   partly; no feature flag or placeholder screen reachable by users. Anything that merged but
   shouldn't ship yet means the release waits, or the user decides to ship it anyway.
4. **CI is green on the head of development**:
   `gh run list --branch development --limit 5 --json name,conclusion,headSha`. Every workflow on the
   current head passed.
5. **The website describes what ships.** Run `website-feature-sync` in audit mode over the changes
   in this release. A gap is fixed by a change merged before the release, not by a commit on
   `development`.
6. **Version.** GitVersion (`GitVersion.yml`) gives every commit on `main` a new patch version, from
   the last `v*` tag. Releases that should be a new minor or major need a `+semver: minor` /
   `+semver: major` line in a commit message, or a tag set by the user. Tell the user the version it
   will get (`<last tag>` with the patch incremented) and ask whether that's right. The Play version
   code is derived from it (`MAJOR*1000000 + MINOR*10000 + PATCH`, ×10 phone, ×10+1 watch); no file
   holds a version to bump.
7. **Manual tests owed.** List any manual-testing callouts in the merged PRs since the last release
   whose checkboxes are still unticked. Those are the user's to do before release.

## Hand over

Report each check as pass, or what's wrong and what to do. When all pass, give the user the command
and a suggested title and body (the list of changes shipping, with their issue numbers):

```bash
gh pr create --base main --head development --title "Release <version>" --body-file <file>
```

Opening it triggers `release-notes.yml`, which comments a draft of the Play notes on the PR for a
human to review and copy. Only open the PR yourself if the user explicitly hands you that job for
this release.

## Guardrails

- Never open the release PR unasked, never merge it, never push to `main`, never create tags.
- Never "fix" a release blocker by committing directly to `development`; the fix goes through a
  feature branch like any other change (OpenSpec bookkeeping — archiving — is the exception the
  `git-workflow` skill allows).
- Never run `release.yml` by `workflow_dispatch` or change its track inputs.
