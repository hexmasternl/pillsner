## MODIFIED Requirements

### Requirement: Schema history
The database SHALL be at the latest version listed here. Every version's schema MUST be exported to the repository and every step MUST have a migration. Destructive migration fallback MUST NOT be enabled in release builds.

Version 1: `medications` and `schedules` tables related one-to-many with cascade delete.

Version 2: adds a `doses` table with a nullable medication reference that is set to null when the medication is deleted, snapshot columns for name and amount, the scheduled moment, optional outcome and recorded moment, optional snooze-until and first-reminded moments, a unique index on medication and scheduled moment, and an index on the scheduled moment.

Version 3: adds `last_reminded_at` (nullable) and `reminder_count` (default 0) to `doses`, so a repeating reminder can remember when it last posted and how often it has asked again since.

Version 4: adds `planned_at` (default 0, backfilled from `scheduled_at` on migration) to `doses`, the moment a dose row was first stored, used to tell a reminder the platform did not deliver from a dose that never had a chance to be announced.

Version 5: adds a composite index on the `doses` table's outcome and scheduled moment columns, so a query for pending doses (outcome not yet recorded) ordered by scheduled moment stays a single index scan as the table grows. No column or data change.

Version 6: adds a `stock_batches` table (medication reference with cascade delete, a remaining-amount value column, a remaining-amount unit column, an expiry date column, an added-at moment column, an index on medication and expiry date for first-expiry-first-out queries) and a nullable `low_stock_acknowledgement` column on `medications` (default `NULL`), owned by the `medicine-stock-tracking` capability.

#### Scenario: Schema exports present
- **WHEN** the project is built
- **THEN** schema JSON files for versions 1 through 6 exist under the app module's schemas directory and are checked in

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

#### Scenario: Migrate 5 to 6 keeps data
- **WHEN** the migration test creates a version 5 database with a medication, a schedule and a dose row and runs the migration to version 6
- **THEN** validation against the exported version 6 schema passes, the medication, schedule and dose rows are unchanged, the medication's new `low_stock_acknowledgement` column is `NULL`, and the `stock_batches` table exists and is empty

#### Scenario: Stock batch cascade delete
- **WHEN** a medication row with stock batch rows is deleted directly through the DAO
- **THEN** its stock batch rows are deleted as well

#### Scenario: Stock batch round-trip
- **WHEN** a stock batch of 30 tablets expiring 1 June is saved for a medicine and read back
- **THEN** its remaining amount is exactly 30 tablets and its expiry date is 1 June
