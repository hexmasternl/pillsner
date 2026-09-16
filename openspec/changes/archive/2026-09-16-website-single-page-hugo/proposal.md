## Why

Pillsner has no public web presence. Someone who hears about the app has nowhere to read what it does, see what it looks like, or find the Google Play listing, and the app itself has no page to link back to for support, privacy or discovery purposes. A small, static, single-page marketing site closes that gap without touching the Android app, its data model or its privacy stance: the site is purely informational, ships no user data anywhere, and links out to the Play Store listing (`nl.hexmaster.pillsner`) for installation.

## What Changes

- Add a new Hugo static site under `src/website/` that builds a single-page, informational marketing site for Pillsner.
- Visual design follows `docs/design-system.md`: Pillsner green/blue/white palette, Raleway display type and Montserrat body type, light and dark variants that follow the visitor's OS/browser preference, red reserved for nothing (the site has no danger states).
- Content sections on the one page: hero (name, tagline, hero visual, Play Store link button), why Pillsner / problem it solves, feature highlights (medication management, schedules & reminders, wearable support, intake tracking, privacy by default), a privacy/permissions callout, and a footer with links to the Play Store listing, the privacy policy (`PRIVACY.md` content) and the GitHub repository.
- A single, prominent call-to-action button linking to the Google Play listing for application id `nl.hexmaster.pillsner` (`https://play.google.com/store/apps/details?id=nl.hexmaster.pillsner`), repeated in the hero and the footer.
- Visual assets: reuse `docs/icon.png` (app icon) and `docs/feature-graphic.png` (banner) where useful; generate additional supporting illustrations/graphics for the page (e.g. an Open Graph/social share image, favicon set) consistent with the design system's colours and mood.
- Internationalisation: content authored in and translated to English, Dutch, French, Spanish, Portuguese and German, using Hugo's multilingual support. The site auto-selects the visitor's language from their browser's `Accept-Language` header/JavaScript negotiation on first visit (no account, no server-side geolocation), defaulting to English when no supported language matches, with a visible manual language switcher so the auto-choice is never a trap.
- SEO: descriptive `<title>`/meta description per language, canonical URLs, `hreflang` alternate links across all six locales, a generated `sitemap.xml` and `robots.txt`, semantic heading structure, and fast-loading static output (no client-side framework, minimal JS).
- Open Graph and Twitter Card meta tags per page/locale: `og:title`, `og:description`, `og:image` (generated share image), `og:type`, `og:url`, `og:locale` (+ `og:locale:alternate`), `twitter:card`, so links shared in chat apps and social media render with a title, description and image.
- No analytics, no third-party trackers, no cookies beyond what a static host requires — consistent with Pillsner's no-network, privacy-first stance for the app itself (the website is a separate, unauthenticated, data-free surface, so this is a site-level policy choice, not an app permission change).
- Build/deploy tooling: a Hugo build step (and, if useful, a GitHub Actions workflow) that produces static output for hosting; no runtime server dependency required.

## Capabilities

### New Capabilities
- `marketing-website`: the public, single-page, multilingual Hugo site describing Pillsner, its Play Store link, SEO metadata and Open Graph support, built from `src/website/`.

### Modified Capabilities
- (none — this change adds a new, self-contained capability and does not alter any existing app behaviour or specs)

## Impact

- **New code**: `src/website/` — a new Hugo project (config, layouts, content per locale, static assets, generated images). This lives alongside the existing `src/app`, `src/wear` and `src/shared` Gradle modules but is not part of the Gradle build; it uses its own Hugo toolchain.
- **New assets**: generated Open Graph/social share images and favicons, derived from `docs/icon.png` and `docs/feature-graphic.png` plus the design system's colour and type tokens.
- **README.md**: repository layout table gains a `src/website/` row, and the Technology/Toolchain sections note the Hugo static site as a separate, non-Android deliverable with its own version pin.
- **CI**: optionally a new, separate workflow to build (and publish) the site; does not touch `ci.yml`'s Android jobs.
- **No changes** to `src/app`, `src/wear`, `src/shared`, Room schemas, alarm/reminder scheduling, or any Android permission — the app's privacy promise and no-network stance are unaffected.
