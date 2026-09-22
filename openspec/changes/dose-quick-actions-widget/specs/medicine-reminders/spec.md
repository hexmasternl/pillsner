## MODIFIED Requirements

### Requirement: Three actions
The notification SHALL offer exactly three actions, in this order and with these labels from string resources: "I took it", "Not yet", "Not going to". Each action MUST work without opening the app. The home-screen widget's three actions for a due dose SHALL be the same three actions, with the same labels and the same order.

#### Scenario: Actions present
- **WHEN** a reminder notification is shown
- **THEN** it has the actions "I took it", "Not yet" and "Not going to" in that order

#### Scenario: Action does not open the app
- **WHEN** the user taps any of the three actions
- **THEN** the app's activity is not brought to the foreground

#### Scenario: Widget offers the same actions
- **WHEN** the widget shows a due dose
- **THEN** it offers "I took it", "Not yet" and "Not going to" in that order, and none of them opens the app

### Requirement: I took it
Tapping "I took it" SHALL record the dose as taken at the moment of the tap, remove the notification, and stop any further reminder for that dose. This applies equally whether "I took it" is tapped on the notification or on the home-screen widget; both go through the same recording path.

#### Scenario: Taken
- **WHEN** the user taps "I took it" at 08:12 on a dose scheduled at 08:00
- **THEN** the dose's intake is taken with recorded moment 08:12 and the notification is gone

#### Scenario: Taken from the widget
- **WHEN** the user taps "I took it" on the widget instead of the notification
- **THEN** the dose's intake is recorded exactly as it would be from the notification, and the notification (if shown) is removed

### Requirement: Not yet snoozes for 15 minutes
Tapping "Not yet" SHALL remove the notification and post it again 15 minutes later. This MAY repeat
any number of times until the user answers or the dose lapses as missed. The snooze MUST NOT extend
beyond the dose's lapse moment.

A snooze SHALL reset the dose's repeat sequence: the user has acknowledged the reminder, so the
repeats accumulated before the snooze MUST NOT count against them afterwards.

Tapping "Not yet" on the home-screen widget SHALL have the same effect as tapping it on the notification.

#### Scenario: Snooze
- **WHEN** the user taps "Not yet" at 08:00
- **THEN** the notification is gone and reappears at 08:15

#### Scenario: Repeated snooze
- **WHEN** the user taps "Not yet" on the re-posted reminder at 08:15
- **THEN** the reminder reappears at 08:30

#### Scenario: Snooze bounded by lapse
- **WHEN** the user taps "Not yet" at 19:50 on a dose that lapses at 20:00 because the next dose is due then
- **THEN** no reminder for that dose reappears and at 20:00 it becomes missed

#### Scenario: Snooze resets the repeats
- **WHEN** a dose has already repeated three times and the user then taps "Not yet"
- **THEN** the reminder reappears 15 minutes later and may repeat up to four times again

#### Scenario: Snoozed from the widget
- **WHEN** the user taps "Not yet" on the widget at 08:00
- **THEN** the notification's snooze cadence proceeds exactly as if "Not yet" had been tapped on the notification, reappearing at 08:15

### Requirement: Not going to skips
Tapping "Not going to" SHALL record the dose as skipped at the moment of the tap, remove the notification, and stop any further reminder for that dose. A skipped dose MUST NOT be recorded as missed. This applies equally whether "Not going to" is tapped on the notification or on the home-screen widget.

#### Scenario: Skipped
- **WHEN** the user taps "Not going to"
- **THEN** the dose's intake is skipped, the notification is gone and no reminder for that dose appears again

#### Scenario: Skipped from the widget
- **WHEN** the user taps "Not going to" on the widget
- **THEN** the dose's intake is skipped exactly as it would be from the notification, and the notification (if shown) is removed
