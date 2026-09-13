# dose-records Specification

## Purpose
TBD - created by archiving change app-medicine-alarm. Update Purpose after archive.
## Requirements
### Requirement: Dose record model
The domain layer SHALL define a `Dose` with an identifier, an optional medication identifier, a snapshot of the medicine name, a snapshot of the amount as a `Quantity`, the scheduled moment as an instant, an optional `Intake`, an optional snooze-until moment and an optional first-reminded moment. An `Intake` SHALL carry an outcome of taken, skipped or missed and the moment it was recorded; for taken, that moment is when the medicine was actually taken. A dose without an intake is pending. Domain types MUST NOT depend on Android framework classes.

#### Scenario: Pending dose
- **WHEN** a dose has no intake
- **THEN** it is pending and has no outcome

#### Scenario: Taken dose records the time taken
- **WHEN** a dose scheduled at 08:00 is recorded as taken at 08:12
- **THEN** its intake outcome is taken and its recorded moment is 08:12

#### Scenario: Skipped is distinct from missed
- **WHEN** one dose is recorded as skipped and another lapses as missed
- **THEN** the two doses have different outcomes and neither is pending

### Requirement: Snapshots protect history
A dose SHALL store the medicine name and amount as they were when the dose was generated. Changing or deleting the medicine afterwards MUST NOT change existing doses.

#### Scenario: Medicine renamed
- **WHEN** a dose was generated for "Ibuprofen 400" and the medicine is later renamed "Ibuprofen"
- **THEN** the existing dose still reads "Ibuprofen 400"

#### Scenario: Medicine deleted
- **WHEN** a medicine row is deleted from the database
- **THEN** its doses remain with their name and amount and their medication identifier becomes absent

### Requirement: Dose generation from schedules
The domain layer SHALL provide a generator that, for a medication and a range of local dates in a given time zone, produces the planned doses of every schedule the medication has. Generation MUST skip inactive medications, dates before used-since and dates after use-until. Two schedules producing the same moment MUST yield one dose. Results MUST be ordered by scheduled moment.

#### Scenario: Every day, two times
- **WHEN** a medication used since 1 March has a schedule every 1 day at 08:00 and 20:00, and the window is 10 March to 11 March
- **THEN** four doses are produced: 10 March 08:00, 10 March 20:00, 11 March 08:00, 11 March 20:00

#### Scenario: Every other day anchored on used-since
- **WHEN** a medication used since Monday 2 March has a schedule every 2 days at 09:00 and the window is 2 March to 5 March
- **THEN** doses are produced on 2 March and 4 March only

#### Scenario: Weekdays
- **WHEN** a medication has a schedule on Monday, Wednesday and Friday at 08:00 and the window is Monday to Sunday
- **THEN** three doses are produced, on Monday, Wednesday and Friday

#### Scenario: Every 8 hours from 06:00
- **WHEN** a medication has a schedule every 8 hours from 06:00 and the window is one day
- **THEN** doses are produced at 06:00, 14:00 and 22:00 and none the next day from this window

#### Scenario: Every 12 hours from 20:00 does not spill into the next day
- **WHEN** a medication has a schedule every 12 hours from 20:00 and the window is one day
- **THEN** exactly one dose at 20:00 is produced for that day

#### Scenario: Inactive medication
- **WHEN** a medication is inactive
- **THEN** no doses are produced for it

#### Scenario: Before used-since and after use-until
- **WHEN** a medication is used since 10 March until 12 March with a daily 08:00 schedule, and the window is 9 March to 13 March
- **THEN** doses are produced on 10, 11 and 12 March only

#### Scenario: Two schedules coincide
- **WHEN** a medication has one schedule every day at 08:00 and another on Mondays at 08:00, and the window includes a Monday
- **THEN** one dose is produced for that Monday at 08:00

#### Scenario: Month boundary
- **WHEN** a medication used since 30 January has a schedule every 2 days at 08:00 and the window is 31 January to 2 February
- **THEN** a dose is produced on 1 February only

#### Scenario: Leap day
- **WHEN** a medication has a daily schedule at 08:00 and the window is 28 February to 1 March of a leap year
- **THEN** three doses are produced, including 29 February

#### Scenario: Midnight time
- **WHEN** a medication has a daily schedule at 00:00 and the window is one day
- **THEN** exactly one dose is produced at 00:00 of that day, not at the end of it

### Requirement: Daylight-saving and time zone handling
Scheduled moments SHALL be derived from wall-clock times in the device time zone. On a spring-forward gap the moment MUST be moved later by the length of the gap. On an autumn overlap the earlier occurrence MUST be used. Planned doses that have not yet been reminded MUST be regenerated when the time zone changes so they keep their wall-clock time in the new zone.

#### Scenario: Spring-forward gap
- **WHEN** a schedule is daily at 02:30 in a zone where 02:00 to 03:00 does not exist on the transition day
- **THEN** the dose for that day is at 03:30 local time and the doses on the surrounding days are at 02:30

#### Scenario: Autumn overlap
- **WHEN** a schedule is daily at 02:30 in a zone where 02:00 to 03:00 occurs twice on the transition day
- **THEN** exactly one dose is produced for that day, at the first 02:30

#### Scenario: Time zone move
- **WHEN** an un-reminded planned dose exists at 08:00 Amsterdam time and the device zone changes to New York
- **THEN** after refresh the planned dose is at 08:00 New York time

#### Scenario: Reminded dose keeps its moment
- **WHEN** a dose has already been reminded and the device zone changes
- **THEN** that dose's scheduled instant is unchanged

### Requirement: Rolling two-day window
The app SHALL keep planned doses materialised for today and tomorrow in the device time zone. Refreshing the window SHALL add missing planned doses, remove pending un-reminded doses that no longer match any schedule, and never alter doses that have an intake or have been reminded. Refresh SHALL run at app start, whenever the medication list changes, on every reminder wake, and once per day shortly after midnight.

#### Scenario: New medicine appears
- **WHEN** a medicine with a daily 08:00 schedule is saved at 12:00
- **THEN** planned doses exist for tomorrow 08:00, and for today 08:00 as a pending dose that is already due

#### Scenario: Idempotent refresh
- **WHEN** refresh runs twice without any change
- **THEN** the set of doses is identical after both runs

#### Scenario: Schedule removed
- **WHEN** a medicine's only schedule is removed and refresh runs
- **THEN** its pending un-reminded doses in the window are deleted and its taken, skipped, missed and reminded doses remain

#### Scenario: Medicine deactivated
- **WHEN** a medicine becomes inactive and refresh runs
- **THEN** its pending un-reminded doses in the window are deleted

#### Scenario: Day rollover
- **WHEN** the clock passes midnight and the daily refresh runs
- **THEN** planned doses for the new tomorrow exist

### Requirement: Missed rule
A pending dose SHALL become missed at the earlier of 24 hours after its scheduled moment and the scheduled moment of the next dose of the same medicine. Marking a dose missed SHALL clear any snooze and remove its notification. A skipped dose MUST NOT become missed.

#### Scenario: Superseded by next dose
- **WHEN** a dose at 08:00 is unanswered and the same medicine's next dose is at 20:00
- **THEN** at 20:00 the 08:00 dose becomes missed

#### Scenario: 24-hour cap
- **WHEN** a dose at 08:00 Monday is unanswered and the same medicine's next dose is Wednesday 08:00
- **THEN** at 08:00 Tuesday the Monday dose becomes missed

#### Scenario: Skipped stays skipped
- **WHEN** a dose was recorded as skipped and 24 hours pass
- **THEN** its outcome is still skipped

#### Scenario: Snoozed dose lapses
- **WHEN** a snoozed dose reaches its lapse moment before the snooze ends
- **THEN** it becomes missed and no further reminder is shown for it

### Requirement: Dose repository contract
The domain layer SHALL define a `DoseRepository` that can observe pending doses, observe upcoming doses with a limit, list doses due at a moment, insert planned doses ignoring duplicates, delete pending un-reminded doses in a window that are not in a keep set, record an intake, set or clear a snooze, set the first-reminded moment, and find the next scheduled moment of a medicine after a given moment.

#### Scenario: Duplicate insert ignored
- **WHEN** a planned dose with the same medicine and scheduled moment as an existing dose is inserted
- **THEN** the existing dose is unchanged and no second row is created

#### Scenario: Observe pending emits on change
- **WHEN** a collector observes pending doses and one of them is recorded as taken
- **THEN** the collector receives a list without that dose

