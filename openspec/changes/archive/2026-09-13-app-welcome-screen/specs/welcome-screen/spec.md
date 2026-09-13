## ADDED Requirements

### Requirement: Welcome screen header
The welcome screen SHALL display the Pillsner logo and the app title "Pillsner" at the top of the screen. The title MUST come from a string resource and the logo MUST be a drawable resource that can be replaced without code changes.

#### Scenario: Header visible on launch
- **WHEN** the app is launched and the welcome screen is shown
- **THEN** the logo image and the title "Pillsner" are visible above any dose content

#### Scenario: Logo has an accessible description
- **WHEN** a screen reader focuses the header
- **THEN** the logo exposes a content description naming the app, and the title is read as text

### Requirement: Upcoming doses list
The welcome screen SHALL show the user's upcoming doses ordered by scheduled time, soonest first. It MUST show at most five doses, even when more are available.

#### Scenario: Fewer than five upcoming doses
- **WHEN** three upcoming doses exist
- **THEN** exactly three tiles are shown, ordered by scheduled time ascending

#### Scenario: More than five upcoming doses
- **WHEN** eight upcoming doses exist
- **THEN** exactly five tiles are shown, and they are the five with the earliest scheduled times

#### Scenario: Doses update while the screen is visible
- **WHEN** the set of upcoming doses changes while the welcome screen is displayed
- **THEN** the tiles update to reflect the new set without the user leaving and re-entering the screen

### Requirement: Upcoming dose tile content
Each upcoming dose SHALL be rendered as a distinct tile that shows the medication name, the dose amount, the scheduled time and its intake status as an icon, a label and a colour, following the dose tile definition in the design system. The scheduled time MUST be formatted for the device locale and time zone. When the dose is scheduled on a day other than today, the tile MUST also indicate the day. Status MUST NOT be conveyed by colour alone.

#### Scenario: Dose scheduled later today
- **WHEN** a dose for "Ibuprofen", amount "1 tablet", is scheduled at 20:00 today
- **THEN** the tile shows "Ibuprofen", "1 tablet", the status "Due" with its icon, and the time 20:00 in the device's time format, with no day indication

#### Scenario: Dose scheduled tomorrow
- **WHEN** a dose is scheduled at 08:00 the following day
- **THEN** the tile shows the time and an indication that it is tomorrow

#### Scenario: Tile is read as a single item by a screen reader
- **WHEN** a screen reader focuses a tile
- **THEN** the medication name, amount and scheduled time are announced together as one item

### Requirement: Empty state
When there are no upcoming doses, the welcome screen SHALL show an empty state instead of an empty list: an icon, a headline and a hint, following the empty state definition in the design system. The texts MUST come from string resources.

#### Scenario: No upcoming doses
- **WHEN** the repository reports zero upcoming doses
- **THEN** no tiles are shown and the empty state with the headline "Nothing due right now" and the hint "Your next dose will appear here." is displayed below the header

#### Scenario: Doses appear after empty state
- **WHEN** the screen shows the empty state and one upcoming dose becomes available
- **THEN** the empty-state message is replaced by a single tile

### Requirement: Upcoming doses are provided through a domain contract
The welcome screen SHALL obtain upcoming doses only through an `UpcomingDosesRepository` interface in the domain layer that exposes them as a reactive stream. The domain layer types for this contract MUST NOT depend on Android framework classes.

#### Scenario: Placeholder repository
- **WHEN** no medication or schedule data source exists
- **THEN** the app is wired with a repository implementation that emits an empty list, and the welcome screen shows the empty state

#### Scenario: View model enforces the cap
- **WHEN** the repository emits more than five doses despite being asked for five
- **THEN** the view model exposes only the first five to the screen

### Requirement: Visual baseline from the design system
The welcome screen SHALL be rendered with the Pillsner theme defined in `docs/design-system.md`: the brand light and dark colour schemes following the system setting, the bundled Montserrat and Raleway type scale, and the shape and spacing tokens. The app MUST NOT use Material You dynamic colour, and no screen MAY define a colour, text size or spacing that is not a theme token.

#### Scenario: Dark mode follows the system
- **WHEN** the system is set to dark theme
- **THEN** the welcome screen renders with the dark colour scheme from the design system and the same layout

#### Scenario: Wallpaper colour ignored
- **WHEN** the device wallpaper produces a purple dynamic palette
- **THEN** the app still renders in Pillsner green, blue and white

### Requirement: Large font and one-handed use
The welcome screen SHALL remain usable when the system font scale is at its largest setting. Dose content MUST scroll when it does not fit, and no text MUST be clipped.

#### Scenario: Largest font scale with five tiles
- **WHEN** the system font scale is at maximum and five upcoming doses are shown
- **THEN** all five tiles are reachable by scrolling and every tile's text is fully visible
