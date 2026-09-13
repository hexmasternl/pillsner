## REMOVED Requirements

### Requirement: Schema version 1
**Reason**: Superseded by the "Schema history" requirement, which records every version so the spec does not need rewriting on each schema change.
**Migration**: The version 1 constraints (medications and schedules tables, cascade delete, exported schema, no destructive fallback in release) are carried forward verbatim into "Schema history".

## ADDED Requirements

### Requirement: Schema history
The database SHALL be at the latest version listed here. Every version's schema MUST be exported to the repository and every step MUST have a migration. Destructive migration fallback MUST NOT be enabled in release builds.

Version 1: `medications` and `schedules` tables related one-to-many with cascade delete.

Version 2: adds a `doses` table with a nullable medication reference that is set to null when the medication is deleted, snapshot columns for name and amount, the scheduled moment, optional outcome and recorded moment, optional snooze-until and first-reminded moments, a unique index on medication and scheduled moment, and an index on the scheduled moment.

#### Scenario: Schema exports present
- **WHEN** the project is built
- **THEN** schema JSON files for versions 1 and 2 exist under the app module's schemas directory and are checked in

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

## MODIFIED Requirements

### Requirement: Migration test harness
The project SHALL include Room migration tests that validate the exported version 1 schema and migrate a seeded version 1 database to version 2, so that every later version adds a migration and a test to the same harness.

#### Scenario: Version 1 validates
- **WHEN** the migration test creates the database at version 1 using the exported schema and opens it with the current database class through all migrations
- **THEN** validation passes

#### Scenario: Migrate 1 to 2 keeps data
- **WHEN** the migration test creates a version 1 database with one medication and one schedule row and runs the migration to version 2
- **THEN** validation against the exported version 2 schema passes, the medication and schedule rows are unchanged and the doses table is empty
