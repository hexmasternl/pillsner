## 1. Inventory current content

- [x] 1.1 Read the current `README.md` end to end and list every distinct fact/section it contains, tagged as "product/selling" or "developer/contributor".
- [x] 1.2 Search the repository for links pointing at README section anchors (e.g. `#branching-model`, `#permissions`) so headings that move can be re-linked.

## 2. Create CONTRIBUTING.md

- [x] 2.1 Create `CONTRIBUTING.md` at the repository root with: Prerequisites, Toolchain table, Building and running, Development workflow (OpenSpec loop), Branching model, and the existing Contributing bullet list, carried over from the current README with no loss of detail.
- [x] 2.2 Update any internal links (README, CLAUDE.md, other docs) that pointed at the moved sections' old README anchors so they point at `CONTRIBUTING.md` instead.

## 3. Rewrite README.md

- [x] 3.1 Reorder and rewrite the top of `README.md` so title, tagline, project status note and "Why Pillsner" lead the file, per `design.md`'s structure.
- [x] 3.2 Keep and tighten Features, Privacy by default, and Permissions sections so they read as reasons to trust/use the app.
- [x] 3.3 Keep a short Technology summary and the Repository layout table, trimmed to what orients a reader rather than a full contributor reference.
- [x] 3.4 Replace the removed developer-setup content with a brief "Contributing" section linking to `CONTRIBUTING.md`.
- [x] 3.5 Add a Play Store listing link near the top if a canonical URL is available; otherwise leave the section structured so it can be added later without further restructuring.
- [x] 3.6 Update `CLAUDE.md`'s repository layout table to list `CONTRIBUTING.md` alongside `README.md`.
- [x] 3.7 Give the screen dumps in `docs/screens/` descriptive filenames and embed the app logo (`docs/feature-graphic.png`) plus the most relevant screenshots (home, dose confirmation, medicines, usage history) in `README.md`.

## 4. Verify

- [x] 4.1 Diff the original README against the new README + CONTRIBUTING.md to confirm every fact from the inventory (task 1.1) survives somewhere.
- [x] 4.2 Confirm no broken internal links remain (task 1.2 follow-up).
- [x] 4.3 Proofread both files for tone: README reads as a pitch to a prospective user, CONTRIBUTING.md reads as a guide for a contributor.
