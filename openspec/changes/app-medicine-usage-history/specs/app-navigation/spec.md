## ADDED Requirements

### Requirement: Usage history destination
The medicine form flow SHALL include a nested Usage history destination, reached only from the overflow menu of the medicine form in edit mode, carrying the medicine's identifier as a route argument. The bottom navigation bar MUST remain hidden on it. The destination SHALL take its own view model rather than the form's shared draft, so nothing it does can touch the draft. Back SHALL return to the form with its draft, including unsaved edits, exactly as it was. Leaving the whole flow SHALL remove the Usage history destination from the back stack with it. The identifier MUST survive configuration changes and process death as part of the route.

#### Scenario: Open from the details screen
- **WHEN** the user taps the overflow action on the medicine form in edit mode and chooses "Usage history"
- **THEN** the Usage history destination is shown for that medicine and the bottom navigation bar is not visible

#### Scenario: Back returns to the form with the draft intact
- **WHEN** the user has changed the dose on the form, opens the Usage history and presses back
- **THEN** the form is shown with the changed dose still present, and no discard confirmation was asked

#### Scenario: Not reachable while adding
- **WHEN** the form is open in add mode
- **THEN** there is no way to reach the Usage history destination

#### Scenario: Saving clears the whole flow
- **WHEN** the user returns from the Usage history to the form and saves
- **THEN** the Medicines destination is shown, and pressing back afterwards goes to Home rather than back into the flow or the Usage history

#### Scenario: Identifier survives process death
- **WHEN** the process is killed and restored while the Usage history destination is open
- **THEN** the Usage history is restored for the same medicine

#### Scenario: Rotation on the Usage history
- **WHEN** the device rotates while the Usage history is open
- **THEN** the same medicine's history is shown with the same period selected
