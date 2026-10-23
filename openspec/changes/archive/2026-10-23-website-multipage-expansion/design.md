## Context

`src/website/` is a Hugo site with `disableKinds = ["taxonomy", "term", "section"]` and `[outputs] home = ["html"]` — deliberately built for exactly one page per locale (`content/<lang>/_index.md`). Issue #58 asks for that to grow into several pages per locale, plus an FAQ and two forms that end up as GitHub issues, without giving up any of the existing site's constraints: no backend, no tracking, per-locale SEO/Open Graph/`hreflang`, and the design-system visual identity.

## Goals / Non-Goals

**Goals:**
- Reuse Hugo's existing `[permalinks] page = "/:slug/"` rule: every new page is a plain content file, `content/<lang>/<slug>.md`, needing no change to `disableKinds` (still no taxonomies/sections/terms — just more `page`-kind content).
- Give each page family its own template (content page, FAQ, issue-report form) while sharing the existing header/footer/SEO/Open Graph partials so every new page automatically gets the metadata the `marketing-website` spec already requires.
- Add real, in-page navigation (header menu, footer links) so the site reads as one connected site, not nine disconnected pages.
- Keep the report-a-bug/request-a-feature pages fully static: no token, no server call, and a no-JS fallback that still reaches GitHub.

**Non-Goals:**
- No CMS, no dynamic backend, no analytics — unchanged from the site's existing constraints.
- No change to `src/app`, `src/wear`, `src/shared` or any Android behaviour.
- No new hosting or CI changes — `.github/workflows/website.yml` already triggers on any `src/website/**` change and needs no path updates.
- Deciding the exact FAQ questions/answers is a content-authoring task, not a design decision here.

## Decisions

### 1. Page structure: flat content files, not Hugo sections
Each new page is `content/<lang>/<slug>.md` (`medicines.md`, `schedules.md`, `tracking.md`, `wearable.md`, `privacy.md`, `faq.md`, `report-a-bug.md`, `request-a-feature.md`), exactly like the existing `_index.md` but for a non-home slug. This already resolves to `/<lang>/<slug>/` via the existing `[permalinks] page = "/:slug/"` rule, so no routing or `disableKinds` change is needed. Home (`_index.md`) stays the site's `home`-kind page but its content shrinks to a hero, a short why-Pillsner paragraph, and a feature-summary list that links out to the new pages.

**Alternative considered:** Hugo sections (`content/<lang>/medicines/_index.md` as a section index). Rejected — sections exist for listing children, which none of these pages need; flat pages keep `disableKinds` untouched and match the site's existing single-page-per-slug mental model.

### 2. Templates: one shared "content page" layout, two specialised ones
- `layouts/_default/single.html` (new): the shared template for the five informational pages (Medicines, Schedules & reminders, Tracking, Wearable, Privacy & security) — a headline, a body rendered from Markdown, and the existing header/footer/SEO partials. Front matter supplies the hero-less heading and body copy per locale, mirroring how `_index.md` already structures the home page's sections.
- `layouts/faq/single.html` (new, selected via `type: faq` front matter): renders a `params.questions` list (question/answer pairs) as a translated accordion/list, native `<details>`/`<summary>` elements so it needs no JS.
- `layouts/issue-report/single.html` (new, selected via `type: issue-report` front matter): shared by both `/report-a-bug/` and `/request-a-feature/`, parameterised by front matter (`params.issueLabel: bug|feature`, `params.fields: [...]` — title, description, and for bugs, steps-to-reproduce/app version/device+Android version). Renders the static form described in Decision 3.

### 3. Report a bug / Request a feature: a plain GET form to GitHub, no backend
Both pages render a static form whose `action` is `https://github.com/hexmasternl/pillsner/issues/new` and whose hidden fields carry `template` (`bug_report.yml` / `feature_request.yml`) and `labels` (`bug` / `feature`). Each visible field is named after the matching field `id` in that GitHub Issue Form, so submitting the form — with or without JavaScript — lands the visitor on a prefilled, correctly-labelled new-issue page they complete and submit with their own GitHub account. Two new templates, `.github/ISSUE_TEMPLATE/bug_report.yml` and `.github/ISSUE_TEMPLATE/feature_request.yml`, back the two pages. No fetch call, no token, nothing leaves the browser except the visitor's own navigation to GitHub.

`assets/js/issue-report.js` builds the same URL itself on submit, purely so it can trim values that would otherwise produce an unusable URL length. Hidden fields are used rather than a query string on `action` because a GET submission replaces any query string already present in `action`, which would silently drop the template and label.

**Alternative considered:** a server-side function (e.g. an Azure Static Web Apps API route) that calls the GitHub REST API to create the issue directly. Rejected — it would need a GitHub token held server-side, turning a static site into one with a live backend and a secret to protect, which contradicts the site's "static build, no runtime server dependency" requirement and its no-account, no-data-collected stance. The prefilled-link approach needs the visitor to have a GitHub account, which is an accepted trade-off given `CONTRIBUTING.md` already directs bug reports and feature ideas to GitHub issues.

### 4. Navigation
A `[menus]` table in `hugo.toml` (or per-language `[languages.<lang>.menus]` if labels need translation beyond what `i18n/<lang>.toml` already provides) drives the header nav and footer links, read by an updated `layouts/partials/header.html`/`footer.html`. Labels come from `i18n/<lang>.toml` keys (one new key per page) so English stays the source of truth and other locales fall back to English exactly as today.

### 5. Screenshots: mounted from docs/screens, shown small, enlarged in an overlay
The app screenshots already in `docs/screens/` are mounted into Hugo's assets (`[[module.mounts]] source = "../../docs/screens"`) rather than copied into `src/website/assets/`, so there is only ever one copy of each PNG in the repository. Hugo resizes each one into a small thumbnail and a larger version at build time. The thumbnail is wrapped in a link to the larger version, so a visitor without JavaScript gets the full image; `assets/js/lightbox.js` upgrades that link into an overlay that centres the large version on a 50%-black backdrop, which any click — or Escape — closes again. `.github/workflows/website.yml` gains `docs/screens/**` to its path filter so a new screenshot redeploys the site.

## Risks / Trade-offs

- **[Risk]** Nine pages × six locales multiplies the translation surface eightfold versus today's one page. → **Mitigation:** unchanged fallback-to-English behaviour for any missing string, English authored first as already required, and a translation-completeness check remains a good follow-up (not blocking this change).
- **[Risk]** A visitor without a GitHub account cannot actually file the issue after reaching GitHub. → **Mitigation:** both pages say plainly that a GitHub account is needed; a fallback support inbox is out of scope for this change (see Open Questions).
- **[Risk]** Existing bookmarks/shares of the old single-page anchors (`#features`, `#privacy`, etc.) will 404 once that content moves to dedicated pages. → **Mitigation:** keep matching anchor ids on the trimmed home-page summary sections so old anchor links still land on relevant (if shorter) content rather than nothing.
- **[Risk]** Very long form input could produce a GitHub "new issue" URL near/over practical URL-length limits. → **Mitigation:** cap the description/steps-to-reproduce fields' length client-side before building the URL.

## Migration Plan

Additive and content-only: no data model, no schema, nothing to roll back beyond reverting the `src/website/` commit. Existing `_index.md` per locale is edited in place (trimmed), not replaced; the site's build/deploy pipeline (`hugo --minify`, `.github/workflows/website.yml`) needs no changes.

## Open Questions

- Should the pre-existing "stock tracking and refill reminders" claim in `README.md` and the current home page be corrected now that it's known to describe a capability that doesn't exist yet? Left out of this change's scope (it predates this proposal and touches copy this change isn't otherwise editing); worth a small follow-up.
- Should the report-a-bug/request-a-feature pages also mention the Play Store support-address channel (`PRIVACY.md` §14) as a fallback for visitors without a GitHub account? Left out of this change; a small follow-up if it turns out to matter.
