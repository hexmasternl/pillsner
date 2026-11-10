# Spec Delta

## MODIFIED Requirements

### Requirement: Wear form factor and accessibility
Both watch screens SHALL be usable on round and square watch screens, support rotary and touch scrolling, respect the watch font size setting without clipping text, and keep the system time visible at the top. The watch activity SHALL enable edge-to-edge explicitly at creation (see `app-theme`, "Every shipped activity enables edge-to-edge") and leave inset and round-screen padding to the Wear Compose scaffolds, so the app looks the same on every supported Wear OS version.

#### Scenario: Round screen
- **WHEN** the app runs on a round watch
- **THEN** no text is cut off by the screen edge and the list scrolls to reveal every entry

#### Scenario: Empty state on a round screen
- **WHEN** the app runs on a small round watch, no dose is pending today or tomorrow, and the phone is connected
- **THEN** every line of "No medicines scheduled for today or tomorrow" lies entirely inside the circle of the display

#### Scenario: Footer on a round screen
- **WHEN** the app runs on a small round watch and the phone is not connected, whether or not data was ever received
- **THEN** every line of the empty state and of the footer lies entirely inside the circle of the display

#### Scenario: Round screen at the largest font
- **WHEN** the watch font size is at its largest on a small round watch and the screen shows the empty state with or without the footer, in any of the supported languages
- **THEN** every line in the upper half of the screen lies entirely inside the circle at rest, and where the text is taller than the screen, every line in the lower half lies inside it once scrolled to the end

#### Scenario: First lines clear the bezel
- **WHEN** the agenda or the details screen opens on a small round watch, scrolled to the top
- **THEN** the first heading or line of text lies entirely inside the circle of the display

#### Scenario: Last entry clears the bezel
- **WHEN** the agenda holds more doses than fit on a small round watch and the user scrolls to the end, at the default or at the largest watch font size
- **THEN** the whole outline of the last dose entry, including its rounded corners, lies inside the circle of the display

#### Scenario: Mark and version clear the bezel
- **WHEN** the agenda is scrolled to its end on a small round watch, with doses or in the empty state, with or without the phone connection footer, at the default or at the largest watch font size, in any of the supported languages
- **THEN** the whole product mark and every line of the version text lie inside the circle of the display

#### Scenario: Large font
- **WHEN** the watch font size is at its largest
- **THEN** every entry's name and amount and every details line remain fully visible, wrapping where needed

#### Scenario: Rotary scrolling
- **WHEN** the user turns the rotary input
- **THEN** the list scrolls

#### Scenario: Edge-to-edge on the watch
- **WHEN** the watch app is started on a watch running a Wear OS version based on Android 15 or later
- **THEN** the activity has enabled edge-to-edge, the system time stays visible at the top and no entry is hidden behind system UI

### Requirement: Language follows the phone app
The watch SHALL render its text and time formatting in the language published by the phone app. When no data has been received, it SHALL use the watch's system language. All watch strings MUST have English, Dutch, German, French, Spanish and Portuguese translations, and a missing translation MUST fail the build.

#### Scenario: Dutch phone app
- **WHEN** the phone app is set to Dutch and publishes data
- **THEN** the watch headings, empty state, details labels, footer and version line are shown in Dutch and amounts read as published

#### Scenario: New language phone app
- **WHEN** the phone app is set to Portuguese and publishes data
- **THEN** the watch headings, empty state, details labels, footer and version line are shown in Portuguese and amounts read as published

#### Scenario: Language changed on the phone
- **WHEN** the user switches the phone app from Dutch to English and the phone republishes
- **THEN** the watch renders in English on the next update

#### Scenario: Missing translation
- **WHEN** a watch string has no translation for one of the supported languages
- **THEN** the build fails

## ADDED Requirements

### Requirement: App mark and version on the agenda
The agenda screen SHALL end with one item showing the product mark and, under it, the installed version name of the watch app as "Version" followed by the version name, from a string resource, both horizontally centred. The item SHALL be the last thing on the screen in every state: after the doses or the empty state, and after the phone connection footer when that is shown. The version name MUST come from the build configuration rather than from text written in code. The item MUST NOT be tappable or offer any action, and a screen reader SHALL read the version text and skip the mark. The details screen MUST NOT show the item.

#### Scenario: End of the agenda
- **WHEN** the agenda holds doses and the installed watch build has version name `0.1.0`
- **THEN** the last item on the screen, below the last dose, shows the product mark with "Version 0.1.0" under it, horizontally centred

#### Scenario: Empty state
- **WHEN** no dose is pending today or tomorrow
- **THEN** the mark and the version are shown below "No medicines scheduled for today or tomorrow"

#### Scenario: Below the connection footer
- **WHEN** the phone is not connected
- **THEN** the mark and the version are shown below the phone connection footer

#### Scenario: Version bump is reflected without code changes
- **WHEN** the version name is raised in the build configuration and the watch app is rebuilt
- **THEN** the agenda shows the new version without any change to Kotlin source

#### Scenario: Not an action
- **WHEN** the user taps the mark or the version
- **THEN** nothing happens

#### Scenario: Screen reader
- **WHEN** a screen reader reaches the end of the agenda
- **THEN** it announces the version text once and does not announce the mark separately

#### Scenario: Details screen
- **WHEN** the details of a dose are shown
- **THEN** neither the mark nor the version is shown
