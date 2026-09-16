---
name: git-workflow
description: Pillsner's branching model - main always mirrors production, development is the integration branch every feature lands on, and feature branches are cut from development per OpenSpec change. Use this before creating, merging, deleting or pushing any branch; right after /opsx:propose, to cut the change's feature branch; after a feature branch's pull request merges into development, to clean it up; and whenever asked how branching or releases work here.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
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
that still needs its own branch), cut from the current tip of `development`, named after the change
exactly as `openspec/changes/<name>/` is named. They merge back into `development` by pull request
and are deleted once merged.

## Lifecycle

1. **Starting a change** — right after `/opsx:propose` creates `openspec/changes/<name>/`, before
   the first implementation commit:
   - Bring `development` up to date: `git fetch origin`, then fast-forward the local branch if
     it's behind `origin/development`.
   - Cut the feature branch from it, never from `main`: `git checkout -b <name> development`.
2. **Working the change** — commit to the feature branch as usual, following `CLAUDE.md`'s commit
   message conventions. Nothing here changes.
3. **Finishing a change** — this is what the `github-openspec-sync` skill's **apply-complete** mode
   does: it pushes the feature branch and opens a pull request **against `development`**, not
   `main`. Because `development` is not the repository's default branch, GitHub's `Closes
   #<number>` keyword will not auto-close the linked issue when that PR merges — so
   `github-openspec-sync`'s **archive** mode explicitly closing the issue is the normal path now,
   not a rare fallback.
4. **After the feature PR merges into `development`** — confirm the merge
   (`gh pr view <number> --json state,mergedAt`), then delete the feature branch, local and remote:
   `git branch -d <name>` and `git push origin --delete <name>`. Confirm with the user first if
   there's any doubt the merge actually happened, or if the branch holds commits `development`
   doesn't have yet.
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
