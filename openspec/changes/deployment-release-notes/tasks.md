## 1. Collection script

- [ ] 1.1 Add `.github/scripts/collect_release_notes.py` (stdlib only) that, given a base SHA and a head SHA:
  - Lists proposal files under `openspec/changes/archive/**/proposal.md` added between base and head (`git diff --diff-filter=A --name-only`).
  - Parses each one for its `## Why` and `## What Changes` sections and its `**GitHub Issue:** #<number> (<url>)` line, if present.
  - Lists merge-commit subjects (`git log --merges --pretty=%s base..head`), extracts PR numbers, and for each one not already linked to an archived change (via `gh pr view <N> --json body` and its `Closes #<issue>` line), records the PR title as a fallback item.
  - Emits the collected context as JSON on stdout.
- [ ] 1.2 Add a second mode/entry point to the same script that takes the drafted English and Dutch text and the PR number, finds an existing comment containing the marker `<!-- release-notes-drafting -->` on that PR (`gh pr view --json comments`), and either edits it (`gh api -X PATCH`) or creates a new one (`gh pr comment`).
- [ ] 1.3 Handle the "nothing new" case: if collection finds no archived changes and no fallback PRs, the script signals this distinctly so the workflow can post a short "nothing new to summarize" comment instead of calling GitHub Models.

## 2. Workflow

- [ ] 2.1 Add `.github/workflows/release-notes.yml`:
  - `on: pull_request: branches: [main], types: [opened, reopened, synchronize]`.
  - Job-level `if: github.event.pull_request.head.ref == 'development'`.
  - `permissions: contents: read, pull-requests: write, models: read`.
  - Checkout with `fetch-depth: 0` at the PR head SHA.
- [ ] 2.2 Run the collection script (task 1.1) with the PR's base and head SHA (`github.event.pull_request.base.sha` / `head.sha`), capturing its JSON output.
- [ ] 2.3 If collection found nothing (task 1.3), skip straight to posting the "nothing new" comment (task 2.5) and end the job there.
- [ ] 2.4 Build the GitHub Models prompt from the collected JSON plus the current contents of `distribution/whatsnew/whatsnew-en-US` and `distribution/whatsnew/whatsnew-nl-NL` as a style reference, and call `actions/ai-inference@v1` with model `openai/gpt-4o-mini`, requesting a brief English and a brief Dutch "what's new" draft.
- [ ] 2.5 Run the comment-upsert mode of the script (task 1.2) with the two drafts (or the "nothing new" text) and the PR number.

## 3. Documentation

- [ ] 3.1 Update `README.md`'s `.github/workflows/` layout row to mention `release-notes.yml` and what it does.

## 4. Verification

- [ ] 4.1 Confirm `collect_release_notes.py` correctly finds newly archived proposals and fallback PRs against a real range in this repository's history (e.g. the range that produced `43e334e`), run locally.
- [ ] 4.2 Confirm the workflow's YAML is valid and its `if` condition correctly restricts drafting to `development` → `main` pull requests (review; GitHub Actions has no local dry-run for `pull_request` events).
- [ ] 4.3 Manually verify, the first time this workflow runs on a real `development` → `main` pull request, that the posted comment is legible, both languages read as brief and human, and a second synchronize event on the same PR edits the existing comment rather than duplicating it.
- [ ] 4.4 Record verification results (or what could not be verified before a real release PR exists) in this section.
