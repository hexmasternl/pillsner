## ADDED Requirements

### Requirement: Timing deviation of a taken dose
For a dose recorded as taken, its timing deviation SHALL be the absolute number of minutes between its scheduled moment and the moment it was recorded taken, rounded to the nearest minute, so that a dose taken either early or late produces a positive number and never a negative one. A skipped or missed dose SHALL have no timing deviation, because neither carries a moment it was actually taken.

#### Scenario: Taken late
- **WHEN** a dose scheduled at 08:00 is recorded as taken at 08:13
- **THEN** its timing deviation is 13 minutes

#### Scenario: Taken early
- **WHEN** a dose scheduled at 08:00 is recorded as taken at 07:53
- **THEN** its timing deviation is 7 minutes

#### Scenario: Skipped or missed doses are excluded
- **WHEN** the period holds a skipped dose and a missed dose alongside taken doses
- **THEN** neither the skipped nor the missed dose contributes a timing deviation to any figure or bucket

### Requirement: Timing accuracy bucketing
The timing accuracy chart SHALL bucket the "1 week" and "1 month" periods by day and the "3 months" period by week, aligned to the first day of the week of the device locale, with the earliest bucket starting on the window's first day when that falls mid-week. This bucketing is independent of, and may differ from, the bucketing used by the usage chart's outcome bars for the same period.

#### Scenario: One week is daily
- **WHEN** the period is "1 week"
- **THEN** the timing accuracy chart has seven daily buckets

#### Scenario: One month is daily
- **WHEN** the period is "1 month"
- **THEN** the timing accuracy chart has one bucket per day of the window

#### Scenario: Three months is weekly
- **WHEN** the period is "3 months"
- **THEN** the timing accuracy chart has one bucket per week of the window, the earliest starting on the window's first day

### Requirement: Timing accuracy bucket average
A bucket's average timing deviation SHALL be the mean of the timing deviations of the taken doses whose scheduled moment falls in that bucket, rounded to the nearest minute. A bucket with no taken dose SHALL have no average, distinct from an average of zero.

#### Scenario: Bucket average
- **WHEN** a bucket holds taken doses with timing deviations of 10, 14 and 6 minutes
- **THEN** the bucket's average timing deviation is 10 minutes

#### Scenario: Bucket with nothing taken
- **WHEN** a bucket holds only a skipped dose and a missed dose
- **THEN** the bucket has no average timing deviation

### Requirement: Timing accuracy chart
The Usage history screen SHALL show a timing accuracy chart as a row of bars, one per bucket, left to right in time order, below the usage chart. Each bar's height SHALL be its bucket's average timing deviation relative to the largest average in the period. Every bar SHALL be drawn in one colour that carries no meaning of good or bad, so the chart does not imply that any deviation is a failure. The first and last bucket dates SHALL be labelled beneath the row, as they are for the usage chart.

#### Scenario: Bar heights are relative
- **WHEN** one bucket averages 20 minutes and another averages 5 minutes
- **THEN** the second bar is a quarter the height of the first

#### Scenario: A bucket with no average
- **WHEN** a bucket has no average timing deviation
- **THEN** its column is drawn empty and it still keeps its place in the row

### Requirement: Overall timing accuracy figure
Alongside the chart, the screen SHALL show the period's overall average timing deviation in minutes as a figure, with a caption stating that a lower number is better. When no dose was taken anywhere in the period, the timing accuracy chart, its figure and its caption SHALL all be omitted; the rest of the screen's figures, breakdown and usage chart, or the empty state, SHALL be unaffected.

#### Scenario: Overall figure with a caption
- **WHEN** the period's taken doses average 9 minutes of deviation
- **THEN** the screen shows "9 minutes" with a caption saying that less is better

#### Scenario: Nothing taken in the period
- **WHEN** every dose in the period was skipped or missed and none was taken
- **THEN** no timing accuracy chart, figure or caption is shown, and the rest of the screen renders as it would without this feature

### Requirement: Timing accuracy chart accessibility
Every bar in the timing accuracy chart SHALL be a single accessibility node whose description names the bucket and states its average number of minutes and how many taken doses it is averaged over. A bucket with no average SHALL be announced as holding no taken dose, worded so it is not mistaken for a missed or skipped dose. The overall figure and its caption SHALL be reachable as ordinary text.

#### Scenario: A daily bar is announced
- **WHEN** a screen reader focuses the bar for 9 September, averaging 12 minutes over 2 taken doses
- **THEN** it announces the date and that doses that day averaged 12 minutes, over 2 taken doses

#### Scenario: A bucket with no taken dose is announced
- **WHEN** a screen reader focuses a bar whose bucket has no average timing deviation
- **THEN** it announces the bucket and that no dose was taken in it, without stating a minute count
