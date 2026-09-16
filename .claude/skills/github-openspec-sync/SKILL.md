---
name: github-openspec-sync
description: Keep a GitHub issue, a pull request, and an OpenSpec change in sync - create and link an issue when a change is proposed, then once the change is fully implemented push the branch, open a pull request that closes the issue on merge, and comment on the issue with a detailed implementation summary. Invoke this proactively, without being asked, immediately after openspec-propose finishes creating a change's artifacts, immediately after openspec-apply-change reports every task complete, and immediately after openspec-archive-change archives a change. Also use it when the user explicitly asks to link, sync, or create a GitHub issue or pull request for a change.
license: MIT
compatibility: Requires the gh CLI (authenticated) and the openspec CLI.
metadata:
  author: pillsner
  version: "1.0"
---

Keep an OpenSpec change, a GitHub issue, and a pull request in sync, end to end: create the issue
when the change is proposed, and once the change is implemented push the branch, open a pull
request that links to (and closes on merge) the issue, and comment on the issue with what was
actually done.

This is a hand-maintained companion to the OpenSpec workflow skills, not part of them — it never
edits `openspec/changes/**` content beyond the one linking line described below, and it never
touches `.claude/skills/openspec-*` or `.claude/commands/opsx/*`, which are generated and must not
be hand-edited (per `CLAUDE.md`).

**When to use this, without being asked**

- Right after `openspec-propose` (`/opsx:propose`) finishes creating a new change's artifacts →
  run in **propose** mode.
- Right after `openspec-apply-change` (`/opsx:apply`) reports every task in `tasks.md` complete →
  run in **apply-complete** mode.
- Right after `openspec-archive-change` (`/opsx:archive`) moves a change into
  `openspec/changes/archive/` → run in **archive** mode.
- Whenever the user explicitly asks to link, sync, back-fill, or create a GitHub issue for a change.

**Input**: `args` is `"<mode> <change-name>"` where mode is `propose`, `apply-complete`, `archive`,
`link`, or `status`. If the mode is omitted, infer it: no linked issue yet → `propose`; every task
in `tasks.md` just turned `[x]` → `apply-complete`; the change just moved into
`openspec/changes/archive/` → `archive`. If the change name is omitted, infer it from conversation
context (the change just proposed/applied/archived) — do not guess across unrelated changes.

## How a change, an issue, and a PR are linked

The source of truth is a pair of lines near the top of the change's `proposal.md` (search for them
there first, before creating anything):

```
**GitHub Issue:** #<number> (<url>)
**Pull Request:** #<number> (<url>)
```

The `Pull Request` line only appears once **apply-complete** has opened one; its absence means no
PR has been created yet for this change, not that the change has no issue.

If the issue line is missing, check whether an issue already exists before assuming there is none:

```bash
gh issue list --search "<change-name> in:title" --state all --json number,title,url,state
```

If a matching issue is found, treat it as linked and back-fill the `proposal.md` line (see
**Mode: link** below) instead of creating a duplicate. Likewise, if the PR line is missing, check
for an existing PR before opening a new one:

```bash
gh pr list --search "<change-name> in:title" --state all --json number,title,url,state,headRefName
```

## Mode: propose

1. Read `openspec/changes/<name>/proposal.md`. If it already has a `**GitHub Issue:**` line, stop —
   report the existing issue, do nothing else (idempotent).
2. Otherwise, search for an existing issue as described above. If one is found, go to **Mode: link**
   instead of creating a new one.
3. Compose the issue:
   - **Title**: a short, human sentence describing the change (not the kebab-case name verbatim) —
     draw it from the proposal's `## Why` section.
   - **Body**: the proposal's `## Why` and `## What Changes` sections (verbatim or lightly
     tightened for a GitHub audience), then a line pointing at the change folder
     (`openspec/changes/<name>/`), and ending with a footer line exactly:
     `_Tracked by OpenSpec change \`<name>\`._`
     (this footer is the fallback search marker used above).
4. Show the user the title and body before creating it, unless this is a direct, obvious
   continuation of a propose flow they just asked for — when in doubt, show it.
5. Create it: `gh issue create --title "<title>" --body "<body>"`.
6. Add `**GitHub Issue:** #<number> (<url>)` as its own line directly under the `## Why` heading in
   `proposal.md`. Do not commit this edit yourself — leave it staged like any other artifact edit;
   committing follows the same "only when asked" rule as the rest of the OpenSpec workflow.
7. Report the issue URL to the user.

## Mode: apply-complete

Runs once, right when a change's tasks all just became complete — not after every individual task.
This mode both comments on the issue and opens the pull request that will close it on merge.

1. Find the linked issue (see lookup above). If none exists, tell the user and offer to create one
   now (**Mode: propose**) before continuing; if they decline, stop here.
2. Check the issue's existing comments (`gh issue view <number> --json comments`) for one already
   containing the marker `<!-- github-openspec-sync: apply-complete -->`, and check `proposal.md`
   for an existing `**Pull Request:**` line. If both are found, this mode already ran for this
   change — report the existing comment and PR and stop (idempotent). If only one is found (e.g. a
   previous run posted the comment but the PR creation was declined), pick up from the missing step
   rather than redoing the part that already happened.
3. Gather what actually happened, from what is already on disk — do not re-derive or re-verify
   anything the apply step already recorded:
   - The commit list on the change's branch: `git log --oneline <base>..<branch>` (the branch is
     usually the current one, or one named after the change — confirm rather than guess if unclear).
   - The completed items and any verification notes already written into `tasks.md`'s
     "Verification" section (test/lint results, anything explicitly left unchecked or flagged as not
     run).
   - Any "Correction found during implementation" notes or similar in `design.md`, since these are
     exactly the kind of detail a human watching the issue would want surfaced, not buried.
4. Compose the summary text once: what was implemented, key decisions or corrections made along the
   way, verification status (call out anything **not** verified as plainly as `tasks.md` does — do
   not round a partial verification up to "done"), and the commit list. This same text is used for
   both the PR body and the issue comment below.
5. Push the branch and open the PR:
   - Confirm which branch holds the change's commits (usually the current branch, named after the
     change per `CLAUDE.md`'s git conventions and cut from `development` per
     `.claude/skills/git-workflow/SKILL.md`) and that it is based on an up-to-date `development`.
   - Show the user the branch name, PR title, and PR body — the PR body is the summary from step 4
     plus a closing line `Closes #<issue-number>` and a link to the change folder
     (`openspec/changes/<name>/`) — and ask for confirmation before doing anything that touches the
     remote. This is a push and a publicly visible PR, not a local edit, so always confirm even when
     the rest of this mode is being run without being asked.
   - On confirmation: push the branch (`git push -u origin <branch>` if it has no upstream yet, or a
     plain `git push` otherwise), then
     `gh pr create --title "<title>" --body "<body>" --base development --head <branch>`.
   - Add `**Pull Request:** #<number> (<url>)` as its own line directly under the
     `**GitHub Issue:**` line in `proposal.md`. Leave this edit staged, same as the issue link edit.
   - If the user declines the push/PR step, still complete step 6 (the issue comment) so the
     implementation summary isn't lost, and tell them the PR is still pending.
6. Compose the issue comment: the same summary from step 4, plus a line noting the PR
   (`Opened #<pr-number>, which will close this issue on merge.`) when one was created. End it with
   the marker line `<!-- github-openspec-sync: apply-complete -->` (on its own line, so it does not
   have to be shown to the user as part of the readable comment — mention to them that it's there
   for idempotency).
7. Post it: `gh issue comment <number> --body "<comment>"`. Do **not** close the issue in this
   mode — merging the PR is the normal closing event; **Mode: archive** closes it explicitly as a
   fallback if the merge didn't.
8. Report to the user, with the PR URL and the comment URL.

## Mode: archive

The linked PR merges into `development`, not the repository's default branch (`main`), so GitHub's
`Closes #<number>` keyword does **not** auto-close the issue on merge — that only fires for merges
into the default branch. So this mode's explicit close is the normal path, not a rare fallback. (An
issue can still already be closed here — someone closed it by hand, or a PR was merged straight
into `main` in an unusual case — hence the check below still branches on the issue's current state
rather than assuming it's always open.)

1. Find the linked issue. If none exists, tell the user there is nothing to close and stop — do not
   create one retroactively without asking.
2. Check the linked PR (`proposal.md`'s `**Pull Request:**` line, if present) and the issue's state:
   - **Issue already closed**: report that (and, if the PR is linked, that it was closed by the PR
     merge) and post a short comment noting the archive path
     (`openspec/changes/archive/<dated-name>/`) — no need to repeat the full summary. Do not call
     `gh issue close` again.
   - **Issue open, and a PR is linked but not yet merged**: this is unusual — archiving normally
     happens after merge. Tell the user the PR (`#<number>`) hasn't merged yet and confirm they
     still want to close the issue now before doing so.
   - **Issue open, and no PR was ever created (or none is linked)**: this is the fallback path this
     mode exists for. Proceed to close it directly.
3. When closing directly (either fallback case above), check whether **apply-complete** already ran
   (the marker comment from that mode).
   - If yes: compose a short closing comment (what was archived, and the archive path) — no need to
     repeat the full summary.
   - If no: compose the full summary described in **Mode: apply-complete** step 3-4, since this is
     the first and only chance to record it.
   - Close with the comment in one call: `gh issue close <number> --comment "<comment>"`.
4. Report to the user, with the issue URL, and note whether it was already closed by the merge or
   closed explicitly just now.

## Mode: link

Manually associates an existing change with an existing issue (backfill), or with one just found by
the title search in **Mode: propose** step 2.

1. Confirm the issue number and change name with the user if either was inferred rather than given
   directly — never guess-link an issue found only by a fuzzy title match without confirming first.
2. Add `**GitHub Issue:** #<number> (<url>)` under the `## Why` heading in `proposal.md`, exactly as
   in **Mode: propose** step 6.
3. Report the link.

## Mode: status

Reports, without changing anything: whether the change has a linked issue and the issue's state
(open/closed), whether apply-complete has already posted its summary, whether a PR has been opened
and, if so, its number, URL and merged/open state. Useful before deciding which mode to run, or
when the user just wants to check.

## Guardrails

- The only git/GitHub write actions this skill performs are: pushing the change's existing branch,
  creating a pull request from it, commenting on/closing a GitHub issue, and editing the two linking
  lines in `proposal.md`. It never creates or deletes branches, never merges a PR, never force-pushes,
  and never touches any other git remote operation. Branch creation and cleanup are
  `.claude/skills/git-workflow/SKILL.md`'s job, not this skill's.
- Always show the user the exact branch name, PR title, and PR body before pushing or running
  `gh pr create`, and wait for confirmation — this holds every time this mode runs, whether invoked
  proactively or on request, because a push and a public PR are visible, shared-state actions per
  `CLAUDE.md`'s risk policy, not local edits.
- Merging the pull request is always a human action. This skill never merges, and never asks the
  user to merge as a way of forcing the issue closed sooner.
- Never close or comment on an issue this skill did not itself locate via the `proposal.md` link or
  an explicit, user-confirmed issue number. A title search alone is a candidate, not a match — confirm
  before linking or acting on it.
- Never edit anything under `openspec/changes/archive/` (read-only, per `CLAUDE.md`) beyond reading
  it for context.
- Never edit `.claude/skills/openspec-*` or `.claude/commands/opsx/*` — those are generated; if this
  skill's own behaviour needs to change, edit this file instead.
- If `gh auth status` fails, stop and say so rather than guessing at authentication or retrying blindly.
- Every mode is idempotent: re-running it on a change that's already at or past that stage reports
  the current state instead of duplicating an issue, comment, or close.
- Keep the GitHub-facing text plain and factual — no medicine names, dosages, or other
  `CLAUDE.md`-restricted content ever end up in an issue (none of this repo's changes should produce
  any, but the rule that governs logs applies here too: never write user data to a public,
  third-party surface).
