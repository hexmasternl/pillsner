## ADDED Requirements

### Requirement: Edge-to-edge display uses no deprecated window APIs
The app's window theme (`Theme.Pillsner` and its night variant) SHALL NOT set `android:statusBarColor`, `android:navigationBarColor`, `android:windowLightStatusBar` or any other window attribute deprecated by Android's edge-to-edge enforcement (API 35+). Status and navigation bar transparency and icon contrast SHALL be produced solely by calling the platform's edge-to-edge API at activity creation and by setting bar appearance through `WindowInsetsControllerCompat` at runtime, matching the chosen theme (see "The whole app follows the chosen theme").

#### Scenario: No deprecated theme attributes
- **WHEN** `Theme.Pillsner`'s light and dark definitions are inspected
- **THEN** neither sets `android:statusBarColor`, `android:navigationBarColor` nor `android:windowLightStatusBar`

#### Scenario: Edge-to-edge still active on Android 15 and later
- **WHEN** the app is cold started on a device or emulator running Android 15 (API 35) or later
- **THEN** content draws edge-to-edge behind transparent status and navigation bars, with no content obscured by either bar

#### Scenario: Edge-to-edge still active below Android 15
- **WHEN** the app is cold started on a device or emulator running an Android version below API 35, down to the app's minimum supported version
- **THEN** content draws edge-to-edge behind transparent status and navigation bars, matching the API 35+ behaviour
