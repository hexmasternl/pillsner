# medicine-add Specification

## Purpose
TBD - created by archiving change app-medicine-add. Update Purpose after archive.
## Requirements
### Requirement: Add medicine form fields
The Add medicine screen SHALL present fields for name, default dose (amount and unit), used since, use until, prescribed by, and a schedules section. All labels, hints and error messages MUST come from string resources.

#### Scenario: Form opens from the overview
- **WHEN** the user taps the add button on the Medicines screen
- **THEN** the Add medicine form is shown with a title "Add medicine", a back affordance and a Save action, and every field listed above is present

#### Scenario: Default values
- **WHEN** the form opens
- **THEN** name is empty, default dose amount is empty with a unit preselected, used since is today's date, use until is empty, prescribed by is "General practitioner", and the schedules list is empty

### Requirement: Name is required
The form SHALL require a non-blank name.

#### Scenario: Blank name on save
- **WHEN** the user taps Save with an empty or whitespace-only name
- **THEN** the medicine is not saved and the name field shows an error stating that a name is required

### Requirement: Default dose is a positive quantity with a unit
The form SHALL accept a default dose as a decimal number greater than zero, parsed using the device locale, together with a unit from a fixed list (mg, g, mcg, ml, tablet, capsule, drop, puff, unit).

#### Scenario: Valid decimal dose
- **WHEN** the user enters "2.5" (or the locale equivalent) and selects "ml"
- **THEN** the default dose is accepted as 2.5 ml

#### Scenario: Zero or negative dose
- **WHEN** the user enters "0" and taps Save
- **THEN** the medicine is not saved and the dose field shows an error stating the amount must be greater than zero

#### Scenario: Non-numeric dose
- **WHEN** the user enters "abc" in the amount field
- **THEN** the dose field shows an error and Save does not proceed

### Requirement: Used since and use until dates
The form SHALL let the user pick "used since" (required, default today) and "use until" (optional) with date pickers. "Use until" MUST NOT be before "used since". The user MUST be able to clear "use until".

#### Scenario: Pick a past start date
- **WHEN** the user opens the used since picker and selects a date last week
- **THEN** the field shows that date formatted for the device locale

#### Scenario: End before start is rejected
- **WHEN** used since is 10 September and the user attempts to set use until to 5 September
- **THEN** the date is not accepted and the use until field shows an error stating it must be on or after used since

#### Scenario: Clear use until
- **WHEN** use until holds a date and the user taps its clear affordance
- **THEN** use until is empty again and the medicine is treated as open-ended

### Requirement: Prescribed by
The form SHALL let the user choose who prescribed the medicine from: General practitioner, Specialist, Pharmacist, Self, Other.

#### Scenario: Choose a prescriber
- **WHEN** the user opens the prescribed by field and selects "Specialist"
- **THEN** the field shows "Specialist" and the saved medicine records Specialist

### Requirement: Schedules section
The form SHALL show the draft's schedules as a list, each with its human-readable description including the amount, and an "Add schedule" button that opens the schedule editor. Tapping a schedule SHALL open it in the editor for editing. Each schedule SHALL have a remove affordance. When there are no schedules the section SHALL state that the medicine will be taken as needed.

#### Scenario: No schedules yet
- **WHEN** the form has no schedules
- **THEN** the section shows the as-needed explanation and the "Add schedule" button

#### Scenario: Schedule added from the editor
- **WHEN** the user completes the schedule editor with 40 mg every 12 hours from 08:00
- **THEN** the form shows one schedule row reading "40 mg every 12 hours"

#### Scenario: Edit an existing schedule
- **WHEN** the user taps an existing schedule row and changes its amount to 20 mg in the editor and taps Done
- **THEN** the same row now reads with 20 mg and no additional row was added

#### Scenario: Remove a schedule
- **WHEN** the user taps the remove affordance on a schedule row
- **THEN** that schedule is removed from the draft and the remaining rows keep their order

#### Scenario: Multiple schedules
- **WHEN** the draft holds two schedules
- **THEN** both rows are shown in the order they were added

### Requirement: Saving a medicine
Tapping Save with a valid form SHALL persist the medicine as active with its schedules, then return to the Medicines screen where it appears in the active section. Saving MUST be a single atomic write so a medicine is never stored without its schedules.

#### Scenario: Save with schedules
- **WHEN** the user enters "Metoprolol", 40 mg, used since today, prescribed by General practitioner, one schedule of 40 mg every 12 hours from 08:00, and taps Save
- **THEN** the Medicines screen is shown and lists "Metoprolol" under Active with the description "40 mg every 12 hours"

#### Scenario: Save without schedules
- **WHEN** the user enters a valid name and dose with no schedules and taps Save
- **THEN** the medicine is saved and listed under Active with the description "<default dose> as needed"

#### Scenario: Save fails
- **WHEN** persisting the medicine fails
- **THEN** the user stays on the form, a message states the medicine could not be saved, and no medication name or dose is written to the log

### Requirement: Discarding a draft
Leaving the form with unsaved edits SHALL ask the user to confirm discarding. Leaving an untouched form SHALL not ask.

#### Scenario: Back on an untouched form
- **WHEN** the user presses back without changing any field
- **THEN** the Medicines screen is shown immediately

#### Scenario: Back with edits, keep editing
- **WHEN** the user has typed a name and presses back, then chooses "Keep editing"
- **THEN** the form remains with the typed name intact

#### Scenario: Back with edits, discard
- **WHEN** the user has typed a name and presses back, then chooses "Discard"
- **THEN** the Medicines screen is shown and no medicine was saved

### Requirement: Draft survives configuration changes
The draft, including its schedules, SHALL survive rotation and process death while the add flow is on the back stack.

#### Scenario: Rotate with a schedule in the draft
- **WHEN** the user has added one schedule and rotates the device
- **THEN** the form still shows the entered fields and the schedule row

### Requirement: Form accessibility
Every field SHALL have a spoken label, every error SHALL be announced when it appears, and the form SHALL scroll fully at the largest font scale with no clipped text.

#### Scenario: Screen reader on a field with an error
- **WHEN** the name field shows the required error and a screen reader focuses it
- **THEN** the label and the error text are both announced

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum and the draft has three schedules
- **THEN** every field, schedule row and the Save action are reachable and fully visible by scrolling

