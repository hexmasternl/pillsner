# medicine-details Specification

## Purpose
TBD - created by archiving change app-medicine-details. Update Purpose after archive.
## Requirements
### Requirement: Medicine details screen opens pre-populated
Tapping a medicine tile on the Medicines screen SHALL open the Medicine details screen for that medicine. The screen SHALL be the medicine form in edit mode: it SHALL present the same fields as the Add medicine form (name, default dose, used since, use until, prescribed by, schedules), each pre-populated with the medicine's saved values, and its schedules section SHALL list the saved schedules in their saved order with the same descriptions the tile shows. While the medicine is being loaded the screen SHALL show a loading indicator and MUST NOT show editable fields with default values.

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

### Requirement: Details screen title and actions
The details screen SHALL be titled "Medicine details" from a string resource in its top bar, SHALL have a back affordance, SHALL present Save as the single primary button pinned to the bottom of the form exactly as the Add medicine form does, and SHALL carry one overflow action in its top bar holding a menu whose only item, "Usage history" from a string resource, opens that medicine's usage history. The overflow action MUST NOT be present when the form is in add mode. The screen MUST NOT offer any delete, remove or archive action anywhere on the screen, in its top bar, in the overflow menu or in its dialogs.

#### Scenario: Title and actions
- **WHEN** the details screen is shown
- **THEN** the top bar reads "Medicine details" with a back affordance and an overflow action, and a Save button is pinned at the bottom of the form

#### Scenario: The overflow menu holds one item
- **WHEN** the user taps the overflow action on the details screen
- **THEN** a menu opens containing exactly one item, "Usage history"

#### Scenario: No overflow in add mode
- **WHEN** the form is opened from the add button
- **THEN** the top bar has no overflow action

#### Scenario: No removal action
- **WHEN** every action, menu and dialog reachable from the details screen is inspected, the overflow menu included
- **THEN** none of them deletes, removes or archives the medicine

### Requirement: Details screen applies the add form's rules
The details screen SHALL apply every field rule, validation rule and schedule-editing rule of the Add medicine form unchanged: name required, default dose positive with a unit, use until not before used since, prescriber from the fixed list, and schedules added, edited and removed through the schedule editor. Error presentation and string resources SHALL be shared with the add form.

#### Scenario: Clear the name and save
- **WHEN** the user clears the name field on the details screen and taps Save
- **THEN** the medicine is not updated and the name field shows the same required error as the add form

#### Scenario: Edit a saved schedule
- **WHEN** the user taps the "40 mg every 12 hours" row, changes the amount to 20 mg in the schedule editor and taps Done
- **THEN** the row reads "20 mg every 12 hours" and the number of rows is unchanged

#### Scenario: Remove the last schedule
- **WHEN** the user removes the only schedule row
- **THEN** the section states that the medicine will be taken as needed, and Save remains available

#### Scenario: Add a schedule to a saved medicine
- **WHEN** the user taps "Add schedule", completes the editor and taps Done
- **THEN** the new row appears after the existing rows

### Requirement: Active switch
In edit mode the form SHALL show a switch labelled "Active" from a string resource, with supporting text explaining that inactive medicines are not reminded, reflecting the medicine's active flag. The switch MUST NOT be shown in add mode. Its value SHALL be saved with the rest of the medicine.

#### Scenario: Switch reflects the saved flag
- **WHEN** an active medicine is opened
- **THEN** the switch is on

#### Scenario: Deactivate from the details screen
- **WHEN** the user turns the switch off and taps Save
- **THEN** the Medicines screen shows the medicine in the inactive section

#### Scenario: Activate from the details screen
- **WHEN** the user opens an inactive medicine, turns the switch on and taps Save
- **THEN** the Medicines screen shows the medicine in the active section

#### Scenario: Not shown when adding
- **WHEN** the form is opened from the add button
- **THEN** no active switch is present

### Requirement: Saving updates the medicine in place
Tapping Save with a valid form SHALL update the existing medicine, identified by the identifier it was opened with, replacing all of its fields and its whole schedule list in one atomic write, then return to the Medicines screen where the tile shows the new values. Saving MUST NOT create a second medicine. When the write fails the user SHALL stay on the details screen, a message SHALL state the medicine could not be saved, and no medication name or dose SHALL be written to the log.

#### Scenario: Rename and save
- **WHEN** the user opens "Metoprolol", changes the name to "Metoprolol 50" and taps Save
- **THEN** the Medicines screen lists exactly one medicine for it, named "Metoprolol 50", in the same section, at its new alphabetical position

#### Scenario: Replace schedules and save
- **WHEN** the user opens a medicine with one schedule "40 mg every 12 hours", removes it, adds "1 tablet twice a day" and taps Save
- **THEN** the tile shows only "1 tablet twice a day"

#### Scenario: Save without changes
- **WHEN** the user opens a medicine and taps Save without changing anything
- **THEN** the Medicines screen is shown and the medicine's values are unchanged

#### Scenario: Save fails
- **WHEN** the update fails in the repository
- **THEN** the user remains on the details screen with the edited values intact, a "Could not save" message is shown, and the log contains neither the name nor the dose

### Requirement: Discarding edits
Leaving the details screen with values that differ from the loaded medicine SHALL ask the user to confirm discarding, using the same dialog as the add form. Leaving with values identical to the loaded medicine SHALL not ask. Discarding MUST leave the saved medicine unchanged.

#### Scenario: Back without edits
- **WHEN** the user opens a medicine and presses back without changing anything
- **THEN** the Medicines screen is shown immediately

#### Scenario: Back after an edit, keep editing
- **WHEN** the user changes the dose, presses back and chooses "Keep editing"
- **THEN** the details screen remains with the changed dose

#### Scenario: Back after an edit, discard
- **WHEN** the user changes the dose, presses back and chooses "Discard"
- **THEN** the Medicines screen is shown and the tile still shows the original dose

#### Scenario: Edit reverted by hand
- **WHEN** the user changes the name and then types the original name back, then presses back
- **THEN** the Medicines screen is shown without a confirmation

### Requirement: Edited draft survives configuration changes
The edited draft, including its schedules and the knowledge of which values were loaded, SHALL survive rotation and process death while the details screen is on the back stack. After restoration the screen MUST NOT reload the saved medicine over the user's edits.

#### Scenario: Rotate after an edit
- **WHEN** the user changes the name and adds a schedule, then rotates the device
- **THEN** the details screen still shows the changed name and the added schedule, and pressing back asks to confirm discarding

#### Scenario: Process death after an edit
- **WHEN** the process is killed and restored while the details screen holds an edited name
- **THEN** the edited name is shown, not the saved one

### Requirement: Medicines are never removed
The app SHALL provide no way for the user to remove a medicine. Neither the details screen nor the Medicines screen SHALL offer a delete, remove or archive action, and no gesture SHALL delete a medicine. Deactivating SHALL be the only way to stop a medicine, and a deactivated medicine SHALL remain stored with its schedules and its dose history.

#### Scenario: Deactivated medicine remains
- **WHEN** the user deactivates a medicine from the details screen and relaunches the app
- **THEN** the medicine is listed in the inactive section and can be opened with all of its values intact

#### Scenario: No delete on the overview
- **WHEN** the user swipes a tile in either direction or long-presses it
- **THEN** no delete action is offered

### Requirement: Editing preserves history
Saving an edit SHALL NOT alter any recorded intake. Doses that are still pending SHALL follow the edited medicine: their name and amount SHALL become the medicine's current name and amount, and doses the edited schedules no longer call for SHALL be withdrawn, including a dose the user has already been reminded about. Withdrawing a dose SHALL remove any reminder showing for it. A dose that already has an outcome — taken, skipped or missed — SHALL be left exactly as it is.

#### Scenario: Rename keeps past intakes
- **WHEN** a dose of "Ibuprofen 400" was recorded as taken yesterday and the user renames the medicine to "Ibuprofen"
- **THEN** yesterday's intake still reads "Ibuprofen 400" with its original amount and time

#### Scenario: Rename reaches pending doses
- **WHEN** a medicine named "Ibuprofen 400" has a pending dose today and the user renames it to "Ibuprofen" and saves
- **THEN** that pending dose reads "Ibuprofen", on the Welcome screen and in its reminder

#### Scenario: Schedule change replans future doses
- **WHEN** a medicine has a planned, un-reminded dose at 20:00 today and the user changes its only schedule from 08:00 and 20:00 to 09:00 and 21:00
- **THEN** after saving, the planned dose at 20:00 is gone and a planned dose at 21:00 exists, while any dose already taken, skipped or missed today is unchanged

#### Scenario: Edit withdraws a reminder that no longer applies
- **WHEN** a reminder for today's 08:00 dose is showing unanswered and the user changes the medicine's schedule so that it no longer has an 08:00 dose, then saves
- **THEN** that dose is withdrawn and its reminder is removed

#### Scenario: Reminded dose that survives the edit picks up the new amount
- **WHEN** a reminder for today's 08:00 dose is showing unanswered and the user changes the medicine's amount while keeping 08:00, then saves
- **THEN** that dose remains pending at 08:00 and shows the new amount

#### Scenario: Edit does not disturb another medicine
- **WHEN** two medicines both have a pending dose at 08:00 and the user moves one of them to 09:00
- **THEN** the other medicine's 08:00 dose is untouched

### Requirement: Medicine cannot be loaded
When the medicine for the given identifier cannot be read, the details screen SHALL return to the Medicines screen and show a message stating that the medicine could not be opened. The failure SHALL be logged without the medicine name.

#### Scenario: Unknown identifier
- **WHEN** the details screen is opened for an identifier that the repository does not return
- **THEN** the Medicines screen is shown with a "Could not open medicine" message

### Requirement: Details screen accessibility
Every field, the active switch, the overflow action and every schedule row on the details screen SHALL have a spoken label and state, every validation error SHALL be announced when it appears, and the screen SHALL scroll fully at the largest system font scale with no clipped text. The overflow action SHALL be at least the minimum touch target in both dimensions and SHALL announce a description of its own.

#### Scenario: Screen reader on the active switch
- **WHEN** a screen reader focuses the active switch of an active medicine
- **THEN** it announces the label "Active" and that the switch is on

#### Scenario: Screen reader on the overflow action
- **WHEN** a screen reader focuses the overflow action
- **THEN** it announces a description such as "More options" and that it is a button

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum and the opened medicine has three schedules
- **THEN** every field, the switch, every schedule row and the Save action are reachable and fully visible by scrolling

