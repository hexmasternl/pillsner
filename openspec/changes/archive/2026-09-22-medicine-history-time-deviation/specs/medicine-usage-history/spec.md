## ADDED Requirements

### Requirement: Timing accuracy chart
When at least one dose in the period was taken, the Usage history screen SHALL show a "timing accuracy" bar chart, separate from the outcome chart, with one bar per bucket in time order. Each bucket's value SHALL be the average number of minutes between a taken dose's scheduled moment and the moment it was recorded taken; a dose taken early SHALL contribute the same positive number of minutes as a dose taken the same amount late. Only taken doses SHALL contribute; skipped and missed doses have no recorded intake time and MUST NOT be counted. The "1 week" and "1 month" periods SHALL bucket this chart per day; the "3 months" period SHALL bucket it per week, aligned to the first day of the week of the device locale. Each bar's height SHALL be its bucket's average relative to the largest average in the period. Bars SHALL use a single neutral colour distinct from the outcome chart's taken/skipped/missed/unanswered colours, since a timing deviation is not itself good or bad. The screen SHALL also show one overall average-deviation figure for the whole period, with a caption stating that a lower number is better. When no dose was taken anywhere in the period, the timing accuracy chart and its overall figure SHALL be omitted entirely; the rest of the screen (figures, breakdown, outcome chart or empty state) MUST be unaffected.

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

#### Scenario: A bucket with no taken dose is empty, not zero
- **WHEN** a bucket holds a missed dose and a skipped dose but no taken dose
- **THEN** its column is shown empty, not as a zero-minute bar

#### Scenario: Overall figure and caption
- **WHEN** the period's taken doses average 13 minutes of deviation
- **THEN** the screen shows "13 minutes" as the overall figure alongside a caption stating that lower is better

#### Scenario: Nothing taken omits the chart
- **WHEN** no dose was taken anywhere in the period, whether because none were scheduled or all were skipped or missed
- **THEN** the timing accuracy chart and its overall figure are not shown, and the rest of the screen renders as it would without this feature

### Requirement: Timing accuracy chart accessibility
Every bar in the timing accuracy chart SHALL be a single accessibility node whose description names the bucket (its date or week) and states the average number of minutes off schedule and how many taken doses that average is over. A bucket with no taken dose SHALL be announced as holding no taken dose, worded so it is not confused with a missed dose. The overall average-deviation figure and its "lower is better" caption SHALL be readable as ordinary text.

#### Scenario: A daily bar is announced
- **WHEN** a screen reader focuses the bar for a day whose two taken doses averaged 6 minutes off schedule
- **THEN** it announces the date, that the average was 6 minutes off schedule, and that this is over two doses

#### Scenario: A weekly bar is announced
- **WHEN** a screen reader focuses a weekly bar beginning 8 September whose taken doses averaged 4 minutes off schedule over nine doses
- **THEN** it announces the week's starting date, the 4-minute average and the count of nine doses

#### Scenario: An empty bar is announced without implying a miss
- **WHEN** a screen reader focuses a bucket with no taken dose
- **THEN** it announces that no doses were recorded taken for that bucket, without stating or implying that a dose was missed

### Requirement: Chart value axis
Both bar charts on the Usage history screen - the outcome chart and the timing accuracy chart - SHALL show a value axis to the left of their bars: the busiest bucket's value at the top and zero at the bottom. Each axis label SHALL state its unit ("doses" for the outcome chart, "minutes" for the timing accuracy chart) so it is unambiguous read on its own, including by a screen reader that lands on it directly. The axis MUST NOT introduce any new interaction; the screen remains read-only beyond the period selector.

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
