---
name: bug-triage
description: Turn a Pillsner bug report (a GitHub issue labelled bug and triage, from the bug report template or the website's report-a-bug page) into a reproduction, a failing test, and the right route to a fix - a trivial fix on its own feature branch, or an OpenSpec change linked to the issue. Use when asked to "triage", "look at issue #n", "fix this bug", or to go through the open triage queue.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

Bug reports arrive through `.github/ISSUE_TEMPLATE/bug_report.yml` (labels `bug` and `triage`),
directly or from the website's report-a-bug form. Each one gets reproduced before anything is changed,
and every fix starts with a test that fails. The `pillsner-issue-triager` agent can run steps 1-5 in the
background and hand back the failing test, the route and a draft reply.

## 1. Pick and read

- The queue: `gh issue list --label triage --state open --json number,title,createdAt`.
- `gh issue view <n> --comments`. Note app version, Android version, device, and the steps.
- Reporters sometimes paste their own medicine names or doses. Never repeat those anywhere — not in
  comments, commits, test names or test data. Use made-up names.

## 2. Reproduce

- Find the spec that covers the behaviour (`openspec/specs/`). Is the report a bug (the app
  contradicts the spec), a missing spec (nobody decided), or a request (the spec says the app should
  do what it does)?
- Reproduce on the emulator with the debug build. For reminders that didn't arrive or arrived wrong,
  use the `adb-device-scenarios` skill; the in-app reminder diagnostics (Settings) show what the app
  recorded.
- Can't reproduce: say what was tried, and ask the reporter one specific question (after showing the
  user the comment, see step 5). Leave `triage` on.

## 3. Write the failing test

Before any fix: the smallest test that shows the bug — a unit test for domain and scheduling logic
(edge cases around midnight, month ends, DST, time zones), a DAO or migration test for data, a
Compose test for UI. It must fail on `development` for the reason the report describes.

## 4. Choose the route

`CLAUDE.md` decides:

- **Trivial** (an obvious cause, a small fix, no behaviour or spec change, no scheduling logic, no
  dependency): a feature branch per `git-workflow` — `feature/fix-<short-slug>` in
  `../pillsner-worktrees/fix-<short-slug>`, cut from `development`. Commit the test, then the fix,
  run `verify-change`, push, and open a PR into `development` whose body says `Fixes #<n>` and
  describes cause and fix.
- **Anything else** — scheduling or reminder logic, a behaviour change, a spec that's wrong or
  missing, a migration: `/opsx:propose` a change, then link the existing issue with
  `github-openspec-sync` in **link** mode (not propose mode, which would open a duplicate issue). The
  failing test becomes the change's first task.
- **Not a bug** (works as specified): explain on the issue which behaviour is intended and why
  (quoting the spec), and suggest a feature request if the reporter wants it to change.

## 5. Update the issue

Show the user the comment before posting — it's a public reply to someone outside the project.
Then post it: what was found (cause in one or two plain sentences), and what happens next (the PR or
the change). Swap the labels once triaged: `gh issue edit <n> --remove-label triage`. Don't add
labels the repository doesn't already have (`gh label list`).

## Guardrails

- Never fix a bug without a test that failed first.
- Never close an issue from this skill; a merged fix or the archive step closes it.
- Never promise a release date or version in an issue comment.
