## MODIFIED Requirements

### Requirement: Add medicine destination
The navigation host SHALL include a nested add-medicine flow reachable from the Medicines screen, whose start destination is the Add medicine form. The bottom navigation bar MUST be hidden throughout the flow. Back from the form SHALL return to Medicines, subject to the form's discard confirmation when it has unsaved edits. Saving SHALL return to Medicines and remove the whole flow from the back stack.

#### Scenario: Reached from the overview
- **WHEN** the user taps the add button on the Medicines screen
- **THEN** the Add medicine form is shown and the bottom navigation bar is not visible

#### Scenario: Back returns to Medicines
- **WHEN** the user is on an untouched Add medicine form and presses back or the back affordance
- **THEN** the Medicines destination is shown with the Medicines item selected and the bottom navigation bar visible again

#### Scenario: Save returns to Medicines
- **WHEN** the user saves a valid medicine
- **THEN** the Medicines destination is shown, and pressing back afterwards goes to Home rather than back into the add flow

## ADDED Requirements

### Requirement: Schedule editor destination
The add-medicine flow SHALL include a nested Schedule editor destination reached from the form's "Add schedule" button or from tapping an existing schedule row. It SHALL share the form's draft so that the schedule list persists across the two screens. The bottom navigation bar MUST remain hidden. Back and Done SHALL both return to the form.

#### Scenario: Open the editor
- **WHEN** the user taps "Add schedule" on the form
- **THEN** the Schedule editor is shown and the bottom navigation bar is not visible

#### Scenario: Done returns to the form with the draft intact
- **WHEN** the user completes a schedule and taps Done
- **THEN** the form is shown with the fields entered earlier still present and the new schedule listed

#### Scenario: Back returns to the form
- **WHEN** the user presses back in the editor
- **THEN** the form is shown with its draft unchanged

#### Scenario: Rotation inside the editor
- **WHEN** the device rotates while the editor is open
- **THEN** the editor is still shown with its entered values and the form's draft is preserved behind it
