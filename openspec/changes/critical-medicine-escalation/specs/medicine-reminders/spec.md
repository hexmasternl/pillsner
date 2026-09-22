## MODIFIED Requirements

### Requirement: Reminder notification is shown when a dose is due
The app SHALL post a notification for a dose on a high-importance channel with sound and vibration
when that pending dose's scheduled moment arrives, when a snooze ends, or when a reminder repeat is
due. A dose whose medicine is flagged critical SHALL be posted on a separate, more insistent channel
with its own distinct sound and vibration pattern instead. The notification MUST remain until the
user answers it and MUST alert again each time it is re-posted after a snooze or as a repeat.

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

#### Scenario: Critical dose uses the critical channel
- **WHEN** a dose whose medicine is flagged critical falls due
- **THEN** the notification is posted on the critical reminder channel with its distinct sound and vibration pattern

#### Scenario: Non-critical dose uses the standard channel
- **WHEN** a dose whose medicine is not flagged critical falls due
- **THEN** the notification is posted on the standard reminder channel

### Requirement: Not yet snoozes for 15 minutes
Tapping "Not yet" SHALL remove the notification and post it again 15 minutes later, or 5 minutes
later when the dose's medicine is flagged critical. This MAY repeat any number of times until the
user answers or the dose lapses as missed. The snooze MUST NOT extend beyond the dose's lapse moment.

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

#### Scenario: Critical snooze is shorter
- **WHEN** the user taps "Not yet" at 08:00 on a dose whose medicine is flagged critical
- **THEN** the notification is gone and reappears at 08:05

#### Scenario: Critical snooze resets the critical repeats
- **WHEN** a critical dose has already repeated seven times and the user then taps "Not yet"
- **THEN** the reminder reappears 5 minutes later and may repeat up to eight times again

### Requirement: A reminder repeats until it is answered
A pending dose that has been reminded, is not snoozed and has not lapsed SHALL have its notification
posted again every 15 minutes, up to four repeats, or, when its medicine is flagged critical, every 5
minutes, up to eight repeats. A repeat MUST NOT be posted at or after the dose's lapse moment. Each
repeat SHALL alert again rather than appearing silently.

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

#### Scenario: Critical dose repeats faster and more often
- **WHEN** a dose whose medicine is flagged critical is reminded at 08:00 and the user does not answer
- **THEN** the notification is posted again at 08:05, 08:10, 08:15, 08:20, 08:25, 08:30, 08:35 and 08:40

#### Scenario: Critical repeats still never outlive the dose
- **WHEN** a critical dose is reminded at 19:57 and lapses at 20:00 because the next dose is due then
- **THEN** no repeat is posted and at 20:00 the dose becomes missed
