---
name: change-delivery-loop
description: The main session's loop for delivering a Pillsner change with agents - have pillsner-proposal-critic review the proposal before any code, hand each task to the implementer (pillsner-kotlin-expert for non-UI Kotlin, pillsner-designer for UI) with pillsner-translator translating new English text in parallel, send what it changed to pillsner-test-runner, then have the finished change reviewed by the matching critics (pillsner-app-critic, pillsner-wear-critic, pillsner-play-critic) for at most two rounds, record the outcomes in the critic ledger, and stop and report when the critics are still not satisfied. Use during /opsx:apply when implementing a change with agents, when asked to "implement this with the agents", "run the critic loop" or "get this reviewed", and when resuming a change whose review is in progress.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

Subagents cannot start other agents, so the coordination lives here, in the main session. The agents
each do one job: implementers write code and tests, the test runner runs tests, critics find what is
wrong. This skill passes work between them, enforces the limits, and keeps the record.

It runs inside the `/opsx:apply` lifecycle: the change's worktree and `feature/<change-name>` branch
exist (the `git-workflow` skill), and `tasks.md` is the list of work.

## 0. Review the proposal first

Before the first task, run `pillsner-proposal-critic` on the change, at most two rounds: round 1 on the proposal as written; if a Blocker or Major is left, revise the proposal, design, delta specs or tasks (or have the author do it) and run round 2. Open findings after round 2 go to the user, who decides whether to apply anyway. Record the rows in the ledger (step 5). Skip this step only when resuming a change whose proposal was already reviewed and has not changed since.

## 1. Implement, one task at a time

Pick the implementer by what the task produces:

| Task produces | Implementer |
| --- | --- |
| Domain models, use cases, scheduling, Room, repositories, alarms, receivers, sync, view models and UI state | `pillsner-kotlin-expert` |
| Composables, theme, notification layouts, copy, icons | `pillsner-designer` |
| Both (a new screen with its view model) | The Kotlin expert first (view model and `UiState`), then the designer (the composables that consume them) |

Brief the implementer with the change name, the worktree, the task (number and text) and anything
earlier tasks left for it. Every implementer ends with a **handoff**:

```text
change: <name>
worktree: ../pillsner-worktrees/<name>
task: <number>
changed: <main and test files>
new tests: <test classes added or changed>
notes: <anything the next step must know>
```

Keep each implementer alive across the change and continue it with `SendMessage`, so it keeps what it
learned about the code.

**Translations run in parallel.** Implementers write English only, following the `app-strings` skill. When a handoff changes English strings, website pages or release notes, send a translation request to `pillsner-translator` (change, worktree, the keys and files) and carry on with the next task while it works. Lint reports `MissingTranslation` until it is done; that is expected between the two, never a reason to put English in a locale file. All translations must be committed before step 3, and its length flags go to the designer.

## 2. Test each task

Send the handoff to `pillsner-test-runner` as a test request with `scope: task`. Start it once per
change and continue it with `SendMessage` for each later task. It runs every unit test and only the
instrumented tests the task affects.

- **Green**: tick nothing yourself; the implementer has already marked the task. Go to the next task.
- **Red**: send the failures verbatim to the implementer, which fixes them and returns a new handoff
  with the failing tests named under `rerun`. Send that to the test runner. If the same test is still
  red after two fix attempts, stop and report to the user with both reports.

## 3. Verify the branch

When every task is done:

1. Run the `verify-change` skill. Its test step goes to the test runner with `scope: branch`; the
   test runner's report becomes the instrumented-test line of the Verification section.
2. Fix anything red through the implementer, as in step 2.

## 4. Review: at most two rounds

Pick the critics from the branch diff (`git diff --name-only development...HEAD`). Run every critic
that applies, in parallel:

| Diff touches | Critic |
| --- | --- |
| `src/app` (except `data/wear/`) | `pillsner-app-critic` |
| `src/wear`, `src/shared`, `src/app/.../data/wear/` | `pillsner-wear-critic` |
| A manifest, a Gradle file, the version catalog, permissions, backup rules, `.github/workflows/release.yml`, the website privacy page, or the change is a release | `pillsner-play-critic` |

**Brief for independence.** Give each critic the change name, the worktree, the scope
(`git diff development...HEAD`) and the round number; in round 2, also the previous round's findings
with the implementer's fix or dispute for each. Do not pass the implementer's reasoning, a summary of
how the change was built, or your own opinion of what is fine. That is what anchors a reviewer on the
author's blind spots.

**Round 1.** A critic is **satisfied** when its verdict is *ship*, or no *Blocker* or *Major* is left.
When all critics are satisfied, have the implementer fix the cheap Minors and go to step 5.

**Fix.** Otherwise send every Blocker and Major to the implementer that owns the code. For each one it
either fixes it (a finding whose proof is a failing test gets that test first, watched failing) or
disputes it in one or two sentences grounded in the code or the spec. Never accept a fix that weakens
or deletes a test. Re-test through step 2 with the new handoff.

**Round 2.** Run the critics that were not satisfied, with round 2 and the previous findings, fixes
and disputes.

**Stop.** There is no round 3. If a Blocker or Major is still open or disputed after round 2, stop the
change here and report to the user (step 6). The critics' own definitions allow a third round; this
loop deliberately does not use it.

## 5. Record the outcomes

The critics are read-only, so you record. In `.claude/critic-ledger.md`, paste each review's ledger
rows and set the outcome: `real` for findings that were fixed, `noise` for findings the critic
withdrew or that turned out wrong, `accepted-risk` only when the user decides so, `open` for anything
left when the loop stopped. A review with no findings gets one `clean` row. Commit the ledger with the
change.

## 6. Report

Lead with one line: **ready for pull request**, **ready with open findings** or **stopped**, and why.

Then: the tasks done and by whom; the last test runner report, verbatim where red; each critic round
with its verdict and every finding by ID with what happened to it; and, when stopped, the open
findings with the critic's position and the implementer's side by side. When ready, continue with
`github-openspec-sync` apply-complete as usual.

## 7. When CI fails on the pull request

Hand the failed run to `pillsner-ci-doctor`. It classifies each failure (regression, pre-existing, flaky, infrastructure) and returns test requests for the regressions. Send those to the test runner, the confirmed regressions to the implementer as in step 2, and push the fix. Re-run a job only when the CI doctor classified every failure in it as flaky or infrastructure. A CI fix after review does not reopen the critic rounds unless it changes behaviour; if it does, it uses the remaining round, if any.

## Guardrails

- Never review the work yourself in place of a critic, and never mark a finding resolved that its
  critic has not confirmed.
- Never run a third review round, and never re-run round 1 to reset the count.
- Never let the implementer and the critic argue directly; every message between them goes through
  this loop, so the round count holds.
- If the user asks to skip review for a change, record that in the report and the ledger note, and do
  not open the pull request as if it had been reviewed.
