# dose-detail Specification

## Purpose
TBD - created by archiving change app-welcome-screen-reminder-details. Update Purpose after archive.
## Requirements
### Requirement: Dose detail screen opens from a dose on the Welcome screen
Tapping an upcoming dose on the Welcome screen SHALL open a dose detail screen for that dose and nothing else. The screen SHALL be reached only that way: no notification, no other screen and no wearable action opens it. Opening it MUST NOT record any outcome, clear any snooze or change any reminder.

#### Scenario: Opening a dose
- **WHEN** the user taps the tile for a dose of "Ibuprofen", "1 tablet", scheduled at 08:00
- **THEN** the dose detail screen for that dose is shown and the dose is still pending

#### Scenario: Opening changes nothing
- **WHEN** the user opens a snoozed dose's detail screen and takes no action
- **THEN** the dose is still pending, its snooze is unchanged and its reminder is still due to return at the snoozed moment

#### Scenario: Tapping the notification does not open it
- **WHEN** the user taps the body of a reminder notification
- **THEN** the app opens on the Welcome screen, not on the dose detail screen

### Requirement: Dose detail content
The dose detail screen SHALL show, for the dose it was opened with: the scheduled time, the medicine name, the dose amount and the dose's intake status as an icon, a label and a colour. The scheduled time MUST be formatted for the device locale and time zone, and when the dose is scheduled on a day other than today the screen MUST also indicate the day. The amount MUST be formatted with the same formatter the Welcome screen and the medicine overview use. Status MUST NOT be conveyed by colour alone. All texts MUST come from string resources.

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

### Requirement: Three answers, the same three the reminder offers
The dose detail screen SHALL offer exactly three answers, in this order and with the same string resources the reminder notification uses: "I took it", "Not yet", "Not going to". Each SHALL record exactly what the notification action of the same name records: "I took it" records the dose as taken at the moment of the tap; "Not yet" postpones the reminder by fifteen minutes without recording an outcome, never beyond the dose's lapse moment; "Not going to" records the dose as skipped at the moment of the tap. A skipped dose MUST NOT become missed.

The answers MUST be implemented by the same code path as the notification actions, so the two surfaces cannot record different things for the same answer.

#### Scenario: Answers present and in order
- **WHEN** the detail screen is shown for a pending dose
- **THEN** it offers "I took it", "Not yet" and "Not going to" in that order, and a way to close

#### Scenario: I took it
- **WHEN** the user taps "I took it" at 08:12 on a dose scheduled at 08:00
- **THEN** the dose's intake is taken with recorded moment 08:12

#### Scenario: Not yet
- **WHEN** the user taps "Not yet" at 08:00
- **THEN** the dose is still pending and its reminder is due to return at 08:15

#### Scenario: Repeated postponement
- **WHEN** the user opens a dose already postponed to 08:15 and taps "Not yet" at 08:15
- **THEN** the reminder is due to return at 08:30 and the dose is still pending

#### Scenario: Postponement bounded by the lapse moment
- **WHEN** the user taps "Not yet" at 19:50 on a dose that lapses at 20:00 because the next dose of that medicine is due then
- **THEN** no reminder for that dose returns and at 20:00 it becomes missed

#### Scenario: Not going to
- **WHEN** the user taps "Not going to"
- **THEN** the dose's intake is skipped, and after twenty-four hours its outcome is still skipped

### Requirement: An answer settles the dose and returns to the Welcome screen
Answering on the dose detail screen SHALL, as one sequence: record the answer, remove any reminder notification showing for that dose, bring the planned doses and the next reminder alarm up to date, and return to the Welcome screen. The Welcome screen the user returns to MUST already reflect the answer without being left and re-entered.

#### Scenario: Taken from the detail screen
- **WHEN** the user taps "I took it" on the detail screen of the only dose on the Welcome screen
- **THEN** the Welcome screen is shown with that dose no longer listed

#### Scenario: Notification for the answered dose disappears
- **WHEN** a reminder notification for a dose is on display and the user answers that dose from the detail screen
- **THEN** the notification is gone and no reminder for that dose is shown again, unless the answer was "Not yet"

#### Scenario: Postponed dose stays on the list
- **WHEN** the user taps "Not yet"
- **THEN** the Welcome screen is shown with that dose still listed and its status reading "Snoozed"

#### Scenario: The next alarm is brought up to date
- **WHEN** the user answers the dose whose moment the next reminder alarm was set for
- **THEN** the alarm is recomputed, so the next reminder the user receives is for the next dose that is still to be answered

#### Scenario: Skipped dose leaves the list
- **WHEN** the user taps "Not going to"
- **THEN** the Welcome screen is shown with that dose no longer listed

### Requirement: Close returns without recording anything
The dose detail screen SHALL offer a "Close" control aligned to the bottom of the screen that returns to the Welcome screen without recording an outcome and without changing any snooze or reminder. The back arrow and the system back gesture SHALL do the same.

#### Scenario: Close
- **WHEN** the user taps "Close"
- **THEN** the Welcome screen is shown and the dose is still pending with its snooze and its reminder unchanged

#### Scenario: System back
- **WHEN** the user performs the system back gesture on the dose detail screen
- **THEN** the Welcome screen is shown and the dose is still pending

#### Scenario: Back arrow
- **WHEN** the user taps the back arrow in the screen's top app bar
- **THEN** the Welcome screen is shown and the dose is still pending

### Requirement: Early and late warnings
The dose detail screen SHALL warn the user when they are answering well outside the dose's scheduled moment. When the scheduled moment is one hour or more in the future, the screen SHALL show a warning that the dose is not due yet. When the scheduled moment is one hour or more in the past, the screen SHALL show a warning that the dose is overdue. Otherwise it SHALL show no warning.

Both warnings are advisory. Every answer MUST remain available and MUST record exactly what it records without a warning; no warning MAY disable a control, require a confirmation or change what an answer does. Each warning MUST state that the dose can still be recorded, MUST come from a string resource, and MUST carry an icon so its meaning does not rest on colour alone. The "not due yet" warning MUST NOT use the error colour role, which is reserved for danger.

#### Scenario: More than an hour early
- **WHEN** the detail screen is open at 10:00 for a dose scheduled at 14:00
- **THEN** the screen warns that the dose is not due yet and says it can still be recorded

#### Scenario: Exactly an hour early
- **WHEN** the detail screen is open at 13:00 for a dose scheduled at 14:00
- **THEN** the screen warns that the dose is not due yet

#### Scenario: Just inside the early boundary
- **WHEN** the detail screen is open at 13:01 for a dose scheduled at 14:00
- **THEN** no warning is shown

#### Scenario: On time
- **WHEN** the detail screen is open at 14:05 for a dose scheduled at 14:00
- **THEN** no warning is shown

#### Scenario: Just inside the late boundary
- **WHEN** the detail screen is open at 14:59 for a dose scheduled at 14:00
- **THEN** no warning is shown

#### Scenario: Exactly an hour late
- **WHEN** the detail screen is open at 15:00 for a dose scheduled at 14:00
- **THEN** the screen warns that the dose is overdue and says it can still be recorded

#### Scenario: More than an hour late
- **WHEN** the detail screen is open at 18:00 for a dose scheduled at 14:00
- **THEN** the screen warns that the dose is overdue

#### Scenario: A warning never blocks an answer
- **WHEN** the screen warns that a dose is not due yet and the user taps "I took it" at 10:00
- **THEN** the dose's intake is taken with recorded moment 10:00, with no confirmation asked

#### Scenario: The warning follows the clock
- **WHEN** the detail screen is left open from 12:59 to 13:01 for a dose scheduled at 14:00
- **THEN** the "not due yet" warning appears when the hour boundary is crossed, without the user leaving and reopening the screen

### Requirement: A dose that has already been answered
When the dose the screen was opened for has an intake, the dose detail screen SHALL state the recorded outcome and, for a taken dose, when it was taken, and SHALL NOT offer the three answers. "Close" MUST remain available. This includes the case where the dose is answered from a notification or a wearable while the screen is open, which MUST update the screen without the user leaving and reopening it.

#### Scenario: Opening a dose answered a moment ago
- **WHEN** the detail screen is opened for a dose that was recorded as taken at 08:12
- **THEN** the screen states that it was taken at 08:12, offers no answers, and offers "Close"

#### Scenario: Answered from the notification while the screen is open
- **WHEN** the detail screen for a pending dose is open and the user taps "I took it" on that dose's notification
- **THEN** the screen states that the dose was taken and its answers are gone

#### Scenario: Dose lapses while the screen is open
- **WHEN** the detail screen for a pending dose is open and the dose becomes missed
- **THEN** the screen states that the dose was not taken and its answers are gone

#### Scenario: Skipped dose
- **WHEN** the detail screen is opened for a dose recorded as skipped
- **THEN** the screen states that the user chose not to take it and offers no answers

### Requirement: A dose that is no longer scheduled
When the dose the screen was opened for no longer exists, because the user changed or deactivated its medicine and the planning window withdrew it, the dose detail screen SHALL say that the dose is no longer scheduled and SHALL offer only "Close". It MUST NOT show an error, and it MUST NOT offer answers that would resolve to nothing.

#### Scenario: Dose withdrawn while the screen is open
- **WHEN** the detail screen for a pending dose is open and that medicine's schedule is changed so the dose is withdrawn
- **THEN** the screen says the dose is no longer scheduled and offers only "Close"

#### Scenario: Close from the withdrawn state
- **WHEN** the user taps "Close" on a dose that is no longer scheduled
- **THEN** the Welcome screen is shown

### Requirement: Dose detail visual baseline and accessibility
The dose detail screen SHALL follow `docs/design-system.md`: a top app bar with a back arrow on a secondary screen, the scheduled time as the largest element, the medicine name below it, the amount below that, and the three answers as full-width stacked buttons — filled for "I took it" at the primary confirm height, tonal for "Not yet", outlined for "Not going to" — with "Close" as a low-emphasis control in the bottom third. Every colour, text style, shape and spacing value MUST come from a theme token. The screen MUST remain usable at the largest system font scale, scrolling rather than clipping or truncating any text.

Every control MUST have a touch target of at least the design system's minimum, with the primary confirm at the primary confirm height. A screen reader MUST be able to reach and identify the time, the name, the amount, the status, any warning and every control, and decorative icons MUST NOT be announced.

#### Scenario: Largest font scale
- **WHEN** the system font scale is at its largest setting and the detail screen shows a warning and three answers
- **THEN** every element is reachable by scrolling, no text is clipped or truncated, and "Close" is still reachable

#### Scenario: Dark mode follows the system
- **WHEN** the system is set to dark theme
- **THEN** the detail screen renders with the dark colour scheme from the design system and the same layout

#### Scenario: Screen reader reads the screen
- **WHEN** a screen reader moves through the detail screen of an overdue dose
- **THEN** it announces the time, the medicine name, the amount, the status "Overdue", the overdue warning and each of the four controls by its label, and announces no decorative icon

#### Scenario: One-handed reach
- **WHEN** the detail screen is shown
- **THEN** the primary confirm and "Close" both sit in the bottom third of the screen

### Requirement: The dose detail screen survives configuration changes and process death
The dose being shown SHALL be identified by the route, so the screen is restored for the same dose after a rotation and after process death, with nothing to rebuild.

#### Scenario: Rotation
- **WHEN** the device rotates while the detail screen is open
- **THEN** the same dose is shown, with the same warning state

#### Scenario: Process death
- **WHEN** the process is killed and restored while the detail screen is open
- **THEN** the detail screen is restored for the same dose

### Requirement: No sensitive logging on the dose detail path
The dose detail screen and the code that records an answer from it MUST NOT log medicine names or amounts at info level or above.

#### Scenario: Log inspection
- **WHEN** the dose detail and intake packages are inspected
- **THEN** no info, warning or error log statement includes a medicine name or amount

