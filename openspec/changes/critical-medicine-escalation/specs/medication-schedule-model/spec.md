## MODIFIED Requirements

### Requirement: Medication domain model
The domain layer SHALL define a `Medication` with an identifier, a non-blank name, a default dose (`Quantity`), a used-since date, an optional use-until date that is not before used-since, a prescriber, an ordered list of zero or more `Schedule`s, an active flag, and a critical flag defaulting to false. An empty schedule list means the medication is taken as needed. Domain model types MUST NOT depend on Android framework classes.

#### Scenario: Active flag meaning
- **WHEN** a medication has its active flag set to false
- **THEN** it is treated as inactive by every consumer: the overview lists it under Inactive and no doses are expected from it

#### Scenario: No Android imports
- **WHEN** the domain model source files are inspected
- **THEN** none of them import `android.*` or `androidx.*` classes

#### Scenario: Multiple schedules
- **WHEN** a medication is created with two schedules
- **THEN** both are retained in the order given

#### Scenario: Empty schedules means as needed
- **WHEN** a medication is created with no schedules
- **THEN** it is valid and its schedule summary is "as needed"

#### Scenario: Use until before used since
- **WHEN** a medication is created with use until earlier than used since
- **THEN** construction fails with an argument error

#### Scenario: Blank name
- **WHEN** a medication is created with a blank name
- **THEN** construction fails with an argument error

#### Scenario: Critical defaults to false
- **WHEN** a medication is created without specifying the critical flag
- **THEN** it is not critical

#### Scenario: Critical flag retained
- **WHEN** a medication is created with the critical flag set to true
- **THEN** reading the medication reports it as critical
