# marketing-website Specification

## Purpose
TBD - created by archiving change website-single-page-hugo. Update Purpose after archive.
## Requirements
### Requirement: Single-page informational layout
The marketing website SHALL present all content as one page per locale (no multi-page navigation), organised into a hero, a "why Pillsner" section, a feature highlights section, a privacy/permissions callout, and a footer.

#### Scenario: Visitor lands on the site
- **WHEN** a visitor opens the site's root URL
- **THEN** the hero, why-Pillsner, features, privacy callout and footer sections are all present on that single page, in that order, reachable by scrolling or in-page anchor links

### Requirement: Play Store call to action
The site SHALL link to the Google Play Store listing for application id `nl.hexmaster.pillsner` via a prominent button, present in both the hero and the footer.

#### Scenario: Visitor wants to install the app
- **WHEN** a visitor clicks the "Get it on Google Play" button in the hero or the footer
- **THEN** the browser navigates to `https://play.google.com/store/apps/details?id=nl.hexmaster.pillsner`

### Requirement: Multilingual content
The site SHALL provide fully translated content in English, Dutch, French, Spanish, Portuguese and German, built with Hugo's multilingual content structure, with English as the default/fallback locale.

#### Scenario: Content exists for every supported locale
- **WHEN** the site is built
- **THEN** a complete, translated page (hero copy, feature copy, privacy copy, footer copy, meta tags) is produced for each of English, Dutch, French, Spanish, Portuguese and German

#### Scenario: Fallback for an untranslated string
- **WHEN** a locale is missing a translated string for a given key
- **THEN** the English string is used for that key rather than leaving it blank

### Requirement: Automatic language selection with manual override
The site SHALL detect a visitor's preferred language from their browser and route or redirect them to the matching locale on first visit, defaulting to English when no supported locale matches, while always offering a visible manual language switcher.

#### Scenario: Browser language matches a supported locale
- **WHEN** a first-time visitor's browser reports a preferred language that is one of the six supported locales
- **THEN** the site presents that locale's version of the page without requiring the visitor to choose it manually

#### Scenario: Browser language is not supported
- **WHEN** a first-time visitor's browser reports a preferred language that is not one of the six supported locales
- **THEN** the site presents the English version of the page

#### Scenario: Visitor overrides the detected language
- **WHEN** a visitor selects a different language from the language switcher
- **THEN** the site displays the page in the selected language and remembers the choice for subsequent page views in that browser

### Requirement: Search engine optimisation
Each locale's page SHALL include a descriptive, locale-specific `<title>` and meta description, a canonical URL, `hreflang` alternate links to every other locale (plus an `x-default`), a generated `sitemap.xml`, and a `robots.txt` that allows indexing.

#### Scenario: Search engine crawls a locale page
- **WHEN** a search engine crawler requests any locale's page
- **THEN** the response includes a unique `<title>`, a meta description, a `rel="canonical"` link, and `hreflang` alternate links for all six locales plus `x-default`

#### Scenario: Crawler discovers site structure
- **WHEN** a search engine crawler requests `/sitemap.xml` or `/robots.txt`
- **THEN** the sitemap lists every locale's page and robots.txt permits crawling of the site

### Requirement: Open Graph and social sharing metadata
Each locale's page SHALL include Open Graph and Twitter Card meta tags (`og:title`, `og:description`, `og:image`, `og:type`, `og:url`, `og:locale`, `og:locale:alternate` for the other five locales, `twitter:card`) so that shared links render a title, description and image in chat apps and social platforms.

#### Scenario: Link is shared in a chat app or social platform
- **WHEN** a visitor shares the site URL for any locale in a chat app or social platform that unfurls Open Graph metadata
- **THEN** the unfurled preview shows that locale's title, description and a generated share image sized for social previews

### Requirement: Visual identity follows the design system
The site's colours, typography and light/dark presentation SHALL follow `docs/design-system.md` (Pillsner green/blue/white palette, Raleway display type, Montserrat body type), and SHALL follow the visitor's OS/browser colour scheme preference for light or dark presentation.

#### Scenario: Visitor's system is set to dark mode
- **WHEN** a visitor with an OS/browser dark-mode preference opens the site
- **THEN** the site renders using the dark variant of the Pillsner colour roles rather than the light variant

#### Scenario: Brand assets are used
- **WHEN** the hero and footer render
- **THEN** they use the Pillsner app icon and/or feature graphic, or graphics generated to match the same palette and mood, rather than unrelated stock imagery

### Requirement: No tracking, no non-essential cookies
The site SHALL NOT load analytics scripts, third-party trackers, or advertising pixels, and SHALL NOT set cookies beyond what is strictly required to remember a manually chosen language.

#### Scenario: Page loads with no tracking scripts
- **WHEN** any locale page is loaded
- **THEN** no analytics, advertising or third-party tracking script is requested and no tracking cookie is set

### Requirement: Static build with no runtime server dependency
The site SHALL build to static HTML/CSS/JS/assets via Hugo such that it can be served by any static host with no server-side runtime, database or API dependency.

#### Scenario: Site is built
- **WHEN** the Hugo build command runs against `src/website/`
- **THEN** it produces a complete static output directory that can be served as-is by a static file host, with no server-side code required at request time
