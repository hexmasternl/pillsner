## MODIFIED Requirements

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
