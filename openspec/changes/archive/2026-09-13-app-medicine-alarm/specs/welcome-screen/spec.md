## MODIFIED Requirements

### Requirement: Upcoming doses list
The welcome screen SHALL show the user's pending doses ordered by scheduled time, soonest first. A pending dose whose scheduled time has passed but which has not been answered and has not lapsed as missed MUST be included and appears first. Doses that are taken, skipped or missed MUST NOT be shown. It MUST show at most five doses, even when more are available.

#### Scenario: Fewer than five upcoming doses
- **WHEN** three pending doses exist
- **THEN** exactly three tiles are shown, ordered by scheduled time ascending

#### Scenario: More than five upcoming doses
- **WHEN** eight pending doses exist
- **THEN** exactly five tiles are shown, and they are the five with the earliest scheduled times

#### Scenario: Overdue pending dose
- **WHEN** a dose scheduled at 08:00 is unanswered at 09:00 and another dose is due at 20:00
- **THEN** the 08:00 dose is shown first, followed by the 20:00 dose

#### Scenario: Answered dose disappears
- **WHEN** a dose shown on the welcome screen is recorded as taken from the notification
- **THEN** its tile disappears without the user leaving and re-entering the screen

#### Scenario: Doses update while the screen is visible
- **WHEN** the set of pending doses changes while the welcome screen is displayed
- **THEN** the tiles update to reflect the new set without the user leaving and re-entering the screen

### Requirement: Upcoming doses are provided through a domain contract
The welcome screen SHALL obtain upcoming doses only through an `UpcomingDosesRepository` interface in the domain layer that exposes them as a reactive stream. `UpcomingDose` SHALL carry the amount as a typed `Quantity`, formatted for display in the UI layer. The domain layer types for this contract MUST NOT depend on Android framework classes. The app SHALL be wired with the Room-backed implementation that reads pending doses from the dose records.

#### Scenario: Room-backed repository
- **WHEN** the dose records contain pending doses
- **THEN** the welcome screen shows them, formatted with the same amount formatter as the medicine overview

#### Scenario: Empty records
- **WHEN** there are no pending doses
- **THEN** the welcome screen shows the empty state

#### Scenario: View model enforces the cap
- **WHEN** the repository emits more than five doses despite being asked for five
- **THEN** the view model exposes only the first five to the screen

## ADDED Requirements

### Requirement: Reminder readiness banner
The welcome screen SHALL show a banner above the dose list when reminders cannot be delivered as designed: when notification permission is denied, or when exact alarms are not permitted. The banner text MUST come from string resources and MUST offer a button that opens the relevant system setting. The banner MUST disappear once the condition is resolved.

#### Scenario: Notifications denied
- **WHEN** notification permission is denied
- **THEN** the banner says reminders cannot be shown and its button opens the app's notification settings

#### Scenario: Exact alarms not permitted
- **WHEN** exact alarms are not permitted
- **THEN** the banner says reminders may be up to ten minutes late and its button opens the exact-alarm setting

#### Scenario: Everything permitted
- **WHEN** notifications and exact alarms are both permitted
- **THEN** no banner is shown

#### Scenario: Banner is accessible
- **WHEN** a screen reader focuses the banner
- **THEN** it reads the message and the button label
