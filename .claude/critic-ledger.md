# Critic ledger

How the findings of `pillsner-app-critic`, `pillsner-wear-critic` and `pillsner-play-critic` turned out. The critics read this before every review to decide how deep to go per area; whoever ran the critic records the outcome here once the author has acted on the findings. It answers one question: is each critic pulling its weight, and where?

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

## Precision

Review periodically: per critic and per area, real ÷ (real + accepted-risk + noise). An area that stays at zero real findings over many reviews is one the critics should skip; a critic whose precision stays low needs its mandate narrowed.

## Findings

| Date | Critic | Scope | Round | ID | Severity | Area | Outcome | Note |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
