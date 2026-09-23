## MODIFIED Requirements

### Requirement: Details screen title and actions
The details screen SHALL be titled "Medicine details" from a string resource in its top bar, SHALL have a back affordance, SHALL present Save as the single primary button pinned to the bottom of the form exactly as the Add medicine form does, and SHALL carry one overflow action in its top bar holding a menu whose only item, "Usage history" from a string resource, opens that medicine's usage history. The overflow action MUST NOT be present when the form is in add mode. The screen MUST NOT offer any delete, remove or archive action anywhere on the screen, in its top bar, in the overflow menu or in its dialogs. In edit mode the screen SHALL additionally present a Stock section, described below.

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

#### Scenario: No Stock section in add mode
- **WHEN** the form is opened from the add button
- **THEN** no Stock section is shown, since an unsaved medicine cannot hold stock

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
