## MODIFIED Requirements

### Requirement: Add medicine form fields
The Add medicine screen SHALL present fields for name, default dose (amount and unit), used since, use until, prescribed by, and a schedules section. All labels, hints and error messages MUST come from string resources. The form SHALL be reachable in add mode only once the current legal documents are accepted; when they are not, the add button SHALL lead to the legal acceptance screen first, and the form SHALL open only after the user has accepted. Opening the form in edit mode for an existing medicine is never gated.

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
- **THEN** name is empty, default dose amount is empty with a unit preselected, used since is today's date, use until is empty, prescribed by is "General practitioner", and the schedules list is empty
