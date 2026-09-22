## Why

**GitHub Issue:** #27 (https://github.com/hexmasternl/pillsner/issues/27)

The README currently reads like a developer onboarding guide: prerequisites, toolchain tables, Gradle commands and the branching model come before a visitor even finishes learning what Pillsner does for them. Anyone landing on the repository from the Play Store listing, a GitHub search or a link shared by a user sees setup instructions first and the product pitch second. The README should sell the app to the people it is for — people who want a reliable medication reminder — and only then, briefly, point contributors elsewhere for the developer detail. (GitHub issue #27)

## What Changes

- Rewrite `README.md` so the top of the file is a product pitch: what Pillsner does, who it is for, and why it is trustworthy (reliable exact-alarm reminders, one-tap confirmation, on-device privacy), leading with the strongest, most concrete benefits from the existing Features section.
- Keep the full feature list, permissions rationale and privacy stance in the README — they are selling points, not developer detail — but reframe them toward a prospective user rather than a contributor.
- Move developer-oriented content (prerequisites, toolchain table, building/running instructions, development workflow, branching model, contributing guidelines) out of `README.md` into a new `CONTRIBUTING.md` at the repository root.
- Replace that material in `README.md` with a short "Contributing" section that links to `CONTRIBUTING.md`.
- Add a "Get it on Google Play" link near the top of the README pointing at `https://play.google.com/store/apps/details?id=nl.hexmaster.pillsner` (the same canonical listing URL the `marketing-website` spec already links to).
- Keep the repository layout table, license section and link to `docs/design-system.md`, but move them below the pitch so they no longer dominate the top of the file.
- Rename the untracked screen dumps in `docs/screens/` from their raw `Screenshot_*.png` capture names to descriptive names (e.g. `today-view.png`, `reminder-notification.png`) and embed the most representative ones in the README at a consistent, fixed display width so they render the same size regardless of their native resolution.

## Capabilities

### New Capabilities
- `project-documentation`: the repository-root documentation a visitor or contributor sees first — what `README.md` must lead with for a prospective user, what belongs in `CONTRIBUTING.md` instead, and how product screenshots are presented.

### Modified Capabilities
(none — no existing spec capability covers repository-root documentation content)

## Impact

- Affected files: `README.md` (rewritten), `CONTRIBUTING.md` (new), `docs/screens/*.png` (renamed and a subset referenced from the README).
- No application code, build configuration or `src/` changes.
- No dependency, permission or spec-level app-behavior changes.
