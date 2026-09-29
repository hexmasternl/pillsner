## ADDED Requirements

### Requirement: Scan a label entry point
In add mode the form SHALL show a "Scan a label" action above the name field, and after a scan has been applied it SHALL show the review banner described in `medicine-label-scan`. The action and the banner MUST NOT change any other field's default value, validation or save behaviour, and the form MUST remain fully usable without them.

#### Scenario: Action present in add mode
- **WHEN** the Add medicine form opens in add mode
- **THEN** a "Scan a label" button is shown above the name field, and every other field shows its usual default

#### Scenario: Action absent in edit mode
- **WHEN** the form opens on an existing medicine
- **THEN** no "Scan a label" button is shown

#### Scenario: Manual entry unaffected
- **WHEN** the user never taps "Scan a label" and fills the form by hand
- **THEN** validation and Save behave exactly as before this change
