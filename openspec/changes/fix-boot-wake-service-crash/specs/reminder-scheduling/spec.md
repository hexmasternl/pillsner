# Spec Delta

## MODIFIED Requirements

### Requirement: Processing on wake
When an alarm fires, the app SHALL hand the work to a short foreground service rather than performing
it inside the broadcast receiver, so that a cold start that has to open the database has a real
window in which to finish.

The service SHALL, in order: mark lapsed doses as missed, refresh the two-day window, post
notifications for every dose that is due or due to repeat, record as reminded only those doses whose
notification was posted, and reconcile the alarm set. The alarm set MUST be reconciled even if an
earlier step fails.

When the platform refuses the foreground service, whether it refuses starting the service or
promoting it to the foreground, the app MUST NOT crash. It SHALL record the refusal in the reminder
delivery log and SHALL still perform the work it was asked to do.

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

#### Scenario: Platform refuses foreground status
- **WHEN** the wake service has been started but the platform refuses to promote it to the foreground
- **THEN** the app does not crash, the refusal is recorded in the delivery log, and the wake still runs and reconciles the alarm set

#### Scenario: Due dose already reminded is re-posted on its repeat
- **WHEN** a wake occurs and a pending dose has already been reminded, is not snoozed, and its repeat moment has arrived
- **THEN** the notification for it is posted again

#### Scenario: Due dose already reminded is not re-posted early
- **WHEN** a wake occurs and a pending dose has already been reminded, is not snoozed, and its next repeat moment has not arrived
- **THEN** no second notification is posted for it

### Requirement: Recovery after reboot and app update
After the device reboots or the app is updated, the app SHALL refresh the window, post reminders for
doses that fell due in the meantime and have not lapsed, mark lapsed doses missed, and set the next
alarm, without the user opening the app.

The platform does not allow a foreground service of the kind the wake uses to start from a boot
broadcast. The wake that follows a boot broadcast SHALL therefore run without a foreground service,
and a reboot MUST NOT crash the app.

A reboot SHALL leave an alarm set before the device is first unlocked. Because medicines and doses
cannot be read before unlock, the app SHALL record the moment of every alarm it arms in storage that
is readable before unlock, and SHALL re-arm from that record on locked boot. What is recorded MUST be
the alarm moment alone: it MUST NOT include a medicine name, an amount or a dose identifier.

A full wake SHALL follow as soon as the user unlocks the device.

#### Scenario: Reboot across a dose
- **WHEN** the device is off from 07:50 to 08:10 and a dose was due at 08:00
- **THEN** shortly after boot the reminder for the 08:00 dose is shown

#### Scenario: Reboot does not crash
- **WHEN** the device finishes booting on a platform version that forbids starting the wake's foreground service from a boot broadcast
- **THEN** the app does not crash, the boot wake runs without a foreground service and the next alarm is set

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
