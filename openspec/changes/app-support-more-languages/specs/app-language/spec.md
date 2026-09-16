## MODIFIED Requirements

### Requirement: Supported languages
The app SHALL support English, Dutch, German, French, Spanish and Portuguese. The set of supported languages MUST be defined in one place in the domain layer so that adding a language requires adding it there and adding its translated resources, and nothing else.

#### Scenario: Six languages available
- **WHEN** the supported-language list is read
- **THEN** it contains exactly English, Dutch, German, French, Spanish and Portuguese, with English marked as the fallback

#### Scenario: Language options derive from the list
- **WHEN** a language is added to the supported-language list with its resources
- **THEN** the Settings dropdown offers it without further code changes to the Settings screen

### Requirement: Language section on Settings
The Settings screen SHALL show a Language section containing a dropdown labelled "Language" whose current value is the stored selection. The options SHALL be, in order, "System default" followed by each supported language shown by its own native name ("English", "Nederlands", "Deutsch", "Français", "Español", "Português"). Native names MUST NOT be translated.

#### Scenario: Default display
- **WHEN** no language has ever been chosen and the user opens Settings
- **THEN** the dropdown shows "System default"

#### Scenario: Options listed
- **WHEN** the user opens the dropdown on a phone set to Dutch
- **THEN** the options read "Systeemstandaard", "English", "Nederlands", "Deutsch", "Français", "Español", "Português" in that order

#### Scenario: Stored selection shown
- **WHEN** the user previously chose Dutch and reopens Settings
- **THEN** the dropdown shows "Nederlands"

#### Scenario: New language stored selection shown
- **WHEN** the user previously chose German and reopens Settings
- **THEN** the dropdown shows "Deutsch"

### Requirement: Language resolution at startup
On each cold start the app SHALL determine its language as follows: if a language is stored, use it; otherwise use the first language in the phone's preferred-language list whose language matches a supported language, ignoring region; otherwise use English. The resolution rule MUST be implemented without Android framework dependencies and MUST be unit-tested.

#### Scenario: Stored language wins
- **WHEN** Dutch is stored and the phone's languages are English then German
- **THEN** the app starts in Dutch

#### Scenario: System default, supported phone language
- **WHEN** nothing is stored and the phone's languages are German then Dutch
- **THEN** the app starts in German

#### Scenario: System default, regional variant
- **WHEN** nothing is stored and the phone's first language is Belgian Dutch (nl-BE)
- **THEN** the app starts in Dutch

#### Scenario: System default, another regional variant
- **WHEN** nothing is stored and the phone's first language is Brazilian Portuguese (pt-BR)
- **THEN** the app starts in Portuguese

#### Scenario: System default, unsupported phone language
- **WHEN** nothing is stored and the phone's languages are Italian then Polish
- **THEN** the app starts in English

#### Scenario: Corrupt stored value
- **WHEN** the stored value is not a supported language tag
- **THEN** the app treats it as System default and resolves from the phone's languages

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

### Requirement: Translation completeness
Every translatable string resource SHALL have a Dutch, German, French, Spanish and Portuguese translation, and a missing or extra translation for any supported language MUST fail the project's lint task.

#### Scenario: Missing translation
- **WHEN** a string is added to the default resources without a translation for every supported language and lint runs
- **THEN** lint fails with a missing-translation error

#### Scenario: Complete translation
- **WHEN** lint runs on the completed change
- **THEN** no missing-translation or extra-translation errors are reported for any of the six languages
