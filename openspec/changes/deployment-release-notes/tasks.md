## 1. Collection script

- [x] 1.1 Add `.github/scripts/collect_release_notes.py` (stdlib only) that, given a base SHA and a head SHA:
  - Lists proposal files under `openspec/changes/archive/**/proposal.md` added between base and head (`git diff --diff-filter=A --name-only`).
  - Parses each one for its `## Why` and `## What Changes` sections and its `**GitHub Issue:** #<number> (<url>)` line, if present.
  - Lists merge-commit subjects (`git log --merges --pretty=%s base..head`), extracts PR numbers, and for each one not already linked to an archived change (via `gh pr view <N> --json body` and its `Closes #<issue>` line), records the PR title as a fallback item.
  - Emits the collected context as JSON on stdout.
- [x] 1.2 Add a second mode/entry point to the same script that takes the drafted English and Dutch text and the PR number, finds an existing comment containing the marker `<!-- release-notes-drafting -->` on that PR (`gh pr view --json comments`), and either edits it (`gh api -X PATCH`) or creates a new one (`gh pr comment`).
- [x] 1.3 Handle the "nothing new" case: if collection finds no archived changes and no fallback PRs, the script signals this distinctly so the workflow can post a short "nothing new to summarize" comment instead of calling GitHub Models.

## 2. Workflow

- [x] 2.1 Add `.github/workflows/release-notes.yml`:
  - `on: pull_request: branches: [main], types: [opened, reopened, synchronize]`.
  - Job-level `if: github.event.pull_request.head.ref == 'development'`.
  - `permissions: contents: read, pull-requests: write, models: read`.
  - Checkout with `fetch-depth: 0` at the PR head SHA.
- [x] 2.2 Run the collection script (task 1.1) with the PR's base and head SHA (`github.event.pull_request.base.sha` / `head.sha`), capturing its JSON output.
- [x] 2.3 If collection found nothing (task 1.3), skip straight to posting the "nothing new" comment (task 2.5) and end the job there.
- [x] 2.4 Build the GitHub Models prompt from the collected JSON plus the current contents of `distribution/whatsnew/whatsnew-en-US` and `distribution/whatsnew/whatsnew-nl-NL` as a style reference, and call `actions/ai-inference@v1` with model `openai/gpt-4o-mini`, requesting a brief English and a brief Dutch "what's new" draft.
- [x] 2.5 Run the comment-upsert mode of the script (task 1.2) with the two drafts (or the "nothing new" text) and the PR number.

## 3. Documentation

- [x] 3.1 Update `README.md`'s `.github/workflows/` layout row to mention `release-notes.yml` and what it does.

## 4. Verification

- [x] 4.1 Confirm `collect_release_notes.py` correctly finds newly archived proposals and fallback PRs against a real range in this repository's history (e.g. the range that produced `43e334e`), run locally.
- [x] 4.2 Confirm the workflow's YAML is valid and its `if` condition correctly restricts drafting to `development` → `main` pull requests (review; GitHub Actions has no local dry-run for `pull_request` events).
- [x] 4.3 Manually verify, the first time this workflow runs on a real `development` → `main` pull request, that the posted comment is legible, both languages read as brief and human, and a second synchronize event on the same PR edits the existing comment rather than duplicating it.
- [x] 4.4 Record verification results (or what could not be verified before a real release PR exists) in this section.

### Verification notes

- **4.1**: Ran `collect_release_notes.py collect` locally against two real ranges in this repository's history:
  - `ae6b656..43e334e` (the range that produced merge commits #17 and #19, neither backed by an archived OpenSpec change): correctly returned an empty `changes` list and both PRs as `fallback_prs`, with `has_content: true`.
  - `678cebd^..678cebd` (the archive commit that moved four completed changes into `openspec/changes/archive/2026-09-16-*`): correctly found all four newly archived proposals, extracted each one's `## Why`/`## What Changes` text (with the `**GitHub Issue:**` line stripped back out of the prose) and linked issue number, and returned `has_content: true`.
  - A same-commit range (`HEAD..HEAD`) correctly returned `has_content: false` with both lists empty (the "nothing new" case).
  - `format-prompt` was also run against the second range's output and produced a well-formed prompt combining both style-reference files with the four changes' summaries.
- **4.2**: `release-notes.yml` parses as valid YAML (checked with PyYAML) and `collect_release_notes.py` compiles cleanly (`python -m py_compile`). The job-level `if: github.event.pull_request.head.ref == 'development'` matches the pattern GitHub Actions documents for this exact scoping and mirrors the condition already reviewed and shipped in this repository's other workflows' branch/path guards.
- **4.3**: Verified against the real release PR #23 (`development` → `main`), after the two corrections below: the workflow posted a comment with brief, legible English and Dutch drafts, each confined to its own language with no cross-contamination, and a subsequent push to `development` (the model-flag fix) triggered a synchronize event that edited the same comment in place (confirmed via `gh pr view 23 --json comments`) rather than creating a second one.

### Correction found after merge

`actions/ai-inference`'s direct GitHub Models support (used by the merged version of this
workflow) was removed in the action's `v3` release — the action now only supports the GitHub
Copilot CLI as its inference provider. The workflow was updated directly on `development` (per
user instruction, bypassing the usual feature-branch/PR ceremony for this follow-up fix) to:
pin `actions/ai-inference@v3`, install the Copilot CLI (`actions/setup-node@v4` +
`npm install -g @github/copilot`), and authenticate it with a new repository secret,
`GIHUB_COPILOT_API_KEY` (a GitHub Copilot API token the user provisioned), passed as
`COPILOT_GITHUB_TOKEN`. The job's `models: read` permission was removed as it's no longer used.
`design.md`, `proposal.md` and `specs/release-notes-drafting/spec.md` were updated to match.
Re-verify 4.3 against this Copilot-based path, not the original GitHub Models path.

### Correction found in first real run

Release PR #23 (`development` → `main`) exercised the workflow and the drafting steps failed:
`Model "gpt-4.1" from --model flag is not available` (confirmed via `gh run rerun --debug`). The
hardcoded `model: gpt-4.1` no longer exists in Copilot's model catalog. Fixed by passing
`model: ""` explicitly — `actions/ai-inference` only appends `--model` to the Copilot CLI
invocation when that input is non-empty, so an empty string lets the CLI fall back to its
account's own default model rather than a name this repo would have to keep chasing as GitHub's
catalog changes. Confirmed fixed: the re-run (triggered automatically by the push, since PR #23's
head is `development`) completed successfully and posted a comment.

That same run's English draft, however, also generated an unrequested Dutch translation
alongside it, duplicating content in the comment. Fixed by tightening both system prompts to
explicitly forbid mixing languages or adding language-labelled headings. The next run (also
auto-triggered) produced a clean, single-language comment in each section -- see 4.3 above.
