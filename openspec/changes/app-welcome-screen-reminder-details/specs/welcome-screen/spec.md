## MODIFIED Requirements

### Requirement: Upcoming dose tile content
Each upcoming dose SHALL be rendered as a distinct tile that shows the medication name, the dose amount, the scheduled time and its intake status as an icon, a label and a colour, following the dose tile definition in the design system. For a dose that is still pending, the name and amount shown SHALL be the medicine's current name and the amount its schedules now call for; for a dose that has an intake, they SHALL be the name and amount recorded with it. The scheduled time MUST be formatted for the device locale and time zone. When the dose is scheduled on a day other than today, the tile MUST also indicate the day. Status MUST NOT be conveyed by colour alone.

The whole tile SHALL be a tap target that opens that dose's detail screen. The tile MUST remain one accessibility node with one complete description, and MUST expose a click action whose label says that tapping opens the dose. Tapping MUST NOT record an outcome or change a reminder; it only opens the screen.

#### Scenario: Dose scheduled later today
- **WHEN** a dose for "Ibuprofen", amount "1 tablet", is scheduled at 20:00 today
- **THEN** the tile shows "Ibuprofen", "1 tablet", the status "Due" with its icon, and the time 20:00 in the device's time format, with no day indication

#### Scenario: Dose scheduled tomorrow
- **WHEN** a dose is scheduled at 08:00 the following day
- **THEN** the tile shows the time and an indication that it is tomorrow

#### Scenario: Medicine renamed while a dose is pending
- **WHEN** the Welcome screen shows a pending dose for "Ibuprofen 400" and the user renames the medicine to "Ibuprofen"
- **THEN** the tile shows "Ibuprofen" without the user leaving or reopening the screen

#### Scenario: Amount changed while a dose is pending
- **WHEN** the Welcome screen shows a pending dose of "1 tablet" and the user changes the schedule's amount to "2 tablets"
- **THEN** the tile shows "2 tablets"

#### Scenario: Schedule changed while a dose is pending
- **WHEN** the Welcome screen shows a pending dose at 20:00 and the user changes that medicine's schedule to 21:00
- **THEN** the list shows a dose at 21:00 and no longer shows one at 20:00

#### Scenario: Tile is read as a single item by a screen reader
- **WHEN** a screen reader focuses a tile
- **THEN** the medication name, amount and scheduled time are announced together as one item

#### Scenario: Tapping a tile opens that dose
- **WHEN** the user taps the tile of the dose scheduled at 20:00
- **THEN** the dose detail screen for that dose is shown

#### Scenario: Tapping the third tile opens the third dose
- **WHEN** three doses are listed and the user taps the third tile
- **THEN** the dose detail screen is shown for the third dose, not for another

#### Scenario: Tap target is announced
- **WHEN** a screen reader focuses a tile
- **THEN** it announces the tile as one clickable item and says that activating it opens the dose

#### Scenario: Tapping records nothing
- **WHEN** the user taps a tile and immediately returns to the Welcome screen
- **THEN** the dose is still listed, still pending, with its status unchanged
