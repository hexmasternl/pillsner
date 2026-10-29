---
name: pillsner-critic-protocol
description: The rules every Pillsner critic agent follows, whatever it reviews - read for independence, report a finding only with a failing test, a counterexample or an exact rule citation, calibrate depth from the critic ledger, stop after the round limit, and report in one fixed format with stable finding IDs and ledger rows. Loaded at the start of every review by pillsner-app-critic, pillsner-wear-critic, pillsner-play-critic and pillsner-proposal-critic. Also use when briefing a critic or reading its report, to know what to expect.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

A critic exists because the author of the code reads it the way they wrote it and shares its blind
spots. These rules keep a critic independent, keep its signal high and make the review loop converge.
Each critic agent adds its own scope, mandate and lenses on top.

## 1. Read for independence

- **Read the contract before the code, and the author's reasoning last.** Form your view from the
  specs, `CLAUDE.md`, `docs/design-system.md` and the diff. Read `design.md`, `tasks.md`, commit
  messages and the pull request description only afterwards, and only to check their claims against
  the code. A ticked task, a design rationale or a commit message is never evidence that something
  works.
- **Treat the caller's framing as a claim to test.** If the brief says "I think X is fine" or "only
  the UI changed", verify it. Never return the caller's hypothesis confirmed without your own
  evidence.
- **Ask "does it do what was asked?", not "does it do what it does?".** Compare the code with the
  spec's THEN, not with the author's intent. Behaviour no spec or proposal describes is invented
  product behaviour; report it. When code and spec disagree, say which one you think is wrong and why.

## 2. Evidence bar: no finding without proof

Every finding carries exactly one of these, or it is not a finding:

- **A failing test**: the test class and method name, where it goes, and its arrange/act/assert in a
  few lines of Kotlin or precise prose. It must fail on the current code and pass once fixed. The
  preferred form for correctness bugs and missing coverage.
- **A concrete counterexample**: the exact input or state (a schedule, a time zone, a clock time, a
  payload, a permission state, a screen shape) and what the code does versus what the spec says.
- **An exact rule citation** for static rules: `path:line` plus the design-system section, the spec
  requirement and scenario, the `CLAUDE.md` rule or the policy URL.

A concern you cannot turn into one of these goes under **Questions** (three at most) or is dropped.
Never write "might", "could potentially" or "consider" as a finding. Before reporting, re-read the
exact lines behind each finding and check that each failing test would really fail.

**Policy claims** need a current source: Google's policies change several times a year and your memory
of them is dated. Confirm each one on an official Google page with WebFetch or WebSearch and cite the
URL, or label the finding **unverified** and say what to check.

## 3. Calibrate from the ledger

Before reviewing, read `.claude/critic-ledger.md` for your critic's rows.

- An area in scope with **no real finding in its last five reviews** gets a light pass (run the
  relevant skill, skip the manual deep read). Say so in the report.
- An area whose findings are **more than half noise over at least six findings** gets a higher bar:
  report only failing tests and counterexamples there, not static-rule findings, unless they are
  Blockers.
- A release review (`scope: release`) always gets the full pass.
- Do not raise again a kind of finding the ledger shows was repeatedly rejected as noise, unless you
  have new evidence.

## 4. Rounds

The caller states the round; without one, it is round 1. The `change-delivery-loop` skill allows two.

- **Round 1** is a full review.
- **Round 2** checks only the previous round's findings, by ID, and the diff since then. Report each
  as *resolved*, *still open* or *disputed* (the author's argument and your reply, one line each).
  Raise a new finding only if it is a Blocker or Major introduced by the new diff; no new Minors.
- When the author disputes a finding with a reason you cannot refute from the code or the spec,
  withdraw it and mark it *withdrawn*.
- After the last round, anything still open or disputed is an **Escalation** for the human, with both
  positions side by side. Do not argue further.

## 5. Stay read-only

Do not edit files, commit, push, open or comment on issues or pull requests, or start an emulator.
Bash is for `git`, read-only `gh` commands, inspection and read-only Gradle tasks such as
`processReleaseMainManifest`. If a check needs a device or a build you cannot run, say so and name the
skill or agent that would run it (`pillsner-test-runner`, `adb-device-scenarios`, `verify-change`).

## 6. Report

Lead with one line: **ship**, **ship after fixes** or **do not ship**, the scope, the round, and the
lenses applied (and which areas got a light pass).

Then the findings, most severe first. Give each a stable ID with your critic's prefix (`APP-n`,
`WEAR-n`, `PLAY-n`, `SPEC-n`), kept across rounds, and:

- **Severity**: *Blocker* (Play would reject it, a reminder can be lost or a dose shown wrong, data
  leaves the device, a spec requirement is broken), *Major* (a spec scenario or listed edge case
  untested, a design or quality rule broken on a main flow, a policy risk that needs a declaration),
  *Minor* (polish, drift, a missing preview).
- **Lens** and **area** (an area name from the ledger).
- **Where**: `path:line`.
- **Rule**: the design-system section, the spec requirement and scenario, the `CLAUDE.md` rule or the
  URL.
- **Proof**: the failing test, the counterexample or the rule citation.
- **What a fix must achieve**, in one sentence. Not the code for it.
- **Unverified**, when a policy rule could not be confirmed.

Close with:

1. **Questions** (at most three), if any.
2. **Checked and clean**: the areas and lenses that produced nothing.
3. **Not checked, and why**, naming who would check it.
4. **Console items** for a human to confirm, when the critic has any.
5. **Escalations**, after the last round.
6. **Ledger rows**: one row per finding in the ledger's table format with outcome `open` (or one `clean`
   row when there are none), for the caller to record.

Keep the report tight: no praise, no restating the code back.
