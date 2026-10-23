---
name: website-feature-sync
description: Keep the Pillsner marketing website (src/website, Hugo, six locales) in step with the app - decide whether an OpenSpec change is visible to users, update the right feature page, the home summary, the FAQ and the privacy page in English, translate into nl, fr, es, pt and de, add screenshots, and verify the Hugo build. Use while applying any OpenSpec change that adds or changes something a user of the app can see or notice, when a change's tasks.md has a website task, when asked to "update the website" or "put this feature on the site", and in audit mode to find features the site doesn't describe yet.
license: MIT
metadata:
  author: pillsner
  version: "1.0"
---

The website at `src/website/` describes what Pillsner does. It's only useful if it describes what the
app does **now**: a feature the site doesn't mention doesn't exist to a visitor, and a claim the app
no longer meets is worse than none (the privacy page and FAQ make promises about permissions and data
people rely on).

## When it runs

- **While applying a change**, in the change's own feature branch and worktree, as one of its
  tasks — never as a separate change afterwards. `website.yml` deploys the site on a push to `main`,
  the same push that releases the app, so a website update that rides in the feature's pull request
  goes live in exactly the release that ships the feature. Not earlier, not later.
- **Audit mode**, on request (`website-feature-sync audit`): compare the site against what has
  shipped and list the gaps. See the end of this file.

`openspec/config.yaml` has a `tasks` rule that adds a website task to every change that changes
something users see, so this skill is normally reached through that task.

## 1. Decide whether the change belongs on the site

Read the change's `proposal.md` and specs. The site describes **what a user can do or rely on**, not
how it's built.

| Belongs on the site | Doesn't |
| --- | --- |
| A new thing the user can do (scan a label, track expiry, lock the app) | Refactors, performance work, dependency bumps |
| A change in how an existing feature behaves that a user would notice | Internal fixes with no visible effect |
| A new or removed permission, or anything about where data goes | Test, CI, build or tooling changes |
| A new language, a new device (watch), a new platform requirement | Changes to wording the site doesn't quote |
| A fix that makes a current website claim true or false | Bug fixes that restore behaviour the site already describes |

If it doesn't belong, mark the website task done with a one-line reason in `tasks.md` ("No user-visible
change: internal refactor") and stop. Don't invent copy for an invisible change.

## 2. Find where it goes

The site's pages and what each owns (English files in `src/website/content/en/`):

| Page | Owns |
| --- | --- |
| `_index.md` (Home) | Hero, "Why Pillsner" points, the four feature cards, a privacy summary. Only changes when a feature is big enough to change the one-line description of a card, or a "why" point. |
| `medicines.md` | Adding and editing medicines, details, stock, expiry, stopping/reactivating, label scanning. |
| `schedules.md` | Schedule types, reminders, alarm reliability, snooze, reboot/time zone/clock handling. |
| `tracking.md` | Today's doses, answering a dose, history, adherence. |
| `wearable.md` | The Wear OS app. |
| `privacy.md` | Permissions, where data lives, app lock, backup, reset. **Must match `src/app/manifest-allowlist.txt` exactly.** |
| `faq.md` | Questions people actually ask. Add one when a feature raises an obvious question ("Can Pillsner read the box?"), or when an existing answer stops being true. |

Prefer adding to an existing page. A content page is `params.sections`, each with `heading`, `body`,
optional `points`, optional `screenshot` (`image`, `alt`) and optional `link`. Add a `point` to an
existing section when the feature extends it; add a section when it's a new thing on that page.

**Also check what the change makes false.** Search all English pages for claims about the area the
change touches and correct them: `grep -rn -i "<keyword>" src/website/content/en/`. The privacy page
and FAQ make specific claims ("declares no internet permission", lists of permissions, "six hours"
on the watch); these are the ones that go stale.

**A new page** is only for a genuinely new area that doesn't fit any page above. It is bigger than a
copy edit: the `marketing-website` spec enumerates the pages and the menu groups, so a new page needs
a delta to that spec in the change, a `[[menus.main]]` entry under `features` or `help` in
`hugo.toml` (never a fifth top-level entry — `docs/design-system.md` section 8.14), a `nav_<id>`
label in every `i18n/<lang>.toml`, and the page in all six locales. Raise it with the user before
doing it.

## 3. Write the English copy

English is the source of truth; write it first. Match the existing pages' voice — read the page
you're editing before writing:

- Second person, plain words, short sentences. Say what the user can do, then why it helps.
- Use the app's real UI wording for buttons and answers ("I took it", "Not yet", "Not going to"),
  taken from `src/app/src/main/res/values/strings.xml`, not paraphrased.
- Use the domain glossary in `CLAUDE.md`: medicine, schedule, dose, reminder, history.
- Be exact and no bigger than the feature. No "smart", "AI", "seamless", no superlatives, no medical
  or health-outcome claims (Pillsner is not a medical device and the FAQ says so).
- Never describe something that isn't in this change or already shipped. No "coming soon".
- If the feature has a limit (works offline only, Android version, phone only), say it.

## 4. Screenshots

Screenshots live in `docs/screens/` (mounted into the site, never copied into `src/website/`) and are
referenced by filename from a section's `screenshot.image`. Add one when a section introduces
something visual — a new screen or a visibly new control:

1. With the app on the emulator (see the `emulator-tests` skill for getting one running), set up a
   realistic state using **made-up medicine names** (e.g. "Examplol 50 mg"), never real user data.
2. `adb exec-out screencap -p > docs/screens/<descriptive-name>.png` (kebab-case, naming the
   screen and the state).
3. Write `alt` text that says what the screenshot shows, in every locale.

Light theme, English UI, the same emulator as the existing screenshots. If a screenshot can't be
taken (no emulator, feature needs hardware), leave the section without one and note it in the PR
summary rather than mocking an image up.

## 5. Translate into every locale

Every edit to `content/en/<page>.md` is repeated in `content/{nl,fr,es,pt,de}/<page>.md` in the same
commit. Hugo does **not** fall back to English for a missing front-matter field, so a locale that
lacks a new section or point simply doesn't show it.

- Keep the structure identical: same sections in the same order, same number of points, same
  screenshot filenames, same link URLs. Only the text differs.
- Translate naturally, not word for word, using the terms that locale's pages already use (read the
  neighbouring sections first), and the UI wording from that locale's
  `src/app/src/main/res/values-<lang>/strings.xml` so the site matches the app the visitor will
  install.
- `i18n/<lang>.toml` only changes for a new page or a new reused UI string; add the key to all six.

## 6. Verify

From `src/website/`:

1. `hugo --minify --panicOnWarning` builds with no errors or warnings. Delete the generated
   `public/` afterwards (it's git-ignored, but don't leave it lying around).
2. Structure check — every locale has the same front-matter shape as English for each page you
   touched. Compare, per page, the list of section headings count, points count per section and
   screenshot filenames across `content/*/<page>.md`; any difference is a missing translation.
3. Skim the result with `hugo server` at `http://localhost:1313/en/<page>/` when a section or
   screenshot was added, to see it renders where intended. Stop the server afterwards.
4. Privacy page: if the change touched permissions, the permissions it lists match
   `src/app/manifest-allowlist.txt` exactly.

Commit the website edits on the feature branch as their own commit (e.g.
`Describe label scanning on the website`), with the change name in the body.

## Audit mode

For catching up, or checking nothing slipped through:

1. List archived changes newer than the site's last content change:
   `git log -1 --format=%ad --date=short -- src/website/content` and compare with the dates in
   `openspec/changes/archive/`.
2. For each, apply step 1. For the ones that belong, check whether the English pages already cover
   them (step 2's search).
3. Check the claims that most often go stale: the privacy page against
   `src/app/manifest-allowlist.txt`, the FAQ's permission and data answers, the supported languages
   against `hugo.toml` and the app's `values-*` folders, the watch app's description against the
   `wearable-app` spec.
4. Report the gaps as a list (change → page → what's missing or wrong) and ask before fixing them.
   A catch-up is its own OpenSpec change, so its edits ship together and get reviewed together.

## Guardrails

- The site never runs ahead of the app: no feature on the site before it ships, which the
  "update inside the feature's own PR" rule guarantees. Don't update the website on `development`
  directly, and don't update it for a change that isn't implemented.
- Never add analytics, trackers, external fonts, CDNs, cookies or any external request to the site.
  The `marketing-website` spec forbids them.
- Never change the site's CSS, templates or layout as part of a feature's copy update. Layout and
  design changes are their own change and follow `docs/design-system.md`.
- Never use real medicine names or personal data in copy or screenshots.
- Never leave a locale behind. If a translation can't be done well, say so in the PR summary rather
  than shipping English text in a non-English file.
