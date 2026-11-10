## ADDED Requirements

### Requirement: Every shipped activity enables edge-to-edge
Every activity in every module Pillsner publishes to Google Play, meaning the phone app and the Wear OS app, SHALL call the platform's edge-to-edge API (`enableEdgeToEdge()`) in `onCreate`, before `super.onCreate()`. No activity SHALL set `android:windowOptOutEdgeToEdgeEnforcement` or otherwise opt out of edge-to-edge. A guard test SHALL fail the build when an activity in either module does not enable edge-to-edge.

#### Scenario: Phone activity enables edge-to-edge
- **WHEN** the phone app's `MainActivity` is created
- **THEN** it calls `enableEdgeToEdge()` before `super.onCreate()`

#### Scenario: Watch activity enables edge-to-edge
- **WHEN** the Wear OS app's `MainActivity` is created
- **THEN** it calls `enableEdgeToEdge()` before `super.onCreate()`

#### Scenario: A new activity forgets edge-to-edge
- **WHEN** an activity is added to either module without calling `enableEdgeToEdge()`
- **THEN** the guard test fails

#### Scenario: No opt-out
- **WHEN** both modules' manifests and themes are inspected
- **THEN** none sets `android:windowOptOutEdgeToEdgeEnforcement`

### Requirement: Content stays clear of system bars, cutouts and the keyboard
Every phone screen, dialog, picker and sheet SHALL draw its background edge-to-edge while keeping its text, tap targets and actions inside the safe drawing area: clear of the status bar, the navigation bar in both gesture and 3-button mode, any display cutout in portrait and landscape, and the on-screen keyboard while it is open. Each inset SHALL be applied exactly once, so that no screen shows doubled padding, for example above the bottom navigation bar. Insets SHALL be obtained from `WindowInsets` or `Scaffold`'s `contentWindowInsets` and MUST NOT be approximated with fixed dp values.

#### Scenario: Confirming a dose with 3-button navigation
- **WHEN** the dose detail screen is open on Android 15 or later with 3-button navigation
- **THEN** every answer button is fully visible above the navigation bar and can be tapped

#### Scenario: Landscape with a display cutout
- **WHEN** any phone screen is shown in landscape on a device with a display cutout
- **THEN** no text or tap target is drawn under the cutout

#### Scenario: Keyboard open on a form
- **WHEN** the user edits a field near the bottom of the medication form or schedule editor
- **THEN** the focused field and the save action stay visible above the keyboard

#### Scenario: Top-level screen above the bottom navigation
- **WHEN** Home, Medicines or Settings is shown with the bottom navigation bar
- **THEN** the last item in the list can be scrolled fully above the bottom navigation bar and there is no empty band the height of the system navigation bar between the content and the bottom navigation bar

#### Scenario: Large font
- **WHEN** the system font size is at its largest on Android 15 or later
- **THEN** no screen title, list item or action is hidden behind the status bar or navigation bar
