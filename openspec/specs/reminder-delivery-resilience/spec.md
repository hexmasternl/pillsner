# reminder-delivery-resilience Specification

## Purpose
TBD - created by archiving change reminder-delivery-reliability. Update Purpose after archive.
## Requirements
### Requirement: Battery optimisation exemption is requested
The app SHALL request exemption from battery optimisation at the moment the user saves the first
active medicine that has at least one schedule, and at most once. The app MUST continue to function
when the exemption is refused: every reminder path SHALL work without it, and the refusal SHALL be
reported through the Home banner rather than by blocking any part of the app.

#### Scenario: First scheduled medicine
- **WHEN** the user saves the first medicine that has a schedule and the app is not exempt from battery optimisation
- **THEN** the system exemption request is shown once

#### Scenario: Asked only once
- **WHEN** the user refuses the exemption and later saves a second scheduled medicine
- **THEN** the exemption request is not shown again

#### Scenario: App works without the exemption
- **WHEN** the exemption has been refused and a dose falls due
- **THEN** the reminder is still scheduled and posted, and the Home banner reports that reminders may be delayed

#### Scenario: Already exempt
- **WHEN** the app is already exempt from battery optimisation and the user saves their first scheduled medicine
- **THEN** no exemption request is shown and no banner appears

### Requirement: Home reports when the system is throttling reminders
When the app is not exempt from battery optimisation, the Home screen SHALL show a banner stating
that the system may delay reminders, with a button that opens the system battery-optimisation
setting for the app. The banner SHALL disappear as soon as the app becomes exempt.

At most one reminder banner SHALL ever be shown. When more than one condition holds, the banner
SHALL report the most severe, in this order: notifications not allowed, then battery optimisation
not exempt, then alarms not exact.

#### Scenario: Not exempt
- **WHEN** the app is not exempt from battery optimisation and notifications are allowed and alarms are exact
- **THEN** Home shows the battery banner and its button opens the system battery-optimisation setting

#### Scenario: Exemption granted later
- **WHEN** the user grants the exemption from system settings and returns to the app
- **THEN** the banner is gone

#### Scenario: Notifications denied takes precedence
- **WHEN** notifications are not allowed and the app is also not exempt from battery optimisation
- **THEN** only the notifications banner is shown

#### Scenario: Battery takes precedence over inexact alarms
- **WHEN** the app is not exempt from battery optimisation and alarms are also not exact
- **THEN** only the battery banner is shown

### Requirement: Vendor auto-start guidance on known devices
On devices whose manufacturer is known to stop background apps, the battery banner SHALL additionally
offer the vendor's own auto-start or protected-app screen. Each vendor intent MUST be checked for a
handling activity before it is offered, and when none resolves the banner SHALL fall back to the
generic system battery-optimisation setting.

#### Scenario: Known vendor with a resolvable screen
- **WHEN** the device manufacturer is one of the known list and its auto-start screen resolves
- **THEN** the banner offers the vendor screen in addition to the system setting

#### Scenario: Known vendor whose screen no longer resolves
- **WHEN** the device manufacturer is on the list but its auto-start intent resolves to nothing
- **THEN** only the generic system battery-optimisation setting is offered and the app does not crash

#### Scenario: Unknown vendor
- **WHEN** the device manufacturer is not on the list
- **THEN** only the generic system battery-optimisation setting is offered

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

