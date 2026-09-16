## Context

Pillsner currently has no public web presence: no landing page, no SEO-indexable description of the app, and no page to point a Play Store listing, a support email, or a social share back to. The rest of the repository is a single Android Gradle project (`src/app`, `src/wear`, `src/shared`) plus the OpenSpec planning tree and design docs. This change adds a static marketing site as a new, independent deliverable placed at `src/website/`, at the explicit direction of the repository owner (the site is unrelated to the Android Gradle build and uses its own toolchain, but is co-located under `src/` rather than at the repository root).

The site is purely informational: it has no backend, no accounts, no forms that collect personal data, and no server-side logic. It must reflect the same privacy stance the app makes (no analytics, no third-party trackers) even though it is a separate, unauthenticated surface not governed by the app's runtime permissions.

## Goals / Non-Goals

**Goals:**
- Ship a single Hugo site, one page per locale, covering English, Dutch, French, Spanish, Portuguese and German.
- Auto-select the visitor's locale from the browser, defaulting to English, with a manual switcher.
- Meet the SEO and Open Graph requirements in `specs/marketing-website/spec.md`: per-locale metadata, hreflang, sitemap, robots.txt, social share images.
- Reuse `docs/design-system.md` tokens (colour roles, Raleway/Montserrat type) so the site reads as the same brand as the app, in both light and dark presentation.
- Reuse `docs/icon.png` and `docs/feature-graphic.png` where they fit, and generate any additional imagery (Open Graph image, favicon set, decorative section art) consistent with the same palette.
- Produce a fully static build with no runtime server, suitable for any static host.

**Non-Goals:**
- No app functionality, accounts, forms, or data collection on the site.
- No changes to `src/app`, `src/wear`, `src/shared`, Room schemas, or reminder/alarm scheduling.
- No CMS or dynamic backend — content is authored as Hugo content files in the repository.
- No commitment to a specific hosting provider in this change; the design produces a static output directory and a build command, and names candidate free static hosts, but the proposal does not require standing one up as part of this change.
- No automated translation pipeline; translations are authored content, reviewed like any other copy.

## Decisions

### 1. Framework: Hugo, as directed
Hugo is the framework named in the request. It is a good fit for this job independent of that: it is a single static binary (no Node/npm toolchain to maintain), has first-class multilingual support (per-locale content trees, `hreflang`, locale-aware permalinks) built in rather than bolted on, and produces plain static output with no client-side framework, which keeps the page fast and keeps the "no tracking, minimal JS" goal easy to hold to.

**Alternative considered:** a JS framework (Astro, Next.js static export). Rejected: heavier toolchain, more dependencies to keep patched, and Hugo's built-in i18n is a better match for six fully static locales than bolting i18n onto a JS framework.

### 2. Location: `src/website/`
Per explicit instruction, the Hugo project lives at `src/website/`, alongside `src/app`, `src/wear` and `src/shared`. It is its own self-contained project (`hugo.toml`/`config.toml`, `content/`, `layouts/`, `assets/`, `static/`) and is **not** wired into the Android Gradle build — Gradle's `settings.gradle.kts` does not include it as a module, and the Hugo build uses the Hugo CLI, not Gradle. Android Studio opening `src/` as the Gradle project root will simply not see it as a Gradle module; it will appear as a plain folder in the IDE's project tree.

**Trade-off:** this mixes an unrelated toolchain (Hugo, Go templates, no JVM) into the same top-level folder the Gradle project treats as its root, which is a departure from the repository-layout convention in `CLAUDE.md` that reserves `src/` for the Android application. This was raised and the repository owner confirmed `src/website/` as the intended location, so the design proceeds on that basis; the README's repository-layout table is updated to document it as an explicit exception (see Impact in the proposal).

### 3. Language detection and routing
Hugo builds one static output tree per locale (`/en/`, `/nl/`, `/fr/`, `/es/`, `/pt/`, `/de/`), plus a root `/` that:
- Runs a small, inline (no external request) script that reads `navigator.languages`/`navigator.language`, matches against the six supported locales, and redirects to the matching locale path; if no match, it redirects to `/en/`.
- Also serves a server-side-free fallback: the redirect script is the only mechanism (no server-side `Accept-Language` negotiation is possible on a static host), and the root page's `<noscript>` content and crawlable link set points to `/en/` so crawlers and non-JS clients still reach real content rather than an empty shell.
- Sets `x-default` in hreflang to the root URL, and each locale's canonical to its own locale path.
- A visible language switcher (present on every locale page, not just root) lets the visitor override the auto-selection at any time; the choice is remembered via `localStorage` (not a tracking cookie) so a returning visitor's manual choice sticks.

**Alternative considered:** server-side `Accept-Language` negotiation via a redirect rule at the hosting edge (e.g. Netlify/Cloudflare redirect rules keyed on the header). This is more correct (works with JS disabled, no flash of English before the script runs) and is noted as the preferred approach when a specific static host is chosen; the client-side script is the framework-agnostic baseline that works on any plain static host per the Non-Goals above. The design allows either, and a hosting-specific redirect rule is a drop-in improvement, not a required rewrite.

**Implementation note:** with `defaultContentLanguageInSubdir = true`, Hugo itself generates a root `/` page (its built-in "alias" redirect, a bare meta-refresh to the default-language home) — a plain `static/index.html` is silently overwritten by it at build time. The redirect/detection page above is therefore implemented as `layouts/alias.html`, Hugo's documented override point for that generated page, rather than as a static file; the behaviour (script, noscript fallback, hreflang set) is unchanged from what's described here.

### 4. Design system reuse
Colour roles, type scale and light/dark behaviour are ported from `docs/design-system.md` into the site's CSS as custom properties (`--pillsner-primary`, `--pillsner-on-primary`, etc.), mirrored for `prefers-color-scheme: dark`. Raleway (200/300) and Montserrat (400/500/600) are loaded as static font files (matching the app's "no downloadable fonts over the network" stance) rather than a Google Fonts CDN link, keeping the site's "no third-party requests" promise consistent with the app's privacy stance. Red/error roles are not used anywhere on the site, since the site has no danger or destructive states.

### 5. Imagery
`docs/icon.png` is used as the favicon source (generated into the standard favicon sizes) and appears in the hero/header. `docs/feature-graphic.png` is used as a hero visual and as the base for the default Open Graph share image. Additional decorative imagery (section dividers, feature icons) is generated to match the same palette and mood rather than pulled from a stock library, keeping the asset set self-authored and licence-clean.

### 6. Build and CI
A `Makefile`/npm-free build step wraps `hugo --minify` from `src/website/`. A separate GitHub Actions workflow (e.g. `.github/workflows/website.yml`) builds the site on changes under `src/website/**` or `docs/**` (so a design-system or asset update rebuilds it too) and can optionally publish the static output as a build artifact or to a static host; it is independent of `ci.yml`'s Android jobs and does not share a working directory or cache key with them.

## Risks / Trade-offs

- **[Risk]** Placing a non-Android toolchain inside `src/` blurs the "Gradle project root" convention and could confuse a contributor who assumes everything under `src/` builds with Gradle. → **Mitigation:** document the exception clearly in the README repository-layout table and in `src/website/README.md`, and keep the Hugo project fully self-contained (its own config, no shared files with the Gradle build) so nothing under `src/website/` is ever picked up by a Gradle sync.
- **[Risk]** Client-side-only language redirection means a visitor with JavaScript disabled always lands on English content at the root, even if their browser prefers another supported locale. → **Mitigation:** the root page still contains real, crawlable English content and a visible manual language switcher works without JS (plain links), so no visitor is stuck; a future change can add a hosting-specific header-based redirect for a fully server-side-correct experience.
- **[Risk]** Maintaining six translations by hand risks drift (a section updated in English but not the other five). → **Mitigation:** the fallback-to-English behaviour (spec: "Fallback for an untranslated string") means a missed translation degrades to English text rather than a blank or broken section, and translation completeness can be checked with a simple key-diff script in CI.
- **[Risk]** Generated Open Graph/social images and other artwork could drift from the design system if produced ad hoc. → **Mitigation:** derive them programmatically/manually from the existing tokens (hex values, type) documented in `docs/design-system.md` rather than free-handing new colours.

## Migration Plan

This is a net-new, additive capability with no existing users or data — there is nothing to migrate and no rollback beyond removing `src/website/` and its CI workflow if the site is abandoned. Deployment (choosing and configuring a static host) is out of scope for this change per the Non-Goals; the deliverable is a working local Hugo build (`hugo server` and `hugo --minify`) plus the CI build check.

## Open Questions

- Which static host (GitHub Pages, Netlify, Cloudflare Pages, Azure Static Web Apps) will actually serve the site, and under what domain? Left open for the apply step or a follow-up change; the design does not depend on a specific host.
- Should the site later grow a support/contact page or changelog page? Explicitly out of scope now (single page only); would be a separate proposal.
