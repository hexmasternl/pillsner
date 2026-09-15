## MODIFIED Requirements

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
