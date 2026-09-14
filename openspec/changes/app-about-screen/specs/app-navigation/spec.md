## ADDED Requirements

### Requirement: About destination
The navigation host SHALL include an About destination that is not top-level, reached only from the About row on the Settings screen. Its route MUST carry no arguments. The bottom navigation bar or rail MUST be hidden while it is shown. Back from the About screen, by either the back affordance or the system back gesture, SHALL return to Settings, and the Settings item SHALL then be selected again.

#### Scenario: Reached from Settings
- **WHEN** the user taps the About row on the Settings screen
- **THEN** the About destination is shown and the bottom navigation bar is not visible

#### Scenario: Back returns to Settings
- **WHEN** the user presses back or the back affordance on the About destination
- **THEN** the Settings destination is shown with the Settings item selected and the bottom navigation bar visible again

#### Scenario: Not a navigation item
- **WHEN** the bottom navigation bar is shown on any top-level destination
- **THEN** it still shows exactly the three items Home, Medicines and Settings, and no About item

#### Scenario: Back from About continues to Home
- **WHEN** the user navigated Home → Settings → About and presses back twice
- **THEN** the welcome screen is shown and the Home item is selected
