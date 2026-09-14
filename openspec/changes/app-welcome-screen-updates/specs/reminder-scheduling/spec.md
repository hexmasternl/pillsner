## ADDED Requirements

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
