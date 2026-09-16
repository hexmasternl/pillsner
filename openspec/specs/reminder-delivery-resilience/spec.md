# reminder-delivery-resilience Specification

## Purpose
TBD - created by archiving change reminder-delivery-reliability. Update Purpose after archive.
## Requirements
### Requirement: Home reports when the system is throttling reminders

When a silently missed reminder has been recorded and not yet acknowledged, the Home screen SHALL
show a banner stating that a reminder did not arrive, with a button that opens a screen where the
user can stop it happening again. The banner SHALL NOT be raised by the battery-optimisation state
alone.

The record SHALL be cleared when the user activates the banner's button, and by the app reset. It
SHALL NOT be cleared merely because a later reminder posted successfully.

At most one reminder banner SHALL ever be shown. When more than one condition holds, the banner SHALL
report the most severe, in this order: notifications not allowed, then a reminder was silently
missed, then alarms not exact.

#### Scenario: A reminder was missed

- **WHEN** a silently missed reminder has been recorded, notifications are allowed and alarms are exact
- **THEN** Home shows the missed-reminder banner

#### Scenario: Not exempt but nothing has been missed

- **WHEN** the app is not exempt from battery optimisation and no reminder has been silently missed
- **THEN** Home shows no banner

#### Scenario: Acknowledged

- **WHEN** the user activates the banner's button and returns to Home
- **THEN** the banner is gone

#### Scenario: A later reminder arrives

- **WHEN** a silently missed reminder has been recorded, the user has not acknowledged it, and a later reminder posts successfully
- **THEN** the banner is still shown

#### Scenario: It happens again

- **WHEN** the user has acknowledged a missed reminder and a further dose later lapses un-reminded
- **THEN** the banner is shown again

#### Scenario: Notifications denied takes precedence

- **WHEN** notifications are not allowed and a silently missed reminder has also been recorded
- **THEN** only the notifications banner is shown

#### Scenario: A missed reminder takes precedence over inexact alarms

- **WHEN** a silently missed reminder has been recorded and alarms are also not exact
- **THEN** only the missed-reminder banner is shown

### Requirement: Vendor auto-start guidance on known devices

On devices whose manufacturer is known to stop background apps, the Home banner's button SHALL open
the vendor's own auto-start or protected-app screen. Each vendor intent MUST be checked for a
handling activity before it is offered, and when none resolves the button SHALL open the generic
system battery-optimisation list instead.

The choice of destination SHALL NOT depend on whether the app is currently exempt from battery
optimisation: a user who is exempt and still missing reminders is exactly the user the vendor screen
is for.

#### Scenario: Known vendor with a resolvable screen

- **WHEN** the device manufacturer is one of the known list, its auto-start screen resolves, and the user activates the banner's button
- **THEN** the vendor screen opens

#### Scenario: Known vendor whose screen no longer resolves

- **WHEN** the device manufacturer is on the list but its auto-start intent resolves to nothing
- **THEN** the system battery-optimisation list opens instead and the app does not crash

#### Scenario: Unknown vendor

- **WHEN** the device manufacturer is not on the list
- **THEN** the system battery-optimisation list opens

#### Scenario: Already exempt on a known vendor

- **WHEN** the app is already exempt from battery optimisation on a device whose vendor screen resolves
- **THEN** the vendor screen still opens

### Requirement: A periodic watchdog repairs a broken schedule
The app SHALL run periodic background work at the platform's minimum period that verifies the alarms
it expects are still armed, re-arms any that are missing, and runs a normal wake so that anything
already due is posted.

This work MUST NOT be the mechanism by which a reminder is delivered on a healthy device: alarms
remain the delivery mechanism and the watchdog SHALL find nothing to do when they have fired as
expected. The watchdog SHALL do nothing when no active medicine produces doses.

#### Scenario: Alarms intact
- **WHEN** the watchdog runs and every expected alarm is armed and no dose is due
- **THEN** nothing is posted, nothing is rescheduled and the run ends

#### Scenario: Alarm was dropped by the platform
- **WHEN** the platform drops the alarm for a dose due at 08:00 and the watchdog runs at 08:12
- **THEN** the reminder for the 08:00 dose is posted and the remaining alarms are re-armed

#### Scenario: Schedule chain broken while the app was not running
- **WHEN** no alarm is armed at all, the app process is not running and a scheduled medicine exists
- **THEN** the next watchdog run arms the alarms for the doses in the planning window without the user opening the app

#### Scenario: Nothing to watch
- **WHEN** the watchdog runs and there are no active medicines with schedules
- **THEN** it ends immediately without querying doses or arming anything

#### Scenario: Survives reboot
- **WHEN** the device reboots
- **THEN** the watchdog is enqueued again and continues to run on its period

### Requirement: Watchdog logging respects privacy
The watchdog and the alarm bookkeeping MUST NOT log medicine names or amounts at any level, and MUST
NOT log dose identifiers above debug level.

#### Scenario: Log inspection
- **WHEN** the watchdog and scheduler code is inspected
- **THEN** no log statement includes a medicine name or amount, and dose identifiers appear only at debug level

### Requirement: The app never requests the battery-optimisation exemption

Pillsner MUST NOT open any system dialog that the user did not ask for. In particular it MUST NOT
start `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, and MUST NOT declare
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

The app MAY read whether it is exempt, because reading is silent, but that state alone SHALL NOT
cause anything to be shown to the user.

#### Scenario: First upcoming dose on a non-exempt device

- **WHEN** the user saves their first scheduled medicine on a device where the app is not exempt from battery optimisation and opens Home
- **THEN** no system dialog appears and no banner is shown

#### Scenario: Returning to Home while not exempt

- **WHEN** the app is not exempt, reminders have all arrived, and the user opens Home any number of times
- **THEN** no system dialog appears and no banner is shown

#### Scenario: Manifest holds no battery permission

- **WHEN** the merged manifest is inspected
- **THEN** it declares no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission

### Requirement: A silently missed reminder is recorded

When a pending dose lapses and is recorded as missed, the app SHALL record that a reminder was
silently missed if, and only if, all of the following hold:

- no reminder was ever posted for that dose;
- the dose was stored before its own due moment, so the app had a window in which to remind; and
- notifications were allowed, so the absence of a reminder is not explained by a denied permission.

The record SHALL survive process death and reboot, and SHALL carry the moment of the most recent such
miss.

#### Scenario: An alarm the platform did not deliver

- **WHEN** a dose planned yesterday for 08:00 today lapses with no reminder ever posted for it, and notifications are allowed
- **THEN** a silently missed reminder is recorded, with that dose's lapse moment

#### Scenario: A dose that was reminded and simply not answered

- **WHEN** a dose was reminded about and the user never answered, and it lapses
- **THEN** nothing is recorded; it is a missed dose, not a missed reminder

#### Scenario: A dose generated after its own moment

- **WHEN** the user saves a medicine at 20:00 whose schedule includes 08:00, and today's 08:00 dose is generated already lapsed
- **THEN** it is recorded as missed but no silently missed reminder is recorded

#### Scenario: Notifications were denied

- **WHEN** a dose lapses with no reminder posted because notifications are not allowed
- **THEN** no silently missed reminder is recorded

#### Scenario: Survives a restart

- **WHEN** a silently missed reminder has been recorded and the process is killed or the device reboots
- **THEN** the record is still present when the app next runs

