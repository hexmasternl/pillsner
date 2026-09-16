## Context

`development` is the integration branch; every finished OpenSpec change lands there by PR, gets archived (`openspec/changes/archive/<dated-name>/`) once its tasks are done, and just sits there until someone opens a `development` → `main` PR to cut a release (`.claude/skills/git-workflow/SKILL.md`). By that point `development` can hold several archived changes at once. `release.yml` already publishes a `whatsNewDirectory: distribution/whatsnew` to Google Play on every push to `main`, but the two files in it (`whatsnew-en-US`, `whatsnew-nl-NL`) have held the same 1.0 launch copy since the app's first release and nothing updates them.

Each archived change's `proposal.md` already carries a `**GitHub Issue:** #<number> (<url>)` line (added by `github-openspec-sync`'s propose mode) and a `## Why` / `## What Changes` summary that's already written in plain, GitHub-facing language — no medication names or dosages, per that skill's guardrails. That makes `openspec/changes/archive/**` the authoritative, low-effort source for "what shipped": diffing it between the release PR's base and head commit gives exactly the changes new to this release, without needing to reconstruct history from merge commits or PR search.

Some shipped work never goes through a proposal at all — `CLAUDE.md` allows "small, clearly-scoped fixes" to skip it — so there can be merged commits in the range with no archived change behind them. Those still deserve a line in the notes; they just don't come with a `## Why` / issue link.

## Goals / Non-Goals

**Goals:**
- On the `development` → `main` PR, automatically draft brief, user-facing "what's new" text in English and Dutch, from the changes (and their linked issues) that PR actually contains.
- Make the draft easy for a human to review and hand-copy into `distribution/whatsnew/*` before merging.
- Keep the workflow self-contained: no new repository secret, no change to `release.yml`'s existing publishing behaviour, no effect on `ci.yml` or `website.yml`.
- Make re-runs (the PR getting re-synchronized because more work landed on `development` before the release actually goes out) update the same comment rather than spamming the PR.

**Non-Goals:**
- Automatically writing to `distribution/whatsnew/whatsnew-en-US` / `whatsnew-nl-NL`, or to any branch. The user chose PR-comment-only for this change: this is a drafting aid, not an automatic content change. A follow-up change can revisit that if hand-copying proves annoying.
- Generating full changelog/commit-level detail. The output is Play-listing-brief (a handful of bullets), not an engineering changelog.
- Covering releases cut any other way than the standard `development` → `main` PR (e.g. a hotfix branched straight from `main`) — that is out of scope for this change and not how this repo releases today.

## Decisions

**Trigger and scope: `pull_request` targeting `main`, gated to PRs whose head is `development`.**
`on: pull_request: branches: [main]` with a job-level `if: github.event.pull_request.head.ref == 'development'`. Only `development` → `main` PRs are release PRs per the branching model; any other PR opened against `main` (which the git-workflow guardrails say shouldn't happen, but the check is one line) is ignored rather than trusted. Types `[opened, reopened, synchronize]` so the draft stays current if the PR is updated before merge.

**Source of "what shipped": diff `openspec/changes/archive/**` between the PR's base and head SHA, not merge-commit parsing.**
`git diff --diff-filter=A --name-only <base-sha> <head-sha> -- 'openspec/changes/archive/**/proposal.md'` lists every archived change's proposal file that's new to `development` since `main` last released. This is exact and needs no assumption about merge strategy (squash vs. merge commit), branch naming, or GitHub search pagination. Each matched path's directory name is the dated change folder; each file's `**GitHub Issue:**` line (if present) gives the issue link; the `## Why` and `## What Changes` sections give the summary. Considered: parsing `git log --merges` for `Merge pull request #N` subjects and looking each PR up via `gh pr view` — rejected as more moving parts (depends on merge style, needs the `gh` API for every PR) for the same answer the archive diff gives directly, and it would also require separately re-deriving each PR's linked issue instead of reading it straight off `proposal.md`.

**Fallback for changes that skipped the proposal step: merge commit subjects in the same range, minus ones already covered.**
`git log --merges --pretty=%s <base-sha>..<head-sha>` lists every "Merge pull request #N from …" subject in range. Any PR number in that list is cross-checked against the issue numbers already pulled from the archived proposals (via `gh pr view <N> --json body` → look for `Closes #<issue>`, since that's the line `github-openspec-sync` writes into every feature PR body); numbers not already accounted for are passed to the model as plain, undated "also included" bullets with just their PR title. This keeps every merged PR represented without double-counting the ones already summarized from `proposal.md`.

**Wording and translation: GitHub Copilot CLI via `actions/ai-inference`, not a template or a separate translation library.**
The user chose this over a hand-written bullet template. `actions/ai-inference` originally called the GitHub Models inference API directly with only the workflow's own `GITHUB_TOKEN` (job permission `models: read`, no dedicated secret) — but the action's `v3` release removed direct GitHub Models support entirely, routing every inference call through the GitHub Copilot CLI instead (`provider: copilot`, the only supported provider from `v2.1.0` on). **Correction found during implementation**: this change now pins `actions/ai-inference@v3`, installs the Copilot CLI on the runner (`actions/setup-node@v4` + `npm install -g @github/copilot`, per the action's own setup requirement), and authenticates it with a dedicated repository secret, `GIHUB_COPILOT_API_KEY` (a GitHub Copilot API token the user provisioned), passed to the action as the `COPILOT_GITHUB_TOKEN` environment variable. The job no longer needs the `models: read` permission. The prompt content is unchanged: the existing `distribution/whatsnew/*` files as a style reference (tone, length, bullet format), the collected `## Why` / `## What Changes` text per archived change, and the fallback PR titles — asking for two outputs, an English and a Dutch version, each phrased as brief, non-technical, Play-listing bullets. **Correction found after merge**: the workflow initially hardcoded `model: gpt-4.1`, which failed in production (`Model "gpt-4.1" from --model flag is not available` — GitHub's Copilot model catalog had already moved past it). `actions/ai-inference` only passes `--model` to the CLI when that input is non-empty after trimming, so the workflow now passes `model: ""` explicitly, letting the Copilot CLI fall back to its account's own default model rather than a hardcoded name this repo would have to keep chasing as GitHub's catalog changes (the changelog shows it does so frequently).

**Delivery: one upserted PR comment, never a commit.**
The workflow uses `gh pr comment` / `gh api` to find an existing comment containing the marker `<!-- release-notes-drafting -->` on the PR (via `gh pr view --json comments`) and edits it in place if found (`gh api -X PATCH .../issues/comments/<id>`), or creates a new one if not (`gh pr comment <number> --body "..."`). This mirrors the idempotent-comment pattern `github-openspec-sync` already uses for its own marker comments, so the two stay consistent for anyone reading this repo's automation.

**Helper script in Python, not inline bash.**
The archive diff, per-file section extraction, `gh pr view`/`gh issue view` calls, and comment upsert are more comfortably expressed as a single `.github/scripts/collect_release_notes.py` (stdlib only — `subprocess`, `re`, `json`, no new dependency) than as a long inline `run:` block. `ubuntu-latest` ships Python 3 by default, so this adds no setup step. The workflow calls the script twice: once to build the AI prompt payload, once (after the `ai-inference` step) to format and upsert the comment.

## Risks / Trade-offs

- **A drafting aid that nobody reads is worse than no automation.** → The comment is deliberately the only output (per the user's choice), so it costs a merge-checklist habit, not a code change, if it's ignored; nothing downstream depends on it existing.
- **GitHub Copilot CLI availability/quota, and the `GIHUB_COPILOT_API_KEY` secret's validity, are outside this repo's control.** → The `ai-inference` step failing (quota, outage, an expired token) should fail only this workflow, not `ci.yml` or `release.yml` — it has no `needs:` relationship to either, and a `development` → `main` PR can still be merged and released without this comment ever posting.
- **An archived change's `proposal.md` might be thin or stale relative to what was actually implemented** (design.md may record a "correction found during implementation" that changed scope). → Out of scope to reconcile here; the release-notes draft is explicitly a starting point for a human to edit, not a guaranteed-accurate record. If this becomes a recurring problem, a follow-up change can also feed `tasks.md`'s verification notes into the prompt.
- **Model output quality/tone drift between runs.** → Giving the model the current `distribution/whatsnew/*` files as a style reference keeps successive drafts consistent in voice even though generation isn't deterministic; a human still reviews before anything reaches Play.
- **Re-synchronize storms** (several quick pushes to `development` while the release PR is open) could call the Models API repeatedly. → Low risk in practice (a release PR is opened once, deliberately, to cut a release — per the branching model it isn't a place ongoing feature work lands), and each run is a single short inference call.

## Open Questions

- None outstanding. Generation method (GitHub Models via Copilot's catalog, not the Claude API or a template) and delivery target (PR comment only, no commit) were both resolved with the user before writing this design.
