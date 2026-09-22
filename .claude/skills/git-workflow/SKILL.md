---
name: git-workflow
description: Pillsner's branching model - main always mirrors production, development is the integration branch every feature lands on, and feature branches (named feature/<change-name>) are cut from development per OpenSpec change, each in its own git worktree. Use this before creating, merging, deleting or pushing any branch; before creating a worktree; right after /opsx:propose, to set up the change's worktree and feature branch; after a feature branch's pull request merges into development, to clean it up; and whenever asked how branching, worktrees or releases work here.
license: MIT
metadata:
  author: pillsner
  version: "2.0"
---

Pillsner's branching model, and the one place it's defined in full. `CLAUDE.md`'s Git conventions
section only points here — if the two ever disagree, this file wins and `CLAUDE.md` needs updating
to match.

## The three kinds of branch

**`main`** mirrors what's running in production. `.github/workflows/release.yml` builds, signs and
publishes both app bundles to Google Play on every push to `main`, then tags the commit and creates
the GitHub release; `website.yml` deploys the marketing site on every push to `main` that touches
`src/website/**`. A push to `main` is a release, not a checkpoint. Nothing ever commits to `main`
directly. It is only updated by a `development` → `main` pull request, and that pull request is
opened **by the user, manually**, when they choose to cut a release.

**`development`** is the integration branch. Every finished feature lands here first, by pull
request, before it's ever part of a release. It carries everything merged so far that hasn't been
released yet.

**Feature branches** are where work actually happens: one per OpenSpec change (or per trivial fix
that still needs its own branch), cut from the current tip of `development`, named
`feature/<name>` where `<name>` is exactly how `openspec/changes/<name>/` is named. Each one lives
in its own git worktree, not a `checkout` inside the main working copy. They merge back into
`development` by pull request and both the branch and its worktree are removed once merged.

`development` is a **protected branch** on GitHub (like `main`): nothing pushes to it directly,
including force-pushes. The only way changes reach it is a feature branch's pull request.

## Worktrees

Every feature branch gets its own worktree, checked out as a sibling of the repository, not nested
inside it: `../pillsner-worktrees/<name>`, where `<name>` matches both the OpenSpec change and the
branch (`feature/<name>`). This keeps the main checkout on `development` at all times, lets
multiple changes be worked in parallel without stashing, and keeps a worktree's build output
(`src/build/`, `.gradle/`) fully separate from any other checkout's.

Worktrees share the same repository and refs no matter which checkout you run the command from, so
`git worktree add` can be issued from the main checkout or from inside another worktree — it
doesn't matter which, as long as local `development` is up to date first.

Do not use the `EnterWorktree` tool for this. Its default base ref is
`origin/<repository default branch>`, which on GitHub is `main` here, not `development` — a
worktree it creates would be branched from the wrong place. Use the explicit `git worktree`
commands below instead, which pin the base ref to `development` every time.

## Lifecycle

1. **Starting a change** — right after `/opsx:propose` creates `openspec/changes/<name>/`, before
   the first implementation commit:
   - Bring `development` up to date: `git fetch origin`, then fast-forward the local branch if
     it's behind `origin/development`.
   - Create the worktree and cut the feature branch inside it in one step, from `development`,
     never from `main`: `git worktree add ../pillsner-worktrees/<name> -b feature/<name>
     development`.
   - From here on, all work for this change happens inside that worktree directory, not in the
     main checkout.
2. **Working the change** — commit to the feature branch as usual, from inside its worktree,
   following `CLAUDE.md`'s commit message conventions. Nothing here changes.
3. **Finishing a change** — this is what the `github-openspec-sync` skill's **apply-complete** mode
   does: it pushes the feature branch and opens a pull request **against `development`**, not
   `main`. Because `development` is not the repository's default branch, GitHub's `Closes
   #<number>` keyword will not auto-close the linked issue when that PR merges — so
   `github-openspec-sync`'s **archive** mode explicitly closing the issue is the normal path now,
   not a rare fallback.
4. **After the feature PR merges into `development`** — confirm the merge
   (`gh pr view <number> --json state,mergedAt`), then remove the worktree and delete the branch,
   local and remote: `git worktree remove ../pillsner-worktrees/<name>` (add `--force` only if it
   still holds ignored build output and nothing else), then `git branch -d feature/<name>` and
   `git push origin --delete feature/<name>`. Confirm with the user first if there's any doubt the
   merge actually happened, or if the branch holds commits `development` doesn't have yet.
5. **Cutting a release** — never do this unprompted. When the user asks to release or ship what's
   on `development`, offer to open the `development` → `main` pull request for them to review, or
   tell them the command (`gh pr create --base main --head development`) — do not create or merge
   it yourself unless they explicitly hand you that job for this one instance. Once they merge it,
   `release.yml` takes over.

## Guardrails

- Never commit directly to `main` or `development`. Everything goes through a feature branch, in
  its own worktree, and a pull request — including a one-line fix. The old "trivial fixes can go
  straight to `main`" exception is gone; a trivial fix still gets a feature branch off
  `development`, even a short-lived one.
- Never force-push `main` or `development` — both are protected branches, and `development` will
  reject a direct push or force-push outright.
- Never merge any pull request yourself: not a feature → `development` PR, and especially not the
  `development` → `main` release PR. Merging is always a human action in this repository.
- Never open the `development` → `main` pull request without being asked to release — it is not
  part of the normal per-change loop and skips the deliberate, manual release decision this model
  exists to protect.
- Never use `EnterWorktree`/`ExitWorktree` for feature work here; use `git worktree add` /
  `git worktree remove` directly so the base ref is always `development`.
- Always confirm before removing a worktree or deleting a branch, per `CLAUDE.md`'s risk policy on
  hard-to-reverse, shared-state actions. `git worktree remove` refuses on uncommitted or unpushed
  changes unless forced — treat that refusal as a signal to stop and check with the user, not to
  add `--force`.
- If `development` doesn't exist (a fresh clone made before this workflow existed, or the remote
  branch was deleted), stop and ask rather than inventing a substitute base.
- If a worktree for `<name>` already exists (resuming interrupted work), reuse it — `git worktree
  add` a second time for the same branch fails; switch into the existing directory instead of
  creating a new one.
