## ADDED Requirements

### Requirement: Medications are stored on the device with Room
The app SHALL persist medications and their schedules in a Room database stored only on the device. No medication data MUST leave the device through this capability.

#### Scenario: Saved medicine survives restart
- **WHEN** the user saves a medicine and then force-stops and relaunches the app
- **THEN** the Medicines screen lists that medicine with its schedules

#### Scenario: No network use
- **WHEN** the app manifest and dependencies are inspected
- **THEN** no network permission and no network library were added by this capability

### Requirement: Schema version 1
The database SHALL be at schema version 1 with a `medications` table and a `schedules` table related one-to-many with cascade delete. The schema MUST be exported to the repository. Destructive migration fallback MUST NOT be enabled in release builds.

#### Scenario: Schema export present
- **WHEN** the project is built
- **THEN** a version 1 schema JSON exists under the app module's schemas directory and is checked in

#### Scenario: Cascade delete
- **WHEN** a medication row is deleted directly through the DAO
- **THEN** its schedule rows are deleted as well

#### Scenario: No destructive fallback in release
- **WHEN** the release database builder configuration is inspected
- **THEN** it does not call any destructive migration fallback

### Requirement: Migration test harness
The project SHALL include a Room migration test that validates the exported version 1 schema, so that every later version adds a migration and a test to the same harness.

#### Scenario: Version 1 validates
- **WHEN** the migration test creates the database at version 1 using the exported schema and opens it with the current database class
- **THEN** validation passes

### Requirement: Round-trip fidelity
Every schedule shape and every field of a medication SHALL round-trip through the database without loss: decimal amounts stay exact, dates and times stay wall-clock values, schedule order is preserved.

#### Scenario: Decimal amount
- **WHEN** a medication with default dose 2.5 ml is saved and read back
- **THEN** the default dose is exactly 2.5 ml

#### Scenario: Every N days with times
- **WHEN** a schedule of 40 mg every 2 days at 08:00 and 20:00 is saved and read back
- **THEN** the schedule is every 2 days with times 08:00 and 20:00 and amount 40 mg

#### Scenario: Weekdays
- **WHEN** a schedule on Monday, Wednesday and Friday at 08:00 is saved and read back
- **THEN** the schedule has exactly those three days and that time

#### Scenario: Every N hours
- **WHEN** a schedule of every 12 hours from 08:00 is saved and read back
- **THEN** the schedule has interval 12 and first dose time 08:00

#### Scenario: Schedule order
- **WHEN** a medication with three schedules is saved
- **THEN** reading it back returns the schedules in the same order

#### Scenario: Optional end date
- **WHEN** a medication with no use until date is saved and read back
- **THEN** use until is absent

### Requirement: Atomic save
Saving a medication with its schedules SHALL happen in one transaction.

#### Scenario: Schedule insert fails
- **WHEN** inserting one of the schedule rows fails during a save
- **THEN** no medication row from that save remains in the database

### Requirement: Repository emits on change
The Room-backed repository SHALL emit the full medication list whenever a medication or schedule row changes.

#### Scenario: Add while observing
- **WHEN** a collector observes all medications and a new medication is saved
- **THEN** the collector receives a new list containing the new medication with its schedules

### Requirement: Corrupt rows fail loudly
Reading a schedule row whose columns do not satisfy its kind's invariants SHALL fail with a descriptive error rather than produce a partially valid schedule.

#### Scenario: Missing times on an every-N-days row
- **WHEN** a schedule row of kind every-N-days has a null times column
- **THEN** mapping it to the domain fails with an error naming the row's kind and id
