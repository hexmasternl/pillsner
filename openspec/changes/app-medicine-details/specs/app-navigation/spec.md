## MODIFIED Requirements

### Requirement: Add medicine destination
The navigation host SHALL include a nested medicine form flow whose start destination is the medicine form, reachable from the Medicines screen in two ways: the add button opens it in add mode with no medicine identifier, and tapping a medicine tile opens it in edit mode carrying that medicine's identifier as a route argument. The bottom navigation bar MUST be hidden throughout the flow. Back from the form SHALL return to Medicines, subject to the form's discard confirmation when it has unsaved edits. Saving SHALL return to Medicines and remove the whole flow from the back stack. The identifier MUST survive configuration changes and process death as part of the route.

#### Scenario: Reached from the add button
- **WHEN** the user taps the add button on the Medicines screen
- **THEN** the form is shown in add mode titled "Add medicine" and the bottom navigation bar is not visible

#### Scenario: Reached from a tile
- **WHEN** the user taps a medicine tile on the Medicines screen
- **THEN** the form is shown in edit mode titled "Medicine details" for that medicine and the bottom navigation bar is not visible

#### Scenario: Back returns to Medicines
- **WHEN** the user is on an untouched form, in either mode, and presses back or the back affordance
- **THEN** the Medicines destination is shown with the Medicines item selected and the bottom navigation bar visible again

#### Scenario: Save returns to Medicines
- **WHEN** the user saves a valid medicine, in either mode
- **THEN** the Medicines destination is shown, and pressing back afterwards goes to Home rather than back into the flow

#### Scenario: Identifier survives process death
- **WHEN** the process is killed and restored while the form is open in edit mode
- **THEN** the form is restored in edit mode for the same medicine

### Requirement: Schedule editor destination
The medicine form flow SHALL include a nested Schedule editor destination reached from the form's "Add schedule" button or from tapping an existing schedule row, in both add and edit mode. It SHALL share the form's draft so that the schedule list persists across the two screens. The bottom navigation bar MUST remain hidden. Back and Done SHALL both return to the form.

#### Scenario: Open the editor
- **WHEN** the user taps "Add schedule" on the form
- **THEN** the Schedule editor is shown and the bottom navigation bar is not visible

#### Scenario: Open the editor for a saved schedule
- **WHEN** the form is in edit mode and the user taps a schedule row that was loaded from the saved medicine
- **THEN** the Schedule editor is shown with that schedule's amount, pattern and times

#### Scenario: Done returns to the form with the draft intact
- **WHEN** the user completes a schedule and taps Done
- **THEN** the form is shown with the fields entered earlier still present and the new schedule listed

#### Scenario: Back returns to the form
- **WHEN** the user presses back in the editor
- **THEN** the form is shown with its draft unchanged

#### Scenario: Rotation inside the editor
- **WHEN** the device rotates while the editor is open
- **THEN** the editor is still shown with its entered values and the form's draft is preserved behind it
