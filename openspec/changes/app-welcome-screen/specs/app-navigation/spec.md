## ADDED Requirements

### Requirement: Single-activity shell
The app SHALL consist of a single activity that hosts a Compose navigation host. All screens MUST be composable destinations within that host.

#### Scenario: App launch
- **WHEN** the user opens the app from the launcher
- **THEN** one activity starts and renders the navigation host with the welcome screen as its content

### Requirement: Bottom navigation bar with three destinations
The app SHALL show a bottom navigation bar with exactly three items, in this order: Home, Medicines, Settings. Each item MUST have an icon and a text label sourced from string resources. The bar MUST be visible on each of these three top-level destinations.

#### Scenario: Bar visible on Home
- **WHEN** the welcome screen (Home) is displayed
- **THEN** the bottom bar shows Home, Medicines and Settings items with labels

#### Scenario: Bar visible on Medicines and Settings
- **WHEN** the user is on the Medicines or the Settings destination
- **THEN** the bottom bar remains visible with all three items

### Requirement: Home is the start destination
The Home item SHALL be selected and the welcome screen SHALL be shown when the app launches.

#### Scenario: Selected item on launch
- **WHEN** the app launches
- **THEN** the Home item is indicated as selected and neither Medicines nor Settings is selected

### Requirement: Navigating between top-level destinations
Tapping a bottom navigation item SHALL navigate to that destination and mark it as selected. Tapping Home SHALL return to the welcome screen. Tapping the already selected item MUST NOT create a duplicate destination on the back stack.

#### Scenario: Navigate to Medicines
- **WHEN** the user taps Medicines from Home
- **THEN** the Medicines destination is shown and the Medicines item is indicated as selected

#### Scenario: Navigate to Settings
- **WHEN** the user taps Settings from any top-level destination
- **THEN** the Settings destination is shown and the Settings item is indicated as selected

#### Scenario: Return to Home
- **WHEN** the user is on Settings and taps Home
- **THEN** the welcome screen is shown and the Home item is indicated as selected

#### Scenario: Re-tapping the selected item
- **WHEN** the user taps Medicines while already on Medicines
- **THEN** the destination is unchanged and pressing back afterwards behaves exactly as it would have without the extra tap

### Requirement: Back behaviour between top-level destinations
Pressing back on Medicines or Settings SHALL return to the welcome screen. Pressing back on the welcome screen SHALL leave the app.

#### Scenario: Back from Medicines
- **WHEN** the user navigated Home → Medicines and presses back
- **THEN** the welcome screen is shown and the Home item is selected

#### Scenario: Back from Home
- **WHEN** the user is on the welcome screen with no other destination on the back stack and presses back
- **THEN** the activity finishes

### Requirement: Placeholder Medicines and Settings destinations
Until their own changes deliver content, the Medicines and Settings destinations SHALL each display their title from a string resource so that navigation is observable and testable.

#### Scenario: Medicines placeholder
- **WHEN** the Medicines destination is shown
- **THEN** a screen with the title "Medicines" is displayed

#### Scenario: Settings placeholder
- **WHEN** the Settings destination is shown
- **THEN** a screen with the title "Settings" is displayed

### Requirement: Navigation items are accessible
Each bottom navigation item SHALL be operable with a screen reader and MUST expose its label and selected state.

#### Scenario: Screen reader on a navigation item
- **WHEN** a screen reader focuses the Medicines item while Home is selected
- **THEN** it announces the label "Medicines" and that the item is not selected
