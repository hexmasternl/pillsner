## MODIFIED Requirements

### Requirement: Medicine details screen opens pre-populated
Tapping a medicine tile on the Medicines screen SHALL open the Medicine details screen for that medicine. The screen SHALL be the medicine form in edit mode: it SHALL present the same fields as the Add medicine form (name, default dose, used since, use until, expiry date, prescribed by, schedules), each pre-populated with the medicine's saved values, and its schedules section SHALL list the saved schedules in their saved order with the same descriptions the tile shows. While the medicine is being loaded the screen SHALL show a loading indicator and MUST NOT show editable fields with default values.

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

#### Scenario: Open a medicine with an expiry date
- **WHEN** the user taps the tile of a medicine whose expiry date is set to 1 December
- **THEN** the details screen shows the expiry date as 1 December

### Requirement: Details screen applies the add form's rules
The details screen SHALL apply every field rule, validation rule and schedule-editing rule of the Add medicine form unchanged: name required, default dose positive with a unit, use until not before used since, expiry date not before used since, prescriber from the fixed list, and schedules added, edited and removed through the schedule editor. Error presentation and string resources SHALL be shared with the add form.

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

#### Scenario: Expiry date before used since is rejected on the details screen
- **WHEN** the user sets the expiry date earlier than "used since" on the details screen and taps Save
- **THEN** the medicine is not updated and the expiry date field shows the same error as the add form

### Requirement: Saving updates the medicine in place
Tapping Save with a valid form SHALL update the existing medicine, identified by the identifier it was opened with, replacing all of its fields (including its expiry date) and its whole schedule list in one atomic write, then return to the Medicines screen where the tile shows the new values. Saving MUST NOT create a second medicine. When the write fails the user SHALL stay on the details screen, a message SHALL state the medicine could not be saved, and no medication name or dose SHALL be written to the log.

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

#### Scenario: Change the expiry date
- **WHEN** the user changes the expiry date and taps Save
- **THEN** the medicine is updated with the new expiry date and reopening the details screen shows it

#### Scenario: Clear an existing expiry date
- **WHEN** a medicine has an expiry date and the user clears it and taps Save
- **THEN** the medicine is updated with no expiry date and any expiry heads-up for it no longer shows on the Medicines screen

### Requirement: Editing preserves history
Saving an edit SHALL NOT alter any recorded intake. Doses that are still pending SHALL follow the edited medicine: their name and amount SHALL become the medicine's current name and amount, and doses the edited schedules no longer call for SHALL be withdrawn, including a dose the user has already been reminded about. Withdrawing a dose SHALL remove any reminder showing for it. A dose that already has an outcome — taken, skipped or missed — SHALL be left exactly as it is. Changing a medicine's expiry date SHALL NOT withdraw, alter or otherwise affect any dose, pending or recorded.

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

#### Scenario: Changing expiry date does not affect doses
- **WHEN** a medicine has a pending dose today and the user changes only its expiry date and saves
- **THEN** the pending dose is unchanged and still due at its original time
