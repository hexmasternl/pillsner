# medicine-details Specification

## Purpose
TBD - created by archiving change app-medicine-details. Update Purpose after archive.

## Requirements

### Requirement: Medicine details screen opens pre-populated
Tapping a medicine tile on the Medicines screen SHALL open the Medicine details screen for that medicine. The screen SHALL be the medicine form in edit mode: it SHALL present the same fields as the Add medicine form (name, default dose, used since, use until, prescribed by, schedules), each pre-populated with the medicine's saved values, and its schedules section SHALL list the saved schedules in their saved order with the same descriptions the tile shows. The screen SHALL lay its content out in this order, top to bottom: the primary details (name and default dose), always visible; the secondary details (used since, use until, prescribed by and the Active switch) inside a panel that is collapsed by default behind a toggle button; the Schedules section, always visible; and the Stock section, always visible. The Add medicine form MUST NOT collapse any field. While the medicine is being loaded the screen SHALL show a loading indicator and MUST NOT show editable fields with default values.

#### Scenario: Open a medicine with schedules
- **WHEN** the user taps the tile of "Metoprolol", default dose 40 mg, used since 1 March, no end date, prescribed by Specialist, with schedules "40 mg every 12 hours" and "20 mg once a day on Sat, Sun"
- **THEN** the details screen shows name "Metoprolol" and dose 40 mg above a collapsed "More details" toggle, then two schedule rows reading "40 mg every 12 hours" then "20 mg once a day on Sat, Sun", and after tapping the toggle it shows used since 1 March, use until empty and prescribed by "Specialist"

#### Scenario: Order of the blocks
- **WHEN** the details screen is shown for a saved medicine
- **THEN** reading top to bottom the screen presents the name and default dose, then the secondary details toggle, then the Schedules section, then the Stock section, with the secondary fields hidden

#### Scenario: Open an as-needed medicine
- **WHEN** the user taps the tile of a medicine with no schedules
- **THEN** the details screen shows its primary fields filled and the schedules section states that the medicine is taken as needed

#### Scenario: Open an inactive medicine
- **WHEN** the user taps a tile in the inactive section and expands the secondary details
- **THEN** the details screen shows that medicine's values and its active switch off

#### Scenario: Add form keeps every field visible
- **WHEN** the form is opened from the add button
- **THEN** name, default dose, used since, use until and prescribed by are all visible without any toggle

#### Scenario: Loading state
- **WHEN** the details screen is opening and the medicine has not been read from the repository yet
- **THEN** a loading indicator is shown and no field is editable

### Requirement: Details screen title and actions
The details screen SHALL be titled "Medicine details" from a string resource in its top bar, SHALL have a back affordance, SHALL present Save as the single primary button pinned to the bottom of the form exactly as the Add medicine form does, and SHALL carry one overflow action in its top bar holding a menu with three items from string resources, in this order: "Add schedule", which opens the same schedule editor as the "Add schedule" button below the schedules list; "Add stock", which opens the same add-stock form as the "Add stock" button in the Stock section; a divider; and "Usage history", which opens that medicine's usage history. The overflow action MUST NOT be present when the form is in add mode. The screen MUST NOT offer any delete, remove or archive action anywhere on the screen, in its top bar, in the overflow menu or in its dialogs. In edit mode the screen SHALL additionally present a Stock section, described below.

#### Scenario: Title and actions
- **WHEN** the details screen is shown
- **THEN** the top bar reads "Medicine details" with a back affordance and an overflow action, and a Save button is pinned at the bottom of the form

#### Scenario: The overflow menu holds the two shortcuts above the usage history
- **WHEN** the user taps the overflow action on the details screen
- **THEN** a menu opens reading "Add schedule", "Add stock", a divider, then "Usage history"

#### Scenario: The Add schedule shortcut
- **WHEN** the user taps "Add schedule" in the overflow menu
- **THEN** the menu closes and the schedule editor opens, exactly as tapping the "Add schedule" button below the schedules list does

#### Scenario: The Add stock shortcut
- **WHEN** the user taps "Add stock" in the overflow menu
- **THEN** the menu closes and the add-stock form opens, exactly as tapping "Add stock" in the Stock section does

#### Scenario: No overflow in add mode
- **WHEN** the form is opened from the add button
- **THEN** the top bar has no overflow action

#### Scenario: No removal action
- **WHEN** every action, menu and dialog reachable from the details screen is inspected, the overflow menu included
- **THEN** none of them deletes, removes or archives the medicine

#### Scenario: No Stock section in add mode
- **WHEN** the form is opened from the add button
- **THEN** no Stock section is shown, since an unsaved medicine cannot hold stock

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
Saving an edit SHALL NOT alter any recorded intake. Doses that are still pending SHALL follow the edited medicine: their name and amount SHALL become the medicine's current name and amount, and doses the edited schedules no longer call for SHALL be withdrawn, including a dose the user has already been reminded about. Withdrawing a dose SHALL remove any reminder showing for it. A dose that already has an outcome — taken, skipped or missed — SHALL be left exactly as it is. Editing a medicine's other fields SHALL NOT alter its stock batches, and adding stock from the Stock section SHALL NOT alter any pending or recorded dose.

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

#### Scenario: Renaming a medicine does not touch its stock
- **WHEN** a medicine has two stock batches and the user renames it and saves
- **THEN** both batches remain unchanged, still attached to the medicine under its new name

#### Scenario: Adding stock does not touch pending doses
- **WHEN** a medicine has a pending dose today and the user adds a stock batch from the details screen
- **THEN** the pending dose is unchanged and still due at its original time

### Requirement: Medicine cannot be loaded
When the medicine for the given identifier cannot be read, the details screen SHALL return to the Medicines screen and show a message stating that the medicine could not be opened. The failure SHALL be logged without the medicine name.

#### Scenario: Unknown identifier
- **WHEN** the details screen is opened for an identifier that the repository does not return
- **THEN** the Medicines screen is shown with a "Could not open medicine" message

### Requirement: Details screen accessibility
Every field, the active switch, the secondary details toggle, the overflow action and every schedule row on the details screen SHALL have a spoken label and state, every validation error SHALL be announced when it appears, and the screen SHALL scroll fully at the largest system font scale with no clipped text, whether the secondary details are collapsed or expanded. The overflow action and the secondary details toggle SHALL each be at least the minimum touch target in both dimensions. The overflow action SHALL announce a description of its own, and the toggle SHALL announce its label and whether the panel is expanded or collapsed. After toggling, the fields that became visible SHALL follow the toggle in screen-reader traversal order.

#### Scenario: Screen reader on the active switch
- **WHEN** a screen reader focuses the active switch of an active medicine
- **THEN** it announces the label "Active" and that the switch is on

#### Scenario: Screen reader on the overflow action
- **WHEN** a screen reader focuses the overflow action
- **THEN** it announces a description such as "More options" and that it is a button

#### Scenario: Screen reader on the secondary details toggle
- **WHEN** a screen reader focuses the toggle while the panel is collapsed
- **THEN** it announces "More details", that it is a button, and that it is collapsed; after activation it announces "Less details" and expanded, and the next elements in traversal are the used since, use until and prescribed by fields and the active switch

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum and the opened medicine has three schedules
- **THEN** every field, the switch, every schedule row and the Save action are reachable and fully visible by scrolling, both with the secondary details collapsed and after expanding them

### Requirement: Stock section
The Medicine details screen, in edit mode only, SHALL show a "Stock" section below the schedules section, listing the medicine's stock batches ordered by expiry date ascending, each row showing its remaining amount and its expiry date formatted for the device locale, and an "Add stock" button that opens the add-stock form owned by the `medicine-stock-tracking` capability. When the medicine has no stock batches, the section SHALL state that no stock is tracked for this medicine and SHALL still offer "Add stock". The section SHALL also show, when applicable, an inline note of the medicine's current low-stock and/or expiry-at-use state, recomputed live rather than tied to the last time a dose was taken.

#### Scenario: No stock tracked yet
- **WHEN** the details screen opens for a medicine with no stock batches
- **THEN** the Stock section states that no stock is tracked and offers "Add stock"

#### Scenario: Batches listed by expiry
- **WHEN** a medicine has two batches, one expiring 1 March with 10 tablets remaining and one expiring 1 June with 30 tablets remaining
- **THEN** the section lists the 1 March batch first, then the 1 June batch, each with its own remaining amount and date

#### Scenario: Add stock reachable
- **WHEN** the user taps "Add stock"
- **THEN** the add-stock form opens for that medicine

#### Scenario: Inline low-stock note
- **WHEN** the medicine's current remaining stock does not cover its projected usage for the next 7 days
- **THEN** the Stock section shows an inline note stating stock is low, independent of whether a dose has been taken since the screen opened

#### Scenario: Inline expiry note
- **WHEN** the medicine's soonest-expiring batch with remaining stock is within 30 days of, or past, its expiry date
- **THEN** the Stock section shows an inline note naming that batch's expiry state

#### Scenario: Section reflects a batch just added
- **WHEN** the user adds a stock batch from the section and returns to the details screen
- **THEN** the new batch appears in the list at its expiry-ordered position without leaving and reopening the screen

### Requirement: Secondary details toggle
In edit mode the form SHALL show, directly below the default dose field, a toggle button labelled "More details" from a string resource while the secondary details panel is collapsed and "Less details" from a string resource while it is expanded. The panel SHALL hold, in this order, the used since field, the use until field, the prescribed by field and the Active switch, each behaving exactly as it does on the Add medicine form. Each fresh open of the details screen SHALL start with the panel collapsed. Tapping the toggle SHALL expand a collapsed panel or collapse an expanded one, with the medium motion duration and honouring the system reduce-motion setting. Toggling MUST NOT count as an edit: it MUST NOT change the saved medicine, MUST NOT trigger the discard confirmation and MUST NOT alter any field value. The panel's expanded or collapsed state SHALL survive rotation and process death while the details screen is on the back stack. When the user taps Save and a field inside the panel has a validation error, the panel SHALL be expanded so the error is visible. The toggle MUST NOT be shown in add mode.

#### Scenario: Collapsed by default
- **WHEN** the user opens a medicine from the Medicines screen
- **THEN** the toggle reads "More details" and the used since, use until, prescribed by fields and the active switch are not shown

#### Scenario: Expand
- **WHEN** the user taps "More details"
- **THEN** the panel expands to show used since, use until, prescribed by and the active switch with the medicine's saved values, and the toggle reads "Less details"

#### Scenario: Collapse
- **WHEN** the panel is expanded and the user taps "Less details"
- **THEN** the panel collapses, the four fields are hidden again and the toggle reads "More details"

#### Scenario: Toggling is not an edit
- **WHEN** the user opens a medicine, taps "More details", taps "Less details" and presses back
- **THEN** the Medicines screen is shown immediately without a discard confirmation and the medicine is unchanged

#### Scenario: Edit inside the panel then collapse
- **WHEN** the user expands the panel, changes the prescriber, collapses the panel and taps Save
- **THEN** the medicine is saved with the new prescriber

#### Scenario: Hidden error opens the panel
- **WHEN** the user expands the panel, sets use until to a date before used since, collapses the panel and taps Save
- **THEN** the medicine is not saved, the panel expands and the use until field shows its error

#### Scenario: State survives rotation
- **WHEN** the user taps "More details" and rotates the device
- **THEN** the panel is still expanded and the toggle still reads "Less details"

#### Scenario: State survives process death
- **WHEN** the process is killed and restored while the details screen has the panel expanded
- **THEN** the panel is restored expanded

#### Scenario: Fresh open starts collapsed
- **WHEN** the user expands the panel, presses back to the Medicines screen and opens the same medicine again
- **THEN** the panel is collapsed

#### Scenario: Not shown when adding
- **WHEN** the form is opened from the add button
- **THEN** no "More details" toggle is present
