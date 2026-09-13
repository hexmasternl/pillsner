## MODIFIED Requirements

### Requirement: Medication domain model
The domain layer SHALL define a `Medication` with an identifier, a non-blank name, a default dose (`Quantity`), a used-since date, an optional use-until date that is not before used-since, a prescriber, an ordered list of zero or more `Schedule`s, and an active flag. An empty schedule list means the medication is taken as needed. Domain model types MUST NOT depend on Android framework classes.

#### Scenario: Active flag meaning
- **WHEN** a medication has its active flag set to false
- **THEN** it is treated as inactive by every consumer: the overview lists it under Inactive and no doses are expected from it

#### Scenario: No Android imports
- **WHEN** the domain model source files are inspected
- **THEN** none of them import `android.*` or `androidx.*` classes

#### Scenario: Multiple schedules
- **WHEN** a medication is created with two schedules
- **THEN** both are retained in the order given

#### Scenario: Empty schedules means as needed
- **WHEN** a medication is created with no schedules
- **THEN** it is valid and its schedule summary is "as needed"

#### Scenario: Use until before used since
- **WHEN** a medication is created with use until earlier than used since
- **THEN** construction fails with an argument error

#### Scenario: Blank name
- **WHEN** a medication is created with a blank name
- **THEN** construction fails with an argument error

### Requirement: Schedule shapes
The domain layer SHALL define `Schedule` as a closed set of shapes, each carrying an amount (`Quantity`): one or more clock times every N days counted from the medication's used-since date (N >= 1); one or more clock times on a strict, non-empty subset of weekdays; and one dose every N hours starting from a first dose time and restarting daily (N between 1 and 24). Clock times and weekdays MUST be wall-clock values, not instants. Construction MUST reject an empty times list, an empty or complete weekday set, an interval outside its range, and a non-positive amount.

#### Scenario: Twice daily
- **WHEN** an every-N-days schedule is created with interval 1, times 08:00 and 20:00 and amount 40 mg
- **THEN** it is a valid schedule with two times per day

#### Scenario: Weekday subset
- **WHEN** a weekdays schedule is created with time 08:00 on Monday, Wednesday and Friday
- **THEN** it is a valid schedule restricted to those days

#### Scenario: All seven weekdays rejected
- **WHEN** a weekdays schedule is created with all seven days
- **THEN** construction fails with an argument error directing to every-N-days

#### Scenario: Every 12 hours from 08:00
- **WHEN** an every-N-hours schedule is created with interval 12 and first dose 08:00
- **THEN** it is a valid schedule whose daily dose times are 08:00 and 20:00

#### Scenario: Invalid interval
- **WHEN** an every-N-days schedule is created with an interval of zero
- **THEN** construction fails with an argument error

#### Scenario: Invalid empty times
- **WHEN** an every-N-days schedule is created with no times
- **THEN** construction fails with an argument error

#### Scenario: Non-positive amount
- **WHEN** any schedule is created with amount 0 mg
- **THEN** construction fails with an argument error

### Requirement: Schedule summary
The domain layer SHALL provide a function that summarises any `Schedule` into a `ScheduleSummary`, and a function that summarises a medication's schedule list into a list of summaries, yielding a single "as needed" summary for an empty list. The summary MUST collapse equivalent schedules: every 1 day is times per day, every 2 days is times every other day, every 24 hours is once a day. Duplicate times MUST count once.

#### Scenario: Two times daily
- **WHEN** an every-N-days schedule has interval 1 and two distinct times
- **THEN** the summary is "times per day" with count 2

#### Scenario: Duplicate times
- **WHEN** an every-N-days schedule has interval 1 and times 08:00, 08:00 and 20:00
- **THEN** the summary is "times per day" with count 2

#### Scenario: Fixed times on some days
- **WHEN** a weekdays schedule has one time on Monday, Wednesday and Friday
- **THEN** the summary is "times per day on days" with count 1 and those three days

#### Scenario: Twice every other day
- **WHEN** an every-N-days schedule has interval 2 and two times
- **THEN** the summary is "times every other day" with count 2

#### Scenario: Once every three days
- **WHEN** an every-N-days schedule has interval 3 and one time
- **THEN** the summary is "times every N days" with count 1 and 3 days

#### Scenario: Every 24 hours collapses to daily
- **WHEN** an every-N-hours schedule has interval 24
- **THEN** the summary is "times per day" with count 1

#### Scenario: Every 8 hours
- **WHEN** an every-N-hours schedule has interval 8
- **THEN** the summary is "every N hours" with 8

#### Scenario: Medication with no schedules
- **WHEN** a medication has an empty schedule list
- **THEN** its summaries are a single "as needed"

#### Scenario: Medication with two schedules
- **WHEN** a medication has an every-12-hours schedule and a weekdays schedule
- **THEN** its summaries are, in order, "every N hours" with 12 and "times per day on days"

### Requirement: Human-readable schedule description
The UI layer SHALL map every `ScheduleSummary` together with an amount to a description from string resources. The amount MUST be formatted with the locale's decimal format, up to three fraction digits, and a unit label with plural support. Counts of one and two MUST use the words "once" and "twice"; higher counts MUST use a numbered form. Day names MUST be the locale's short weekday names ordered from the locale's first day of the week. Monday to Friday exactly MUST be described as weekdays rather than listed.

#### Scenario: Once a day
- **WHEN** the summary is "times per day" with count 1 and the amount is 40 mg
- **THEN** the description is "40 mg once a day"

#### Scenario: Twice a day
- **WHEN** the summary is "times per day" with count 2 and the amount is 1 tablet
- **THEN** the description is "1 tablet twice a day"

#### Scenario: Three times a day
- **WHEN** the summary is "times per day" with count 3 and the amount is 2 tablets
- **THEN** the description is "2 tablets 3 times a day"

#### Scenario: Decimal amount
- **WHEN** the amount is 2.5 ml and the summary is "times per day" with count 1, in an English locale
- **THEN** the description is "2.5 ml once a day"

#### Scenario: Once on specific days
- **WHEN** the summary is "times per day on days" with count 1 on Monday, Wednesday and Friday, amount 40 mg, in an English locale
- **THEN** the description is "40 mg once a day on Mon, Wed, Fri"

#### Scenario: Weekdays
- **WHEN** the summary is "times per day on days" with count 2 on Monday through Friday and amount 40 mg
- **THEN** the description is "40 mg twice a day on weekdays"

#### Scenario: Once every other day
- **WHEN** the summary is "times every other day" with count 1 and amount 40 mg
- **THEN** the description is "40 mg once every other day"

#### Scenario: Twice every three days
- **WHEN** the summary is "times every N days" with count 2 and 3 days and amount 40 mg
- **THEN** the description is "40 mg twice every 3 days"

#### Scenario: Every N hours
- **WHEN** the summary is "every N hours" with 8 and amount 5 ml
- **THEN** the description is "5 ml every 8 hours"

#### Scenario: As needed
- **WHEN** the summary is "as needed" and the amount is the medication's default dose of 40 mg
- **THEN** the description is "40 mg as needed"

### Requirement: Medication repository contract
The domain layer SHALL define a `MedicationRepository` interface that exposes all medications, active and inactive, as a reactive stream that re-emits whenever any medication or schedule changes, and a suspending operation that adds a new medication with its schedules and returns its identifier. The app SHALL be wired with the Room-backed implementation; an in-memory implementation SHALL remain available for tests and previews.

#### Scenario: Stream re-emits on change
- **WHEN** a medication is added through the repository while a collector is active
- **THEN** the collector receives a new list containing the added medication with its schedules

#### Scenario: Add returns an identifier
- **WHEN** a new medication is added
- **THEN** the returned identifier matches the identifier of that medication in the next emission

#### Scenario: Empty start
- **WHEN** the app starts on a device with no saved medications
- **THEN** the first emission is an empty list

## ADDED Requirements

### Requirement: Dose quantity
The domain layer SHALL define a `Quantity` as an exact decimal value greater than zero together with a `DoseUnit` from: milligram, gram, microgram, millilitre, tablet, capsule, drop, puff, unit. Equality MUST be by numeric value and unit.

#### Scenario: Valid quantity
- **WHEN** a quantity of 2.5 ml is created
- **THEN** it holds exactly 2.5 and the millilitre unit

#### Scenario: Zero rejected
- **WHEN** a quantity of 0 mg is created
- **THEN** construction fails with an argument error

#### Scenario: Numeric equality
- **WHEN** quantities 1 tablet and 1.0 tablet are compared
- **THEN** they are equal

### Requirement: Prescriber
The domain layer SHALL define a `Prescriber` with the values: general practitioner, specialist, pharmacist, self, other.

#### Scenario: Prescriber recorded
- **WHEN** a medication is created with prescriber Self
- **THEN** reading the medication reports Self
