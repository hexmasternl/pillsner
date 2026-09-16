## MODIFIED Requirements

### Requirement: Language follows the phone app
The watch SHALL render its text and time formatting in the language published by the phone app. When no data has been received, it SHALL use the watch's system language. All watch strings MUST have English, Dutch, German, French, Spanish and Portuguese translations, and a missing translation MUST fail the build.

#### Scenario: Dutch phone app
- **WHEN** the phone app is set to Dutch and publishes data
- **THEN** the watch header, empty state and footer are shown in Dutch and amounts read as published

#### Scenario: New language phone app
- **WHEN** the phone app is set to Portuguese and publishes data
- **THEN** the watch header, empty state and footer are shown in Portuguese and amounts read as published

#### Scenario: Language changed on the phone
- **WHEN** the user switches the phone app from Dutch to English and the phone republishes
- **THEN** the watch renders in English on the next update

#### Scenario: Missing translation
- **WHEN** a watch string has no translation for one of the supported languages
- **THEN** the build fails
