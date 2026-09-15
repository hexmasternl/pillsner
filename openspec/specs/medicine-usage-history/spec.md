# medicine-usage-history Specification

## Purpose
TBD - created by archiving change app-medicine-usage-history. Update Purpose after archive.
## Requirements
### Requirement: Usage history is reached from the medicine details overflow menu
The Medicine details screen's top app bar SHALL carry an overflow action, shown only in edit mode, that opens a menu containing an item labelled "Usage history" from a string resource. Choosing it SHALL open the Usage history screen for the medicine the details screen was opened with. The overflow action MUST NOT be shown when the form is in add mode, because an unsaved medicine has no history.

#### Scenario: Open the history
- **WHEN** the user opens a saved medicine and taps the overflow action, then "Usage history"
- **THEN** the Usage history screen is shown for that medicine

#### Scenario: Not offered while adding
- **WHEN** the medicine form is opened from the add button
- **THEN** no overflow action is present in the top app bar

#### Scenario: Unsaved edits are not disturbed
- **WHEN** the user changes the medicine's name, opens the usage history and presses back
- **THEN** the details screen is shown with the changed name still present and no discard confirmation was asked

### Requirement: Usage history screen identity and actions
The Usage history screen SHALL be a secondary destination titled "Usage history" from a string resource in its top app bar, with a back affordance, and SHALL show the medicine's name in the screen content as a heading rather than in the app bar so that a long name wraps instead of being truncated. The screen SHALL be read-only: it MUST NOT offer any action that records, changes or removes an intake, a dose or a medicine.

#### Scenario: Title and back
- **WHEN** the Usage history screen is shown
- **THEN** the top app bar reads "Usage history" with a back affordance, and the medicine's name appears in the content as a heading

#### Scenario: Nothing writes
- **WHEN** every control on the Usage history screen is inspected
- **THEN** none of them records an intake, edits a dose or changes the medicine

#### Scenario: Back returns to the details screen
- **WHEN** the user presses back or the back affordance
- **THEN** the Medicine details screen for the same medicine is shown

### Requirement: Period selection
The screen SHALL offer exactly three periods as a single-choice segmented button row at the top of the content, labelled "1 week", "1 month" and "3 months" from string resources, with "1 week" selected when the screen opens. Selecting a period SHALL recompute everything the screen shows for that period. The selected period SHALL survive rotation and process death.

#### Scenario: Default period
- **WHEN** the Usage history screen opens
- **THEN** the "1 week" segment is selected and the figures cover the last seven days

#### Scenario: Change the period
- **WHEN** the user taps "3 months"
- **THEN** the "3 months" segment is selected and every figure and the chart are recomputed for that period

#### Scenario: Period survives rotation
- **WHEN** the user selects "1 month" and rotates the device
- **THEN** "1 month" is still selected

#### Scenario: Period survives process death
- **WHEN** the process is killed and restored while "3 months" is selected
- **THEN** "3 months" is still selected

### Requirement: The window a period covers
A period SHALL resolve, in the device time zone, to a window of whole local days ending today, whose upper bound is the present moment. "1 week" SHALL be today and the six days before it. "1 month" SHALL be the day after the same date one month ago, through today. "3 months" SHALL be the day after the same date three months ago, through today. Date arithmetic SHALL be done on local dates so that month lengths, leap days and daylight-saving transitions need no special case.

#### Scenario: One week
- **WHEN** today is Monday 14 September and the period is "1 week"
- **THEN** the window runs from the start of Tuesday 8 September to the present moment

#### Scenario: One month across a short month
- **WHEN** today is 31 March and the period is "1 month"
- **THEN** the window starts on 1 March

#### Scenario: Three months across a leap day
- **WHEN** today is 31 May of a leap year and the period is "3 months"
- **THEN** the window starts on 1 March and includes 29 February of that year only if it falls inside it

#### Scenario: Daylight-saving transition inside the window
- **WHEN** the window spans a day on which the clocks change
- **THEN** each day of the window is still one whole local day and no dose is counted twice or dropped

### Requirement: What counts as a scheduled dose
A dose of the medicine SHALL count towards the period when its scheduled moment falls inside the window. A dose scheduled later than the present moment MUST NOT be counted at all, in any category, because it has not happened yet. The count of scheduled doses SHALL be the total of taken, skipped, missed and unanswered doses in the window.

#### Scenario: Later today is not counted
- **WHEN** it is 12:00, the medicine has doses today at 08:00 and 20:00, and the 08:00 dose was taken
- **THEN** one dose is counted as scheduled and one as taken, and the 20:00 dose appears nowhere

#### Scenario: Tomorrow is not counted
- **WHEN** planned doses exist for tomorrow
- **THEN** they do not appear in any period

#### Scenario: Before the window is not counted
- **WHEN** a dose was taken the day before the window starts
- **THEN** it is not counted

### Requirement: Outcomes are counted separately
The screen SHALL count taken, skipped, missed and unanswered doses as four distinct categories. A dose whose recorded outcome is taken, skipped or missed SHALL count in that category. A dose in the past with no recorded outcome SHALL count as unanswered. A skipped dose MUST NOT be presented as a missed dose, and MUST NOT be described as a failure.

#### Scenario: Four outcomes
- **WHEN** the window holds six doses: three taken, one skipped, one missed and one that has passed but has not lapsed yet
- **THEN** the screen reports six scheduled, three taken, one skipped, one missed and one unanswered

#### Scenario: Skipped is not missed
- **WHEN** a dose was recorded as skipped
- **THEN** it is counted and labelled as skipped, with its own neutral colour and its own icon, and the missed count does not include it

#### Scenario: Unanswered settles later
- **WHEN** an unanswered dose lapses to missed while the screen is open
- **THEN** the screen re-reads the record and moves that dose from unanswered to missed without the user doing anything

### Requirement: Adherence figure
The screen SHALL show adherence as the proportion of scheduled doses that were taken, formatted as a whole percentage in the app's locale, together with the scheduled count and the taken count as figures in their own right. When no dose is scheduled in the period the screen MUST NOT show a percentage of zero; it SHALL show the empty state instead.

#### Scenario: Adherence is shown
- **WHEN** the period holds forty scheduled doses of which thirty-nine were taken
- **THEN** the screen shows 98 %, a scheduled count of 40 and a taken count of 39

#### Scenario: Rounding
- **WHEN** the period holds three scheduled doses of which two were taken
- **THEN** the screen shows 67 %

#### Scenario: Nothing scheduled
- **WHEN** the period holds no scheduled dose
- **THEN** no percentage is shown and the empty state is shown in place of the figures, the breakdown and the chart

### Requirement: Outcome breakdown
Below the figures the screen SHALL show a proportional bar split between taken, skipped, unanswered and missed, and a legend listing every category that has a non-zero count with its icon, its label from a string resource and its count. Category colours SHALL come from the intake state colour roles and MUST NOT introduce a new hue. Every category SHALL carry an icon and a text label, so that no state is conveyed by colour alone.

#### Scenario: Breakdown matches the counts
- **WHEN** the period holds three taken, one skipped and one missed dose
- **THEN** the bar is split three-to-one-to-one and the legend lists Taken 3, Skipped 1 and Missed 1

#### Scenario: Empty categories are omitted from the legend
- **WHEN** the period holds no skipped dose
- **THEN** the legend has no Skipped row

#### Scenario: State is never colour alone
- **WHEN** every legend row is inspected
- **THEN** each carries an icon and a written label as well as its colour

### Requirement: Usage chart
The screen SHALL show the period as a row of bars, one per bucket, left to right in time order. The "1 week" period SHALL use one bucket per day; the "1 month" and "3 months" periods SHALL use one bucket per week, aligned to the first day of the week of the device locale, with the earliest bucket starting on the window's first day when that falls mid-week. Each bar's height SHALL be its bucket's scheduled count relative to the largest bucket's scheduled count in the period, and each bar SHALL be stacked from taken, skipped, unanswered and missed portions using the same colours as the breakdown. The first and last bucket dates SHALL be labelled beneath the row. No bar SHALL carry its own visible date label.

#### Scenario: A week is seven daily bars
- **WHEN** the period is "1 week"
- **THEN** the chart has seven bars, one per day, the last of which is today

#### Scenario: Three months are weekly bars
- **WHEN** the period is "3 months"
- **THEN** the chart has one bar per week of the window, the earliest starting on the window's first day

#### Scenario: Bar heights are relative
- **WHEN** one bucket has four scheduled doses and another has two
- **THEN** the second bar is half the height of the first

#### Scenario: Bar composition
- **WHEN** a bucket holds two taken doses and one missed dose
- **THEN** its bar is two-thirds in the taken colour and one third in the missed colour

#### Scenario: A bucket with nothing scheduled
- **WHEN** a bucket holds no scheduled dose
- **THEN** its column is empty and the chart still shows the bucket's place in the row

### Requirement: Chart accessibility
Every bar SHALL be a single accessibility node whose description names the bucket and states how many of how many doses were taken, so the chart is fully readable without seeing it. A bucket with nothing scheduled SHALL say so. The totals the chart summarises SHALL also be present as text elsewhere on the screen.

#### Scenario: A daily bar is announced
- **WHEN** a screen reader focuses the bar for 9 September, which holds two doses both taken
- **THEN** it announces the date and that two of two doses were taken

#### Scenario: A weekly bar is announced
- **WHEN** a screen reader focuses a weekly bar beginning 8 September holding fourteen doses of which twelve were taken
- **THEN** it announces the week's starting date and that twelve of fourteen doses were taken

#### Scenario: An empty bar is announced
- **WHEN** a screen reader focuses a bar whose bucket holds no scheduled dose
- **THEN** it announces the bucket and that no doses were scheduled

### Requirement: How far the records reach
When the medicine's earliest stored dose falls later than the first day of the chosen period, the screen SHALL state the date the records start from. When the records cover the whole period, no such note SHALL be shown. The note MUST be shown alongside the figures rather than in place of them, so a short record is explained and not hidden.

#### Scenario: Records shorter than the period
- **WHEN** the medicine was added on 8 September, today is 14 September and the period is "3 months"
- **THEN** the figures cover what is recorded and the screen states that records start on 8 September

#### Scenario: Records cover the period
- **WHEN** the medicine has doses recorded from before the window starts
- **THEN** no records-start note is shown

#### Scenario: A gap is not a missing record
- **WHEN** a medicine taken once a week has empty stretches inside the window but has doses recorded from before it starts
- **THEN** no records-start note is shown and the empty stretches appear as empty buckets

### Requirement: Empty state
When the chosen period holds no scheduled dose the screen SHALL show an empty state in place of the figures, the breakdown and the chart, with an icon, a headline stating that there is nothing recorded for this period and a hint suggesting a longer period. The period selector SHALL remain visible and usable so the user can widen the period without leaving the screen.

#### Scenario: A new medicine on the week period
- **WHEN** a medicine has never had a dose reach its moment and the period is "1 week"
- **THEN** the empty state is shown and the three period buttons are still visible and tappable

#### Scenario: Widening the period finds records
- **WHEN** the empty state is shown for "1 week" and the user taps "3 months", which holds recorded doses
- **THEN** the figures, the breakdown and the chart replace the empty state

### Requirement: Medicine cannot be loaded
When the medicine for the given identifier cannot be read, the Usage history screen SHALL return to where it was opened from and a message SHALL state that the medicine could not be opened. The failure SHALL be logged without the medicine name, its dose or any amount.

#### Scenario: Unknown identifier
- **WHEN** the Usage history screen is opened for an identifier the repository does not return
- **THEN** the screen closes and a "Could not open medicine" message is shown

#### Scenario: Nothing sensitive is logged
- **WHEN** the failure is logged
- **THEN** the log contains neither the medicine's name nor any dose or amount

### Requirement: Usage history accessibility and large fonts
Every figure, label and control on the screen SHALL have a spoken label, the medicine name and each card header SHALL be marked as a heading, the period segments SHALL announce their label and selected state, and the whole screen SHALL scroll fully at the largest system font scale with no clipped or truncated text. Times, dates and percentages SHALL be formatted for the user's locale rather than assembled by hand.

#### Scenario: Screen reader on the period selector
- **WHEN** a screen reader focuses the "1 month" segment while it is selected
- **THEN** it announces the label "1 month" and that it is selected

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum on the "3 months" period
- **THEN** every figure, the breakdown legend, the chart and the records note are reachable and fully visible by scrolling, and no text is truncated

#### Scenario: Locale formatting
- **WHEN** the app language is Dutch
- **THEN** the percentage and every date on the screen are formatted for Dutch

