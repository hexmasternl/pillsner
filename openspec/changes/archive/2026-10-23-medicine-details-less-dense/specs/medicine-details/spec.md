## MODIFIED Requirements

### Requirement: Medicine details screen opens pre-populated
Tapping a medicine tile on the Medicines screen SHALL open the Medicine details screen for that medicine. The screen SHALL be the medicine form in edit mode: it SHALL present the same fields as the Add medicine form (name, default dose, used since, use until, prescribed by, schedules), each pre-populated with the medicine's saved values, and its schedules section SHALL list the saved schedules in their saved order with the same descriptions the tile shows. The screen SHALL lay its content out in this order, top to bottom: the primary details (name and default dose), always visible; the secondary details (used since, use until, prescribed by and the Active switch) inside a panel that is collapsed by default behind a toggle button; the Schedules section, always visible; and the Stock section, always visible. The Add medicine form MUST NOT collapse any field. While the medicine is being loaded the screen SHALL show a loading indicator and MUST NOT show editable fields with default values.

#### Scenario: Open a medicine with schedules
- **WHEN** the user taps the tile of "Metoprolol", default dose 40 mg, used since 1 March, no end date, prescribed by Specialist, with schedules "40 mg every 12 hours" and "20 mg once a day on Sat, Sun"
- **THEN** the details screen shows name "Metoprolol" and dose 40 mg above a collapsed "More details" toggle, then two schedule rows reading "40 mg every 12 hours" then "20 mg once a day on Sat, Sun", and after tapping the toggle it shows used since 1 March, use until empty and prescribed by "Specialist"

#### Scenario: Order of the blocks
- **WHEN** the details screen is shown for a saved medicine
- **THEN** reading top to bottom the screen presents the name and default dose, then the secondary details toggle, then the Schedules section, then the Stock section, with the secondary fields hidden

#### Scenario: Open an as-needed medicine
- **WHEN** the user taps the tile of a medicine with no schedules
- **THEN** the details screen shows its primary fields filled and the schedules section states that the medicine is taken as needed

#### Scenario: Open an inactive medicine
- **WHEN** the user taps a tile in the inactive section and expands the secondary details
- **THEN** the details screen shows that medicine's values and its active switch off

#### Scenario: Add form keeps every field visible
- **WHEN** the form is opened from the add button
- **THEN** name, default dose, used since, use until and prescribed by are all visible without any toggle

#### Scenario: Loading state
- **WHEN** the details screen is opening and the medicine has not been read from the repository yet
- **THEN** a loading indicator is shown and no field is editable

### Requirement: Details screen accessibility
Every field, the active switch, the secondary details toggle, the overflow action and every schedule row on the details screen SHALL have a spoken label and state, every validation error SHALL be announced when it appears, and the screen SHALL scroll fully at the largest system font scale with no clipped text, whether the secondary details are collapsed or expanded. The overflow action and the secondary details toggle SHALL each be at least the minimum touch target in both dimensions. The overflow action SHALL announce a description of its own, and the toggle SHALL announce its label and whether the panel is expanded or collapsed. After toggling, the fields that became visible SHALL follow the toggle in screen-reader traversal order.

#### Scenario: Screen reader on the active switch
- **WHEN** a screen reader focuses the active switch of an active medicine
- **THEN** it announces the label "Active" and that the switch is on

#### Scenario: Screen reader on the overflow action
- **WHEN** a screen reader focuses the overflow action
- **THEN** it announces a description such as "More options" and that it is a button

#### Scenario: Screen reader on the secondary details toggle
- **WHEN** a screen reader focuses the toggle while the panel is collapsed
- **THEN** it announces "More details", that it is a button, and that it is collapsed; after activation it announces "Less details" and expanded, and the next elements in traversal are the used since, use until and prescribed by fields and the active switch

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum and the opened medicine has three schedules
- **THEN** every field, the switch, every schedule row and the Save action are reachable and fully visible by scrolling, both with the secondary details collapsed and after expanding them

## ADDED Requirements

### Requirement: Secondary details toggle
In edit mode the form SHALL show, directly below the default dose field, a toggle button labelled "More details" from a string resource while the secondary details panel is collapsed and "Less details" from a string resource while it is expanded. The panel SHALL hold, in this order, the used since field, the use until field, the prescribed by field and the Active switch, each behaving exactly as it does on the Add medicine form. Each fresh open of the details screen SHALL start with the panel collapsed. Tapping the toggle SHALL expand a collapsed panel or collapse an expanded one, with the medium motion duration and honouring the system reduce-motion setting. Toggling MUST NOT count as an edit: it MUST NOT change the saved medicine, MUST NOT trigger the discard confirmation and MUST NOT alter any field value. The panel's expanded or collapsed state SHALL survive rotation and process death while the details screen is on the back stack. When the user taps Save and a field inside the panel has a validation error, the panel SHALL be expanded so the error is visible. The toggle MUST NOT be shown in add mode.

#### Scenario: Collapsed by default
- **WHEN** the user opens a medicine from the Medicines screen
- **THEN** the toggle reads "More details" and the used since, use until, prescribed by fields and the active switch are not shown

#### Scenario: Expand
- **WHEN** the user taps "More details"
- **THEN** the panel expands to show used since, use until, prescribed by and the active switch with the medicine's saved values, and the toggle reads "Less details"

#### Scenario: Collapse
- **WHEN** the panel is expanded and the user taps "Less details"
- **THEN** the panel collapses, the four fields are hidden again and the toggle reads "More details"

#### Scenario: Toggling is not an edit
- **WHEN** the user opens a medicine, taps "More details", taps "Less details" and presses back
- **THEN** the Medicines screen is shown immediately without a discard confirmation and the medicine is unchanged

#### Scenario: Edit inside the panel then collapse
- **WHEN** the user expands the panel, changes the prescriber, collapses the panel and taps Save
- **THEN** the medicine is saved with the new prescriber

#### Scenario: Hidden error opens the panel
- **WHEN** the user expands the panel, sets use until to a date before used since, collapses the panel and taps Save
- **THEN** the medicine is not saved, the panel expands and the use until field shows its error

#### Scenario: State survives rotation
- **WHEN** the user taps "More details" and rotates the device
- **THEN** the panel is still expanded and the toggle still reads "Less details"

#### Scenario: State survives process death
- **WHEN** the process is killed and restored while the details screen has the panel expanded
- **THEN** the panel is restored expanded

#### Scenario: Fresh open starts collapsed
- **WHEN** the user expands the panel, presses back to the Medicines screen and opens the same medicine again
- **THEN** the panel is collapsed

#### Scenario: Not shown when adding
- **WHEN** the form is opened from the add button
- **THEN** no "More details" toggle is present
