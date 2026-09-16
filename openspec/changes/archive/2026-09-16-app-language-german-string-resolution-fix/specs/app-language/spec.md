## MODIFIED Requirements

### Requirement: Everything follows the app language
All user-facing text, dates, times, weekday names, number formats and alphabetical ordering SHALL follow the resolved app language, including text produced outside the activity such as reminder notifications. Code that resolves resources outside an activity MUST obtain them through the app's localised context helper.

#### Scenario: Screen text
- **WHEN** the app starts in Dutch
- **THEN** the bottom navigation reads "Home", "Medicijnen", "Instellingen" and the Settings title reads "Instellingen"

#### Scenario: Schedule description weekday names
- **WHEN** the app runs in Dutch and a schedule is once a day on Monday, Wednesday and Friday
- **THEN** the description uses Dutch short weekday names

#### Scenario: Decimal format
- **WHEN** the app runs in Dutch and a dose is 2.5 ml
- **THEN** the amount is shown as "2,5 ml"

#### Scenario: Notification text
- **WHEN** Dutch is the app language and a reminder notification is posted from a receiver while no activity exists
- **THEN** the notification title and actions are in Dutch

#### Scenario: Overview ordering
- **WHEN** the app runs in Dutch
- **THEN** medicines on the overview are ordered with a Dutch collation

#### Scenario: New language screen text
- **WHEN** the app starts in French
- **THEN** the bottom navigation and Settings title are shown in French

#### Scenario: German screen text through the localised context helper
- **WHEN** the app's language resolves to German, whether chosen explicitly or matched from the phone's own languages, and a string is read through the app's localised context helper
- **THEN** the string returned is the German translation, not the English default

#### Scenario: German restart notice
- **WHEN** the user chooses German in the language section
- **THEN** the restart notice reads "Starte Pillsner neu, um die neue Sprache zu verwenden", matching the checked-in `values-de` resource, not the English default
