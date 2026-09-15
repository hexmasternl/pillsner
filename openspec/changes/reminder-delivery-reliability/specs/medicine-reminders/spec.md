## ADDED Requirements

### Requirement: A reminder repeats until it is answered
A pending dose that has been reminded, is not snoozed and has not lapsed SHALL have its notification
posted again every 15 minutes, up to four repeats. A repeat MUST NOT be posted at or after the dose's
lapse moment. Each repeat SHALL alert again rather than appearing silently.

Any of the three answers SHALL end the repeats for that dose.

#### Scenario: Unanswered reminder repeats
- **WHEN** a dose is reminded at 08:00 and the user does not answer
- **THEN** the notification is posted again at 08:15, 08:30, 08:45 and 09:00

#### Scenario: Repeats are bounded
- **WHEN** a dose reminded at 08:00 is still unanswered at 09:30 and lapses at 20:00
- **THEN** no further repeat is posted and the dose remains pending until it lapses

#### Scenario: Repeat never outlives the dose
- **WHEN** a dose is reminded at 19:50 and lapses at 20:00 because the next dose is due then
- **THEN** no repeat is posted and at 20:00 the dose becomes missed

#### Scenario: Answer ends the repeats
- **WHEN** the user taps "I took it" at 08:20 on a dose reminded at 08:00
- **THEN** no further repeat is posted

#### Scenario: Repeat alerts again
- **WHEN** a repeat is posted
- **THEN** the device alerts again with sound or vibration according to the channel settings

### Requirement: A due dose presents itself on a busy phone
The reminder for a dose that has just fallen due SHALL carry a full-screen intent, so that on a
locked or otherwise occupied phone it presents rather than waiting silently in the shade. Where the
platform does not grant the app the full-screen intent capability, the notification SHALL degrade to
an ordinary heads-up notification and MUST NOT fail to post.

#### Scenario: Locked phone
- **WHEN** a dose falls due while the phone is locked
- **THEN** the reminder presents itself rather than only appearing in the shade

#### Scenario: Capability not granted
- **WHEN** the platform has not granted the full-screen intent capability and a dose falls due
- **THEN** the reminder is posted as a heads-up notification with its three actions

## MODIFIED Requirements

### Requirement: Reminder notification is shown when a dose is due
The app SHALL post a notification for a dose on a high-importance channel with sound and vibration
when that pending dose's scheduled moment arrives, when a snooze ends, or when a reminder repeat is
due. The notification MUST remain until the user answers it and MUST alert again each time it is
re-posted after a snooze or as a repeat.

#### Scenario: Dose falls due
- **WHEN** a pending dose reaches its scheduled moment
- **THEN** a notification for that dose is visible within the same minute

#### Scenario: Notification persists
- **WHEN** the user opens the notification shade and takes no action
- **THEN** the reminder is still present afterwards

#### Scenario: Re-post after snooze alerts
- **WHEN** a snooze ends and the notification is posted again
- **THEN** the device alerts again with sound or vibration according to the channel settings

#### Scenario: Re-post as a repeat alerts
- **WHEN** an unanswered reminder's repeat moment arrives and the notification is posted again
- **THEN** the device alerts again with sound or vibration according to the channel settings

### Requirement: Not yet snoozes for 15 minutes
Tapping "Not yet" SHALL remove the notification and post it again 15 minutes later. This MAY repeat
any number of times until the user answers or the dose lapses as missed. The snooze MUST NOT extend
beyond the dose's lapse moment.

A snooze SHALL reset the dose's repeat sequence: the user has acknowledged the reminder, so the
repeats accumulated before the snooze MUST NOT count against them afterwards.

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

### Requirement: Notification permission
On Android 13 and later the app SHALL request the notification permission the first time an active
medicine with at least one schedule exists and the permission has not been granted, at most once.
When the permission is denied, the Home screen SHALL show a banner stating that reminders cannot be
shown, with a button that opens the app's notification settings.

This banner is the most severe of the reminder banners and takes precedence over the others;
`reminder-delivery-resilience` defines the full order.

A dose that fell due while the permission was absent MUST remain un-reminded, so that granting the
permission before the dose lapses still produces its reminder.

#### Scenario: First scheduled medicine
- **WHEN** the user saves the first medicine that has a schedule and notification permission is not granted
- **THEN** the system permission request is shown once when the user returns to Home

#### Scenario: Permission denied
- **WHEN** the permission has been denied
- **THEN** Home shows the banner and tapping its button opens the system notification settings for the app

#### Scenario: Permission later granted
- **WHEN** the user grants the permission from system settings and returns to the app
- **THEN** the banner is gone and due reminders are shown

#### Scenario: Dose that fell due without permission
- **WHEN** a dose fell due at 08:00 while the permission was absent and the user grants it at 08:20, before the dose lapses
- **THEN** the reminder for that dose is posted

#### Scenario: Dose that lapsed without permission
- **WHEN** a dose fell due while the permission was absent and the user grants it only after the dose has lapsed
- **THEN** the dose stands as missed and no reminder for it is posted
