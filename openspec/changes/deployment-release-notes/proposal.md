## Why

**GitHub Issue:** #21 (https://github.com/hexmasternl/pillsner/issues/21)
**Pull Request:** #22 (https://github.com/hexmasternl/pillsner/pull/22)

Cutting a release means opening a `development` → `main` pull request, and `development` usually carries several finished OpenSpec changes at once by the time that happens. Nobody currently writes down what actually shipped: the reviewer merging that PR, and anyone reading the eventual Google Play listing, has no summary of what changed beyond scrolling commit history. `release.yml` already reads `distribution/whatsnew/whatsnew-en-US` and `whatsnew-nl-NL` for the Play "what's new" text on every release, but today those two files are static leftovers from the 1.0 launch copy and are never updated, so every release currently ships the same day-one notes regardless of what it actually contains.

## What Changes

- Add a new GitHub Actions workflow (`.github/workflows/release-notes.yml`) that runs on the `development` → `main` pull request (opened, reopened, and re-synchronized) and:
  - Diffs `openspec/changes/archive/**` between the PR's base and head commit to find every OpenSpec change newly archived on `development` since the last release — the "changes" being shipped.
  - Reads each of those changes' `proposal.md` for its `## Why` / `## What Changes` summary and its linked `**GitHub Issue:** #<number>` line — the "issues" being shipped.
  - Also collects any merged-PR titles in that commit range that are not backed by an OpenSpec change (small fixes that skipped the proposal step, per `CLAUDE.md`), so those are not silently dropped from the notes.
  - Sends that collected context to GitHub Models (via `actions/ai-inference`, using the default `GITHUB_TOKEN` with `models: read` — no new secret) asking for a short, user-facing "what's new" style summary, in both English and Dutch.
  - Posts the two drafts as a single comment on the `development` → `main` pull request, so a human can read, edit, and hand-copy them into `distribution/whatsnew/whatsnew-en-US` and `whatsnew-nl-NL` before merging. The workflow never commits to `distribution/whatsnew/*` or to any branch itself — the decision was to keep this a drafting aid, not an automatic content change.
  - Re-running the workflow on the same PR (e.g. another change lands on `development` and the PR is re-synchronized) updates the same comment in place rather than piling up duplicates.
- No changes to `release.yml`'s existing `whatsNewDirectory` wiring, `ci.yml`, or `website.yml`.

## Capabilities

### New Capabilities
- `release-notes-drafting`: generates a bilingual (English/Dutch), human-reviewable draft of Play Store "what's new" release notes from the OpenSpec changes and issues shipped in a `development` → `main` pull request, and posts it as a PR comment.

### Modified Capabilities
- (none — this adds a new, self-contained CI capability and does not alter any existing app, spec, or Android behaviour)

## Impact

- **New code**: `.github/workflows/release-notes.yml` and a small helper script under `.github/scripts/` that collects the archived-change and fallback-commit context and formats it for the AI prompt.
- **New workflow permission**: `models: read`, to call GitHub Models via `actions/ai-inference`. No new repository secret is needed — GitHub Models authenticates with the workflow's own `GITHUB_TOKEN`.
- **README.md**: the `.github/workflows/` layout row gains a mention of `release-notes.yml`.
- **No changes** to `src/app`, `src/wear`, `src/shared`, Room schemas, alarm/reminder scheduling, or the app's no-network privacy stance — this workflow only ever reads repository/OpenSpec/GitHub metadata in CI and posts a PR comment; it never touches the Android app's code, data, or connectivity, and it never writes medication names or dosages anywhere (OpenSpec proposals and GitHub issues in this repo already keep that content out, per `CLAUDE.md` and the `github-openspec-sync` skill's guardrails).
