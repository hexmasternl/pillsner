## Why

**GitHub Issue:** #58 (https://github.com/hexmasternl/pillsner/issues/58)

The marketing website (`src/website/`) is currently one single page per locale that has to carry the whole app's story at once. It cannot properly explain medicine management, scheduling, stock, tracking or the wearable companion, has nowhere to answer common questions, and gives a visitor no way to report a bug or request a feature except finding the GitHub repository on their own. Issue #58 asks for the site to grow into a proper multi-page site with dedicated pages per topic, an FAQ, and bug-report/feature-request pages that end up as GitHub issues.

## What Changes

- Restructure the site from one page per locale into several: `/`, `/medicines/`, `/schedules/`, `/tracking/`, `/wearable/`, `/privacy/`, `/faq/`, `/report-a-bug/`, `/request-a-feature/` — each present in all six existing locales (en, nl, fr, es, pt, de).
- Trim the home page to a hero, a short why-Pillsner summary and an at-a-glance feature list, each item linking into its dedicated page; keep the Google Play Store call-to-action button in the hero and footer.
- Add a persistent header/footer navigation across all pages, with the existing language switcher working from any page, not just the home page.
- Author new content pages grounded in the app's actual capabilities (README.md's Features section and the relevant `openspec/specs/` capabilities):
  - **Medicines** (`/medicines/`): adding a medicine (name, dose, unit, dates, prescriber), editing it, activating/deactivating without ever deleting it, multiple schedules per medicine. (Stock tracking and refill reminders are **not** included here: despite being mentioned in `README.md` and the current home page, no such capability exists in `medicine-add`, `medicine-details` or `medicine-overview`, nor in the still-unimplemented `medicine-expiry-tracking` change — this page will not repeat that inaccuracy. Correcting the existing claim elsewhere is out of scope for this change.)
  - **Schedules & reminders** (`/schedules/`): fixed times, every N hours, weekdays, as-needed; reminders as exact alarms surviving reboot/DST/time zone changes; the one-tap take/snooze/skip answers and the missed-dose rule.
  - **Tracking your progress** (`/tracking/`): the daily overview and the adherence history view.
  - **On your wrist** (`/wearable/`): the Wear OS companion app.
  - **Privacy & security** (`/privacy/`): on-device-only data, no account/cloud/analytics, no internet permission, the optional app lock, linking to the full `PRIVACY.md`.
- Add a new FAQ page (`/faq/`) with a translated question-and-answer list.
- Add a new "Report a bug" page (`/report-a-bug/`) and a new "Request a feature" page (`/request-a-feature/`): each a small static form that submits to `https://github.com/hexmasternl/pillsner/issues/new`, carrying the GitHub Issue Form template and the `bug` / `feature` label in hidden fields, so the visitor lands on a prefilled, correctly-labelled new-issue page and finishes filing it with their own GitHub account — no backend, no token, no data collected by the site itself. Two new templates, `.github/ISSUE_TEMPLATE/bug_report.yml` and `.github/ISSUE_TEMPLATE/feature_request.yml`, back those two pages.
- Illustrate the content pages with the app screenshots already in `docs/screens/`, mounted into Hugo's assets rather than copied: each is shown small and opens full size, centred on a 50%-black backdrop that any click closes.
- Extend per-page SEO/Open Graph metadata, `hreflang` alternates and the generated `sitemap.xml` to cover every new page in every locale (the existing per-locale, per-page requirements already specified for the home page apply the same way to each new page).
- Update `src/website/README.md`'s project-layout table for the new content/page structure.

## Capabilities

### New Capabilities
- `website-faq`: the FAQ page — a translated, static question-and-answer list covering common questions about the app.
- `website-issue-reporting`: the report-a-bug and request-a-feature pages — static forms that build a prefilled GitHub "new issue" URL client-side (title, body, `bug`/`feature` label) with no backend involved.

### Modified Capabilities
- `marketing-website`: replaces the "Single-page informational layout" requirement with a multi-page structure (the pages listed above), adds a cross-page navigation requirement, and extends the existing SEO/Open Graph/sitemap requirements to apply per page rather than only to the single home page.

## Impact

- **Changed code**: `src/website/content/<lang>/` gains one content file/bundle per new page per locale; `src/website/layouts/` gains templates for the new page types (content page, FAQ, and the two issue-reporting forms) alongside the existing `index.html`; `src/website/i18n/<lang>.toml` gains navigation and form-chrome strings; `src/website/hugo.toml` needs its `disableKinds`/`outputs` settings revisited since they currently assume a single home-only page.
- **New client-side JS**: two small scripts (no external requests) — one to build the prefilled GitHub issue URL from the report-a-bug/request-a-feature form fields, one for the screenshot overlay.
- **New GitHub Issue Forms**: `.github/ISSUE_TEMPLATE/bug_report.yml` and `.github/ISSUE_TEMPLATE/feature_request.yml`; `.github/workflows/website.yml` gains `docs/screens/**` to its path filter so a screenshot change redeploys the site.
- **No changes** to `src/app`, `src/wear`, `src/shared`, or any Android behaviour — this is a website-only change.
- **No new dependencies, no backend, no analytics** — the site remains a static Hugo build with no runtime server dependency.
