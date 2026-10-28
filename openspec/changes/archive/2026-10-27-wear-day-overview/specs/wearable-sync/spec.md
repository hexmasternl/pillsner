## MODIFIED Requirements

### Requirement: Shared sync contract
The phone and watch apps SHALL share one contract module defining the data path, the payload types and their JSON serialisation. The payload SHALL carry a version, the phone app's language tag, the publish moment, and the list of pending doses, each with a dose identifier, the medicine name, the amount pre-formatted by the phone, the scheduled moment as an instant, and optionally the details of the medicine behind it: its default dose, a line per schedule and its stock, all pre-formatted by the phone. The details and every field within them SHALL be optional with a default, so that a payload carrying them stays readable by a watch build that predates them and the payload version does not change. The contract module MUST NOT depend on Android.

#### Scenario: Round trip
- **WHEN** a payload with two doses, each carrying medicine details, is serialised and deserialised
- **THEN** every field is equal to the original

#### Scenario: Unknown field tolerated
- **WHEN** the watch receives a payload containing a field it does not know
- **THEN** it deserialises the known fields and ignores the unknown one

#### Scenario: Payload from a phone that sends no details
- **WHEN** the watch receives a payload whose doses carry no medicine details
- **THEN** it deserialises the doses and treats their details as absent

#### Scenario: Higher major version refused
- **WHEN** the watch receives a payload with a version higher than it supports
- **THEN** it treats the payload as absent and shows the sync footer

### Requirement: Phone publishes pending doses
The phone app SHALL publish every pending dose in its materialised window, ordered by scheduled time, as one data item at the contract path. It SHALL publish at app start and whenever the set of pending doses changes, debounced so a burst of changes produces one item. Each amount MUST be formatted with the phone's quantity formatter in the phone app's language. With each dose the phone SHALL publish the details of its medicine — the default dose, one line per schedule, and the remaining stock totalled in the medicine's dose unit — formatted with the same formatters the phone's own screens use and in the phone app's language. The stock line MUST be omitted for a medicine that records no stock, and the details as a whole MUST be omitted when the medicine no longer exists, without dropping the dose. Each medicine MUST be resolved at most once per publication. Lapsed, taken and skipped doses MUST NOT be included.

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

#### Scenario: Details travel with the dose
- **WHEN** a medicine with a default dose of 400 mg, a twice-daily schedule and stock is published
- **THEN** each of its doses carries the default dose, the schedule line and the stock line as text

#### Scenario: One lookup per medicine
- **WHEN** a medicine has three pending doses in the window
- **THEN** the phone resolves that medicine once for the publication and all three doses carry the same details

#### Scenario: Medicine without stock
- **WHEN** the medicine behind a published dose records no stock
- **THEN** its details carry no stock line

#### Scenario: Medicine no longer exists
- **WHEN** a pending dose's medicine cannot be found
- **THEN** the dose is still published, without details

### Requirement: Language change republishes
When the phone app's language changes, the phone SHALL publish a new item with the new language tag, and amounts and medicine details re-formatted, even if the dose list is unchanged.

#### Scenario: Language switch
- **WHEN** the user changes the phone app language and restarts it
- **THEN** the next published item carries the new language tag

#### Scenario: Details follow the language
- **WHEN** the phone app language is Dutch
- **THEN** the schedule and stock lines in the payload are written in Dutch
