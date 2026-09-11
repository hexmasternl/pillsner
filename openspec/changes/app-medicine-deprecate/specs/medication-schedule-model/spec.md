## MODIFIED Requirements

### Requirement: Medication repository contract
The domain layer SHALL define a `MedicationRepository` interface that exposes all medications, active and inactive, as a reactive stream that re-emits whenever any medication or schedule changes, a suspending operation that adds a new medication with its schedules and returns its identifier, and a suspending operation that sets the active flag of one medication by identifier. Setting the flag for an unknown identifier SHALL do nothing. The app SHALL be wired with the Room-backed implementation; an in-memory implementation SHALL remain available for tests and previews and SHALL implement every operation.

#### Scenario: Stream re-emits on change
- **WHEN** a medication is added through the repository while a collector is active
- **THEN** the collector receives a new list containing the added medication with its schedules

#### Scenario: Add returns an identifier
- **WHEN** a new medication is added
- **THEN** the returned identifier matches the identifier of that medication in the next emission

#### Scenario: Empty start
- **WHEN** the app starts on a device with no saved medications
- **THEN** the first emission is an empty list

#### Scenario: Set active re-emits
- **WHEN** an active medication's flag is set to false through the repository while a collector is active
- **THEN** the collector receives a new list in which that medication is inactive and every other field, including its schedules, is unchanged

#### Scenario: Set active back
- **WHEN** an inactive medication's flag is set to true through the repository
- **THEN** the next emission lists it as active

#### Scenario: Unknown identifier
- **WHEN** the active flag is set for an identifier that no medication has
- **THEN** no error is raised and the collector receives no changed list

#### Scenario: In-memory implementation
- **WHEN** the in-memory repository holds one active medication and its flag is set to false
- **THEN** its next emission lists that medication as inactive
