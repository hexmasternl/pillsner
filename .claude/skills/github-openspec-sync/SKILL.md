---
name: github-openspec-sync
description: Keep a GitHub issue and an OpenSpec change in sync - create and link an issue when a change is proposed, and update it with a detailed implementation summary (then close it) once the change is fully implemented. Invoke this proactively, without being asked, immediately after openspec-propose finishes creating a change's artifacts, immediately after openspec-apply-change reports every task complete, and immediately after openspec-archive-change archives a change. Also use it when the user explicitly asks to link, sync, or create a GitHub issue for a change.
license: MIT
compatibility: Requires the gh CLI (authenticated) and the openspec CLI.
metadata:
  author: pillsner
  version: "1.0"
---

Keep an OpenSpec change and a GitHub issue in sync, end to end: create the issue when the change is
proposed, and update + close it with what was actually done once the change is implemented.

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

## How a change and an issue are linked

The source of truth is a line near the top of the change's `proposal.md` (search for it there
first, before creating anything):

```
**GitHub Issue:** #<number> (<url>)
```

If that line is missing, check whether an issue already exists before assuming there is none:

```bash
gh issue list --search "<change-name> in:title" --state all --json number,title,url,state
```

If a matching issue is found, treat it as linked and back-fill the `proposal.md` line (see
**Mode: link** below) instead of creating a duplicate.

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

1. Find the linked issue (see lookup above). If none exists, tell the user and offer to create one
   now (**Mode: propose**) before continuing; if they decline, stop here.
2. Check the issue's existing comments (`gh issue view <number> --json comments`) for one already
   containing the marker `<!-- github-openspec-sync: apply-complete -->`. If found, this mode
   already ran for this change — report that and stop (idempotent).
3. Gather what actually happened, from what is already on disk — do not re-derive or re-verify
   anything the apply step already recorded:
   - The commit list on the change's branch: `git log --oneline <base>..<branch>` (the branch is
     usually the current one, or one named after the change — confirm rather than guess if unclear).
   - The completed items and any verification notes already written into `tasks.md`'s
     "Verification" section (test/lint results, anything explicitly left unchecked or flagged as not
     run).
   - Any "Correction found during implementation" notes or similar in `design.md`, since these are
     exactly the kind of detail a human watching the issue would want surfaced, not buried.
4. Compose a comment: what was implemented, key decisions or corrections made along the way,
   verification status (call out anything **not** verified as plainly as `tasks.md` does — do not
   round a partial verification up to "done"), and the commit list. End it with the marker line
   `<!-- github-openspec-sync: apply-complete -->` (on its own line, so it does not have to be shown
   to the user as part of the readable comment — mention to them that it's there for idempotency).
5. Post it: `gh issue comment <number> --body "<comment>"`. Do **not** close the issue in this mode —
   archiving is the closing event.
6. Report to the user, with the comment URL.

## Mode: archive

1. Find the linked issue. If none exists, tell the user there is nothing to close and stop — do not
   create one retroactively without asking.
2. If the issue is already closed, report that and stop (idempotent).
3. Check whether **apply-complete** already ran (the marker comment from step above).
   - If yes: compose a short closing comment (what was archived, and the archive path
     `openspec/changes/archive/<dated-name>/`) — no need to repeat the full summary.
   - If no: compose the full summary described in **Mode: apply-complete** step 3-4, since this is
     the first and only chance to record it.
4. Close with the comment in one call: `gh issue close <number> --comment "<comment>"`.
5. Report to the user, with the issue URL, and note it is now closed.

## Mode: link

Manually associates an existing change with an existing issue (backfill), or with one just found by
the title search in **Mode: propose** step 2.

1. Confirm the issue number and change name with the user if either was inferred rather than given
   directly — never guess-link an issue found only by a fuzzy title match without confirming first.
2. Add `**GitHub Issue:** #<number> (<url>)` under the `## Why` heading in `proposal.md`, exactly as
   in **Mode: propose** step 6.
3. Report the link.

## Mode: status

Reports, without changing anything: whether the change has a linked issue, the issue's state
(open/closed), and whether apply-complete has already posted its summary. Useful before deciding
which mode to run, or when the user just wants to check.

## Guardrails

- Never push commits, create branches, merge, or touch git remotes — this skill only talks to
  GitHub Issues (create, comment, close) and edits one line in one file (`proposal.md`).
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
