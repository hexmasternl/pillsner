## MODIFIED Requirements

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
