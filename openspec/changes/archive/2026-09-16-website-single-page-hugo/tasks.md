## 1. Hugo project scaffold

- [x] 1.1 Verify `src/app`, `src/wear`, `src/shared` (the Android scaffold) already exist; stop and flag if they do not, per `CLAUDE.md`.
- [x] 1.2 Create `src/website/` as a standalone Hugo project: `hugo.toml`, `content/`, `layouts/`, `assets/`, `static/`, `i18n/`, `archetypes/`.
- [x] 1.3 Pin the Hugo version used (extended edition, for image processing and SCSS) in a `src/website/README.md` and, if available, a version-lock mechanism (e.g. `.hugoversion` or documented `go.mod`-based module pin).
- [x] 1.4 Configure `hugo.toml` for multilingual output: `defaultContentLanguage: en`, the six languages (`en`, `nl`, `fr`, `es`, `pt`, `de`) with their `languageName` and `weight`, and `defaultContentLanguageInSubdir: true` so English also serves from `/en/`.
- [x] 1.5 Add `src/website/README.md` documenting the local dev commands (`hugo server`, `hugo --minify`) and that this project is independent of the Gradle build in the rest of `src/`.

## 2. Design tokens and base styles

- [x] 2.1 Port the Material 3 colour roles for light and dark from `docs/design-system.md` §2.2 into CSS custom properties in `assets/css/tokens.css`, switched via `prefers-color-scheme: dark`.
- [x] 2.2 Add the bundled Raleway (200/300) and Montserrat (400/500/600) static font files to `static/fonts/` (or `assets/fonts/`) and declare `@font-face` rules; no external font CDN requests.
- [x] 2.3 Implement the type scale anchors from `docs/design-system.md` §3.2 (Raleway 48/200 display, Montserrat 18/400 body) as CSS classes/utility styles.
- [x] 2.4 Build the base page layout (`layouts/_default/baseof.html`) with header, main, footer regions and the light/dark-aware background/surface colours.

## 3. Visual assets

- [x] 3.1 Generate a favicon set (ico/png/svg, multiple sizes) from `docs/icon.png` and place it under `static/favicons/`, referenced from the base layout head.
- [x] 3.2 Generate a default Open Graph/social share image (1200x630) based on `docs/feature-graphic.png` and the design system palette/type, saved per the naming the OG partial expects.
- [x] 3.3 Generate any additional decorative section imagery (feature icons, dividers) consistent with the same palette; store under `assets/images/` so Hugo's image processing can resize/optimize them at build time.

## 4. Page content — English (source of truth)

- [x] 4.1 Write English hero content: app name, tagline ("Your partner in taking your pills."), short pitch, Play Store button copy.
- [x] 4.2 Write English "why Pillsner" section content from README's "Why Pillsner" section, adapted for a public audience.
- [x] 4.3 Write English feature highlights content covering medication management, schedules & reminders, wearable support, and intake tracking, summarised from README's "Features" section.
- [x] 4.4 Write English privacy/permissions callout content summarising the no-network, on-device-only stance and linking to `PRIVACY.md`'s published equivalent.
- [x] 4.5 Write English footer content: Play Store link, privacy policy link, GitHub repository link, copyright line.
- [x] 4.6 Write English SEO metadata: page title, meta description, keywords (if used).

## 5. Single-page template and sections

- [x] 5.1 Build the single-page home template (`layouts/index.html`) that renders hero, why-Pillsner, features, privacy callout and footer sections in order, reading content from the page's front matter/content body.
- [x] 5.2 Add in-page anchor navigation (e.g. a slim top nav or hero links) so long-page sections are reachable, meeting the "reachable by scrolling or in-page anchor links" scenario.
- [x] 5.3 Implement the Play Store button component (hero + footer) linking to `https://play.google.com/store/apps/details?id=nl.hexmaster.pillsner`.
- [x] 5.4 Ensure semantic heading structure (one `h1`, ordered `h2`/`h3` per section) for accessibility and SEO.

## 6. Translations

- [x] 6.1 Translate all English content (hero, why-Pillsner, features, privacy callout, footer, SEO metadata) into Dutch under `content/nl/` (or `i18n/nl.toml` for UI strings).
- [x] 6.2 Translate into French under `content/fr/`.
- [x] 6.3 Translate into Spanish under `content/es/`.
- [x] 6.4 Translate into Portuguese under `content/pt/`.
- [x] 6.5 Translate into German under `content/de/`.
- [x] 6.6 Implement the English-fallback behaviour for any i18n key missing in a locale (verify with Hugo's `i18n` lookup order, which already falls back to `defaultContentLanguage`).

## 7. Language detection and switcher

- [x] 7.1 Implement the root `/` redirect page: inline script reads `navigator.languages`, matches against the six supported locales, redirects to the matching `/xx/` path, defaults to `/en/` when unmatched.
- [x] 7.2 Add `<noscript>` fallback content and a crawlable link to `/en/` on the root page so non-JS clients and crawlers still reach real content.
- [x] 7.3 Persist a manually chosen language in `localStorage` and honour it on return visits (skip auto-redirect if a stored preference exists).
- [x] 7.4 Build a visible language switcher component (plain links to each locale path) present on every locale page, working without JavaScript.

## 8. SEO

- [x] 8.1 Add per-locale `<title>` and meta description output driven by each page's front matter.
- [x] 8.2 Add `rel="canonical"` pointing to each locale's own URL.
- [x] 8.3 Add `hreflang` alternate `<link>` tags for all six locales plus `x-default` on every locale page.
- [x] 8.4 Enable Hugo's built-in sitemap generation (multilingual-aware) and verify `sitemap.xml` lists every locale's page.
- [x] 8.5 Add `static/robots.txt` allowing crawling and referencing the sitemap URL.

## 9. Open Graph and social sharing

- [x] 9.1 Add an Open Graph partial (`layouts/partials/opengraph.html`) emitting `og:title`, `og:description`, `og:image`, `og:type`, `og:url`, `og:locale`, and `og:locale:alternate` for the other five locales.
- [x] 9.2 Add Twitter Card meta tags (`twitter:card`, `twitter:title`, `twitter:description`, `twitter:image`) using the same generated share image.
- [x] 9.3 Verify the OG/Twitter tags render correctly per locale (manual check with a link-preview debugger against the built output).

## 10. Privacy and no-tracking verification

- [x] 10.1 Audit the built output for any third-party script, font, or image request (analytics, ads, CDNs); remove any found.
- [x] 10.2 Confirm the only client-side storage used is `localStorage` for the language preference, with no cookies set.

## 11. Build, CI and documentation

- [x] 11.1 Add a `Makefile` (or npm-free script) wrapping `hugo --minify` for local and CI builds.
- [x] 11.2 Add `.github/workflows/website.yml` building the site on changes under `src/website/**` and `docs/**`, independent of `ci.yml`'s Android jobs.
- [x] 11.3 Update `README.md`: add a `src/website/` row to the repository layout table, and note the Hugo toolchain/version as a separate entry from the Android toolchain table.
- [x] 11.4 Add a `.gitignore` entry for the Hugo build output directory (e.g. `src/website/public/`) and any Hugo resource cache directory.

## 12. Verification

- [x] 12.1 Run `hugo --minify` from `src/website/` and confirm a clean build with no warnings about missing translations or broken templates.
- [x] 12.2 Manually verify, for at least English and one other locale, that the browser-language auto-redirect, the manual switcher, the Play Store link, and the dark/light presentation all behave as specified.
- [x] 12.3 Validate the generated `sitemap.xml`, `robots.txt`, and one locale page's `hreflang`/canonical/Open Graph tags against the requirements in `specs/marketing-website/spec.md`.
