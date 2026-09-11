## ADDED Requirements

### Requirement: Medicines screen shows active and inactive sections
The Medicines destination SHALL display the user's medicines in two sections in a single scrolling list: active medicines first under a header, then inactive medicines under a second header. Section headers MUST come from string resources. The inactive section MUST be omitted entirely when there are no inactive medicines.

#### Scenario: Both active and inactive medicines exist
- **WHEN** the repository holds two active and one inactive medicine
- **THEN** the screen shows the "Active" header followed by two tiles, then the "Inactive" header followed by one tile

#### Scenario: Only active medicines exist
- **WHEN** the repository holds three active medicines and no inactive ones
- **THEN** the screen shows the "Active" header and three tiles, and no "Inactive" header is shown

#### Scenario: Only inactive medicines exist
- **WHEN** the repository holds no active medicines and two inactive ones
- **THEN** the screen shows the "Active" header with an empty-state message inviting the user to add a medicine, followed by the "Inactive" header and two tiles

### Requirement: Medicines are ordered by name within each section
Within each section the medicines SHALL be ordered by name, ascending, using a locale-aware case-insensitive comparison.

#### Scenario: Mixed-case names
- **WHEN** the active medicines are named "paracetamol", "Ibuprofen" and "Amoxicillin"
- **THEN** the tiles appear in the order Amoxicillin, Ibuprofen, paracetamol

#### Scenario: Ordering is independent per section
- **WHEN** an inactive medicine's name sorts before every active medicine's name
- **THEN** it still appears in the inactive section, after all active tiles

### Requirement: Medicine tile content
Each medicine SHALL be shown as a distinct tile that displays the medicine name and a human-readable description of its schedule. The description MUST be derived from the medicine's schedule and MUST come from string resources.

#### Scenario: Twice-daily medicine
- **WHEN** a medicine "Ibuprofen" has a schedule with two fixed times every day
- **THEN** its tile shows "Ibuprofen" and "Twice a day"

#### Scenario: Every-other-day medicine
- **WHEN** a medicine has a schedule of one dose every two days
- **THEN** its tile shows the description "Once every other day"

#### Scenario: As-needed medicine
- **WHEN** a medicine has no schedule
- **THEN** its tile shows the description "As needed"

#### Scenario: Tile is read as one item by a screen reader
- **WHEN** a screen reader focuses an active tile for "Ibuprofen" taken twice a day
- **THEN** it announces the name and the description together as a single item

### Requirement: Inactive tiles are distinguishable
Inactive medicine tiles SHALL be visually de-emphasised relative to active tiles, and a screen reader MUST announce that the medicine is inactive. The distinction MUST NOT rely on colour alone.

#### Scenario: Screen reader on an inactive tile
- **WHEN** a screen reader focuses a tile in the inactive section
- **THEN** the announcement includes the medicine name, the schedule description and the word "Inactive"

#### Scenario: Visual distinction
- **WHEN** an active and an inactive tile are both visible
- **THEN** the inactive tile uses a different card style from the active tile in addition to sitting under the "Inactive" header

### Requirement: Empty state when there are no medicines
When the user has no medicines at all, the screen SHALL show a single empty-state message that invites the user to add a medicine, instead of section headers or tiles. The message MUST come from a string resource.

#### Scenario: No medicines
- **WHEN** the repository emits an empty list
- **THEN** no section headers or tiles are shown and the empty-state message is displayed

#### Scenario: First medicine appears
- **WHEN** the empty state is shown and one active medicine is added to the repository
- **THEN** the empty-state message is replaced by the "Active" header and one tile

### Requirement: Overview updates live
The screen SHALL reflect changes to the set of medicines, their names, schedules or active flags while it is visible, without the user leaving and re-entering.

#### Scenario: Medicine becomes inactive while visible
- **WHEN** an active medicine's active flag changes to false while the screen is displayed
- **THEN** its tile moves from the active section to the inactive section

### Requirement: Add medicine button
The Medicines screen SHALL show a large floating action button anchored to the bottom right, above the bottom navigation bar, with an add icon and the content description "Add medicine". Tapping it SHALL navigate to the Add medicine destination. The button MUST remain visible while the list scrolls and MUST NOT obscure the last tile when the list is scrolled to its end.

#### Scenario: Button visible with an empty list
- **WHEN** the screen shows the empty state
- **THEN** the add button is visible at the bottom right

#### Scenario: Tapping the button
- **WHEN** the user taps the add button
- **THEN** the Add medicine destination is shown

#### Scenario: Last tile reachable
- **WHEN** the list holds more tiles than fit on screen and the user scrolls to the end
- **THEN** the last tile is fully visible and not covered by the button

### Requirement: Large font and one-handed use
The Medicines screen SHALL remain usable at the largest system font scale: the whole content, including both sections, MUST scroll as one list, no text MUST be clipped, and the add button MUST remain reachable.

#### Scenario: Largest font scale with both sections
- **WHEN** the system font scale is at maximum and there are five active and three inactive medicines
- **THEN** every tile and header is reachable by scrolling, every tile's text is fully visible and the add button stays visible

### Requirement: Medicines are provided through a domain contract
The Medicines screen SHALL obtain medicines only through the `MedicationRepository` interface and SHALL compute partitioning and ordering in its view model. The view model MUST expose a schedule summary, not formatted text, so wording is resolved in the UI layer.

#### Scenario: Placeholder repository
- **WHEN** no persistence exists
- **THEN** the app is wired with an in-memory repository that starts empty and the screen shows the empty state

#### Scenario: View model partitions and orders
- **WHEN** the repository emits an unordered mix of active and inactive medicines
- **THEN** the view model state contains an active list and an inactive list, each ordered by name
