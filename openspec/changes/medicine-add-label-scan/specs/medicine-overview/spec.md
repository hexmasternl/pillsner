## MODIFIED Requirements

### Requirement: Add medicine button
The Medicines screen SHALL show a large floating action button anchored to the bottom right, above the bottom navigation bar, with an add icon and the content description "Add medicine". Tapping it SHALL navigate to the Add medicine destination. The button MUST remain visible while the list scrolls and MUST NOT obscure the last tile when the list is scrolled to its end. The Medicines screen also shows a "Scan medicine label" button (medicine-label-scan) grouped with it at the bottom right; neither button SHALL obscure the other or the last tile.

#### Scenario: Button visible with an empty list
- **WHEN** the screen shows the empty state
- **THEN** the add button is visible at the bottom right

#### Scenario: Tapping the button
- **WHEN** the user taps the add button
- **THEN** the Add medicine destination is shown

#### Scenario: Last tile reachable
- **WHEN** the list holds more tiles than fit on screen and the user scrolls to the end
- **THEN** the last tile is fully visible and not covered by the button

#### Scenario: Both buttons visible together
- **WHEN** the Medicines screen is shown
- **THEN** the add button and the scan button are both visible at the bottom right, neither covering the other nor the last tile when scrolled to the end

### Requirement: Large font and one-handed use
The Medicines screen SHALL remain usable at the largest system font scale: the whole content, including both sections, MUST scroll as one list, no text MUST be clipped, and the add button and the scan button MUST remain reachable.

#### Scenario: Largest font scale with both sections
- **WHEN** the system font scale is at maximum and there are five active and three inactive medicines
- **THEN** every tile and header is reachable by scrolling, every tile's text is fully visible and the add button stays visible

#### Scenario: Largest font scale with the scan button present
- **WHEN** the system font scale is at maximum
- **THEN** both the add button and the scan button stay visible and reachable with one hand
