## ADDED Requirements

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
No production code path SHALL delete a row from the `medications` table. The medication DAO MUST NOT declare a delete operation for medication entities nor a query that deletes from `medications`, and the repository MUST NOT expose a removal operation. The cascade delete declared on `schedules` exists so that schedules can be replaced; it MUST NOT be used to remove a medicine. A guard test SHALL fail the build when a medication delete is added to the DAO. Dose history of every medication SHALL therefore remain attached to an existing medication row.

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
