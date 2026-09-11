## MODIFIED Requirements

### Requirement: Wearable delivery
The reminder notification MUST NOT be marked local-only, so that a paired Wear OS device shows it with its actions. The primary action on the wearable SHALL be "I took it". Answering from the wearable SHALL have the same effect as answering on the phone. Installing the Pillsner watch app MUST NOT disable notification bridging: the watch app's manifest MUST NOT set the bridge mode to no bridging, and the bridged notification remains the only way to answer a dose on the wearable.

#### Scenario: Watch shows the reminder
- **WHEN** a Wear OS watch is paired and connected and a dose falls due
- **THEN** the watch shows the reminder text and the three actions

#### Scenario: Answer from the watch
- **WHEN** the user taps "I took it" on the watch
- **THEN** the dose is recorded as taken and the notification disappears from both devices

#### Scenario: No wearable
- **WHEN** no wearable is paired
- **THEN** the phone notification behaves exactly as specified above and nothing else is attempted

#### Scenario: Watch app installed
- **WHEN** the Pillsner watch app is installed on the paired watch and a dose falls due
- **THEN** the watch still shows the bridged reminder notification with its three actions

#### Scenario: Watch app has no answer actions
- **WHEN** the user opens the Pillsner watch app during a due dose
- **THEN** the dose is listed but can only be answered through the notification
