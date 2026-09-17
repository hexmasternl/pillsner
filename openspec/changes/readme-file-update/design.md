## Context

`README.md` currently front-loads developer setup (prerequisites, toolchain table, build/run steps, development workflow, branching model) before the product pitch. `docs/design-system.md` and `CLAUDE.md` already carry the authoritative technical and process detail, so the README does not need to be the primary home for it — it needs to sell the app first and point elsewhere for contributor detail.

## Goals / Non-Goals

**Goals:**
- Make the product pitch (what Pillsner does, who it's for, why it's trustworthy) the first thing a reader sees in `README.md`.
- Preserve every piece of existing information — nothing described in the current README is dropped, it is relocated or reframed.
- Give contributors a single, clearly-named place (`CONTRIBUTING.md`) for setup, toolchain and workflow detail, linked from the README.

**Non-Goals:**
- No change to app behaviour, specs, or code.
- No change to the toolchain, versions, or build configuration themselves — only where their documentation lives.
- Not redesigning `docs/design-system.md` or `CLAUDE.md`.

## Decisions

- **Split by audience, not by deletion.** Content moves from `README.md` to a new root-level `CONTRIBUTING.md`; nothing is rewritten from scratch except framing and ordering. This keeps the change low-risk and easy to review as a diff.
- **New README structure, top to bottom:** title and tagline → project status note → "Why Pillsner" → Features → Privacy by default → Permissions (rationale, not full technical table detail if that duplicates elsewhere) → Technology summary (short) → Repository layout → short "Contributing" section linking to `CONTRIBUTING.md` → License.
  - Alternative considered: delete the permissions/technology tables from the README entirely and push them into `CONTRIBUTING.md` too. Rejected — permissions and the privacy-preserving technology choices (on-device Room, no network permission) are themselves selling points for a privacy-conscious user, not just developer trivia, so they stay in the README.
- **`CONTRIBUTING.md` contents:** Prerequisites, Toolchain table, Building and running, Development workflow (OpenSpec loop), Branching model, and the existing Contributing bullet list. This mirrors the standard GitHub convention of a root `CONTRIBUTING.md` that tooling (GitHub's "contributing" prompt on new issues/PRs) picks up automatically.
- **Repository layout table stays in the README** (trimmed if needed) because it orients any reader, including non-developers browsing the repo, not just contributors.
- **Play Store link:** add a placeholder-free link only if a real listing URL is available at implementation time; otherwise the section is written so it can be added later without restructuring (e.g., a single line under the tagline).

## Risks / Trade-offs

- [Duplicated or missing content during the split] → Diff the old and new README plus the new CONTRIBUTING.md line-by-line against the original before finishing the task, to confirm every fact survives somewhere.
- [Internal anchor links elsewhere (e.g., in CLAUDE.md or CI docs) pointing at README section headers that move] → Search the repo for links to README section anchors before finalizing and update any that break.
- [README trying to do too much and staying long anyway] → Keep only rationale and selling points in the README tables; move exhaustive version pins and step-by-step commands to `CONTRIBUTING.md`.

## Open Questions

- Is there a canonical Play Store listing URL to include yet? If not, ship without it and note the section is ready for it.
