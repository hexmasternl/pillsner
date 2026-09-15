# reminder-scheduling Specification

## Purpose
TBD - created by archiving change app-medicine-alarm. Update Purpose after archive.
## Requirements
### Requirement: Reminders fire from an exact alarm
The app SHALL schedule the reminder moment with `AlarmManager.setAlarmClock`, the platform's
alarm-clock tier, which is exempt from Doze, from app standby quotas and from vendor power
management. Housekeeping wakes that are not user-facing MAY use an exact allow-while-idle alarm
instead.

Periodic or deferrable background work MUST NOT be used to deliver the reminder moment. Periodic
work MAY be used to verify that the alarms are still armed and to repair them when they are not, as
`reminder-delivery-resilience` requires; on a device whose alarms fire as expected that work MUST
find nothing to do.

#### Scenario: Device idle
- **WHEN** the device has been idle with the screen off for over an hour and a dose falls due
- **THEN** the reminder notification appears within the same minute

#### Scenario: App not running
- **WHEN** the app process has been stopped by the system and a dose falls due
- **THEN** the reminder notification appears within the same minute

#### Scenario: App backgrounded behind another task
- **WHEN** the user takes a phone call, the app is backgrounded for an hour and a dose falls due during the call
- **THEN** the reminder notification appears within the same minute without the app being opened

#### Scenario: Alarm-clock tier is used
- **WHEN** the alarm for an un-reminded dose is armed
- **THEN** it is armed with the alarm-clock tier and the device reports it as the next alarm

### Requirement: Processing on wake
When an alarm fires, the app SHALL hand the work to a short foreground service rather than performing
it inside the broadcast receiver, so that a cold start that has to open the database has a real
window in which to finish.

The service SHALL, in order: mark lapsed doses as missed, refresh the two-day window, post
notifications for every dose that is due or due to repeat, record as reminded only those doses whose
notification was posted, and reconcile the alarm set. The alarm set MUST be reconciled even if an
earlier step fails.

The existing requirements "A dose is only recorded as reminded when it was announced", "A wake that
does not complete is retried" and "The wake cycle does not run while the user is locked" continue to
hold unchanged, and apply to the service exactly as they applied to the receiver. The foreground
service is what makes a timeout rare; the retry remains the backstop for when it still happens.

#### Scenario: Two doses due in the same minute
- **WHEN** two doses fall due at 08:00
- **THEN** one wake posts both notifications

#### Scenario: Failure still reschedules
- **WHEN** posting a notification throws
- **THEN** the alarm set is still reconciled

#### Scenario: Wake runs in a foreground service
- **WHEN** an alarm fires while the app process is not running
- **THEN** the work runs in a foreground service and completes even when opening the database is slow

#### Scenario: Due dose already reminded is re-posted on its repeat
- **WHEN** a wake occurs and a pending dose has already been reminded, is not snoozed, and its repeat moment has arrived
- **THEN** the notification for it is posted again

#### Scenario: Due dose already reminded is not re-posted early
- **WHEN** a wake occurs and a pending dose has already been reminded, is not snoozed, and its next repeat moment has not arrived
- **THEN** no second notification is posted for it

### Requirement: Exact-alarm permission handling
The app SHALL declare the exact-alarm permissions the platform requires for its target versions.
Before every scheduling call it SHALL check whether exact alarms are permitted. When they are not, it
SHALL schedule with a window of at most ten minutes as a degraded fallback, and the Home screen SHALL
show a banner stating that reminders may be up to ten minutes late, with a button that opens the
system exact-alarm setting. When the permission state changes, the alarm set SHALL be reconciled.

This banner is the least severe of the reminder banners; `reminder-delivery-resilience` defines the
order in which one is chosen.

#### Scenario: Exact alarms permitted
- **WHEN** exact alarms are permitted
- **THEN** the alarms are scheduled at the alarm-clock tier and no exactness banner is shown

#### Scenario: Exact alarms not permitted
- **WHEN** exact alarms are not permitted
- **THEN** the alarms are scheduled with a ten-minute window and Home shows the late-reminders banner

#### Scenario: Permission granted later
- **WHEN** the user grants exact alarms in system settings
- **THEN** the alarm set is reconciled at the alarm-clock tier and the banner disappears

### Requirement: Recovery after reboot and app update
After the device reboots or the app is updated, the app SHALL refresh the window, post reminders for
doses that fell due in the meantime and have not lapsed, mark lapsed doses missed, and reconcile the
alarm set, without the user opening the app.

A reboot SHALL leave alarms armed before the device is first unlocked. Because medicines and doses
cannot be read before unlock, the app SHALL record the moment of every alarm it arms in storage that
is readable before unlock, and SHALL re-arm the whole recorded set on locked boot rather than a
single moment. What is recorded MUST be the alarm moments alone: it MUST NOT include a medicine name,
an amount or a dose identifier.

A full wake SHALL follow as soon as the user unlocks the device, and SHALL re-enqueue the watchdog.

#### Scenario: Reboot across a dose
- **WHEN** the device is off from 07:50 to 08:10 and a dose was due at 08:00
- **THEN** shortly after boot the reminder for the 08:00 dose is shown

#### Scenario: Reboot before first unlock with several doses ahead
- **WHEN** the device reboots and is not unlocked, and doses are due at 08:00 and 20:00
- **THEN** alarms for both moments are armed before first unlock, not only the earliest

#### Scenario: First unlock after reboot
- **WHEN** the user unlocks the phone for the first time after a reboot
- **THEN** a full wake runs, the window is refreshed, the alarm set is reconciled and the watchdog is enqueued

#### Scenario: Nothing identifying is stored before unlock
- **WHEN** the storage the app reads on locked boot is inspected
- **THEN** it contains alarm moments and kinds and nothing that names or identifies a medicine or a dose

#### Scenario: Reboot across a lapse
- **WHEN** the device is off from Monday 07:00 to Wednesday 09:00 and a daily 08:00 dose existed for Monday and Tuesday
- **THEN** after boot the Monday dose is missed, the Tuesday dose is missed, Wednesday's dose is reminded and the alarm set covers Thursday 08:00

#### Scenario: App update
- **WHEN** the app is updated while a medicine with a schedule exists
- **THEN** the alarm set is armed and the watchdog is enqueued without the app being opened

### Requirement: Clock and time zone changes
When the system time or time zone changes, the app SHALL regenerate un-reminded planned doses for the
new wall clock, mark lapsed doses missed, post reminders for doses now due, and reconcile the alarm
set. Doses already reminded MUST keep their scheduled instant and MUST NOT be announced again by the
clock change itself; their pending repeats, if any, continue under the repeat rule.

#### Scenario: Clock moved forward past a dose
- **WHEN** the clock is set from 07:00 to 09:00 and a dose was due at 08:00
- **THEN** the 08:00 dose is reminded immediately

#### Scenario: Clock moved backward
- **WHEN** a dose at 08:00 has been reminded and the clock is set from 08:30 back to 07:30
- **THEN** the dose is not announced a second time by the clock change and the alarm set is reconciled

#### Scenario: Time zone change
- **WHEN** the device moves from Amsterdam to New York with an un-reminded dose planned at 08:00 Amsterdam time
- **THEN** the dose is now planned for 08:00 New York time and its alarm is armed accordingly

#### Scenario: Daylight-saving night
- **WHEN** a daily 08:00 dose spans the night the clocks change
- **THEN** the dose fires at 08:00 local time on the following day

### Requirement: Daily refresh
The app SHALL wake once per day shortly after local midnight to extend the two-day window, even when no dose or snooze is pending sooner.

#### Scenario: Quiet day
- **WHEN** the only medicine is due every other day and today has no dose
- **THEN** an alarm is set for shortly after midnight and after it fires tomorrow's dose exists and the alarm is set for it

### Requirement: Notification actions are processed reliably
Notification actions SHALL be handled by a broadcast receiver that records the outcome or snooze,
updates the notification and reconciles the alarm set, all without starting the activity. Work that
cannot be guaranteed to finish inside the receiver budget SHALL be handed to the same short
foreground service the alarm wake uses.

#### Scenario: Action while app is stopped
- **WHEN** the app process is not running and the user taps "Not going to"
- **THEN** the dose is recorded as skipped and the notification disappears

#### Scenario: Action on a cold start
- **WHEN** the app process is not running, the database has to be opened and the user taps "I took it"
- **THEN** the outcome is recorded and the alarm set is reconciled, even when opening the database is slow

### Requirement: A withdrawn dose loses its reminder
When the refresh of the planning window withdraws a pending dose, the app SHALL remove any notification showing for that dose, in the same wake, before it works out when to wake next. Cancelling a notification for a dose that was never shown SHALL have no effect.

#### Scenario: Reminder for a withdrawn dose disappears
- **WHEN** a reminder for a dose is showing and an edit to the medicine withdraws that dose
- **THEN** the notification is gone and no action on it can record an outcome

#### Scenario: Doses withdrawn before they were shown
- **WHEN** the refresh withdraws pending doses that were never reminded
- **THEN** nothing is shown or removed and the wake completes normally

#### Scenario: Next alarm still set
- **WHEN** a wake withdraws doses and cancels their notifications
- **THEN** the next alarm is computed from what remains and is set

### Requirement: The refresh is told whether the user changed a medicine
Each wake SHALL tell the refresh whether it follows a change the user made to a medicine or its schedules. A wake caused by a medication change SHALL say so; a wake caused by an alarm, a boot, an app start, an app update, a notification action, a clock or time-zone change, or an exact-alarm permission change SHALL not. Only a wake that says so may withdraw a dose that has already been reminded.

#### Scenario: Medication change permits withdrawal
- **WHEN** the medication list changes and the wake runs
- **THEN** the refresh is allowed to withdraw doses that have already been reminded

#### Scenario: Time change does not permit withdrawal
- **WHEN** the system time zone changes and the wake runs
- **THEN** the refresh withdraws no dose that has already been reminded

#### Scenario: Alarm wake does not permit withdrawal
- **WHEN** the reminder alarm fires and the wake runs
- **THEN** the refresh withdraws no dose that has already been reminded

### Requirement: An answer given inside the app is processed like a notification action
An answer the user gives inside the app SHALL be processed by the same path as the notification action of the same name: the outcome or the snooze is recorded, any notification showing for that dose is removed, and a wake is run so lapsed doses are settled, the planning window is refreshed and the next alarm is recomputed. The app MUST NOT have a second implementation of what an answer means; both surfaces call one.

The wake that follows an in-app answer SHALL NOT be treated as a change the user made to a medicine, so it MUST NOT withdraw a dose that has already been reminded.

#### Scenario: Taken from inside the app
- **WHEN** the user records a dose as taken inside the app while its notification is showing
- **THEN** the outcome is recorded, the notification is gone, and the next alarm is recomputed from the doses that remain

#### Scenario: Postponed from inside the app
- **WHEN** the user postpones a dose inside the app at 08:00
- **THEN** the dose is still pending, its notification is gone, and the next alarm is set so the reminder returns at 08:15

#### Scenario: Skipped from inside the app
- **WHEN** the user records a dose as skipped inside the app
- **THEN** the outcome is recorded, the notification is gone, and no reminder for that dose is scheduled again

#### Scenario: Answer and notification action agree
- **WHEN** the same answer is given once from the notification and once from inside the app, on two equivalent doses
- **THEN** both doses end in the same state, with the same outcome or the same snooze moment relative to the tap

#### Scenario: The alarm is set even when the wake fails
- **WHEN** an in-app answer is recorded and a later step of the wake throws
- **THEN** the outcome is still recorded and the next alarm is still set

#### Scenario: An in-app answer withdraws no reminded dose
- **WHEN** the user answers one dose inside the app and another of their doses has already been reminded
- **THEN** the other dose is left exactly where it is

### Requirement: Per-dose alarms and one housekeeping alarm
The app SHALL keep one alarm per moment it has to act on, rather than a single alarm for the
earliest of them. The set SHALL consist of:

- one alarm at the scheduled moment of every un-reminded pending dose in the planning window,
- one alarm at the end of every outstanding snooze,
- one alarm at every outstanding reminder repeat,
- one housekeeping alarm at the earliest of the next dose lapse moment and the next daily refresh
  moment.

The set SHALL be reconciled after every wake, every notification action, every medication change and
every system event this specification lists: alarms no longer wanted are cancelled and alarms not yet
armed are set. The app SHALL record which alarms it has armed, so that a later reconcile and the
watchdog can both tell what is missing. When there are no active medicines with schedules and no
pending doses, no alarm MUST be set.

Losing one alarm MUST cost at most the reminder for that alarm: the remaining alarms SHALL still
fire, and the app MUST NOT depend on one alarm firing in order to arm the next.

#### Scenario: Two doses today
- **WHEN** un-reminded doses exist at 08:00 and 20:00 and the next daily refresh is at 00:05 tomorrow
- **THEN** alarms are armed for 08:00, for 20:00 and for the housekeeping moment

#### Scenario: One alarm dropped does not stop the rest
- **WHEN** the platform drops the alarm for the 08:00 dose
- **THEN** the alarm for the 20:00 dose still fires and its reminder is posted at 20:00

#### Scenario: Snooze adds an alarm
- **WHEN** the user taps "Not yet" at 08:00 on a dose that also has a repeat pending
- **THEN** an alarm is armed for 08:15 and the pending repeat alarm for that dose is cancelled

#### Scenario: Answering removes an alarm
- **WHEN** the user taps "I took it" on the 08:00 dose
- **THEN** the alarms for that dose are cancelled and the alarms for the other doses are unchanged

#### Scenario: Medication deactivated
- **WHEN** the user deactivates a medicine whose dose had an alarm armed
- **THEN** that alarm is cancelled

#### Scenario: Nothing to remind
- **WHEN** there are no medicines
- **THEN** no alarm is set

