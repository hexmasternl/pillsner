## 1. Site structure and navigation

- [ ] 1.1 Add a `[menus]` (or per-language `[languages.<lang>.menus]`) table to `hugo.toml` listing every page, and confirm `disableKinds`/`[outputs]` need no change for plain `page`-kind content files
- [ ] 1.2 Add navigation and page-title i18n keys to `i18n/en.toml`, then to `i18n/nl.toml`, `i18n/fr.toml`, `i18n/es.toml`, `i18n/pt.toml`, `i18n/de.toml`
- [ ] 1.3 Update `layouts/partials/header.html` to render the header navigation menu on every page
- [ ] 1.4 Update `layouts/partials/footer.html` to link every page from the footer
- [ ] 1.5 Confirm the existing language switcher (`layouts/partials/language-switcher.html`) keeps the visitor on the equivalent page when switching locale, not just Home

## 2. Page templates

- [ ] 2.1 Create `layouts/_default/single.html` for the five informational pages (headline + body + shared header/footer/SEO partials)
- [ ] 2.2 Create `layouts/faq/single.html` rendering a `params.questions` list as native `<details>`/`<summary>` entries (selected via `type: faq` front matter)
- [ ] 2.3 Create `layouts/issue-report/single.html` shared by the bug-report and feature-request pages, parameterised by `params.issueLabel` and `params.fields` (selected via `type: issue-report` front matter)
- [ ] 2.4 Add `assets/js/issue-report.js`: on form submit, build the prefilled GitHub "new issue" URL from the form fields and navigate to it; ensure the form's plain `action` attribute is a valid no-JS fallback URL (`.../issues/new?labels=bug` or `?labels=feature`)

## 3. Home page

- [ ] 3.1 Trim `content/en/_index.md` to a hero, a short why-Pillsner summary and a feature-summary list linking to the new pages; keep matching anchor ids on the summary sections so old bookmarked anchors still land on relevant content
- [ ] 3.2 Apply the same trim to `content/nl/_index.md`, `content/fr/_index.md`, `content/es/_index.md`, `content/pt/_index.md`, `content/de/_index.md`

## 4. Content pages: Medicines, Schedules & reminders, Tracking, Wearable, Privacy & security

- [ ] 4.1 Author `content/en/medicines.md` (add/edit a medicine, active/inactive without deletion, multiple schedules per medicine, stock tracking and refill heads-up)
- [ ] 4.2 Author `content/en/schedules.md` (fixed times/every N hours/weekdays/as-needed, exact alarms surviving reboot/DST/time zone changes, one-tap take/snooze/skip, missed-dose rule)
- [ ] 4.3 Author `content/en/tracking.md` (daily overview, confirming/skipping from notification or app, adherence history)
- [ ] 4.4 Author `content/en/wearable.md` (Wear OS companion: next-six-hours view, answering stays on phone/watch, follows phone's language)
- [ ] 4.5 Author `content/en/privacy.md` (on-device-only data, no account/cloud/analytics, no internet permission, optional app lock, link to full `PRIVACY.md`)
- [ ] 4.6 Translate all five pages into `nl`, `fr`, `es`, `pt`, `de`

## 5. FAQ page

- [ ] 5.1 Draft the FAQ question set in `content/en/faq.md` (e.g. is Pillsner a medical device, does it work offline, what happens on a new phone, how is data kept private)
- [ ] 5.2 Translate the FAQ into `nl`, `fr`, `es`, `pt`, `de`

## 6. Report a bug / Request a feature pages

- [ ] 6.1 Author `content/en/report-a-bug.md` (title, description, steps to reproduce, app version, device/Android version fields; note that a GitHub account is required to finish filing)
- [ ] 6.2 Author `content/en/request-a-feature.md` (title, description fields; same GitHub-account note)
- [ ] 6.3 Translate both pages into `nl`, `fr`, `es`, `pt`, `de`
- [ ] 6.4 Verify the prefilled GitHub issue URL for both pages opens the correct repository, title, body and label (`bug` / `feature`)

## 7. SEO, Open Graph and documentation

- [ ] 7.1 Verify per-page, per-locale `<title>`, meta description, canonical URL and `hreflang` alternates are generated correctly for every new page (existing partials already apply per-page; confirm rather than rebuild)
- [ ] 7.2 Verify the generated `sitemap.xml` lists every new page for every locale
- [ ] 7.3 Update `src/website/README.md`'s project-layout table to describe the new multi-page content/template structure

## 8. Verification

- [ ] 8.1 Run `hugo --minify` from `src/website/` and confirm a clean build with no broken template references
- [ ] 8.2 Manually check each of the 9 pages in at least English and one other locale, including the header/footer nav and language switcher
- [ ] 8.3 Manually check the bug-report and feature-request forms both with JavaScript enabled and with it disabled
- [ ] 8.4 Confirm no analytics, tracking script or non-essential cookie was introduced
