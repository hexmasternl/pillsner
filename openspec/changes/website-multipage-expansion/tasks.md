## 1. Site structure and navigation

- [x] 1.1 Add a `[menus]` (or per-language `[languages.<lang>.menus]`) table to `hugo.toml` listing every page, and confirm `disableKinds`/`[outputs]` need no change for plain `page`-kind content files
- [x] 1.2 Add navigation and page-title i18n keys to `i18n/en.toml`, then to `i18n/nl.toml`, `i18n/fr.toml`, `i18n/es.toml`, `i18n/pt.toml`, `i18n/de.toml`
- [x] 1.3 Update `layouts/partials/header.html` to render the header navigation menu on every page
- [x] 1.4 Update `layouts/partials/footer.html` to link every page from the footer
- [x] 1.5 Confirm the existing language switcher (`layouts/partials/language-switcher.html`) keeps the visitor on the equivalent page when switching locale, not just Home

## 2. Page templates

- [x] 2.1 Create `layouts/_default/single.html` for the five informational pages (headline + body + shared header/footer/SEO partials)
- [x] 2.2 Create `layouts/faq/single.html` rendering a `params.questions` list as native `<details>`/`<summary>` entries (selected via `type: faq` front matter)
- [x] 2.3 Create `layouts/issue-report/single.html` shared by the bug-report and feature-request pages, parameterised by `params.issueLabel` and `params.fields` (selected via `type: issue-report` front matter)
- [x] 2.4 Add `assets/js/issue-report.js`: on form submit, build the prefilled GitHub "new issue" URL from the form fields and navigate to it; ensure the form is a valid no-JS fallback (a plain GET form to `.../issues/new` with hidden `template`/`labels` fields, since a GET submission would replace a query string written into `action`)
- [x] 2.5 Add the GitHub Issue Forms the two pages prefill: `.github/ISSUE_TEMPLATE/bug_report.yml` (label `bug`) and `.github/ISSUE_TEMPLATE/feature_request.yml` (label `feature`), with field ids matching the form fields
- [x] 2.6 Add `layouts/partials/screenshot.html` and `assets/js/lightbox.js`: mount `docs/screens/` into Hugo's assets, render each screenshot small, and enlarge it centred on a 50%-black backdrop that any click or Escape closes; add `docs/screens/**` to the website workflow's path filter

## 3. Home page

- [x] 3.1 Trim `content/en/_index.md` to a hero, a short why-Pillsner summary and a feature-summary list linking to the new pages; keep matching anchor ids on the summary sections so old bookmarked anchors still land on relevant content
- [x] 3.2 Apply the same trim to `content/nl/_index.md`, `content/fr/_index.md`, `content/es/_index.md`, `content/pt/_index.md`, `content/de/_index.md`

## 4. Content pages: Medicines, Schedules & reminders, Tracking, Wearable, Privacy & security

- [x] 4.1 Author `content/en/medicines.md` (add/edit a medicine, active/inactive without deletion, multiple schedules per medicine — do not claim stock tracking or refill reminders, since neither exists in `medicine-add`, `medicine-details` or `medicine-overview`)
- [x] 4.2 Author `content/en/schedules.md` (fixed times/every N hours/weekdays/as-needed, exact alarms surviving reboot/DST/time zone changes, one-tap take/snooze/skip, missed-dose rule)
- [x] 4.3 Author `content/en/tracking.md` (daily overview, confirming/skipping from notification or app, adherence history)
- [x] 4.4 Author `content/en/wearable.md` (Wear OS companion: next-six-hours view, answering stays on phone/watch, follows phone's language)
- [x] 4.5 Author `content/en/privacy.md` (on-device-only data, no account/cloud/analytics, no internet permission, optional app lock, link to full `PRIVACY.md`)
- [x] 4.6 Translate all five pages into `nl`, `fr`, `es`, `pt`, `de`

## 5. FAQ page

- [x] 5.1 Draft the FAQ question set in `content/en/faq.md` (e.g. is Pillsner a medical device, does it work offline, what happens on a new phone, how is data kept private)
- [x] 5.2 Translate the FAQ into `nl`, `fr`, `es`, `pt`, `de`

## 6. Report a bug / Request a feature pages

- [x] 6.1 Author `content/en/report-a-bug.md` (title, description, steps to reproduce, app version, device/Android version fields; note that a GitHub account is required to finish filing)
- [x] 6.2 Author `content/en/request-a-feature.md` (title, description fields; same GitHub-account note)
- [x] 6.3 Translate both pages into `nl`, `fr`, `es`, `pt`, `de`
- [x] 6.4 Verify the prefilled GitHub issue URL for both pages opens the correct repository, title, body and label (`bug` / `feature`)

## 7. SEO, Open Graph and documentation

- [x] 7.1 Verify per-page, per-locale `<title>`, meta description, canonical URL and `hreflang` alternates are generated correctly for every new page (existing partials already apply per-page; confirm rather than rebuild)
- [x] 7.2 Verify the generated `sitemap.xml` lists every new page for every locale
- [x] 7.3 Update `src/website/README.md`'s project-layout table to describe the new multi-page content/template structure

## 8. Verification

- [x] 8.1 Run `hugo --minify` from `src/website/` and confirm a clean build with no broken template references
- [x] 8.2 Manually check each of the 9 pages in at least English and one other locale, including the header/footer nav and language switcher
- [x] 8.3 Manually check the bug-report and feature-request forms both with JavaScript enabled and with it disabled
- [x] 8.4 Confirm no analytics, tracking script or non-essential cookie was introduced
