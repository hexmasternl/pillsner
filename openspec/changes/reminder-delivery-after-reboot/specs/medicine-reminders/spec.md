## MODIFIED Requirements

### Requirement: Notification permission
On Android 13 and later the app SHALL request the notification permission the first time an active
medicine with at least one schedule exists and the permission has not been granted, at most once.
When the permission is denied, the Home screen SHALL show a banner stating that reminders cannot be
shown, with a button that opens the app's notification settings.

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
