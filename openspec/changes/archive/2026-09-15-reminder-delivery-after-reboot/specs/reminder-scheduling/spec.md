## ADDED Requirements

### Requirement: A dose is only recorded as reminded when it was announced
The app SHALL record that a dose has been reminded only when its reminder notification was actually
posted. When posting does not happen — because notification permission is absent, or because the post
was refused by the system — the dose MUST remain un-reminded, so that a later wake can still announce
it.

A dose that could not be announced SHALL remain a candidate for both the due check and the next-wake
computation until it lapses under the ordinary lapse rule.

#### Scenario: Notification permission absent
- **WHEN** a dose falls due and notification permission is not granted
- **THEN** the dose is not recorded as reminded

#### Scenario: Permission granted before the dose lapses
- **WHEN** notification permission was absent when the dose fell due and the user grants it before the dose lapses
- **THEN** the next wake posts the reminder for that dose

#### Scenario: Posting refused by the system
- **WHEN** posting the notification is refused by the system
- **THEN** the dose is not recorded as reminded, the app does not crash, and the failure is logged at debug level only

#### Scenario: Undeliverable dose still lapses
- **WHEN** a dose could never be announced because permission was never granted, and its lapse moment arrives
- **THEN** it is recorded as missed under the ordinary lapse rule

### Requirement: A wake that does not complete is retried
When the wake cycle does not finish within its budget, the app SHALL arm a retry within two minutes
rather than treating the wake as complete. Retries SHALL be bounded at three consecutive attempts,
after which the ordinary lapse rule applies.

A wake that does not complete MUST NOT cause a dose to lapse as missed without ever having been
announced, and MUST NOT result in the next alarm being computed as though the wake had succeeded.

An alarm MUST still be set on every path, including the timed-out one.

#### Scenario: Cold start runs out of budget
- **WHEN** an alarm starts a dead process and the wake does not finish within its budget
- **THEN** a retry is armed within two minutes and the dose is still un-reminded

#### Scenario: Timed-out wake does not schedule the lapse
- **WHEN** a wake for a dose due at 08:00 times out
- **THEN** the next alarm is the retry, not the dose's lapse moment

#### Scenario: Retry succeeds
- **WHEN** the retry wake completes
- **THEN** the reminder is posted and no further retry is armed

#### Scenario: Retries exhausted
- **WHEN** three consecutive retries do not complete
- **THEN** no further retry is armed, an alarm is still set, and the dose lapses under the ordinary rule

#### Scenario: An exception is not a timeout
- **WHEN** a step of the wake throws but the wake completes
- **THEN** the next alarm is computed normally and no retry is armed

### Requirement: The wake cycle does not run while the user is locked
The app SHALL NOT run the wake cycle before the user has unlocked the device for the first time after
a reboot. While locked it SHALL re-arm its alarm and return, without marking doses missed, refreshing
the planning window or posting anything. Medicines and doses live in credential-encrypted storage and
MUST NOT be read before unlock.

#### Scenario: Alarm fires before first unlock
- **WHEN** an alarm fires after a reboot and the user has not yet unlocked the device
- **THEN** the alarm is re-armed, nothing is posted, no dose is marked missed and the app does not crash

#### Scenario: Device without a secure lock screen
- **WHEN** the device has no secure lock screen and an alarm fires after a reboot
- **THEN** the wake cycle runs normally

## MODIFIED Requirements

### Requirement: Recovery after reboot and app update
After the device reboots or the app is updated, the app SHALL refresh the window, post reminders for
doses that fell due in the meantime and have not lapsed, mark lapsed doses missed, and set the next
alarm, without the user opening the app.

A reboot SHALL leave an alarm set before the device is first unlocked. Because medicines and doses
cannot be read before unlock, the app SHALL record the moment of every alarm it arms in storage that
is readable before unlock, and SHALL re-arm from that record on locked boot. What is recorded MUST be
the alarm moment alone: it MUST NOT include a medicine name, an amount or a dose identifier.

A full wake SHALL follow as soon as the user unlocks the device.

#### Scenario: Reboot across a dose
- **WHEN** the device is off from 07:50 to 08:10 and a dose was due at 08:00
- **THEN** shortly after boot the reminder for the 08:00 dose is shown

#### Scenario: Reboot before first unlock
- **WHEN** the device reboots at 02:00 with a dose due at 08:00 and is not unlocked in between
- **THEN** an alarm is armed before first unlock

#### Scenario: First unlock after reboot
- **WHEN** the user unlocks the device for the first time after a reboot
- **THEN** a full wake runs, the window is refreshed, lapsed doses are marked missed and the next alarm is set

#### Scenario: Nothing identifying is stored before unlock
- **WHEN** the storage the app reads on locked boot is inspected
- **THEN** it contains an alarm moment and nothing that names or identifies a medicine or a dose

#### Scenario: Reboot across a lapse
- **WHEN** the device is off from Monday 07:00 to Wednesday 09:00 and a daily 08:00 dose existed for Monday and Tuesday
- **THEN** after boot the Monday dose is missed, the Tuesday dose is missed, Wednesday's dose is reminded and the alarm is set for Thursday 08:00

#### Scenario: App update
- **WHEN** the app is updated while a medicine with a schedule exists
- **THEN** the alarm is set without the app being opened
