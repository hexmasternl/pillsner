## MODIFIED Requirements

### Requirement: Medicine tile content
Each medicine SHALL be shown as a distinct tile that displays the medicine name and one human-readable description per schedule, each including that schedule's amount, in the medicine's schedule order. A medicine with no schedules SHALL show a single "as needed" description with its default dose. Descriptions MUST be derived from the schedules and MUST come from string resources.

#### Scenario: Twice-daily medicine
- **WHEN** a medicine "Ibuprofen" has one schedule of 400 mg at two times every day
- **THEN** its tile shows "Ibuprofen" and "400 mg twice a day"

#### Scenario: Every-other-day medicine
- **WHEN** a medicine has one schedule of 1 tablet at one time every two days
- **THEN** its tile shows the description "1 tablet once every other day"

#### Scenario: Multiple schedules
- **WHEN** a medicine "Metoprolol" has a schedule of 40 mg every 12 hours and a schedule of 20 mg once a day on Sat, Sun
- **THEN** its tile shows "Metoprolol", then "40 mg every 12 hours", then "20 mg once a day on Sat, Sun" on separate lines in that order

#### Scenario: As-needed medicine
- **WHEN** a medicine with default dose 500 mg has no schedules
- **THEN** its tile shows the description "500 mg as needed"

#### Scenario: Tile is read as one item by a screen reader
- **WHEN** a screen reader focuses an active tile for "Metoprolol" with two schedules
- **THEN** it announces the name and both descriptions together as a single item
