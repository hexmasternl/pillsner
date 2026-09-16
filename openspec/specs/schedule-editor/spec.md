# schedule-editor Specification

## Purpose
TBD - created by archiving change app-medicine-add. Update Purpose after archive.
## Requirements
### Requirement: Schedule editor screen
The schedule editor SHALL be a full screen with a title ("Add schedule" for a new schedule, "Edit schedule" for an existing one), a back affordance, a Done action, an amount field, a pattern selector and pattern-specific inputs. All text MUST come from string resources.

#### Scenario: Open for a new schedule
- **WHEN** the user taps "Add schedule" on the form
- **THEN** the editor opens titled "Add schedule" with the amount prefilled from the medicine's default dose

#### Scenario: Open for an existing schedule
- **WHEN** the user taps a schedule row on the form
- **THEN** the editor opens titled "Edit schedule" with the amount, pattern and inputs of that schedule

### Requirement: Amount
The editor SHALL accept an amount as a positive decimal with a unit, using the same rules as the form's default dose.

#### Scenario: Override the default dose
- **WHEN** the default dose is 40 mg and the user changes the amount to 20 mg
- **THEN** the schedule is saved with 20 mg and the medicine's default dose remains 40 mg

#### Scenario: Invalid amount
- **WHEN** the amount is empty or zero and the user taps Done
- **THEN** the schedule is not saved and the amount field shows an error

### Requirement: Pattern selection
The editor SHALL offer exactly three patterns: "Every N days", "Weekdays" and "Every N hours". Switching pattern MUST keep the amount and MUST keep entered times where the new pattern uses a times list. The three pattern options SHALL render at one shared height regardless of locale or system font scale, and a label MUST wrap onto as many lines as it needs rather than be truncated or clipped when it does not fit on one line.

#### Scenario: Switch from every N days to weekdays
- **WHEN** the user has entered times 08:00 and 20:00 under "Every N days" and switches to "Weekdays"
- **THEN** the times list still shows 08:00 and 20:00 and the weekday chips are shown with none selected

#### Scenario: One label wraps to more lines than another
- **WHEN** the active locale's translation of "Every N days" or "Every N hours" is long enough to wrap onto more lines than "Weekdays" needs
- **THEN** all three pattern options render at the same height, sized to fit the label that needs the most lines, and no label is truncated or ellipsized

#### Scenario: A label needs more lines at a larger font scale
- **WHEN** the system font scale is large enough that a label which fits on two lines at the default scale now needs three or more
- **THEN** all three pattern options grow to that label's height and no word is clipped or hidden

#### Scenario: All labels fit on one line
- **WHEN** the active locale's translations of all three pattern labels fit on one line
- **THEN** all three pattern options render at the same, single-line height

### Requirement: Every N days pattern
Under "Every N days" the editor SHALL provide an interval from 1 to 30 days and a list of one or more distinct clock times. The interval label MUST read "Every day" for 1, "Every other day" for 2, and "Every N days" otherwise.

#### Scenario: Twice a day
- **WHEN** the interval is 1 and the times are 08:00 and 20:00
- **THEN** the preview reads "<amount> twice a day"

#### Scenario: Twice every other day
- **WHEN** the interval is 2 and the times are 08:00 and 20:00
- **THEN** the preview reads "<amount> twice every other day"

#### Scenario: No times
- **WHEN** the times list is empty and the user taps Done
- **THEN** the schedule is not saved and a message states at least one time is required

#### Scenario: Duplicate time
- **WHEN** the user adds 08:00 when 08:00 is already in the list
- **THEN** the duplicate is not added and a message states the time already exists

#### Scenario: Add and remove times
- **WHEN** the user taps "Add time", picks 12:00, then removes 08:00
- **THEN** the times list shows 12:00 and 20:00 in ascending order

### Requirement: Weekdays pattern
Under "Weekdays" the editor SHALL show seven selectable day chips in the locale's week order and a times list with the same rules as above. At least one day MUST be selected and not all seven; selecting all seven SHALL show a hint to use "Every N days" instead and SHALL be rejected on Done.

#### Scenario: Mon, Wed, Fri at 08:00
- **WHEN** Monday, Wednesday and Friday are selected with time 08:00, in an English locale
- **THEN** the preview reads "<amount> once a day on Mon, Wed, Fri"

#### Scenario: No day selected
- **WHEN** no day is selected and the user taps Done
- **THEN** the schedule is not saved and a message states at least one day is required

#### Scenario: All seven days selected
- **WHEN** all seven days are selected
- **THEN** a hint suggests "Every N days" and tapping Done does not save the schedule

### Requirement: Every N hours pattern
Under "Every N hours" the editor SHALL offer intervals 1, 2, 3, 4, 6, 8, 12 and 24 hours and a first dose time, and SHALL display the resulting dose times for one day.

#### Scenario: Every 12 hours from 08:00
- **WHEN** the interval is 12 and the first dose time is 08:00
- **THEN** the resulting times read "08:00, 20:00" and the preview reads "<amount> every 12 hours"

#### Scenario: Every 24 hours collapses to once a day
- **WHEN** the interval is 24 and the first dose time is 09:00
- **THEN** the resulting times read "09:00" and the preview reads "<amount> once a day"

#### Scenario: Every 8 hours from 07:00
- **WHEN** the interval is 8 and the first dose time is 07:00
- **THEN** the resulting times read "07:00, 15:00, 23:00"

### Requirement: Live preview
The editor SHALL show a preview of the human-readable description the overview will display, updated on every change. When the draft is invalid the preview area SHALL show the first validation message instead.

#### Scenario: Preview updates on amount change
- **WHEN** the preview reads "40 mg twice a day" and the user changes the amount to 20 mg
- **THEN** the preview reads "20 mg twice a day"

#### Scenario: Preview shows validation message
- **WHEN** the times list is empty under "Every N days"
- **THEN** the preview area shows the message that at least one time is required

### Requirement: Done and back
Done SHALL validate, add the schedule to the draft (or replace the edited one) and return to the form. Back SHALL return to the form without changing the draft.

#### Scenario: Done on a new schedule
- **WHEN** a valid new schedule is entered and the user taps Done
- **THEN** the form is shown with a new schedule row appended

#### Scenario: Done on an edited schedule
- **WHEN** the user edits the second of two schedules and taps Done
- **THEN** the form shows two rows, with the second reflecting the edit

#### Scenario: Back discards editor changes
- **WHEN** the user edits an existing schedule and presses back
- **THEN** the form shows the schedule unchanged

### Requirement: Editor accessibility
Day chips SHALL announce their day name and selected state, time rows SHALL announce their time, and the editor SHALL scroll fully at the largest font scale.

#### Scenario: Screen reader on a day chip
- **WHEN** a screen reader focuses the Wednesday chip while it is selected
- **THEN** it announces "Wednesday" and that it is selected

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum and the times list holds four times
- **THEN** every control, the preview and the Done action are reachable and fully visible by scrolling

