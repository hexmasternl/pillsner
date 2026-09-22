## ADDED Requirements

### Requirement: Medicine tile expiry heads-up
A medicine tile SHALL show a distinct expiry heads-up indicator when its expiry date is approaching (within 30 days) or has passed, using wording that names expiry explicitly so it is never confused with any other tile state. A tile for a medicine with no expiry date, or whose expiry date is more than 30 days away, SHALL show no expiry indicator. The distinction between "approaching" and "past" MUST NOT rely on colour alone, and a screen reader focusing the tile MUST announce the expiry state as part of the tile's single combined announcement.

#### Scenario: Expiry approaching
- **WHEN** a medicine's expiry date is 10 days from now
- **THEN** its tile shows an indicator stating the medicine expires soon

#### Scenario: Expiry passed
- **WHEN** a medicine's expiry date is in the past
- **THEN** its tile shows an indicator stating the medicine has expired

#### Scenario: No expiry date
- **WHEN** a medicine has no expiry date set
- **THEN** its tile shows no expiry indicator

#### Scenario: Expiry far in the future
- **WHEN** a medicine's expiry date is 90 days from now
- **THEN** its tile shows no expiry indicator

#### Scenario: Indicator applies to inactive tiles too
- **WHEN** an inactive medicine's expiry date has passed
- **THEN** its tile shows both the inactive distinction and the expiry indicator

#### Scenario: Screen reader announces expiry state
- **WHEN** a screen reader focuses a tile whose medicine has expired
- **THEN** the announcement includes the medicine name, its schedule description(s), and that it has expired, as a single combined item
