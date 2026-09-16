# medication-persistence Specification

## Purpose
TBD - created by archiving change app-medicine-add. Update Purpose after archive.
## Requirements
### Requirement: Medications are stored on the device with Room
The app SHALL persist medications and their schedules in a Room database stored only on the device. No medication data MUST leave the device through this capability.

#### Scenario: Saved medicine survives restart
- **WHEN** the user saves a medicine and then force-stops and relaunches the app
- **THEN** the Medicines screen lists that medicine with its schedules

#### Scenario: No network use
- **WHEN** the app manifest and dependencies are inspected
- **THEN** no network permission and no network library were added by this capability

### Requirement: Migration test harness
The project SHALL include Room migration tests that validate the exported version 1 schema and migrate a seeded version 1 database to version 2, so that every later version adds a migration and a test to the same harness.

#### Scenario: Version 1 validates
- **WHEN** the migration test creates the database at version 1 using the exported schema and opens it with the current database class through all migrations
- **THEN** validation passes

#### Scenario: Migrate 1 to 2 keeps data
- **WHEN** the migration test creates a version 1 database with one medication and one schedule row and runs the migration to version 2
- **THEN** validation against the exported version 2 schema passes, the medication and schedule rows are unchanged and the doses table is empty

#### Scenario: Migrate 2 to 3 keeps data
- **WHEN** the migration test creates a version 2 database with a dose row and runs the migration to version 3
- **THEN** validation against the exported version 3 schema passes and the dose row is unchanged except that its new repeat columns take their default values

#### Scenario: Migrate 3 to 4 keeps data
- **WHEN** the migration test creates a version 3 database with a dose row and runs the migration to version 4
- **THEN** validation against the exported version 4 schema passes and the dose row's new `planned_at` column equals its `scheduled_at` value

#### Scenario: Migrate 4 to 5 keeps data
- **WHEN** the migration test creates a version 4 database with a medication, a schedule and a dose row and runs the migration to version 5
- **THEN** validation against the exported version 5 schema passes and the medication, schedule and dose rows are unchanged

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

### Requirement: Schema history
The database SHALL be at the latest version listed here. Every version's schema MUST be exported to the repository and every step MUST have a migration. Destructive migration fallback MUST NOT be enabled in release builds.

Version 1: `medications` and `schedules` tables related one-to-many with cascade delete.

Version 2: adds a `doses` table with a nullable medication reference that is set to null when the medication is deleted, snapshot columns for name and amount, the scheduled moment, optional outcome and recorded moment, optional snooze-until and first-reminded moments, a unique index on medication and scheduled moment, and an index on the scheduled moment.

Version 3: adds `last_reminded_at` (nullable) and `reminder_count` (default 0) to `doses`, so a repeating reminder can remember when it last posted and how often it has asked again since.

Version 4: adds `planned_at` (default 0, backfilled from `scheduled_at` on migration) to `doses`, the moment a dose row was first stored, used to tell a reminder the platform did not deliver from a dose that never had a chance to be announced.

Version 5: adds a composite index on the `doses` table's outcome and scheduled moment columns, so a query for pending doses (outcome not yet recorded) ordered by scheduled moment stays a single index scan as the table grows. No column or data change.

#### Scenario: Schema exports present
- **WHEN** the project is built
- **THEN** schema JSON files for versions 1 through 5 exist under the app module's schemas directory and are checked in

#### Scenario: Cascade delete of schedules
- **WHEN** a medication row is deleted directly through the DAO
- **THEN** its schedule rows are deleted as well

#### Scenario: Doses survive medication deletion
- **WHEN** a medication row is deleted directly through the DAO
- **THEN** its dose rows remain and their medication reference is null

#### Scenario: Duplicate planned dose rejected
- **WHEN** two dose rows with the same medication and scheduled moment are inserted
- **THEN** the second insert is ignored and one row remains

#### Scenario: No destructive fallback in release
- **WHEN** the release database builder configuration is inspected
- **THEN** it does not call any destructive migration fallback

#### Scenario: Pending query uses the composite index
- **WHEN** the query plan for `SELECT * FROM doses WHERE outcome IS NULL ORDER BY scheduled_at ASC` is inspected on a version 5 database
- **THEN** it uses the composite index on `(outcome, scheduled_at)` rather than a full table scan

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

### Requirement: Medication is updated in place with its schedules replaced
The Room-backed repository SHALL update an existing medication in a single transaction that updates the `medications` row identified by its id and replaces all of that medication's `schedules` rows with the new list, preserving the new list's order. The transaction MUST NOT touch the `doses` table. Observers of the medication stream SHALL receive exactly one new emission after the transaction. If the `medications` row does not exist, or any schedule insert fails, the transaction SHALL roll back completely and the error SHALL surface to the caller. No schema change is required.

#### Scenario: Fields replaced
- **WHEN** a stored medication is updated with a new name, default dose, dates and prescriber
- **THEN** reading it back by id returns the new values under the same id

#### Scenario: Schedules replaced in order
- **WHEN** a medication with schedules A, B is updated with schedules C, D, E
- **THEN** reading it back returns exactly C, D, E in that order and no row for A or B remains in `schedules`

#### Scenario: Schedules cleared
- **WHEN** a medication with two schedules is updated with an empty schedule list
- **THEN** reading it back returns no schedules and the medication row still exists

#### Scenario: Doses untouched
- **WHEN** a medication with dose rows, some with intakes and some pending, is updated with new schedules
- **THEN** every dose row is byte-for-byte unchanged by the update transaction

#### Scenario: Single emission
- **WHEN** a collector observes all medications and a medication with three schedules is updated
- **THEN** the collector receives one new list reflecting the finished update, never an intermediate list without schedules

#### Scenario: Unknown id rolls back
- **WHEN** the update transaction runs for an id with no `medications` row
- **THEN** it fails with a descriptive error and no `schedules` row was inserted or deleted

#### Scenario: Schedule insert failure rolls back
- **WHEN** inserting one of the new schedule rows fails during an update
- **THEN** the medication row and its previous schedule rows are unchanged

#### Scenario: Updated medicine survives restart
- **WHEN** the user saves an edit, then force-stops and relaunches the app
- **THEN** the Medicines screen shows the edited values

#### Scenario: No new schema version
- **WHEN** the project is built after this change
- **THEN** the database version and the set of exported schema files are the same as before the change

### Requirement: Medications are never removed
No production code path SHALL delete a row from the `medications` table, with one exception: the user-initiated reset of the entire app described by the `app-reset` capability, which erases every table at once after an explicit, acknowledged confirmation. That reset MUST be performed as a database-wide clear of all tables, not through a delete declared on a DAO. Outside it, the medication DAO MUST NOT declare a delete operation for medication entities nor a query that deletes from `medications`, and the repository MUST NOT expose a removal operation for a single medication. The cascade delete declared on `schedules` exists so that schedules can be replaced; it MUST NOT be used to remove a medicine. A guard test SHALL fail the build when a medication delete is added to the DAO. Dose history of every medication SHALL therefore remain attached to an existing medication row for as long as any medication row exists.

#### Scenario: DAO exposes no medication delete
- **WHEN** the declared operations of the medication DAO are inspected
- **THEN** none is a delete of a medication entity and none is a query that deletes from `medications`

#### Scenario: Guard test catches an added delete
- **WHEN** a delete of a medication entity is added to the medication DAO
- **THEN** the guard unit test fails with a message stating that medicines are never removed

#### Scenario: Schema cascade exercised only by tests
- **WHEN** the production sources are searched for statements that delete from `medications`
- **THEN** none is found; the cascade-delete scenario of the schema is exercised only by raw SQL in tests

#### Scenario: Inactive medicine keeps its history
- **WHEN** a medication with taken, skipped and missed dose rows is deactivated and later edited
- **THEN** all of its dose rows still reference its id

#### Scenario: The reset is the only path that empties the table
- **WHEN** the production sources are searched for every path that removes medication rows
- **THEN** the only one found is the app reset, and it clears every table of the database rather than deleting medications through the DAO

#### Scenario: No single medicine can be removed
- **WHEN** a caller holds one medication and wants it gone
- **THEN** the repository offers no way to remove it; deactivating it is the only option, and its dose history is kept

