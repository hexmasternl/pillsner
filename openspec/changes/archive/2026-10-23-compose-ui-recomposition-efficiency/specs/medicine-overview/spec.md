## MODIFIED Requirements

### Requirement: Only one tile is revealed at a time
The Medicines screen SHALL keep at most one tile revealed. Revealing a tile SHALL close any other revealed tile. The revealed state SHALL survive a configuration change and SHALL be cleared when the revealed tile leaves the list or its action is tapped. Revealing or closing a tile SHALL NOT change the schedule description shown on any other tile.

#### Scenario: Revealing a second tile
- **WHEN** one tile is revealed and the user reveals another
- **THEN** the first tile animates closed and only the second is revealed

#### Scenario: Rotation while revealed
- **WHEN** a tile is revealed and the device rotates
- **THEN** the same tile is still revealed afterwards

#### Scenario: Revealing a tile leaves other tiles' content unchanged
- **WHEN** the active section shows several tiles with their schedule descriptions and the user reveals one tile's swipe action
- **THEN** every other tile's schedule description on screen stays exactly as it was
