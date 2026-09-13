## MODIFIED Requirements

### Requirement: Placeholder Medicines and Settings destinations
The Medicines destination SHALL display the medicine overview screen. The Settings destination SHALL display the settings screen: a scrolling list of sections under the title "Settings", with the Language section first. Other changes add their sections beneath it.

#### Scenario: Medicines shows the overview
- **WHEN** the Medicines destination is shown
- **THEN** the medicine overview screen is displayed with its add button

#### Scenario: Settings shows sections
- **WHEN** the Settings destination is shown
- **THEN** a screen titled "Settings" is displayed whose first section is "Language" with the language dropdown

#### Scenario: Sections scroll at large font
- **WHEN** the system font scale is at maximum and the settings screen has more than one section
- **THEN** every section is reachable by scrolling and no text is clipped
