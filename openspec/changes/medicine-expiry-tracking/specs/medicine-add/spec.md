## MODIFIED Requirements

### Requirement: Add medicine form fields
The Add medicine screen SHALL present fields for name, default dose (amount and unit), used since, use until, expiry date, prescribed by, and a schedules section. All labels, hints and error messages MUST come from string resources. The form SHALL be reachable in add mode only once the current legal documents are accepted; when they are not, the add button SHALL lead to the legal acceptance screen first, and the form SHALL open only after the user has accepted. Opening the form in edit mode for an existing medicine is never gated.

#### Scenario: Form opens from the overview
- **WHEN** the user taps the add button on the Medicines screen while the current legal documents are accepted
- **THEN** the Add medicine form is shown with a title "Add medicine", a back affordance and a Save action, and every field listed above is present

#### Scenario: Form opens after acceptance
- **WHEN** the user taps the add button while the current legal documents are not accepted, and then accepts on the legal acceptance screen
- **THEN** the Add medicine form is shown with a title "Add medicine", a back affordance and a Save action, and every field listed above is present

#### Scenario: Form does not open without acceptance
- **WHEN** the user taps the add button while the current legal documents are not accepted, and leaves the legal acceptance screen without accepting
- **THEN** the Medicines screen is shown and the Add medicine form was never presented

#### Scenario: Default values
- **WHEN** the form opens
- **THEN** name is empty, default dose amount is empty with a unit preselected, used since is today's date, use until is empty, expiry date is empty, prescribed by is "General practitioner", and the schedules list is empty

### Requirement: Expiry date
The form SHALL let the user optionally pick an expiry date with a date picker, independent of "used since" and "use until". When set, the expiry date MUST NOT be before "used since". The user MUST be able to clear the expiry date.

#### Scenario: Set an expiry date
- **WHEN** the user opens the expiry date picker and selects a date six months from now
- **THEN** the field shows that date formatted for the device locale

#### Scenario: Expiry before used since is rejected
- **WHEN** used since is 1 March and the user attempts to set the expiry date to 1 February
- **THEN** the date is not accepted and the expiry date field shows an error stating it must be on or after used since

#### Scenario: Clear expiry date
- **WHEN** the expiry date holds a value and the user taps its clear affordance
- **THEN** the expiry date is empty again and the medicine is treated as having no known expiry

#### Scenario: Expiry date is independent of use until
- **WHEN** the user sets use until to a date but leaves the expiry date empty, or the reverse
- **THEN** both fields are accepted and saved independently, with no requirement that one imply the other

### Requirement: Saving a medicine
Tapping Save with a valid form SHALL persist the medicine as active with its schedules and expiry date, then return to the Medicines screen where it appears in the active section. Saving MUST be a single atomic write so a medicine is never stored without its schedules.

#### Scenario: Save with schedules
- **WHEN** the user enters "Metoprolol", 40 mg, used since today, prescribed by General practitioner, one schedule of 40 mg every 12 hours from 08:00, and taps Save
- **THEN** the Medicines screen is shown and lists "Metoprolol" under Active with the description "40 mg every 12 hours"

#### Scenario: Save without schedules
- **WHEN** the user enters a valid name and dose with no schedules and taps Save
- **THEN** the medicine is saved and listed under Active with the description "<default dose> as needed"

#### Scenario: Save with an expiry date
- **WHEN** the user enters a valid name and dose, sets an expiry date, and taps Save
- **THEN** the medicine is saved with that expiry date and reopening it in the details screen shows the same date

#### Scenario: Save fails
- **WHEN** persisting the medicine fails
- **THEN** the user stays on the form, a message states the medicine could not be saved, and no medication name or dose is written to the log
