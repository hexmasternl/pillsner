# Critic ledger

How the findings of `pillsner-app-critic`, `pillsner-wear-critic`, `pillsner-play-critic` and `pillsner-proposal-critic` turned out. The critics read this before every review to decide how deep to go per area; whoever ran the critic records the outcome here once the author has acted on the findings. It answers one question: is each critic pulling its weight, and where?

## How to record

- Paste the **Ledger rows** from the critic's report into the table below with outcome `open`.
- Update the outcome when the finding is settled:
  - `real`: the finding was right and was fixed, or a test was added for it.
  - `accepted-risk`: the finding was right but deliberately not fixed (say why in the note).
  - `noise`: the finding was wrong, already handled, or withdrawn by the critic.
- A review that produced no findings still gets one row, with ID `-` and outcome `clean`, so the "no real finding in the last five reviews" rule can see it.
- Keep it append-only. Do not rewrite old rows other than their outcome.

## Areas

Use these names in the `Area` column. Add one only when none fits, and add it here too.

| Critic | Areas |
| --- | --- |
| app | `app/reminders` (alarms, notifications, boot and time receivers), `app/intake` (recording outcomes, adherence), `app/schedule` (schedule model and editor), `app/data` (Room, migrations, backup and restore), `app/ui` (screens and theme), `app/strings` |
| wear | `wear/ui`, `wear/strings`, `wear/sync` (src/shared, publisher, listener, repository), `wear/time` (agenda, midnight, time zones, per-minute timer), `wear/quality` (round screens, rotary, fonts, TalkBack) |
| play | `app/play` (phone manifest, permissions, target API, health and Data safety, backup rules), `wear/play` (watch manifest, form factor, Wear target API, distribution, listing), `release` (bundles, signing, workflow) |
| proposal | `proposal` (proposal, design, delta specs and tasks of a change, before apply) |

## Precision

Review periodically (the `pillsner-tech-debt-scout` agent computes it): per critic and per area, real ÷ (real + accepted-risk + noise). An area that stays at zero real findings over many reviews is one the critics should skip; a critic whose precision stays low needs its mandate narrowed.

## Findings

| Date | Critic | Scope | Round | ID | Severity | Area | Outcome | Note |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-10-29 | app | edge-to-edge-correction-and-tests | 1 | APP-1 | Major | app/ui | real | Landscape-cutout test checked only centred nodes; now asserts Back and title, proven by mutation |
| 2026-10-29 | app | edge-to-edge-correction-and-tests | 1 | APP-2 | Major | app/ui | real | Large-font tests now assert title and Back against the status bar |
| 2026-10-29 | app | edge-to-edge-correction-and-tests | 1 | APP-3 | Major | app/ui | real | tasks.md 5.x now say per line what ran and what was handed to manual testing |
| 2026-10-29 | app | edge-to-edge-correction-and-tests | 1 | APP-4 | Minor | app/ui | real | Helper waits out insets animations and its KDoc states the limit |
| 2026-11-10 | proposal | fix-watch-agenda-bottom-inset | 2 | SPEC-1 | Major | proposal | real | Watch packaged only en and nl; fixed in the change by D7, tasks 1.6 and 2.0, with a test that pt and de resolve |
| 2026-11-10 | proposal | fix-watch-agenda-bottom-inset | 2 | SPEC-2 | Major | proposal | real | D5 rewritten: assertion kept, last card selected by greatest bottom edge; tasks 1.4 and 2.2 |
| 2026-11-10 | proposal | fix-watch-agenda-bottom-inset | 2 | SPEC-3 | Minor | proposal | real | D6 pins "end" to the last item and names every state; tasks 1.5 and 1.7 |
| 2026-11-10 | wear | fix-watch-agenda-bottom-inset | 2 | WEAR-1 | Major | wear/strings | real | Version line in nl and pt now asserted (`theVersionLineIsReadInDutch`, `…InPortuguese`); the critic's question also proved real: the non-English footer is now read mid-screen in all six languages |
| 2026-11-10 | wear | fix-watch-agenda-bottom-inset | 2 | WEAR-2 | Minor | wear/ui | real | `VersionNameContractTest` guards that MainActivity passes BuildConfig.VERSION_NAME through WearApp to the agenda |
| 2026-11-10 | play | fix-watch-agenda-bottom-inset | 1 | - | - | wear/play | clean | Build diff is locales and BuildConfig only; no manifest, dependency, distribution or disclosure change. Pre-existing unused `wearMinSdk` raised as a question for the next release review |
