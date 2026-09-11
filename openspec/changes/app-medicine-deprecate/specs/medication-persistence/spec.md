## ADDED Requirements

### Requirement: Active flag is updated in place
The Room-backed repository SHALL set a medication's active flag with a single update statement on the `medications` row identified by its id. The statement MUST NOT touch the `schedules` or `doses` tables and MUST NOT require a schema change: the `is_active` column has existed since version 1. Observers of the medication stream SHALL receive a new emission after the update. Updating an id that does not exist SHALL affect no rows and raise no error.

#### Scenario: Deactivate persists across restart
- **WHEN** the user deactivates a medicine, then force-stops and relaunches the app
- **THEN** the Medicines screen lists that medicine in the inactive section

#### Scenario: Schedules untouched
- **WHEN** a medication with three schedules has its active flag set to false through the DAO
- **THEN** reading it back returns the same three schedules in the same order with the same amounts and times

#### Scenario: Doses untouched by the write itself
- **WHEN** a medication with dose rows has its active flag set to false through the DAO
- **THEN** the dose rows are unchanged by that statement

#### Scenario: Stream emits after update
- **WHEN** a collector observes all medications and one medication's active flag is updated through the repository
- **THEN** the collector receives a new list reflecting the new flag

#### Scenario: Unknown id
- **WHEN** the update runs for an id with no medication row
- **THEN** zero rows are affected and no exception is thrown

#### Scenario: No new schema version
- **WHEN** the project is built after this change
- **THEN** the database version and the set of exported schema files are the same as before the change
