## ADDED Requirements

### Requirement: Legal document destination
The navigation host SHALL include a legal document destination that carries which document to show as a route argument, so that process death restores the same document. It is not a top-level destination: the bottom navigation bar and the navigation rail MUST be hidden while it is shown. It SHALL be reachable from the Legal section on Settings and from the legal acceptance destination. Back SHALL return to whichever destination opened it.

#### Scenario: Opened from Settings
- **WHEN** the user taps the Disclaimer row in the Legal section on Settings
- **THEN** the legal document destination is shown with the Disclaimer and the bottom navigation bar is not visible

#### Scenario: Back returns to Settings
- **WHEN** the user presses back on a legal document opened from Settings
- **THEN** the Settings destination is shown with the Settings item selected and the bottom navigation bar visible again

#### Scenario: Opened from the acceptance destination
- **WHEN** the user opens the Terms of Service from the legal acceptance destination and presses back
- **THEN** the legal acceptance destination is shown again, unchanged

#### Scenario: Document survives process death
- **WHEN** the process is killed and restored while the Terms of Service are open
- **THEN** the destination is restored showing the Terms of Service

### Requirement: Legal acceptance destination
The navigation host SHALL include a legal acceptance destination, reached from the Medicines screen's add button when the current legal documents are not accepted. It is not a top-level destination: the bottom navigation bar and the navigation rail MUST be hidden while it is shown. Accepting SHALL navigate to the medicine form flow in add mode and SHALL remove the acceptance destination from the back stack, so back from the form returns to Medicines. Back from the acceptance destination itself SHALL return to Medicines.

#### Scenario: Reached from the add button
- **WHEN** the user taps the add button on the Medicines screen while the documents are not accepted
- **THEN** the legal acceptance destination is shown and the bottom navigation bar is not visible

#### Scenario: Accepting enters the form flow
- **WHEN** the user accepts on the acceptance destination
- **THEN** the medicine form flow is shown in add mode

#### Scenario: Back from the form skips the gate
- **WHEN** the user accepted, reached the untouched form, and presses back
- **THEN** the Medicines destination is shown with the Medicines item selected, and the acceptance destination is not shown

#### Scenario: Back from the gate
- **WHEN** the user presses back on the acceptance destination without accepting
- **THEN** the Medicines destination is shown with the Medicines item selected and the bottom navigation bar visible again

### Requirement: Legal section on Settings
The Settings screen SHALL show a Legal section after the Security section. The section SHALL contain a row for the Disclaimer, a row for the Terms of Service, and one line stating the state of acceptance. Each row SHALL open the legal document destination for that document. The section header SHALL be exposed as a heading and every row SHALL meet the minimum touch target size.

#### Scenario: Section order on Settings
- **WHEN** the Settings destination is shown
- **THEN** its sections read Language, then Security, then Legal

#### Scenario: Rows open their documents
- **WHEN** the user taps the Terms of Service row
- **THEN** the legal document destination is shown with the Terms of Service

#### Scenario: Screen reader on the section
- **WHEN** a screen reader moves by heading through Settings
- **THEN** the "Legal" header is reached as a heading and each row announces its label and that it is a button

#### Scenario: Sections scroll at large font
- **WHEN** the system font scale is at maximum
- **THEN** the Legal section is reachable by scrolling and none of its text is clipped
