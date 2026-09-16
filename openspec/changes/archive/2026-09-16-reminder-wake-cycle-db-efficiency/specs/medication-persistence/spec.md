## MODIFIED Requirements

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
