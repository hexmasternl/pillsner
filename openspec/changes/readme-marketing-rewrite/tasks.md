## 1. Screenshots

- [ ] 1.1 Rename the nine files in `docs/screens/` per the design's mapping (`home-upcoming-dose.png`, `dose-confirmation.png`, `medicines-overview.png`, `medicines-swipe-deactivate.png`, `medicines-swipe-activate.png`, `medicine-details.png`, `schedule-editor.png`, `settings.png`, `app-lock-pin.png`).

## 2. CONTRIBUTING.md

- [ ] 2.1 Create `CONTRIBUTING.md` at the repository root containing: Prerequisites, Toolchain, Building and running, Permissions the app asks for, Development workflow, Branching model, and the existing Contributing bullet list — moved from `README.md` with headings adjusted to fit a standalone document.
- [ ] 2.2 Check the moved content for any internal links or anchors that assumed they lived in `README.md` and fix them.

## 3. README.md rewrite

- [ ] 3.1 Rewrite the top of `README.md`: title, tagline, a tightened product pitch drawing on the strongest points from "Why Pillsner" and "Features", and a "Get it on Google Play" link to `https://play.google.com/store/apps/details?id=nl.hexmaster.pillsner`.
- [ ] 3.2 Embed `home-upcoming-dose.png`, `dose-confirmation.png`, `medicines-overview.png` and `settings.png` from `docs/screens/` in the pitch section using inline `<img>` tags with a shared fixed `width` and descriptive `alt` text, per the design.
- [ ] 3.3 Keep and lightly reframe the Features, Permissions, Technology, Repository layout and License sections toward a prospective user, removing developer-detail framing where present.
- [ ] 3.4 Remove the Getting started, Development workflow and Branching model sections from `README.md`, replacing them with a short "Contributing" section that links to `CONTRIBUTING.md`.
- [ ] 3.5 Update the Repository layout table's `docs/` row to mention the screenshots, if warranted.

## 4. Verification

- [ ] 4.1 Preview both `README.md` and `CONTRIBUTING.md` rendered as Markdown (e.g. via `gh` or a local Markdown preview) and confirm the embedded screenshots render at a uniform size and all links resolve.
- [ ] 4.2 Confirm no application code, `src/`, or spec content outside `openspec/changes/readme-marketing-rewrite/` changed.
