## ADDED Requirements

### Requirement: Tapping a tile opens the medicine details
Every medicine tile on the Medicines screen SHALL be tappable. Tapping a closed tile SHALL navigate to the Medicine details screen for that medicine. Tapping a tile whose activation action is revealed SHALL close the tile and MUST NOT navigate. Opening the details screen SHALL close any revealed tile. The tile's merged accessibility node SHALL expose the tap as its default click action with the label "Open medicine details" from a string resource, while keeping the Activate or Deactivate custom action.

#### Scenario: Tap a closed active tile
- **WHEN** the user taps the closed tile of "Ibuprofen" in the active section
- **THEN** the Medicine details screen for "Ibuprofen" is shown

#### Scenario: Tap a closed inactive tile
- **WHEN** the user taps a closed tile in the inactive section
- **THEN** the Medicine details screen for that medicine is shown

#### Scenario: Tap a revealed tile
- **WHEN** a tile is revealed and the user taps it
- **THEN** the tile closes and the Medicines screen stays

#### Scenario: Tap while another tile is revealed
- **WHEN** tile A is revealed and the user taps the closed tile B
- **THEN** the details screen for B is shown, and on return tile A is closed

#### Scenario: Short drag is not a tap
- **WHEN** the user drags a tile a short distance and lifts
- **THEN** the tile snaps closed and the details screen is not shown

#### Scenario: Screen reader default action
- **WHEN** a screen reader focuses the tile of "Ibuprofen" and the user performs the default activation
- **THEN** the Medicine details screen for "Ibuprofen" is shown, and the actions menu still lists "Deactivate"

## MODIFIED Requirements

### Requirement: Swiping a tile reveals one activation action
Every medicine tile on the Medicines screen SHALL be draggable horizontally towards the start edge to reveal a single action button behind it at the trailing edge. On an active tile the button SHALL read "Deactivate"; on an inactive tile it SHALL read "Activate". Both labels MUST come from string resources. A drag that does not pass half the button width SHALL snap the tile closed; a drag that passes it, or a fling towards the start, SHALL settle the tile revealed. The tile SHALL remain revealed until the user taps the button, taps the tile itself, drags it closed, or reveals another tile. Dragging towards the end edge from the closed position SHALL do nothing. The button MUST NOT be reachable by touch or accessibility focus while the tile is closed.

#### Scenario: Reveal on an active tile
- **WHEN** the user drags an active tile towards the start edge past half the button width and lifts
- **THEN** the tile settles revealed and a "Deactivate" button is visible at its trailing edge

#### Scenario: Reveal on an inactive tile
- **WHEN** the user drags an inactive tile towards the start edge past half the button width and lifts
- **THEN** the tile settles revealed and an "Activate" button is visible at its trailing edge

#### Scenario: Short drag snaps back
- **WHEN** the user drags a tile less than half the button width and lifts
- **THEN** the tile returns to the closed position and no button is visible

#### Scenario: Drag towards the end edge
- **WHEN** the user drags a closed tile towards the end edge
- **THEN** the tile does not move

#### Scenario: Swiping a tile does not change the medicine
- **WHEN** the user reveals a tile and does not tap the button
- **THEN** the medicine's active flag is unchanged and the tile stays in its section

#### Scenario: Tapping a revealed tile closes it
- **WHEN** a tile is revealed and the user taps the tile
- **THEN** the tile animates closed, the medicine's active flag is unchanged and no other screen is shown

#### Scenario: Right-to-left layout
- **WHEN** the layout direction is right-to-left and the user drags a tile towards the start edge
- **THEN** the button is revealed at the trailing edge of the tile

#### Scenario: Vertical scroll still works
- **WHEN** the list holds more tiles than fit on screen and the user scrolls vertically over a tile
- **THEN** the list scrolls and no tile is revealed
