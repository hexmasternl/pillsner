## ADDED Requirements

### Requirement: Shared sync contract
The phone and watch apps SHALL share one contract module defining the data path, the payload types and their JSON serialisation. The payload SHALL carry a version, the phone app's language tag, the publish moment, and the list of pending doses, each with a dose identifier, the medicine name, the amount pre-formatted by the phone, and the scheduled moment as an instant. The contract module MUST NOT depend on Android.

#### Scenario: Round trip
- **WHEN** a payload with two doses is serialised and deserialised
- **THEN** every field is equal to the original

#### Scenario: Unknown field tolerated
- **WHEN** the watch receives a payload containing a field it does not know
- **THEN** it deserialises the known fields and ignores the unknown one

#### Scenario: Higher major version refused
- **WHEN** the watch receives a payload with a version higher than it supports
- **THEN** it treats the payload as absent and shows the sync footer

### Requirement: Phone publishes pending doses
The phone app SHALL publish every pending dose in its materialised window, ordered by scheduled time, as one data item at the contract path. It SHALL publish at app start and whenever the set of pending doses changes, debounced so a burst of changes produces one item. Each amount MUST be formatted with the phone's quantity formatter in the phone app's language. Lapsed, taken and skipped doses MUST NOT be included.

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

### Requirement: Language change republishes
When the phone app's language changes, the phone SHALL publish a new item with the new language tag and amounts re-formatted, even if the dose list is unchanged.

#### Scenario: Language switch
- **WHEN** the user changes the phone app language and restarts it
- **THEN** the next published item carries the new language tag

### Requirement: Watch reads the persisted item
The watch app SHALL read the last persisted data item when opened, without needing the phone to be connected, and SHALL update while visible when a new item arrives. The watch MUST NOT keep any other copy of the data.

#### Scenario: Open offline
- **WHEN** the watch has received an item earlier and the phone is now disconnected
- **THEN** opening the watch app shows the list from that item

#### Scenario: Live update
- **WHEN** the watch app is open and the phone publishes a new item
- **THEN** the list updates without the user leaving the screen

### Requirement: Transport and privacy
Synchronisation SHALL use the Wearable Data Layer over the direct link between the paired devices. Neither the phone module nor the watch module MUST declare the network permission. The README SHALL disclose the Play services Wearable dependency, what data is sent to the watch and that it does not leave the two devices.

#### Scenario: No network permission
- **WHEN** both manifests are inspected
- **THEN** neither declares the internet permission

#### Scenario: Disclosure
- **WHEN** the README is read
- **THEN** it names the Play services Wearable dependency and states which data is synced to the watch

### Requirement: Missing Play services
When Play services is unavailable on the phone, the phone app SHALL behave exactly as without the watch feature and MUST NOT crash or show errors.

#### Scenario: Phone without Play services
- **WHEN** Play services is not available on the phone
- **THEN** reminders, the Home screen and the medicine screens work unchanged and nothing is published
