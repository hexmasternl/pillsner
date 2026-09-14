## ADDED Requirements

### Requirement: App identity is read from the build
The app SHALL expose its own identity as a single value holding the app name, the application id, the version name and the version code. The application id, version name and version code MUST come from the build configuration rather than from text written in code, so they can never disagree with the installed package. The value MUST be a domain type with no Android framework dependencies.

#### Scenario: Values match the build
- **WHEN** the app identity is read in a build whose application id is `nl.hexmaster.pillsner`, version name `0.1.0` and version code `1`
- **THEN** it reports exactly those three values, and the app name "Pillsner"

#### Scenario: Version bump is reflected without code changes
- **WHEN** the version name and version code are raised in the build configuration and the app is rebuilt
- **THEN** the app identity reports the new version without any change to Kotlin source

### Requirement: About section on Settings
The Settings screen SHALL show an About section as its last section, beneath Security. The section SHALL contain a single row titled "About Pillsner" whose supporting text states the installed version name and version code, with a chevron indicating that it opens another screen. The row MUST be at least the minimum touch target high and MUST be operable as a button.

#### Scenario: Section is present and last
- **WHEN** the user opens Settings
- **THEN** an About section is shown below the Security section, and no section appears after it

#### Scenario: Version visible without opening the screen
- **WHEN** the installed build has version name `0.1.0` and version code `1`
- **THEN** the About row's supporting text reads "Version 0.1.0 (1)"

#### Scenario: Screen reader on the row
- **WHEN** a screen reader focuses the About row
- **THEN** it announces the row's title and that it is a button

#### Scenario: Reachable at large font scale
- **WHEN** the system font scale is at maximum
- **THEN** the About row is reachable by scrolling Settings and none of its text is clipped

### Requirement: Opening and leaving the About screen
Activating the About row SHALL open the About screen. The About screen SHALL be a secondary destination: the bottom navigation bar or rail MUST NOT be visible while it is shown. Both the screen's back affordance and the system back gesture SHALL return to Settings with the Settings navigation item still selected.

#### Scenario: Open from Settings
- **WHEN** the user taps the About row on Settings
- **THEN** the About screen is shown and the bottom navigation bar is not visible

#### Scenario: Back affordance returns to Settings
- **WHEN** the user taps the back arrow on the About screen
- **THEN** Settings is shown with the Settings navigation item selected and the bottom navigation bar visible again

#### Scenario: System back returns to Settings
- **WHEN** the user presses back while the About screen is shown
- **THEN** Settings is shown

#### Scenario: Survives process death
- **WHEN** the process is killed and restored while the About screen is open
- **THEN** the About screen is shown again with the same content

### Requirement: About screen content
The About screen SHALL have a top app bar titled "About" with a back affordance, and SHALL show, in this order: the product mark with the app name "Pillsner"; a paragraph explaining that the name blends *Pills* and *Partner*; and labelled values for the version, the application id and the author. The author SHALL be "Eduard Keilholz". The version SHALL show both the version name and the version code. The application id SHALL be shown in full.

#### Scenario: Identity is shown
- **WHEN** the About screen is shown
- **THEN** the product mark and the name "Pillsner" are displayed above everything else

#### Scenario: The name is explained
- **WHEN** the About screen is shown
- **THEN** a paragraph states that Pillsner blends the words *Pills* and *Partner*

#### Scenario: Facts are shown
- **WHEN** the About screen is shown for a build with version name `0.1.0`, version code `1` and application id `nl.hexmaster.pillsner`
- **THEN** a version row reads "0.1.0 (1)", an application id row reads "nl.hexmaster.pillsner", and an author row reads "Eduard Keilholz"

#### Scenario: Nothing on the screen navigates away
- **WHEN** the user taps anywhere in the content of the About screen
- **THEN** the screen does not change and no other app, browser or screen is opened

### Requirement: About screen is readable and accessible
The About screen SHALL remain fully readable at a system font scale of 200 %: content MUST scroll and text MUST wrap, and no text may be truncated. Each labelled value SHALL be announced by a screen reader as its label followed by its value. The product mark SHALL carry the app name as its content description.

#### Scenario: Largest font scale
- **WHEN** the About screen is shown at maximum font scale
- **THEN** every piece of content is reachable by scrolling, and no label or value is cut off or ellipsised

#### Scenario: Screen reader on a value
- **WHEN** a screen reader focuses the author row
- **THEN** it announces the label "Author" followed by "Eduard Keilholz"

#### Scenario: Dark theme
- **WHEN** the phone is set to dark theme and the About screen is shown
- **THEN** the screen uses the app's dark colour scheme with no hard-coded light colours

### Requirement: About text is translated
Every user-facing string introduced by the About section and the About screen SHALL exist in English and Dutch. The app name, the author's name and the application id MUST NOT be translated, and the words *Pills* and *Partner* MUST stay in English in the Dutch paragraph because the name is a play on those English words.

#### Scenario: Dutch UI
- **WHEN** the app language is Dutch and the user opens the About screen
- **THEN** the top app bar title, the labels and the explanation paragraph are in Dutch

#### Scenario: Names untranslated
- **WHEN** the app language is Dutch and the About screen is shown
- **THEN** the app name reads "Pillsner", the author reads "Eduard Keilholz", the application id reads "nl.hexmaster.pillsner", and the paragraph still contains the words "Pills" and "Partner"

#### Scenario: Lint completeness
- **WHEN** the lint task runs after this change
- **THEN** no missing-translation or extra-translation error is reported for the new strings

### Requirement: About adds no capability beyond showing information
The About section and the About screen SHALL NOT introduce any permission, network access, third-party dependency or outbound link, and SHALL NOT read or write any user data.

#### Scenario: No new permission
- **WHEN** the manifest is compared before and after this change
- **THEN** the set of declared permissions is unchanged

#### Scenario: No stored data touched
- **WHEN** the About screen is opened and closed
- **THEN** no medication, dose, intake or settings value is read or written
