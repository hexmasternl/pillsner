## ADDED Requirements

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
