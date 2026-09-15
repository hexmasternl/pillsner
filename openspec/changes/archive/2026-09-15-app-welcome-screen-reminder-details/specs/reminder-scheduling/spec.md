## ADDED Requirements

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
