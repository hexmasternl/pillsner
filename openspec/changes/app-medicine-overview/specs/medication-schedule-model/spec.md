## ADDED Requirements

### Requirement: Medication domain model
The domain layer SHALL define a `Medication` with an identifier, a name, exactly one `Schedule` and an active flag. Domain model types MUST NOT depend on Android framework classes.

#### Scenario: Active flag meaning
- **WHEN** a medication has its active flag set to false
- **THEN** it is treated as inactive by every consumer: the overview lists it under Inactive and no doses are expected from it

#### Scenario: No Android imports
- **WHEN** the domain model source files are inspected
- **THEN** none of them import `android.*` or `androidx.*` classes

### Requirement: Schedule shapes
The domain layer SHALL define `Schedule` as a closed set of shapes: fixed clock times on a set of weekdays, one dose every N days at a clock time, one dose every N hours, and as-needed. Clock times and weekdays MUST be wall-clock values, not instants. Construction MUST reject an empty set of times, an empty set of days, or an interval below one.

#### Scenario: Daily fixed times
- **WHEN** a fixed-times schedule is created with times 08:00 and 20:00 on all seven days
- **THEN** it is a valid schedule with two times per day

#### Scenario: Weekday subset
- **WHEN** a fixed-times schedule is created with time 08:00 on Monday, Wednesday and Friday
- **THEN** it is a valid schedule restricted to those days

#### Scenario: Invalid interval
- **WHEN** an every-N-days schedule is created with an interval of zero
- **THEN** construction fails with an argument error

#### Scenario: Invalid empty times
- **WHEN** a fixed-times schedule is created with no times
- **THEN** construction fails with an argument error

### Requirement: Schedule summary
The domain layer SHALL provide a function that summarises any `Schedule` into a `ScheduleSummary` suitable for producing a human-readable description. The summary MUST collapse equivalent schedules: every 1 day is once a day, every 24 hours is once a day, every 2 days is every other day. Duplicate times in a fixed-times schedule MUST count once.

#### Scenario: Two times daily
- **WHEN** a fixed-times schedule has two distinct times on all seven days
- **THEN** the summary is "times per day" with count 2

#### Scenario: Duplicate times
- **WHEN** a fixed-times schedule has times 08:00, 08:00 and 20:00 on all seven days
- **THEN** the summary is "times per day" with count 2

#### Scenario: Fixed times on some days
- **WHEN** a fixed-times schedule has one time on Monday, Wednesday and Friday
- **THEN** the summary is "times per day on days" with count 1 and those three days

#### Scenario: Every one day collapses to daily
- **WHEN** an every-N-days schedule has interval 1
- **THEN** the summary is "times per day" with count 1

#### Scenario: Every two days
- **WHEN** an every-N-days schedule has interval 2
- **THEN** the summary is "every other day"

#### Scenario: Every three or more days
- **WHEN** an every-N-days schedule has interval 3
- **THEN** the summary is "every N days" with 3

#### Scenario: Every 24 hours collapses to daily
- **WHEN** an every-N-hours schedule has interval 24
- **THEN** the summary is "times per day" with count 1

#### Scenario: Every 8 hours
- **WHEN** an every-N-hours schedule has interval 8
- **THEN** the summary is "every N hours" with 8

#### Scenario: As needed
- **WHEN** the schedule is as-needed
- **THEN** the summary is "as needed"

### Requirement: Human-readable schedule description
The UI layer SHALL map every `ScheduleSummary` to a description from string resources. Counts of one and two MUST use the words "Once" and "Twice"; higher counts MUST use a numbered form. Day names MUST be the locale's short weekday names ordered from the locale's first day of the week. Monday to Friday exactly MUST be described as weekdays rather than listed.

#### Scenario: Once a day
- **WHEN** the summary is "times per day" with count 1
- **THEN** the description is "Once a day"

#### Scenario: Twice a day
- **WHEN** the summary is "times per day" with count 2
- **THEN** the description is "Twice a day"

#### Scenario: Three times a day
- **WHEN** the summary is "times per day" with count 3
- **THEN** the description is "3 times a day"

#### Scenario: Once on specific days
- **WHEN** the summary is "times per day on days" with count 1 on Monday, Wednesday and Friday, in an English locale
- **THEN** the description is "Once a day on Mon, Wed, Fri"

#### Scenario: Weekdays
- **WHEN** the summary is "times per day on days" with count 2 on Monday through Friday
- **THEN** the description is "Twice a day on weekdays"

#### Scenario: Every other day
- **WHEN** the summary is "every other day"
- **THEN** the description is "Once every other day"

#### Scenario: Every N days
- **WHEN** the summary is "every N days" with 3
- **THEN** the description is "Once every 3 days"

#### Scenario: Every N hours
- **WHEN** the summary is "every N hours" with 8
- **THEN** the description is "Every 8 hours"

#### Scenario: As needed
- **WHEN** the summary is "as needed"
- **THEN** the description is "As needed"

### Requirement: Medication repository contract
The domain layer SHALL define a `MedicationRepository` interface that exposes all medications, active and inactive, as a reactive stream that re-emits whenever any medication changes. Until persistence exists, the app SHALL be wired with an in-memory implementation that starts empty.

#### Scenario: Stream re-emits on change
- **WHEN** a medication is added to the in-memory repository while a collector is active
- **THEN** the collector receives a new list containing the added medication

#### Scenario: Empty start
- **WHEN** the app starts with the in-memory implementation
- **THEN** the first emission is an empty list
