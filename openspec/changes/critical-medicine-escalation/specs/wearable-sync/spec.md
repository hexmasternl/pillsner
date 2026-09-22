## MODIFIED Requirements

### Requirement: Shared sync contract
The phone and watch apps SHALL share one contract module defining the data path, the payload types and their JSON serialisation. The payload SHALL carry a version, the phone app's language tag, the publish moment, and the list of pending doses, each with a dose identifier, the medicine name, the amount pre-formatted by the phone, the scheduled moment as an instant, and whether the medicine is flagged critical. The contract module MUST NOT depend on Android.

#### Scenario: Round trip
- **WHEN** a payload with two doses is serialised and deserialised
- **THEN** every field is equal to the original

#### Scenario: Unknown field tolerated
- **WHEN** the watch receives a payload containing a field it does not know
- **THEN** it deserialises the known fields and ignores the unknown one

#### Scenario: Higher major version refused
- **WHEN** the watch receives a payload with a version higher than it supports
- **THEN** it treats the payload as absent and shows the sync footer

#### Scenario: Critical flag round trip
- **WHEN** a payload with one dose whose medicine is flagged critical is serialised and deserialised
- **THEN** the deserialised dose is still marked critical

#### Scenario: Missing critical field defaults to not critical
- **WHEN** the watch receives a payload from an older phone app version that carries no critical field
- **THEN** every dose deserialises as not critical

### Requirement: Phone publishes pending doses
The phone app SHALL publish every pending dose in its materialised window, ordered by scheduled time, as one data item at the contract path. It SHALL publish at app start and whenever the set of pending doses changes, debounced so a burst of changes produces one item. Each amount MUST be formatted with the phone's quantity formatter in the phone app's language. Each dose MUST carry whether its medicine is flagged critical. Lapsed, taken and skipped doses MUST NOT be included.

#### Scenario: Dose taken
- **WHEN** a pending dose is recorded as taken
- **THEN** the phone publishes a new item without that dose

#### Scenario: New medicine
- **WHEN** a medicine with a schedule is saved
- **THEN** the phone publishes an item containing its planned doses

#### Scenario: Burst of changes
- **WHEN** the daily refresh inserts several doses within a few milliseconds
- **THEN** exactly one item is published for the burst

#### Scenario: Formatted amount
- **WHEN** the phone app language is Dutch and a dose of 2 tablets is published
- **THEN** the amount text in the payload reads "2 tabletten"

#### Scenario: Critical dose published
- **WHEN** a pending dose belongs to a medicine flagged critical
- **THEN** the published item marks that dose critical

#### Scenario: Medicine unflagged critical
- **WHEN** the user turns off the critical toggle for a medicine with pending doses
- **THEN** the phone publishes a new item in which those doses are no longer marked critical
