## MODIFIED Requirements

### Requirement: Entry content
Each list entry SHALL show the medicine name, the dose amount as formatted by the phone, the scheduled time formatted for the display language in the watch's time zone, and, when the dose is marked critical, a critical badge with an icon and text. When the scheduled date is not today the entry MUST indicate that it is tomorrow. A screen reader MUST read each entry as one item containing name, amount, time and, when present, that it is critical.

#### Scenario: Entry today
- **WHEN** a dose of 40 mg of "Ibuprofen" is scheduled at 14:00 today
- **THEN** the entry shows "Ibuprofen", "40 mg" and "14:00"

#### Scenario: Entry tomorrow
- **WHEN** it is 22:00 and a dose is scheduled at 02:00 the next day
- **THEN** the entry shows the time with an indication that it is tomorrow

#### Scenario: Screen reader
- **WHEN** a screen reader focuses an entry
- **THEN** it announces the name, the amount, the time and, when the dose is critical, that it is critical, together

#### Scenario: Critical entry
- **WHEN** a dose marked critical is in the six-hour window
- **THEN** its entry shows the critical badge alongside the name, amount and time

#### Scenario: Non-critical entry
- **WHEN** a dose not marked critical is in the six-hour window
- **THEN** its entry shows no critical badge
