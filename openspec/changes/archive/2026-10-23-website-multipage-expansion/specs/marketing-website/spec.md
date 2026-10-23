## RENAMED Requirements

- FROM: `### Requirement: Single-page informational layout`
- TO: `### Requirement: Multi-page informational layout`

## MODIFIED Requirements

### Requirement: Multi-page informational layout
The marketing website SHALL present its content as a set of dedicated pages per locale — Home, Medicines, Schedules & reminders, Tracking your progress, On your wrist, Privacy & security, FAQ, Report a bug and Request a feature — rather than one single page, with Home summarising the other pages and linking to each of them.

#### Scenario: Visitor lands on the site
- **WHEN** a visitor opens a locale's root URL
- **THEN** the Home page presents a hero, a short why-Pillsner summary and a feature-summary list, each feature-summary item linking to its own dedicated page

#### Scenario: Visitor opens a dedicated page
- **WHEN** a visitor navigates to the Medicines, Schedules & reminders, Tracking your progress, On your wrist, Privacy & security, FAQ, Report a bug or Request a feature page for a given locale
- **THEN** that page renders fully translated content specific to its topic, using the same header, footer and visual identity as every other page on the site

## ADDED Requirements

### Requirement: Cross-page navigation
The site SHALL provide a persistent header navigation menu and footer links present on every page, listing every page of the site, and the language switcher SHALL remain available and functional from any page, not only Home.

#### Scenario: Visitor browses from a non-home page
- **WHEN** a visitor is on any page other than Home
- **THEN** the header navigation menu lists links to every other page, and selecting a different language in the switcher shows the equivalent page in that language rather than redirecting to Home

#### Scenario: Visitor follows a feature-summary link from Home
- **WHEN** a visitor clicks a feature-summary item on the Home page
- **THEN** the browser navigates to that feature's dedicated page in the visitor's current locale
