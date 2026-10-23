# marketing-website Specification

## Purpose
TBD - created by archiving change website-single-page-hugo. Update Purpose after archive.

## Requirements

### Requirement: Multi-page informational layout
The marketing website SHALL present its content as a set of dedicated pages per locale — Home, Medicines, Schedules & reminders, Tracking your progress, On your wrist, Privacy & security, FAQ, Report a bug and Request a feature — rather than one single page, with Home summarising the other pages and linking to each of them.

#### Scenario: Visitor lands on the site
- **WHEN** a visitor opens a locale's root URL
- **THEN** the Home page presents a hero, a short why-Pillsner summary and a feature-summary list, each feature-summary item linking to its own dedicated page

#### Scenario: Visitor opens a dedicated page
- **WHEN** a visitor navigates to the Medicines, Schedules & reminders, Tracking your progress, On your wrist, Privacy & security, FAQ, Report a bug or Request a feature page for a given locale
- **THEN** that page renders fully translated content specific to its topic, using the same header, footer and visual identity as every other page on the site

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

### Requirement: Cross-page navigation
The site SHALL provide a persistent header navigation menu and footer links present on every page, listing every page of the site, and the language switcher SHALL remain available and functional from any page, not only Home.

#### Scenario: Visitor browses from a non-home page
- **WHEN** a visitor is on any page other than Home
- **THEN** the header navigation menu lists links to every other page, and selecting a different language in the switcher shows the equivalent page in that language rather than redirecting to Home

#### Scenario: Visitor follows a feature-summary link from Home
- **WHEN** a visitor clicks a feature-summary item on the Home page
- **THEN** the browser navigates to that feature's dedicated page in the visitor's current locale

### Requirement: Responsive grouped header navigation
The header navigation SHALL group the site's pages into four top-level entries — Home, Features (Medicines, Schedules, Tracking, On your wrist), Privacy, and Help (FAQ, Report a bug, Request a feature) — and SHALL adapt to the viewport: on viewports 960 px wide and wider it SHALL fit on a single row with the Features and Help groups opening as dropdown menus, and on narrower viewports it SHALL collapse behind a single Menu button that opens a panel listing every page under its group. The header SHALL never wrap onto more than one row of navigation at any viewport width in any supported locale.

#### Scenario: Visitor on a wide screen
- **WHEN** a visitor opens any page in a viewport at least 960 px wide
- **THEN** the header shows the brand, the four top-level entries and the language switcher on one row, and no page link other than Home and Privacy is visible until its group is opened

#### Scenario: Visitor opens a group on a wide screen
- **WHEN** a visitor activates the Features or Help entry with a pointer, touch, or the Enter or Space key
- **THEN** a dropdown lists that group's pages, and pressing Escape or activating anywhere outside it closes it again

#### Scenario: Visitor on a phone
- **WHEN** a visitor opens any page in a viewport narrower than 960 px
- **THEN** the header shows only the brand, the language switcher and a Menu button, each with a touch target of at least 48 × 48 px

#### Scenario: Visitor opens the menu on a phone
- **WHEN** the visitor activates the Menu button
- **THEN** a panel below the header lists every page grouped under the Features and Help headings, each link with a touch target at least 48 px tall, the panel scrolls independently if it is taller than the remaining viewport, and the button's accessible name and expanded state change to reflect that the menu is open

#### Scenario: Visitor closes the menu on a phone
- **WHEN** the menu panel is open and the visitor presses Escape, activates the Menu button again or selects a link
- **THEN** the panel closes and keyboard focus returns to the Menu button unless the visitor navigated away

#### Scenario: Visitor sees where they are
- **WHEN** a visitor is on any page
- **THEN** that page's link is marked as the current page both visually and with `aria-current="page"`, and on wide screens the group containing it is visually marked as well

#### Scenario: Long labels in another locale
- **WHEN** the site is shown in any supported locale, including the one with the longest navigation labels
- **THEN** the wide-screen header still fits on one row at 960 px, and no navigation label is truncated

#### Scenario: Visitor has JavaScript disabled
- **WHEN** a visitor without JavaScript opens any page
- **THEN** every page remains reachable from the header: on narrow viewports the menu is rendered expanded as a stacked list, and on wide viewports the Features and Help dropdowns still open and close

#### Scenario: Visitor prefers reduced motion
- **WHEN** the visitor's system requests reduced motion
- **THEN** the menu panel and dropdowns open and close without animation
