---
name: changes-status
description: A read-only overview of every in-flight Pillsner OpenSpec change - task progress, worktree, feature branch, pull request and issue state - with the inconsistencies flagged (merged but not archived, finished but no PR, worktree without a change, stale work). Use when asked "what's in progress?", "what should I finish next?", "status of the changes", before starting a new change, and before preparing a release.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

Read-only. It changes nothing; it reports, and suggests the next action for each change.

## Gather

From the main checkout:

```bash
git fetch origin --prune
openspec list --json                       # in-flight changes
git worktree list --porcelain
git branch --list 'feature/*' --format='%(refname:short) %(upstream:track) %(committerdate:short)'
gh pr list --state all --base development --limit 50 --json number,title,state,headRefName,mergedAt,mergeable,isDraft
```

For each change `<name>` in `openspec/changes/` (not `archive/`):

- Tasks: count `- [x]` / `- [X]` against all `- [ ]`-style boxes in `tasks.md`. Note manual-test
  tasks still open.
- Issue and PR: the `**GitHub Issue:**` and `**Pull Request:**` lines in `proposal.md`; the PR's
  state from the list above (match on `feature/<name>`).
- Worktree: `../pillsner-worktrees/<name>` present? Uncommitted work in it
  (`git -C ../pillsner-worktrees/<name> status --porcelain`)? Commits not pushed?
- Last activity: newest commit date on `feature/<name>`, or the proposal's commit date if never applied.

## Report

One table, sorted by what needs attention first:

| Change | Tasks | Worktree | PR | Issue | Last activity | Next step |
| --- | --- | --- | --- | --- | --- | --- |
| `medicine-details-less-dense` | 18/19 | yes, clean | #84 merged | #80 open | 2026-10-20 | Archive |

Then a short list of flags:

- **Merged, not archived** — the PR merged into `development` but the change is still in
  `openspec/changes/`. It won't appear in the release notes (those are drafted from
  `openspec/changes/archive/` only) and its worktree is still around. Next step: `/opsx:archive`.
- **Finished, no PR** — every task done, no PR. Next step: `verify-change`, then
  `github-openspec-sync apply-complete`.
- **PR blocked** — `mergeable` is `CONFLICTING`, or checks are failing, or review threads are open.
  Next step: `git-workflow` step 6 / `pr-review-response`.
- **Uncommitted or unpushed work** in a worktree.
- **Orphans** — a worktree or `feature/*` branch with no matching change folder (and no archived one),
  or an archived change whose worktree or branch still exists. Next step: cleanup per `git-workflow`
  step 7, after confirming nothing unique is on it.
- **Stale** — no activity in 14 days. Ask whether it's still wanted rather than suggesting to finish it.
- **Overlapping** — two in-flight changes whose `design.md` or task lists touch the same files or the
  same spec; worth finishing one before starting the other.

End with a one-line recommendation: the change that's cheapest to get over the line.

## Guardrails

- Never archive, delete, push, comment or close anything from this skill. It reports; the user picks.
- Don't estimate effort beyond "tasks left" — the task list is the honest measure.
