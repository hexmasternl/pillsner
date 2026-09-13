## MODIFIED Requirements

### Requirement: Medication repository contract
The domain layer SHALL define a `MedicationRepository` interface that exposes all medications, active and inactive, as a reactive stream that re-emits whenever any medication or schedule changes, a suspending operation that returns one medication with its schedules by identifier or nothing when the identifier is unknown, a suspending operation that adds a new medication with its schedules and returns its identifier, a suspending operation that updates an existing medication by replacing every field and its whole schedule list atomically and fails for an unknown identifier, and a suspending operation that sets the active flag of one medication by identifier. Setting the flag for an unknown identifier SHALL do nothing. The contract MUST NOT expose any operation that removes a medication; medicines are never deleted. The app SHALL be wired with the Room-backed implementation; an in-memory implementation SHALL remain available for tests and previews and SHALL implement every operation.

#### Scenario: Stream re-emits on change
- **WHEN** a medication is added through the repository while a collector is active
- **THEN** the collector receives a new list containing the added medication with its schedules

#### Scenario: Add returns an identifier
- **WHEN** a new medication is added
- **THEN** the returned identifier matches the identifier of that medication in the next emission

#### Scenario: Empty start
- **WHEN** the app starts on a device with no saved medications
- **THEN** the first emission is an empty list

#### Scenario: Get by identifier
- **WHEN** a medication with two schedules was added and its identifier is passed to the get operation
- **THEN** the returned medication has the same name, default dose, dates, prescriber, active flag and both schedules in order

#### Scenario: Get unknown identifier
- **WHEN** the get operation is called with an identifier no medication has
- **THEN** it returns nothing and raises no error

#### Scenario: Update replaces fields and schedules
- **WHEN** a medication with schedules A and B is updated with a new name, a new default dose and schedules C only, while a collector is active
- **THEN** the collector receives a new list in which that medication, under the same identifier, has the new name, the new dose and exactly schedule C, and no additional medication exists

#### Scenario: Update changes the active flag
- **WHEN** an active medication is updated with its active flag set to false
- **THEN** the next emission lists it as inactive

#### Scenario: Update unknown identifier
- **WHEN** the update operation is called with a medication whose identifier no stored medication has
- **THEN** it fails with an error and the collector receives no changed list

#### Scenario: Set active re-emits
- **WHEN** an active medication's flag is set to false through the repository while a collector is active
- **THEN** the collector receives a new list in which that medication is inactive and every other field, including its schedules, is unchanged

#### Scenario: Set active back
- **WHEN** an inactive medication's flag is set to true through the repository
- **THEN** the next emission lists it as active

#### Scenario: Unknown identifier
- **WHEN** the active flag is set for an identifier that no medication has
- **THEN** no error is raised and the collector receives no changed list

#### Scenario: No removal operation
- **WHEN** the repository interface is inspected
- **THEN** it declares no operation that deletes or removes a medication

#### Scenario: In-memory implementation
- **WHEN** the in-memory repository holds one active medication and its flag is set to false
- **THEN** its next emission lists that medication as inactive

#### Scenario: In-memory update
- **WHEN** the in-memory repository holds one medication and it is updated with a new name
- **THEN** its next emission lists exactly one medication, with the new name and the same identifier
