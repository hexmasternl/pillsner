# reminder-scheduling Specification

## Purpose
TBD - created by archiving change app-medicine-alarm. Update Purpose after archive.
## Requirements
### Requirement: Reminders fire from an exact alarm
The app SHALL schedule reminder processing with an exact, device-waking platform alarm. Periodic or deferrable background work MUST NOT be used to deliver the reminder moment.

#### Scenario: Device idle
- **WHEN** the device has been idle with the screen off for over an hour and a dose falls due
- **THEN** the reminder notification appears within the same minute

#### Scenario: App not running
- **WHEN** the app process has been stopped by the system and a dose falls due
- **THEN** the reminder notification appears within the same minute

### Requirement: Single next-wake alarm
The app SHALL keep exactly one alarm set, at the earliest of: the scheduled moment of the next un-reminded pending dose, the end of the next snooze, the next dose lapse moment, and the next daily refresh moment. The alarm SHALL be recomputed after every wake, every notification action, every medication change and every system event listed below. When there are no active medicines with schedules and no pending doses, no alarm MUST be set.

#### Scenario: Next dose is the earliest
- **WHEN** the next un-reminded dose is at 08:00, a snooze ends at 08:15 and the daily refresh is at 00:05 tomorrow
- **THEN** the alarm is set for 08:00

#### Scenario: Snooze is the earliest
- **WHEN** a snooze ends at 07:45 and the next dose is at 08:00
- **THEN** the alarm is set for 07:45

#### Scenario: Recomputed after an action
- **WHEN** the user taps "I took it" on the only due dose and the next dose is at 20:00
- **THEN** the alarm is set for 20:00

#### Scenario: Nothing to remind
- **WHEN** there are no medicines
- **THEN** no alarm is set

### Requirement: Processing on wake
When the alarm fires, the app SHALL, in order: mark lapsed doses as missed, refresh the two-day window, post notifications for every dose that is due, and set the next alarm. The next alarm MUST be set even if an earlier step fails. All processing MUST complete within the platform's broadcast receiver budget.

#### Scenario: Two doses due in the same minute
- **WHEN** two doses fall due at 08:00
- **THEN** one wake posts both notifications

#### Scenario: Failure still reschedules
- **WHEN** posting a notification throws
- **THEN** the next alarm is still set

#### Scenario: Due dose already reminded is not re-posted
- **WHEN** a wake occurs and a pending dose has already been reminded and is not snoozed
- **THEN** no second notification is posted for it

### Requirement: Exact-alarm permission handling
The app SHALL declare the exact-alarm permissions the platform requires for its target versions. Before every scheduling call it SHALL check whether exact alarms are permitted. When they are not, it SHALL schedule with a window of at most ten minutes as a degraded fallback, and the Home screen SHALL show a banner stating that reminders may be up to ten minutes late, with a button that opens the system exact-alarm setting. When the permission state changes, the alarm SHALL be rescheduled.

#### Scenario: Exact alarms permitted
- **WHEN** exact alarms are permitted
- **THEN** the alarm is scheduled exactly and no banner is shown

#### Scenario: Exact alarms not permitted
- **WHEN** exact alarms are not permitted
- **THEN** the alarm is scheduled with a ten-minute window and Home shows the late-reminders banner

#### Scenario: Permission granted later
- **WHEN** the user grants exact alarms in system settings
- **THEN** the alarm is rescheduled exactly and the banner disappears

### Requirement: Recovery after reboot and app update
After the device reboots or the app is updated, the app SHALL refresh the window, post reminders for doses that fell due in the meantime and have not lapsed, mark lapsed doses missed, and set the next alarm, without the user opening the app.

#### Scenario: Reboot across a dose
- **WHEN** the device is off from 07:50 to 08:10 and a dose was due at 08:00
- **THEN** shortly after boot the reminder for the 08:00 dose is shown

#### Scenario: Reboot across a lapse
- **WHEN** the device is off from Monday 07:00 to Wednesday 09:00 and a daily 08:00 dose existed for Monday and Tuesday
- **THEN** after boot the Monday dose is missed, the Tuesday dose is missed, Wednesday's dose is reminded and the alarm is set for Thursday 08:00

#### Scenario: App update
- **WHEN** the app is updated while a medicine with a schedule exists
- **THEN** the alarm is set without the app being opened

### Requirement: Clock and time zone changes
When the system time or time zone changes, the app SHALL regenerate un-reminded planned doses for the new wall clock, mark lapsed doses missed, post reminders for doses now due, and set the next alarm. Doses already reminded MUST keep their scheduled instant and MUST NOT be reminded again unless snoozed.

#### Scenario: Clock moved forward past a dose
- **WHEN** the clock is set from 07:00 to 09:00 and a dose was due at 08:00
- **THEN** the 08:00 dose is reminded immediately

#### Scenario: Clock moved backward
- **WHEN** a dose at 08:00 has been reminded and the clock is set from 08:30 back to 07:30
- **THEN** the dose is not reminded a second time and the alarm is set for the next event

#### Scenario: Time zone change
- **WHEN** the device moves from Amsterdam to New York with an un-reminded dose planned at 08:00 Amsterdam time
- **THEN** the dose is now planned for 08:00 New York time and the alarm is set accordingly

#### Scenario: Daylight-saving night
- **WHEN** a daily 08:00 dose spans the night the clocks change
- **THEN** the dose fires at 08:00 local time on the following day

### Requirement: Daily refresh
The app SHALL wake once per day shortly after local midnight to extend the two-day window, even when no dose or snooze is pending sooner.

#### Scenario: Quiet day
- **WHEN** the only medicine is due every other day and today has no dose
- **THEN** an alarm is set for shortly after midnight and after it fires tomorrow's dose exists and the alarm is set for it

### Requirement: Notification actions are processed reliably
Notification actions SHALL be handled by a broadcast receiver that records the outcome or snooze, updates the notification and recomputes the alarm, all without starting the activity and within the receiver budget.

#### Scenario: Action while app is stopped
- **WHEN** the app process is not running and the user taps "Not going to"
- **THEN** the dose is recorded as skipped and the notification disappears

