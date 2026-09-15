# welcome-screen Specification

## Purpose
TBD - created by archiving change app-welcome-screen. Update Purpose after archive.
## Requirements
### Requirement: Welcome screen header
The welcome screen SHALL display the Pillsner logo and the app title "Pillsner" at the top of the screen. The title MUST come from a string resource and the logo MUST be a drawable resource that can be replaced without code changes.

The title SHALL be rendered as a two-colour wordmark that shows the two words the name blends: the leading fragment "Pills" in the secondary colour role and the trailing fragment "ner" in the primary colour role, in both the light and the dark scheme. The two fragments MUST render as one unbroken word with no inserted space or gap, in a single text element, at one type role and one font weight. The colours MUST come from the theme's colour scheme and MUST NOT be hard-coded in the screen.

The colouring is decorative. The title MUST remain a single element whose text is the whole word "Pillsner", so that assistive technologies announce it as one word and text matching finds it as one word.

#### Scenario: Header visible on launch
- **WHEN** the app is launched and the welcome screen is shown
- **THEN** the logo image and the title "Pillsner" are visible above any dose content

#### Scenario: Title is split into two colours
- **WHEN** the welcome screen header is rendered
- **THEN** the characters "Pills" use the secondary colour role and the characters "ner" use the primary colour role, with no gap between them and no difference in size or weight

#### Scenario: Both schemes
- **WHEN** the device is in dark mode
- **THEN** the two fragments use the dark scheme's secondary and primary colours, and both remain legible against the surface

#### Scenario: Logo has an accessible description
- **WHEN** a screen reader focuses the header
- **THEN** the logo exposes a content description naming the app, and the title is read as text

#### Scenario: Title is one node for accessibility and tests
- **WHEN** a screen reader focuses the title, or a test matches on the text "Pillsner"
- **THEN** exactly one element is found and its text is "Pillsner", not two separate elements reading "Pills" and "ner"

#### Scenario: Title resource does not contain the accent fragment
- **WHEN** the app title resource is changed to a name that does not end in "ner"
- **THEN** the whole title is rendered in the leading colour as one word, and the header renders without error

### Requirement: Upcoming doses list
The welcome screen SHALL show the user's pending doses ordered by scheduled time, soonest first. A pending dose whose scheduled time has passed but which has not been answered and has not lapsed as missed MUST be included and appears first. Doses that are taken, skipped or missed MUST NOT be shown. It MUST show at most five doses, even when more are available.

#### Scenario: Fewer than five upcoming doses
- **WHEN** three pending doses exist
- **THEN** exactly three tiles are shown, ordered by scheduled time ascending

#### Scenario: More than five upcoming doses
- **WHEN** eight pending doses exist
- **THEN** exactly five tiles are shown, and they are the five with the earliest scheduled times

#### Scenario: Overdue pending dose
- **WHEN** a dose scheduled at 08:00 is unanswered at 09:00 and another dose is due at 20:00
- **THEN** the 08:00 dose is shown first, followed by the 20:00 dose

#### Scenario: Answered dose disappears
- **WHEN** a dose shown on the welcome screen is recorded as taken from the notification
- **THEN** its tile disappears without the user leaving and re-entering the screen

#### Scenario: Doses update while the screen is visible
- **WHEN** the set of pending doses changes while the welcome screen is displayed
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
The welcome screen SHALL obtain upcoming doses only through an `UpcomingDosesRepository` interface in the domain layer that exposes them as a reactive stream. `UpcomingDose` SHALL carry the amount as a typed `Quantity`, formatted for display in the UI layer. The domain layer types for this contract MUST NOT depend on Android framework classes. The app SHALL be wired with the Room-backed implementation that reads pending doses from the dose records.

#### Scenario: Room-backed repository
- **WHEN** the dose records contain pending doses
- **THEN** the welcome screen shows them, formatted with the same amount formatter as the medicine overview

#### Scenario: Empty records
- **WHEN** there are no pending doses
- **THEN** the welcome screen shows the empty state

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

### Requirement: Reminder readiness banner
The welcome screen SHALL show a banner above the dose list when reminders cannot be delivered as designed: when notification permission is denied, or when exact alarms are not permitted. The banner text MUST come from string resources and MUST offer a button that opens the relevant system setting. The banner MUST disappear once the condition is resolved.

#### Scenario: Notifications denied
- **WHEN** notification permission is denied
- **THEN** the banner says reminders cannot be shown and its button opens the app's notification settings

#### Scenario: Exact alarms not permitted
- **WHEN** exact alarms are not permitted
- **THEN** the banner says reminders may be up to ten minutes late and its button opens the exact-alarm setting

#### Scenario: Everything permitted
- **WHEN** notifications and exact alarms are both permitted
- **THEN** no banner is shown

#### Scenario: Banner is accessible
- **WHEN** a screen reader focuses the banner
- **THEN** it reads the message and the button label

