# Design

## Context

See proposal.md, Why. The site is Hugo with six content trees (`src/website/content/{en,nl,fr,es,pt,de}/`). Each page keeps its copy in front-matter structures (sections, bullets, FAQ question/answer lists), so a content edit has the same shape in every locale. `website-feature-sync` already sets out which page owns what, and how to translate and verify.

## Goals / Non-Goals

**Goals**
- Every claim on the site matches what the app on `development` does today.
- Every user-visible feature archived since 2026-09-23 is described on the page that owns it.

**Non-Goals**
- Rewording copy that is already correct, or restructuring pages.
- Templates, CSS, navigation, new pages, new FAQ layout.
- The in-app privacy policy and legal texts (`app-legal`).
- Any feature from a change that hasn't been archived yet.

## Decisions

- **One change, not one per feature.** The audit-mode rule in `website-feature-sync` says a catch-up is its own change. The edits are reviewed together and go live together, and all six locales stay in step. The alternative, small fixes straight to `development`, is forbidden by the git workflow and would ship partly translated pages.
- **Correct the false claims first.** The FAQ new-phone answer and the privacy page's data and permission claims are the first task group. If the change has to be cut short, the misleading copy is fixed before anything purely additive.
- **Source of truth for the copy is the spec, not the code.** Describe `data-backup-restore`, `medicine-label-scan` and `app-theme` from their specs in `openspec/specs/`. Use the app's own string resources (in each locale) for the names of buttons and settings, so the site and the app use the same words in every language.
- **Screenshots only where a page already uses them for a comparable feature.** Medicines has stock screenshots, so label scanning may get one. They're taken from the debug build on the emulator with made-up medicine names. They aren't required to close the change: a missing screenshot is better than one with real data.
- **No spec delta** (`skip_specs: true`). The site's structure and behaviour requirements in `marketing-website` and `website-faq` don't change, only its content.

## Risks / Trade-offs

- [The audit finds more than the three known gaps and the change grows] → Fix gaps that are copy on existing pages within this change. Anything that needs a new page, a new section type or layout work is noted in `tasks.md` and the PR summary, and gets its own change.
- [Translations drift in meaning across locales, especially the passphrase and no-recovery warning] → Translate from the app's existing locale strings for the same feature where they exist. The PR summary flags any locale whose wording couldn't be matched to the app.
- [Copy promises more than the feature does, such as "restore merges your data"] → Every new sentence is checked against a requirement or scenario in the feature's spec before it's committed.

## Migration Plan

Content-only. The change merges into `development` by PR and goes live with the next release that deploys the site from `main`. Rolling back means reverting the PR.
