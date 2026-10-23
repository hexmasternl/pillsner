---
name: git-workflow
description: Pillsner's branching model - main always mirrors production, development is the integration branch where OpenSpec proposals and archives are committed and pushed directly, and every change is implemented on its own feature/<change-name> branch in a git worktree under ../pillsner-worktrees, then delivered by pull request into development. Use this before creating, merging, deleting or pushing any branch; before creating a worktree; right after /opsx:propose and /opsx:archive, to commit and push on development; when /opsx:apply starts, to create the change's worktree and feature branch; when a change's tasks complete, to push and open and monitor the pull request; when archiving, to remove the change's worktree and branches; and whenever asked how branching, worktrees or releases work here.
license: MIT
metadata:
  author: pillsner
  version: "3.0"
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

**`development`** is the integration branch. It receives exactly two kinds of change:

- **OpenSpec bookkeeping, committed and pushed directly**: a newly proposed change
  (`openspec/changes/<name>/`) and an archived change (the move into `openspec/changes/archive/`
  plus the spec sync into `openspec/specs/`). These are planning artifacts, not code, and land on
  `development` straight away so every worktree and collaborator sees them.
- **Implementation, by pull request** from a feature branch. No application code is ever committed
  to `development` directly.

**Feature branches** are where implementation happens: one per OpenSpec change (or per trivial fix
that still needs its own branch), cut from the current tip of `development`, named
`feature/<name>` where `<name>` is exactly how `openspec/changes/<name>/` is named. Each one lives
in its own git worktree, not a `checkout` inside the main working copy. They merge back into
`development` by pull request, and are removed together with their worktree when the change is
archived.

## Worktrees

**`main` and `development` always live in the default worktree** — the repository root itself
(the main checkout). Neither is ever checked out in a worktree under `../pillsner-worktrees/`, and
no feature branch is ever checked out in the default worktree. The default worktree normally sits
on `development`; it only switches to `main` briefly when there's a reason to look at `main`, and
then goes back to `development`.

Every feature branch gets its own worktree, outside the repository root, in the sibling folder
`../pillsner-worktrees/<name>`, where `<name>` matches both the OpenSpec change and the branch
(`feature/<name>`). This keeps the main checkout on `development` at all times, lets multiple
changes be worked in parallel without stashing, and keeps a worktree's build output
(`src/build/`, `.gradle/`) fully separate from any other checkout's.

Worktrees share the same repository and refs no matter which checkout you run the command from, so
`git worktree add` can be issued from the main checkout or from inside another worktree — it
doesn't matter which, as long as local `development` is up to date first.

Do not use the `EnterWorktree` tool for this. Its default base ref is
`origin/<repository default branch>`, which on GitHub is `main` here, not `development` — a
worktree it creates would be branched from the wrong place. Use the explicit `git worktree`
commands below instead, which pin the base ref to `development` every time.

## Lifecycle

0. **Making sure `development` exists** — before any step below, run `git fetch origin` and check
   for `origin/development`. If it doesn't exist, create it from `main` and publish it:
   `git branch development origin/main`, then `git push -u origin development`, then switch the
   main checkout to it (`git switch development`). If only the local branch is missing, track the
   remote one instead (`git switch development`, which creates it from `origin/development`).
1. **Proposing a change** — `/opsx:propose` runs in the main checkout, on `development`:
   - Before proposing, bring `development` up to date (`git pull --ff-only`).
   - Right after the change's artifacts are created (and after `github-openspec-sync`'s
     **propose** mode has added the issue link to `proposal.md`), immediately commit everything
     under `openspec/changes/<name>/` on `development` and push it: `git push origin development`.
     Use a message such as `Propose <name>`, with the change name in the body.
   - No worktree or feature branch is created yet — that happens when the change is applied.
2. **Starting to apply a change** — when `/opsx:apply` begins on a change that has no worktree yet:
   - Bring `development` up to date: `git fetch origin`, then fast-forward the local branch if
     it's behind `origin/development`.
   - Create the worktree and cut the feature branch inside it in one step, from `development`,
     never from `main`: `git worktree add ../pillsner-worktrees/<name> -b feature/<name>
     development`.
   - From here on, all work for this change happens inside that worktree directory, not in the
     main checkout — including the `tasks.md` checkbox updates.
   - If the worktree already exists (resuming interrupted work), reuse it instead.
3. **Working the change** — commit very frequently, from inside the worktree, so the branch
   history is a clear trace of the work done:
   - At minimum one commit per completed task in `tasks.md`, together with that task's checkbox
     update. Commit smaller logical steps within a task too (a new model, its tests, the wiring)
     rather than batching a whole task into one commit.
   - Never leave a session with uncommitted work in the worktree.
   - Follow `CLAUDE.md`'s commit message conventions: imperative mood, change name in the body.
4. **Manual testing tasks** — when a task can only be verified by a human (on a device, with
   TalkBack, across a reboot or a time zone change, etc.), do everything that can be done for it,
   then mark it complete and move on. It does not block the push or the pull request. Record
   exactly what still has to be tested by hand, with steps and expected results, so it can go into
   the pull request (step 5).
5. **Finishing a change** — once every task in `tasks.md` is `[x]`:
   - Run the `verify-change` skill, which writes the change's `## Verification` section.
   - Make a final commit of anything outstanding, then push the feature branch:
     `git push -u origin feature/<name>` (or plain `git push` once it has an upstream).
   - Once everything is pushed, open a pull request from `feature/<name>` **into `development`**.
     This is what `github-openspec-sync`'s **apply-complete** mode does; it does not ask for
     confirmation first, because pushing and opening the PR is the agreed end of every change.
   - If any task needs manual testing, the PR summary must open with a prominent callout, before
     anything else in the body, so a reviewer cannot miss it:

     ```markdown
     > [!WARNING]
     > **Manual testing required before merge**
     >
     > - [ ] <what to test> — <steps> — <expected result>
     ```

     One unchecked checkbox per manual test, so the reviewer can tick them off in the PR itself.
   - Because `development` is not the repository's default branch, GitHub's `Closes #<number>`
     keyword will not auto-close the linked issue when the PR merges — `github-openspec-sync`'s
     **archive** mode closes it explicitly.
6. **Monitoring the pull request** — after opening it, watch it until it is mergeable:
   - Check `gh pr view <number> --json mergeable,mergeStateStatus,statusCheckRollup`.
     `mergeable` is often `UNKNOWN` right after a push while GitHub computes it; check again
     shortly after (use the `Monitor` tool or `gh pr checks <number> --watch` rather than a sleep
     loop).
   - **Merge conflicts** (`mergeable: CONFLICTING` / `mergeStateStatus: DIRTY`): in the worktree,
     `git fetch origin` and `git merge origin/development`, resolve the conflicts — respecting what
     landed on `development` and keeping this change's intent — run the unit tests and lint again,
     commit the merge and `git push`. Never rebase a pushed feature branch and never force-push;
     merging `development` in keeps the history intact.
   - **Failing checks**: read the failing check's log (`gh run view <run-id> --log-failed`), fix
     the cause in the worktree, commit and push.
   - **Review comments** (Copilot reviews every push): handle them with the `pr-review-response`
     skill.
   - If a conflict can't be resolved without a product decision (both sides changed the same
     behaviour in incompatible ways), stop and ask the user rather than picking a side.
   - Repeat until the PR reports no conflicts and no failing checks, then tell the user it's ready
     for review. Merging stays a human action.
7. **Archiving a change** — `/opsx:archive` runs in the main checkout, on `development`, after the
   feature PR has merged:
   - Confirm the merge (`gh pr view <number> --json state,mergedAt`) and pull `development`
     (`git pull --ff-only`) so the archive starts from the merged `tasks.md`. If the PR hasn't
     merged, stop and ask the user — archiving on `development` before the implementation is there
     would archive an incomplete change.
   - Run the archive, then immediately commit the result (the move into
     `openspec/changes/archive/`, the synced `openspec/specs/`, and nothing else) on `development`
     and push it: `git push origin development`. Use a message such as `Archive <name>`.
   - Remove everything created for the change:
     - Stop any Gradle daemon running from the worktree first (`./gradlew --stop` from its `src`),
       since on Windows it holds file locks that make the removal fail.
     - `git worktree remove ../pillsner-worktrees/<name>`. Add `--force` only when the refusal is
       about ignored build output alone; if it reports uncommitted or unpushed work, stop and ask.
     - `git branch -d feature/<name>`. If `-d` refuses because the PR was squash-merged (its
       commits aren't literally in `development`), use `-D` — the merge was already confirmed
       above.
     - If the remote branch still exists (`git ls-remote --heads origin feature/<name>`), delete it
       with `git push origin --delete feature/<name>`.
     - `git worktree prune` to clear any stale worktree metadata.
8. **Cutting a release** — never do this unprompted. When the user asks to release or ship what's
   on `development`, offer to open the `development` → `main` pull request for them to review, or
   tell them the command (`gh pr create --base main --head development`) — do not create or merge
   it yourself unless they explicitly hand you that job for this one instance. Once they merge it,
   `release.yml` takes over.

## Guardrails

- Never commit directly to `main`. Everything reaches `main` through the user's release pull
  request.
- Commit directly to `development` only for the two OpenSpec bookkeeping steps (propose and
  archive), and commit only the OpenSpec files those steps produced. All other work — including a
  one-line fix — goes through a feature branch, in its own worktree, and a pull request.
- If a push to `development` is rejected (the `protect-development` ruleset on GitHub can require
  pull requests for every change), stop and tell the user. Do not work around it by force-pushing,
  pushing to a different branch, or reconfiguring the ruleset unasked.
- Never force-push `main`, `development`, or a pushed feature branch.
- Never merge any pull request yourself: not a feature → `development` PR, and especially not the
  `development` → `main` release PR. Merging is always a human action in this repository.
- Never open the `development` → `main` pull request without being asked to release — it is not
  part of the normal per-change loop and skips the deliberate, manual release decision this model
  exists to protect.
- Never use `EnterWorktree`/`ExitWorktree` for feature work here; use `git worktree add` /
  `git worktree remove` directly so the base ref is always `development`.
- Never create a worktree inside the repository root; worktrees always live in
  `../pillsner-worktrees/`.
- `main` and `development` are only ever checked out in the default worktree (the repository
  root). Never `git worktree add` either of them, and never check a feature branch out in the
  default worktree.
- Never discard work while cleaning up: a worktree or branch holding uncommitted or unpushed
  changes, or one whose PR hasn't merged, is a reason to stop and ask, not to force removal.
- If a worktree for `<name>` already exists (resuming interrupted work), reuse it — `git worktree
  add` a second time for the same branch fails; switch into the existing directory instead of
  creating a new one.
