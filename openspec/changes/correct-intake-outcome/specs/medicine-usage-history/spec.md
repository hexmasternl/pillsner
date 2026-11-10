# Spec Delta

## MODIFIED Requirements

### Requirement: Usage history screen identity and actions
The Usage history screen SHALL be a secondary destination titled "Usage history" from a string resource in its top app bar, with a back affordance, and SHALL show the medicine's name in the screen content as a heading rather than in the app bar so that a long name wraps instead of being truncated. The only action on the screen that changes a record SHALL be saving the correction sheet (`intake-correction`). The screen MUST NOT offer any other action that records, changes or removes an intake, a dose or a medicine.

#### Scenario: Title and back
- **WHEN** the Usage history screen is shown
- **THEN** the top app bar reads "Usage history" with a back affordance, and the medicine's name appears in the content as a heading

#### Scenario: Nothing writes
- **WHEN** every control on the Usage history screen is inspected
- **THEN** none of them records an intake, edits a dose or changes the medicine, except the correction sheet's Save button, and it only ever changes the outcome of the one dose the sheet was opened for

#### Scenario: Back returns to the details screen
- **WHEN** the user presses back or the back affordance
- **THEN** the Medicine details screen for the same medicine is shown

### Requirement: Timing accuracy chart
When at least one dose in the period was taken, the Usage history screen SHALL show a "timing accuracy" bar chart, separate from the outcome chart, with one bar per bucket in time order. Each bucket's value SHALL be the average number of minutes between a taken dose's scheduled moment and the moment it was recorded taken; a dose taken early SHALL contribute the same positive number of minutes as a dose taken the same amount late. Only taken doses SHALL contribute; skipped and missed doses have no recorded intake time and MUST NOT be counted. A dose corrected to taken (`intake-correction`) has no real taken time and MUST NOT be counted either. The "1 week" and "1 month" periods SHALL bucket this chart per day; the "3 months" period SHALL bucket it per week, aligned to the first day of the week of the device locale. Each bar's height SHALL be its bucket's average relative to the largest average in the period. Bars SHALL use a single neutral colour distinct from the outcome chart's taken/skipped/missed/unanswered colours, since a timing deviation is not itself good or bad. The screen SHALL also show one overall average-deviation figure for the whole period, with a caption stating that a lower number is better. When no dose that counts for this chart was taken anywhere in the period, the timing accuracy chart and its overall figure SHALL be omitted entirely; the rest of the screen (figures, breakdown, outcome chart or empty state) MUST be unaffected.

#### Scenario: A week is seven daily bars
- **WHEN** the period is "1 week" and doses were taken on each day
- **THEN** the timing accuracy chart has seven bars, one per day

#### Scenario: A month is daily bars, unlike the outcome chart
- **WHEN** the period is "1 month"
- **THEN** the timing accuracy chart buckets per day while the outcome chart on the same screen continues to bucket per week

#### Scenario: Three months are weekly bars
- **WHEN** the period is "3 months"
- **THEN** the timing accuracy chart has one bar per week of the window, the earliest starting on the window's first day

#### Scenario: Early and late are both positive minutes
- **WHEN** one taken dose was recorded 5 minutes before its scheduled time and another was recorded 5 minutes after its scheduled time
- **THEN** both doses contribute a deviation of 5 minutes to their bucket's average

#### Scenario: Only taken doses count
- **WHEN** a bucket holds one taken dose recorded 10 minutes late, one skipped dose and one missed dose
- **THEN** the bucket's average deviation is 10 minutes, unaffected by the skipped and missed doses

#### Scenario: A dose corrected to taken does not count
- **WHEN** a bucket holds one taken dose recorded 10 minutes late and one dose corrected from missed to taken
- **THEN** the bucket's average deviation is 10 minutes over one dose, while the outcome chart counts both doses as taken

#### Scenario: A bucket with no taken dose is empty, not zero
- **WHEN** a bucket holds a missed dose and a skipped dose but no taken dose
- **THEN** its column is shown empty, not as a zero-minute bar

#### Scenario: Overall figure and caption
- **WHEN** the period's taken doses average 13 minutes of deviation
- **THEN** the screen shows "13 minutes" as the overall figure alongside a caption stating that lower is better

#### Scenario: Nothing taken omits the chart
- **WHEN** no dose was taken anywhere in the period, whether because none were scheduled or all were skipped or missed
- **THEN** the timing accuracy chart and its overall figure are not shown, and the rest of the screen renders as it would without this feature

### Requirement: Chart value axis
Both bar charts on the Usage history screen - the outcome chart and the timing accuracy chart - SHALL show a value axis to the left of their bars: the busiest bucket's value at the top and zero at the bottom. Each axis label SHALL state its unit ("doses" for the outcome chart, "minutes" for the timing accuracy chart) so it is unambiguous read on its own, including by a screen reader that lands on it directly. The axis MUST NOT introduce any new interaction.

#### Scenario: Outcome chart axis
- **WHEN** the outcome chart's busiest bucket has 12 scheduled doses
- **THEN** the axis beside the outcome chart reads "12 doses" at the top and "0 doses" at the bottom

#### Scenario: Timing accuracy chart axis
- **WHEN** the timing accuracy chart's busiest bucket averages 22 minutes of deviation
- **THEN** the axis beside the timing accuracy chart reads "22 minutes" at the top and "0 minutes" at the bottom

#### Scenario: Axis label is self-explanatory in isolation
- **WHEN** a screen reader lands directly on the bottom axis label of either chart without any other context
- **THEN** the label states its unit rather than reading a bare "0"

#### Scenario: No new interaction
- **WHEN** either chart's value axis is inspected
- **THEN** it offers no tap, tooltip or other affordance beyond displaying its two labels

## ADDED Requirements

### Requirement: Recent doses list
Below the charts, or below the empty state, the Usage history screen SHALL show a "Recent doses" section listing every dose of the medicine scheduled in the last 14 days up to the present moment, newest first, whatever period is selected. A caption SHALL state that it covers the last 14 days. Each row SHALL show the scheduled date and time, the dose's amount, and its outcome as a chip carrying an icon and a label: Taken, Skipped, Missed or Not answered yet. When nothing was scheduled in those 14 days, the section SHALL say so.

#### Scenario: Newest first
- **WHEN** the medicine had doses at 08:00 on each of the last three days
- **THEN** the list shows today's dose first and the dose of two days ago last

#### Scenario: Independent of the period
- **WHEN** the user switches the period from "1 week" to "3 months"
- **THEN** the Recent doses list still covers the last 14 days

#### Scenario: Future doses are not listed
- **WHEN** it is 12:00 and a dose is scheduled at 20:00 today
- **THEN** the 20:00 dose is not in the list

#### Scenario: Nothing in 14 days
- **WHEN** the medicine had no dose scheduled in the last 14 days
- **THEN** the section states that there are no doses in the last 14 days

### Requirement: Taken time and difference in the list
A row for a taken dose SHALL also show the moment it was taken and the difference from the scheduled moment in minutes or hours, stating whether it was early or late. A row for a dose corrected to taken SHALL show neither, and SHALL instead say that the outcome was corrected. A row for any other corrected dose SHALL also say that it was corrected.

#### Scenario: Taken late
- **WHEN** a dose scheduled at 08:00 was taken at 08:25
- **THEN** its row shows that it was taken at 08:25, 25 minutes late

#### Scenario: Corrected to taken
- **WHEN** a missed dose was corrected to taken
- **THEN** its row shows the Taken chip and says that it was corrected, with no taken time or difference

### Requirement: Correction sheet
Tapping a row whose dose can be corrected (`intake-correction`) SHALL open a modal bottom sheet naming the dose's scheduled date, time and amount and its current outcome. The sheet SHALL offer exactly two single-choice options, "I took it" and "I didn't take it", with the option matching the current outcome preselected and none preselected for a missed dose, and a "Save" button. Save SHALL be enabled only when the chosen option would change the outcome. Dismissing the sheet SHALL change nothing. A row whose dose cannot be corrected MUST NOT respond to a tap.

#### Scenario: Open the sheet
- **WHEN** the user taps the row of a dose recorded skipped two days ago
- **THEN** a bottom sheet opens with "I didn't take it" selected and Save disabled

#### Scenario: Save a correction
- **WHEN** the user selects "I took it" in that sheet and taps Save
- **THEN** the sheet closes and the row shows the Taken chip and that it was corrected

#### Scenario: Missed dose starts with no choice
- **WHEN** the sheet is opened for a missed dose
- **THEN** neither option is selected and Save is disabled until one is chosen

#### Scenario: Dismiss changes nothing
- **WHEN** the user selects a different option and then dismisses the sheet without saving
- **THEN** the dose's outcome is unchanged

#### Scenario: A dose not yet answered is not tappable
- **WHEN** the user taps a row showing "Not answered yet"
- **THEN** no sheet opens

#### Scenario: Figures follow the correction
- **WHEN** a missed dose inside the selected period is corrected to taken
- **THEN** the adherence figure, the breakdown and the outcome chart move that dose from missed to taken without the user leaving the screen

### Requirement: Recent doses and correction accessibility
Each correctable row SHALL be a single accessibility node announcing the scheduled date and time, the amount, the outcome, whether it was corrected and, for a taken dose, when it was taken, with a click action labelled "Change outcome". A row that cannot be corrected SHALL announce the same content without a click action. The sheet's options SHALL be announced as a single-choice group with their selected state. The list and the sheet SHALL be fully usable at the largest font scale without clipped or truncated text.

#### Scenario: Screen reader on a row
- **WHEN** a screen reader focuses the row of a dose scheduled yesterday at 08:00 and taken at 08:10
- **THEN** it announces the date and time, the amount, that it was taken at 08:10, 10 minutes late, and offers the action "Change outcome"

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum
- **THEN** every row and every element of the sheet is fully visible, wrapping where needed
