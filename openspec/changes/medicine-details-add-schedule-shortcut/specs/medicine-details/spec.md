## MODIFIED Requirements

### Requirement: Details screen title and actions
The details screen SHALL be titled "Medicine details" from a string resource in its top bar, SHALL have a back affordance, SHALL present Save as the single primary button pinned to the bottom of the form exactly as the Add medicine form does, and SHALL carry one overflow action in its top bar holding a menu with exactly two items separated by a divider, in order: "Add schedule" from a string resource, then a divider, then "Usage history" from a string resource. "Add schedule" SHALL perform the same action as the form's own "Add schedule" button (opening the schedule editor to add a new schedule); "Usage history" SHALL open that medicine's usage history. The overflow action MUST NOT be present when the form is in add mode. The screen MUST NOT offer any delete, remove or archive action anywhere on the screen, in its top bar, in the overflow menu or in its dialogs.

#### Scenario: Title and actions
- **WHEN** the details screen is shown
- **THEN** the top bar reads "Medicine details" with a back affordance and an overflow action, and a Save button is pinned at the bottom of the form

#### Scenario: The overflow menu holds two items and a divider
- **WHEN** the user taps the overflow action on the details screen
- **THEN** a menu opens containing, in order, "Add schedule", a divider, then "Usage history"

#### Scenario: Add schedule from the overflow menu
- **WHEN** the user taps "Add schedule" in the overflow menu, completes the schedule editor and taps Done
- **THEN** the new schedule appears in the schedules list, exactly as it would if added from the form's own "Add schedule" button

#### Scenario: No overflow in add mode
- **WHEN** the form is opened from the add button
- **THEN** the top bar has no overflow action

#### Scenario: No removal action
- **WHEN** every action, menu and dialog reachable from the details screen is inspected, the overflow menu included
- **THEN** none of them deletes, removes or archives the medicine
