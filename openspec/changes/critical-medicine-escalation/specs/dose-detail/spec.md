## MODIFIED Requirements

### Requirement: Dose detail content
The dose detail screen SHALL show, for the dose it was opened with: the scheduled time, the medicine name, the dose amount, the dose's intake status as an icon, a label and a colour, and, when the medicine is flagged critical, a critical badge with an icon and text. The scheduled time MUST be formatted for the device locale and time zone, and when the dose is scheduled on a day other than today the screen MUST also indicate the day. The amount MUST be formatted with the same formatter the Welcome screen and the medicine overview use. Status and criticality MUST NOT be conveyed by colour alone. All texts MUST come from string resources.

#### Scenario: Dose due later today
- **WHEN** the detail screen is opened for a dose of "1 tablet" of "Ibuprofen" scheduled at 20:00 today, in an English locale with a 24-hour clock
- **THEN** the screen shows "20:00", "Ibuprofen", "1 tablet" and the status "Due" with its icon, and indicates no day

#### Scenario: Dose due tomorrow
- **WHEN** the detail screen is opened for a dose scheduled at 08:00 the following day
- **THEN** the screen shows the time and an indication that it is tomorrow

#### Scenario: Overdue dose
- **WHEN** the detail screen is opened for a dose scheduled at 08:00 that is unanswered at 09:30
- **THEN** the status shown is "Overdue" with its icon

#### Scenario: Snoozed dose
- **WHEN** the detail screen is opened for a dose the user postponed
- **THEN** the status shown is "Snoozed" with its icon

#### Scenario: Dose of a critical medicine
- **WHEN** the detail screen is opened for a dose whose medicine is flagged critical
- **THEN** the screen shows a critical badge with an icon and text alongside the other dose content

#### Scenario: Dose of a non-critical medicine
- **WHEN** the detail screen is opened for a dose whose medicine is not flagged critical
- **THEN** no critical badge is shown
