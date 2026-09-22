# Pillsner website

The public, multilingual marketing site for Pillsner: what the app does, a link to install it, an FAQ, and a way to report a bug or request a feature. Built with [Hugo](https://gohugo.io/), Hugo's extended edition (needed for its built-in image processing, used here to generate favicons and the Open Graph share image from `assets/images/`).

This project is **independent of the Android Gradle build** in the rest of `src/`. It is not a Gradle module, is not referenced from `settings.gradle.kts`, and does not share any files with `src/app`, `src/wear` or `src/shared`. It does read one folder outside its own: the app screenshots in `docs/screens/`, mounted read-only into Hugo's assets so the site does not have to keep a second copy of them. It happens to live under `src/` at the repository owner's explicit direction rather than at the repository root; see `openspec/changes/website-single-page-hugo/design.md` (decision 2) for the rationale and trade-off.

## Toolchain

| Component | Version |
| --- | --- |
| Hugo | 0.165.0 extended (or newer 0.16x extended — never a pre-release) |

Pin your local Hugo install to the extended edition. Check with:

```sh
hugo version
# should report "extended" in the output
```

## Local development

From this directory (`src/website/`):

```sh
hugo server -D          # live-reloading local server, http://localhost:1313
hugo --minify            # production build, output in ./public
```

`public/` and Hugo's resource cache (`resources/_gen/`) are build output and are git-ignored; never commit them.

## Project layout

| Path | Holds |
| --- | --- |
| `hugo.toml` | Site config: languages, params (Play Store URL, GitHub URL, Open Graph locale codes), sitemap settings. |
| `content/<lang>/_index.md` | The home page's copy for each locale (`en`, `nl`, `fr`, `es`, `pt`, `de`), as structured front matter: hero, why-Pillsner, a feature summary linking to the topic pages, privacy, help, footer. |
| `content/<lang>/<slug>.md` | One file per other page, per locale: `medicines`, `schedules`, `tracking`, `wearable`, `privacy` (shared content-page front matter: hero + `sections` + `cta`), `faq` (`type: faq`, a `questions` list) and `report-a-bug`/`request-a-feature` (`type: issue-report`, an `issueTemplate`/`issueLabel` and a `fields` list). Slugs stay English in every locale so the six versions of a page stay linked to each other. |
| `i18n/<lang>.toml` | Short, reused chrome strings (nav labels, the Play Store button label, the language switcher). Hugo falls back to `en.toml` for any key missing from another locale's file. |
| `layouts/` | Templates: `_default/baseof.html` (page shell), `index.html` (home), `_default/single.html` (the shared content page), `faq/single.html`, `issue-report/single.html`, and `partials/` (SEO, Open Graph, favicons, header with the site navigation, page hero, screenshot, call to action, footer, language switcher). |
| `assets/css/` | Design tokens ported from `docs/design-system.md`, the type scale, and layout/component styles. |
| `assets/images/` | Source images (`icon-source.png`, `feature-source.png`, copied from `docs/`) that Hugo resizes at build time into favicons and the Open Graph share image. No other raster processing tool is required. |
| `assets/js/` | `language.js` (remembers a manually chosen language), `lightbox.js` (shows a screenshot full size on a dimmed backdrop) and `issue-report.js` (builds the prefilled GitHub "new issue" URL). All three are bundled into one script; none makes an external request. |
| `../../docs/screens/` | The app screenshots shown on the content pages. They are not copied here: `hugo.toml` mounts that folder as `assets/images/screens`, and Hugo resizes each one into a thumbnail plus a larger version for the overlay. |
| `static/fonts/` | The same bundled Montserrat/Raleway static TTFs the Android app uses, served directly — no external font requests. |
| `layouts/alias.html` | The language-neutral root page (`/`). Hugo generates this page itself whenever `defaultContentLanguageInSubdir = true` (a built-in redirect to the default language) and silently overwrites a plain `static/index.html`, so this template is Hugo's documented override point for it instead. It detects the browser's language client-side, redirects into the matching `/<lang>/` page (default `/en/`), and has a no-JS `<noscript>` fallback with a manual language list. |
| `static/robots.txt` | Allows crawling and points at the generated sitemap. |

## Adding or updating a translation

Edit `content/<lang>/<page>.md` for the page copy and `i18n/<lang>.toml` for chrome strings (navigation labels, button labels). English (`en`) is the source of truth: `content/en/` and `i18n/en.toml` should always be updated first, and every other locale should carry the same set of front-matter keys / TOML keys. A missing `i18n` key falls back to English automatically; a missing front-matter field does not, so keep the locale files structurally in sync with English.

## Deployment

`.github/workflows/website.yml` builds this site and deploys it to an Azure Static Web Apps (Free tier) resource on every push to `main` that changes `src/website/**`, or on a manual `workflow_dispatch` run. It authenticates via OpenID Connect using the repository's `AZURE_WEBSITE_CLIENT_ID`, `AZURE_WEBSITE_TENANT_ID` and `AZURE_WEBSITE_SUBSCRIPTION_ID` secrets, then provisions the resource group and the Static Web App itself via the Bicep templates in `infra/website/` (idempotent — safe to run on every deployment) and deploys using a deployment token fetched fresh from that provisioning step. No deployment token is stored in GitHub. The only remaining manual step is the Microsoft Entra app registration and its federated credential/role assignment; see `openspec/changes/website-deployment-workflow/tasks.md` (section 1) for that one-time setup checklist.

## Privacy

No analytics, no third-party trackers, no advertising pixels, and no cookies. The only client-side storage used is `localStorage`, to remember a visitor's manually chosen language — see `assets/js/language.js`.

The report-a-bug and request-a-feature pages keep that stance: they have no backend and hold no GitHub token. Each form is a plain `GET` form whose action is GitHub's own "new issue" page, carrying the issue-form template and label in hidden fields, so it prefills the matching GitHub Issue Form (`.github/ISSUE_TEMPLATE/bug_report.yml` / `feature_request.yml`) even with JavaScript switched off. `assets/js/issue-report.js` only trims over-long values before building the same URL. Nothing a visitor types is sent anywhere except to GitHub, by the visitor, and a GitHub account is needed to actually file the issue — both pages say so.
