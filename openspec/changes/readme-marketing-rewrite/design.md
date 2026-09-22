## Context

`README.md` currently opens with a short pitch (title, tagline, "Why Pillsner") but reaches developer material — prerequisites, a toolchain table, build instructions, the branching model and contributing guidelines — within the first screenful for anyone scrolling past the feature list. `docs/screens/` holds nine untracked phone screenshots (raw `Screenshot_YYYYMMDD_HHMMSS.png` capture names) taken from a running build, showing: the Home screen with an upcoming dose, the dose-confirmation sheet, the Medicines list (active/inactive), two mid-swipe states of that list (deactivate/activate), the medicine-details form, the schedule editor, Settings, and the "Choose a PIN" app-lock screen. None of this is referenced anywhere yet.

There is no existing `openspec/specs/` capability for repository-root documentation — `marketing-website` governs the separate Hugo site under `src/website/`, not the GitHub-facing README.

## Goals / Non-Goals

**Goals:**
- Make the product pitch the first thing anyone reading `README.md` sees, backed by real screenshots.
- Preserve every piece of existing developer-facing content by relocating it to `CONTRIBUTING.md`, not deleting it.
- Give the screenshots stable, descriptive names and display them at a uniform size in the README.
- Record the Play Store URL as the canonical link, matching what `marketing-website`'s spec already uses, so the two never drift.

**Non-Goals:**
- No changes to the app, its specs, or `src/`.
- No redesign of `docs/design-system.md` or the Hugo site.
- Not adding every screenshot to the README — only the ones that best carry the pitch.

## Decisions

- **Split point**: `README.md` keeps everything through "Permissions" (pitch, why-Pillsner, features, permissions table) plus the Technology, Repository layout and License sections, trimmed to context-setting length. Everything from "Getting started" through "Development workflow" and "Branching model" moves to `CONTRIBUTING.md` verbatim (content preserved, headings renumbered), because that is exactly the prerequisites/toolchain/build/workflow/branching block CLAUDE.md and the issue call out as developer-oriented.
- **Contributing section stays, shrinks**: the existing "Contributing" bullet list (start with a proposal, branch from `development`, keep the app small, preserve privacy, write tests, use clear commits) moves into `CONTRIBUTING.md` as its lead section; `README.md` keeps a two-to-three sentence "Contributing" section that links to it.
- **Play Store link**: use `https://play.google.com/store/apps/details?id=nl.hexmaster.pillsner` — this is not a placeholder; it is the same URL the `marketing-website` spec already requires the marketing site to link to (application id `nl.hexmaster.pillsner`), so both stay in sync automatically.
- **Screenshot renaming**: rename in place under `docs/screens/`, by what each shows:
  - `Screenshot_20260922_114328.png` → `home-upcoming-dose.png`
  - `Screenshot_20260922_114340.png` → `dose-confirmation.png`
  - `Screenshot_20260922_114350.png` → `medicines-overview.png`
  - `Screenshot_20260922_114356.png` → `medicines-swipe-deactivate.png`
  - `Screenshot_20260922_114401.png` → `medicines-swipe-activate.png`
  - `Screenshot_20260922_114406.png` → `medicine-details.png`
  - `Screenshot_20260922_114429.png` → `schedule-editor.png`
  - `Screenshot_20260922_114439.png` → `settings.png`
  - `Screenshot_20260922_114451.png` → `app-lock-pin.png`
- **Screenshots featured in the README**: `home-upcoming-dose.png`, `dose-confirmation.png`, `medicines-overview.png` and `settings.png` — one per core loop (see what's due, confirm it, manage medicines, configure privacy/security). The two mid-swipe states and the two deeper form screens (`medicine-details.png`, `schedule-editor.png`) stay in `docs/screens/` for later use (e.g. a future marketing site or store listing) but are not needed to make the README's point and would crowd it.
- **Uniform display size**: all four screenshots are native 1080×2424 (same device, same session), so a single fixed `width` on each `<img>` tag renders them at identical size without re-encoding the PNGs. Plain Markdown `![]()` image syntax cannot set a width, so this section uses inline HTML `<img>` tags with `width="220"` (an arbitrary but consistent value that keeps four side-by-side images readable without the README becoming image-dominated), each with descriptive `alt` text — GitHub renders inline HTML inside Markdown files.

## Risks / Trade-offs

- [Renaming untracked files loses their original capture timestamps] → not a concern: the files are untracked (never committed), so there is no history to preserve, and the images are more discoverable by content-based names going forward.
- [Inline `<img width>` HTML is less portable than Markdown image syntax if the README is ever rendered outside GitHub] → accepted: GitHub is the primary rendering target for this file, and the fallback (browsers rendering raw HTML) degrades gracefully.
- [Moving content to `CONTRIBUTING.md` could orphan links elsewhere in the repo that anchor into README's old headings (e.g. `#branching-model`)] → mitigated by checking for `README.md#` anchor references elsewhere in the repo (e.g. `.claude/skills/git-workflow/SKILL.md`, other docs) during implementation and updating any that break.

## Open Questions

None — the Play Store URL and screenshot content were resolved during design by reading the existing `marketing-website` spec and the images themselves.
