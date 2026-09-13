# medicine-reminders Specification

## Purpose
TBD - created by archiving change app-medicine-alarm. Update Purpose after archive.
## Requirements
### Requirement: Reminder notification is shown when a dose is due
When a pending dose's scheduled moment arrives, or a snooze ends, the app SHALL post a notification for that dose on a high-importance channel with sound and vibration. The notification MUST remain until the user answers it and MUST alert again each time it is re-posted after a snooze.

#### Scenario: Dose falls due
- **WHEN** a pending dose reaches its scheduled moment
- **THEN** a notification for that dose is visible within the same minute

#### Scenario: Notification persists
- **WHEN** the user opens the notification shade and takes no action
- **THEN** the reminder is still present afterwards

#### Scenario: Re-post after snooze alerts
- **WHEN** a snooze ends and the notification is posted again
- **THEN** the device alerts again with sound or vibration according to the channel settings

### Requirement: Reminder text
The notification's title SHALL be the snapshot medicine name. Its text SHALL read, from a string resource with placeholders: "Take {{dose}} of your medicine '{{name}}', on {{time-scheduled}}", where the dose is the formatted amount, the name is the snapshot medicine name and the time is the scheduled time formatted for the device locale and time zone, preceded by a short day indication when the scheduled date is not today. The small icon SHALL be the monochrome Pillsner mark and the accent colour SHALL be Pillsner Green, as the design system defines. The text MUST NOT be truncated at the largest font scale.

#### Scenario: Dose due today
- **WHEN** a dose of 40 mg of "Ibuprofen" scheduled at 08:00 today falls due in an English locale with a 24-hour clock
- **THEN** the notification is titled "Ibuprofen" and reads "Take 40 mg of your medicine 'Ibuprofen', on 08:00"

#### Scenario: Unit with plural
- **WHEN** a dose of 2 tablets of "Paracetamol" falls due
- **THEN** the notification reads "Take 2 tablets of your medicine 'Paracetamol', on " followed by the time

#### Scenario: Overdue dose from yesterday re-posted after snooze
- **WHEN** a dose scheduled yesterday at 23:50 is re-posted at 00:05
- **THEN** the time part indicates yesterday and 23:50

### Requirement: Three actions
The notification SHALL offer exactly three actions, in this order and with these labels from string resources: "I took it", "Not yet", "Not going to". Each action MUST work without opening the app.

#### Scenario: Actions present
- **WHEN** a reminder notification is shown
- **THEN** it has the actions "I took it", "Not yet" and "Not going to" in that order

#### Scenario: Action does not open the app
- **WHEN** the user taps any of the three actions
- **THEN** the app's activity is not brought to the foreground

### Requirement: I took it
Tapping "I took it" SHALL record the dose as taken at the moment of the tap, remove the notification, and stop any further reminder for that dose.

#### Scenario: Taken
- **WHEN** the user taps "I took it" at 08:12 on a dose scheduled at 08:00
- **THEN** the dose's intake is taken with recorded moment 08:12 and the notification is gone

### Requirement: Not yet snoozes for 15 minutes
Tapping "Not yet" SHALL remove the notification and post it again 15 minutes later. This MAY repeat any number of times until the user answers or the dose lapses as missed. The snooze MUST NOT extend beyond the dose's lapse moment.

#### Scenario: Snooze
- **WHEN** the user taps "Not yet" at 08:00
- **THEN** the notification is gone and reappears at 08:15

#### Scenario: Repeated snooze
- **WHEN** the user taps "Not yet" on the re-posted reminder at 08:15
- **THEN** the reminder reappears at 08:30

#### Scenario: Snooze bounded by lapse
- **WHEN** the user taps "Not yet" at 19:50 on a dose that lapses at 20:00 because the next dose is due then
- **THEN** no reminder for that dose reappears and at 20:00 it becomes missed

### Requirement: Not going to skips
Tapping "Not going to" SHALL record the dose as skipped at the moment of the tap, remove the notification, and stop any further reminder for that dose. A skipped dose MUST NOT be recorded as missed.

#### Scenario: Skipped
- **WHEN** the user taps "Not going to"
- **THEN** the dose's intake is skipped, the notification is gone and no reminder for that dose appears again

### Requirement: Dismissing the notification counts as Not yet
Swiping a reminder notification away SHALL have the same effect as tapping "Not yet".

#### Scenario: Swipe away
- **WHEN** the user swipes the reminder away at 08:03
- **THEN** the reminder reappears at 08:18 and the dose is still pending

### Requirement: Several doses due at once
When more than one dose is due, each dose SHALL have its own notification with its own actions, and a group summary MUST state how many medicines are due.

#### Scenario: Two doses due
- **WHEN** two medicines are due at 08:00
- **THEN** two reminder notifications with their own actions are shown, grouped under a summary reading that two medicines are due

### Requirement: Lock-screen visibility
The notification SHALL be marked private and SHALL provide a public version that reads "Time for your medicine" without the medicine name or amount, carrying the same three actions. Which version appears on the lock screen is left to the system's lock-screen notification setting.

#### Scenario: Sensitive content hidden
- **WHEN** the system is set to hide sensitive notification content on the lock screen
- **THEN** the lock screen shows "Time for your medicine" with no medicine name or amount

#### Scenario: Sensitive content allowed
- **WHEN** the system is set to show all notification content on the lock screen
- **THEN** the lock screen shows the full reminder text

### Requirement: Tapping the notification opens the app
Tapping the notification body SHALL open the app on the Home screen without recording any outcome.

#### Scenario: Open from notification
- **WHEN** the user taps the reminder text
- **THEN** the app opens on Home, the dose is still pending and the notification remains

### Requirement: Wearable delivery
The reminder notification MUST NOT be marked local-only, so that a paired Wear OS device shows it with its actions. The primary action on the wearable SHALL be "I took it". Answering from the wearable SHALL have the same effect as answering on the phone.

#### Scenario: Watch shows the reminder
- **WHEN** a Wear OS watch is paired and connected and a dose falls due
- **THEN** the watch shows the reminder text and the three actions

#### Scenario: Answer from the watch
- **WHEN** the user taps "I took it" on the watch
- **THEN** the dose is recorded as taken and the notification disappears from both devices

#### Scenario: No wearable
- **WHEN** no wearable is paired
- **THEN** the phone notification behaves exactly as specified above and nothing else is attempted

### Requirement: Notification permission
On Android 13 and later the app SHALL request the notification permission the first time an active medicine with at least one schedule exists and the permission has not been granted, at most once. When the permission is denied, the Home screen SHALL show a banner stating that reminders cannot be shown, with a button that opens the app's notification settings.

#### Scenario: First scheduled medicine
- **WHEN** the user saves the first medicine that has a schedule and notification permission is not granted
- **THEN** the system permission request is shown once when the user returns to Home

#### Scenario: Permission denied
- **WHEN** the permission has been denied
- **THEN** Home shows the banner and tapping its button opens the system notification settings for the app

#### Scenario: Permission later granted
- **WHEN** the user grants the permission from system settings and returns to the app
- **THEN** the banner is gone and due reminders are shown

### Requirement: No sensitive logging
The reminder code MUST NOT log medicine names or amounts at info level or above.

#### Scenario: Log inspection
- **WHEN** the reminder package is inspected by lint
- **THEN** no info, warning or error log statement includes a medicine name or amount

