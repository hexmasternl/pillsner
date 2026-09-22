## ADDED Requirements

### Requirement: Critical tiles carry a badge
A tile for a medicine flagged critical SHALL show a badge with an icon and the text "Critical" from a string resource, next to the medicine name. The distinction MUST NOT rely on colour alone, and MUST be announced by a screen reader together with the rest of the tile's content.

#### Scenario: Critical medicine tile
- **WHEN** a medicine "Amiodarone" is flagged critical
- **THEN** its tile shows "Amiodarone" together with a critical badge carrying both an icon and the text "Critical"

#### Scenario: Non-critical medicine tile
- **WHEN** a medicine is not flagged critical
- **THEN** its tile shows no critical badge

#### Scenario: Screen reader includes the badge
- **WHEN** a screen reader focuses a critical tile
- **THEN** the announcement includes the medicine name, its schedule description, and that it is critical, as one item
