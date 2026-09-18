---
name: git-workflow
description: Pillsner's branching model - main always mirrors production, development is the integration branch every feature lands on, and feature branches (named feature/<change-name>) are cut from development per OpenSpec change, each in its own git worktree. Use this before creating, merging, deleting or pushing any branch; before creating, entering or removing any worktree; right after /opsx:propose, to cut the change's feature branch and worktree; after a feature branch's pull request merges into development, to clean both up; and whenever asked how branching, worktrees or releases work here.
license: MIT
metadata:
  author: pillsner
  version: "1.1"
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
`feature/<name>` where `<name>` is exactly how `openspec/changes/<name>/` is named. Each feature
branch lives in its own **git worktree** (see below), never in the main checkout. They merge back
into `development` by pull request and are deleted, worktree included, once merged.

## Worktrees

Every change is worked on in its own [git worktree](https://git-scm.com/docs/git-worktree), not in
the main checkout. A worktree is a second working directory backed by the same `.git` history, with
its own branch checked out — so the main checkout can stay on `development` untouched while any
number of changes are being implemented in parallel, each in its own folder, without stashing or
switching branches in place.

- **Location**: a sibling directory next to the main checkout, named `<repo-dir>-<change-name>`.
  E.g. if the main checkout is at `.../pillsner`, the change `medicine-history-time-deviation` gets
  the worktree `.../pillsner-medicine-history-time-deviation`. Never create a worktree nested inside
  the repo itself (under `src/`, `openspec/`, or anywhere else in the tree) — Gradle and the IDE
  both walk the whole project tree and would pick it up as ordinary project content.
- **Creating one**: `git worktree add ../pillsner-<name> -b feature/<name> development`, run from
  the main checkout, as part of **Lifecycle** step 1 below.
- **Working in one**: once a change has a worktree, everything for that change — editing, building,
  running tests, committing, pushing — happens with that worktree's directory as the working
  directory, never in the main checkout. Any agent picking up a change locates or creates its
  worktree first (`git worktree list`, matched against `feature/<name>`), then works there for the
  rest of the session. The main checkout is never repurposed for feature work now that worktrees
  exist for that.
- **Listing**: `git worktree list` shows every worktree and the branch it has checked out. Check
  this before adding one — the change may already have one from an earlier session.
- **Removing one**: `git worktree remove ../pillsner-<name>`, run from the main checkout, as part of
  **Lifecycle** step 4, once the branch is merged and about to be deleted. It refuses if the
  worktree has uncommitted or unpushed changes — do not force past that without checking with the
  user first, per `CLAUDE.md`'s risk policy on hard-to-reverse actions.
- **Stale entries**: if a worktree's directory was deleted by hand instead of via `git worktree
  remove`, `git worktree list` still shows it. Clean that up with `git worktree prune`, not by
  editing anything under `.git/worktrees` directly.

## Lifecycle

1. **Starting a change** — right after `/opsx:propose` creates `openspec/changes/<name>/`, before
   the first implementation commit:
   - From the main checkout (never from inside another worktree), bring `development` up to date:
     `git fetch origin`, then fast-forward the local branch if it's behind `origin/development`.
   - Give the change its own worktree with its own feature branch, cut from `development`, never
     from `main`: `git worktree add ../pillsner-<name> -b feature/<name> development`.
   - From here on, all implementation work for this change happens inside `../pillsner-<name>`, not
     the main checkout.
2. **Working the change** — work inside the change's worktree directory. Commit to the feature
   branch as usual, following `CLAUDE.md`'s commit message conventions. Nothing about commits or
   messages changes; only where the work physically happens does.
3. **Finishing a change** — this is what the `github-openspec-sync` skill's **apply-complete** mode
   does: it pushes the feature branch and opens a pull request **against `development`**, not
   `main`, run from the change's worktree. Because `development` is not the repository's default
   branch, GitHub's `Closes #<number>` keyword will not auto-close the linked issue when that PR
   merges — so `github-openspec-sync`'s **archive** mode explicitly closing the issue is the normal
   path now, not a rare fallback.
4. **After the feature PR merges into `development`** — confirm the merge
   (`gh pr view <number> --json state,mergedAt`). Then, from the main checkout:
   - Remove the worktree first — a branch can't be deleted while a worktree still has it checked
     out: `git worktree remove ../pillsner-<name>`. If it refuses over uncommitted changes, stop and
     check with the user before forcing it; that may be work that was never pushed anywhere.
   - Delete the feature branch, local and remote: `git branch -d feature/<name>` and
     `git push origin --delete feature/<name>`.
   - Confirm with the user first if there's any doubt the merge actually happened, or if the branch
     holds commits `development` doesn't have yet.
5. **Cutting a release** — never do this unprompted. When the user asks to release or ship what's
   on `development`, offer to open the `development` → `main` pull request for them to review, or
   tell them the command (`gh pr create --base main --head development`) — do not create or merge
   it yourself unless they explicitly hand you that job for this one instance. Once they merge it,
   `release.yml` takes over.

## Guardrails

- Never commit directly to `main` or `development`. Everything goes through a feature branch and a
  pull request — including a one-line fix. The old "trivial fixes can go straight to `main`"
  exception is gone; a trivial fix still gets a feature branch off `development`, even a
  short-lived one.
- Never force-push `main` or `development`.
- Never merge any pull request yourself: not a feature → `development` PR, and especially not the
  `development` → `main` release PR. Merging is always a human action in this repository.
- Never open the `development` → `main` pull request without being asked to release — it is not
  part of the normal per-change loop and skips the deliberate, manual release decision this model
  exists to protect.
- Always confirm before deleting a branch, per `CLAUDE.md`'s risk policy on hard-to-reverse,
  shared-state actions.
- If `development` doesn't exist (a fresh clone made before this workflow existed, or the remote
  branch was deleted), stop and ask rather than inventing a substitute base.
- Never do feature work directly in the main checkout once a change has its own worktree — that
  reintroduces exactly the branch-juggling worktrees exist to avoid. Check `git worktree list`
  first; if a change's worktree already exists, use it instead of creating a second one or working
  in the main checkout.
- Always confirm before forcing a worktree removal over uncommitted or unpushed changes, same as
  before deleting a branch — it can lose work that exists nowhere else.
