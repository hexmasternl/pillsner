## MODIFIED Requirements

### Requirement: Medicine details screen opens pre-populated
Tapping a medicine tile on the Medicines screen SHALL open the Medicine details screen for that medicine. The screen SHALL be the medicine form in edit mode: it SHALL present the same fields as the Add medicine form (name, default dose, used since, use until, prescribed by, schedules, critical toggle), each pre-populated with the medicine's saved values, and its schedules section SHALL list the saved schedules in their saved order with the same descriptions the tile shows. While the medicine is being loaded the screen SHALL show a loading indicator and MUST NOT show editable fields with default values.

#### Scenario: Open a medicine with schedules
- **WHEN** the user taps the tile of "Metoprolol", default dose 40 mg, used since 1 March, no end date, prescribed by Specialist, with schedules "40 mg every 12 hours" and "20 mg once a day on Sat, Sun"
- **THEN** the details screen shows name "Metoprolol", dose 40 mg, used since 1 March, use until empty, prescribed by "Specialist", and two schedule rows reading "40 mg every 12 hours" then "20 mg once a day on Sat, Sun"

#### Scenario: Open an as-needed medicine
- **WHEN** the user taps the tile of a medicine with no schedules
- **THEN** the details screen shows its fields filled and the schedules section states that the medicine is taken as needed

#### Scenario: Open an inactive medicine
- **WHEN** the user taps a tile in the inactive section
- **THEN** the details screen opens with that medicine's values and its active switch off

#### Scenario: Loading state
- **WHEN** the details screen is opening and the medicine has not been read from the repository yet
- **THEN** a loading indicator is shown and no field is editable

#### Scenario: Open a critical medicine
- **WHEN** the user taps the tile of a medicine flagged critical
- **THEN** the details screen opens with the critical toggle on

## ADDED Requirements

### Requirement: Critical toggle is editable like any other field
The details screen SHALL let the user turn the critical toggle on or off, and saving SHALL persist the new value the same way any other field change is persisted.

#### Scenario: Flag a medicine critical from the details screen
- **WHEN** the user turns on the critical toggle for a previously non-critical medicine and saves
- **THEN** the medicine is updated with its critical flag set to true

#### Scenario: Unflag a critical medicine
- **WHEN** the user turns off the critical toggle for a critical medicine and saves
- **THEN** the medicine is updated with its critical flag set to false, and its future reminders use the standard cadence
