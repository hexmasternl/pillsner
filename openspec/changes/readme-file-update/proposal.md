## Why

**GitHub Issue:** #27 (https://github.com/hexmasternl/pillsner/issues/27)
**Pull Request:** #28 (https://github.com/hexmasternl/pillsner/pull/28)

The README currently reads like a developer onboarding guide: prerequisites, toolchain tables, Gradle commands and branching model come before a visitor even finishes learning what Pillsner does for them. Anyone landing on the repository from the Play Store listing, a GitHub search or a link shared by a user sees setup instructions first and the product pitch second. The README should sell the app to the people it is for — people who want a reliable medication reminder — and only then, briefly, point contributors elsewhere for the developer detail.

## What Changes

- Rewrite `README.md` so the top of the file is a product pitch: what Pillsner does, who it is for, and why it is trustworthy (reliable exact-alarm reminders, one-tap confirmation, on-device privacy), leading with the strongest, most concrete benefits from the existing Features section.
- Keep the full feature list, permissions rationale and privacy stance — these are selling points, not developer detail — but tighten their framing toward a prospective user rather than a contributor.
- Move developer-oriented content (prerequisites, toolchain table, building/running instructions, development workflow, branching model, contributing guidelines) out of `README.md` into a new `CONTRIBUTING.md` at the repository root.
- Replace that material in `README.md` with a short "Contributing" or "For developers" section that links to `CONTRIBUTING.md`.
- Add a link to the Pillsner Play Store listing near the top of the README once a canonical placeholder is agreed (or omit cleanly if no listing exists yet, without leaving a dead link).
- Keep the repository layout table, license section and links to `docs/design-system.md` where useful for context, but do not let them dominate the top of the file.

## Capabilities

### New Capabilities

- `project-readme`: the repository's `README.md` as a product-facing document — what it must lead with, what it must contain, and what it defers to `CONTRIBUTING.md`. Mirrors the precedent set by `marketing-website` for documenting a non-runtime, audience-facing deliverable.

### Modified Capabilities

None. No existing `openspec/specs/` capability describes README or contributor-documentation content.

## Impact

- `README.md`: substantially restructured and rewritten.
- `CONTRIBUTING.md`: new file at the repository root, holding prerequisites, toolchain, build/run instructions, development workflow and branching model.
- No source code, specs, or build configuration changes.
