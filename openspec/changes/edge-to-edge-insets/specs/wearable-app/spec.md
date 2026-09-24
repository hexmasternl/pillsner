## MODIFIED Requirements

### Requirement: Wear form factor and accessibility
The screen SHALL be usable on round and square watch screens, support rotary and touch scrolling, respect the watch font size setting without clipping text, and keep the system time visible at the top. The watch activity SHALL enable edge-to-edge explicitly at creation (see `app-theme`, "Every shipped activity enables edge-to-edge") and leave inset and round-screen padding to the Wear Compose scaffolds, so the app looks the same on every supported Wear OS version.

#### Scenario: Round screen
- **WHEN** the app runs on a round watch
- **THEN** no text is cut off by the screen edge and the list scrolls to reveal every entry

#### Scenario: Large font
- **WHEN** the watch font size is at its largest
- **THEN** every entry's name, amount and time remain fully visible, wrapping where needed

#### Scenario: Rotary scrolling
- **WHEN** the user turns the rotary input
- **THEN** the list scrolls

#### Scenario: Edge-to-edge on the watch
- **WHEN** the watch app is started on a watch running a Wear OS version based on Android 15 or later
- **THEN** the activity has enabled edge-to-edge, the system time stays visible at the top and no entry is hidden behind system UI
