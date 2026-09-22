## 1. Screenshots

- [x] 1.1 Rename the nine files in `docs/screens/` per the design's mapping (`home-upcoming-dose.png`, `dose-confirmation.png`, `medicines-overview.png`, `medicines-swipe-deactivate.png`, `medicines-swipe-activate.png`, `medicine-details.png`, `schedule-editor.png`, `settings.png`, `app-lock-pin.png`).

## 2. CONTRIBUTING.md

- [x] 2.1 Create `CONTRIBUTING.md` at the repository root containing: Prerequisites, Toolchain, Building and running, Permissions the app asks for, Development workflow, Branching model, and the existing Contributing bullet list — moved from `README.md` with headings adjusted to fit a standalone document.
- [x] 2.2 Check the moved content for any internal links or anchors that assumed they lived in `README.md` and fix them.

## 3. README.md rewrite

- [x] 3.1 Rewrite the top of `README.md`: title, tagline, a tightened product pitch drawing on the strongest points from "Why Pillsner" and "Features", and a "Get it on Google Play" link to `https://play.google.com/store/apps/details?id=nl.hexmaster.pillsner`.
- [x] 3.2 Embed `home-upcoming-dose.png`, `dose-confirmation.png`, `medicines-overview.png` and `settings.png` from `docs/screens/` in the pitch section using inline `<img>` tags with a shared fixed `width` and descriptive `alt` text, per the design.
- [x] 3.3 Keep and lightly reframe the Features, Permissions, Technology, Repository layout and License sections toward a prospective user, removing developer-detail framing where present.
- [x] 3.4 Remove the Getting started, Development workflow and Branching model sections from `README.md`, replacing them with a short "Contributing" section that links to `CONTRIBUTING.md`.
- [x] 3.5 Update the Repository layout table's `docs/` row to mention the screenshots, if warranted.

## 4. Verification

- [x] 4.1 Preview both `README.md` and `CONTRIBUTING.md` rendered as Markdown (e.g. via `gh` or a local Markdown preview) and confirm the embedded screenshots render at a uniform size and all links resolve.
- [x] 4.2 Confirm no application code, `src/`, or spec content outside `openspec/changes/readme-marketing-rewrite/` changed.

## 5. Stronger visual pitch (follow-up)

- [x] 5.1 Replace the plain `# Pillsner` heading with `docs/feature-graphic.png` (the wordmark plus tagline banner) as a centered hero image at the top of `README.md`.
- [x] 5.2 Add a link to the informational website `https://pillsner.hexmaster.nl` alongside the existing Google Play link.
- [x] 5.3 Add GitHub status badges near the top of `README.md`: CI workflow status, release workflow status, and license, each linking to the page it reports on.
- [x] 5.4 Remove em dashes from `README.md` and `CONTRIBUTING.md`, rephrasing with commas, colons, parentheses or separate sentences instead.
- [x] 5.5 Re-verify: badges render, links resolve, `docs/feature-graphic.png` and `docs/icon.png` exist and are referenced correctly, and `openspec validate --strict` still passes.
