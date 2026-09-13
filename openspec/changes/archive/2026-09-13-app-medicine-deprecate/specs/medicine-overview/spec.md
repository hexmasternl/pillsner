## ADDED Requirements

### Requirement: Swiping a tile reveals one activation action
Every medicine tile on the Medicines screen SHALL be draggable horizontally towards the start edge to reveal a single action button behind it at the trailing edge. On an active tile the button SHALL read "Deactivate"; on an inactive tile it SHALL read "Activate". Both labels MUST come from string resources. A drag that does not pass half the button width SHALL snap the tile closed; a drag that passes it, or a fling towards the start, SHALL settle the tile revealed. The tile SHALL remain revealed until the user taps the button, drags it closed, or reveals another tile. Dragging towards the end edge from the closed position SHALL do nothing. The button MUST NOT be reachable by touch or accessibility focus while the tile is closed.

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

#### Scenario: Right-to-left layout
- **WHEN** the layout direction is right-to-left and the user drags a tile towards the start edge
- **THEN** the button is revealed at the trailing edge of the tile

#### Scenario: Vertical scroll still works
- **WHEN** the list holds more tiles than fit on screen and the user scrolls vertically over a tile
- **THEN** the list scrolls and no tile is revealed

### Requirement: Only one tile is revealed at a time
The Medicines screen SHALL keep at most one tile revealed. Revealing a tile SHALL close any other revealed tile. The revealed state SHALL survive a configuration change and SHALL be cleared when the revealed tile leaves the list or its action is tapped.

#### Scenario: Revealing a second tile
- **WHEN** one tile is revealed and the user reveals another
- **THEN** the first tile animates closed and only the second is revealed

#### Scenario: Rotation while revealed
- **WHEN** a tile is revealed and the device rotates
- **THEN** the same tile is still revealed afterwards

### Requirement: Deactivating and activating a medicine from its tile
Tapping "Deactivate" on a revealed active tile SHALL set the medicine's active flag to false; tapping "Activate" on a revealed inactive tile SHALL set it to true. The change SHALL be made through the repository, and the screen SHALL reflect it from the repository stream: the tile closes and moves to the other section at its alphabetical position with an animated move. No confirmation MUST be asked. When the write fails, the screen SHALL show a snackbar "Could not update medicine" from a string resource and the tile SHALL stay where it was.

#### Scenario: Deactivate moves the tile down
- **WHEN** the active section holds "Amoxicillin", "Ibuprofen" and "Paracetamol", the inactive section holds "Metoprolol", and the user reveals "Ibuprofen" and taps "Deactivate"
- **THEN** the active section shows "Amoxicillin", "Paracetamol" and the inactive section shows "Ibuprofen", "Metoprolol" in that order

#### Scenario: Activate moves the tile up
- **WHEN** the inactive section holds "Ibuprofen" and "Metoprolol", the active section holds "Amoxicillin" and "Paracetamol", and the user reveals "Metoprolol" and taps "Activate"
- **THEN** the active section shows "Amoxicillin", "Metoprolol", "Paracetamol" and the inactive section shows "Ibuprofen"

#### Scenario: Last inactive medicine activated
- **WHEN** the inactive section holds one medicine and the user activates it
- **THEN** the "Inactive" header disappears and the medicine appears in the active section

#### Scenario: Last active medicine deactivated
- **WHEN** the active section holds one medicine, the inactive section holds none, and the user deactivates it
- **THEN** the "Active" header shows the no-active-medicines message, followed by the "Inactive" header and the tile

#### Scenario: Tile closes on action
- **WHEN** the user taps the revealed button
- **THEN** the tile is closed before it appears in its new section

#### Scenario: Write fails
- **WHEN** the repository write fails
- **THEN** a snackbar "Could not update medicine" is shown and the tile remains in its original section

### Requirement: Activation action is reachable without a gesture
Every medicine tile SHALL expose an accessibility custom action labelled "Deactivate" (active tile) or "Activate" (inactive tile), using the same string resources as the button, whose effect is identical to tapping the revealed button. The tile's merged spoken description MUST be unchanged by this action.

#### Scenario: TalkBack custom action on an active tile
- **WHEN** a screen reader focuses an active tile for "Ibuprofen" and the user opens the actions menu
- **THEN** it lists "Deactivate", and choosing it moves "Ibuprofen" to the inactive section

#### Scenario: TalkBack custom action on an inactive tile
- **WHEN** a screen reader focuses an inactive tile and the user opens the actions menu
- **THEN** it lists "Activate", and choosing it moves the medicine to the active section

#### Scenario: Spoken description unchanged
- **WHEN** a screen reader focuses an inactive tile for "Ibuprofen" taken twice a day
- **THEN** it still announces the name, the description and "Inactive" as one item

### Requirement: Action button is usable at large fonts and one-handed
The revealed button SHALL be at least 48 dp tall and 96 dp wide, SHALL fill the tile's height, SHALL show a text label and an icon, and MUST NOT rely on colour alone to convey its meaning. At the largest system font scale the label SHALL wrap or the action area SHALL widen so no text is clipped.

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum and the user reveals a tile
- **THEN** the full button label is visible and the tile's own text is not clipped

#### Scenario: Touch target
- **WHEN** the button is revealed
- **THEN** its touch target is at least 48 dp in both dimensions

## MODIFIED Requirements

### Requirement: Medicines are provided through a domain contract
The Medicines screen SHALL obtain medicines only through the `MedicationRepository` interface, SHALL change a medicine's active flag only through that interface, and SHALL compute partitioning and ordering in its view model. The view model MUST expose a schedule summary, not formatted text, so wording is resolved in the UI layer. The view model MUST NOT update its own state ahead of the repository stream when the active flag changes.

#### Scenario: Placeholder repository
- **WHEN** no persistence exists
- **THEN** the app is wired with an in-memory repository that starts empty and the screen shows the empty state

#### Scenario: View model partitions and orders
- **WHEN** the repository emits an unordered mix of active and inactive medicines
- **THEN** the view model state contains an active list and an inactive list, each ordered by name

#### Scenario: Active flag changed through the repository
- **WHEN** the view model receives a set-active event for a medicine
- **THEN** it calls the repository's set-active operation with that identifier and flag and does not modify its state until the repository stream emits

#### Scenario: Repository failure surfaces as an effect
- **WHEN** the repository's set-active operation throws
- **THEN** the view model emits a one-shot update-failed effect and its state is unchanged
