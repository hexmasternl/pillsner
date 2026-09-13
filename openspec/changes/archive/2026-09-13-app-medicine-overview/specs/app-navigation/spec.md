## MODIFIED Requirements

### Requirement: Placeholder Medicines and Settings destinations
The Medicines destination SHALL display the medicine overview screen. Until its own change delivers content, the Settings destination SHALL display its title from a string resource so that navigation is observable and testable.

#### Scenario: Medicines shows the overview
- **WHEN** the Medicines destination is shown
- **THEN** the medicine overview screen is displayed with its add button

#### Scenario: Settings placeholder
- **WHEN** the Settings destination is shown
- **THEN** a screen with the title "Settings" is displayed

## ADDED Requirements

### Requirement: Add medicine destination
The navigation host SHALL include a nested Add medicine destination reachable from the Medicines screen. The bottom navigation bar MUST be hidden on this destination. Until the add-medicine change delivers the form, the destination SHALL display the title "Add medicine" from a string resource and a back affordance.

#### Scenario: Reached from the overview
- **WHEN** the user taps the add button on the Medicines screen
- **THEN** the Add medicine destination is shown and the bottom navigation bar is not visible

#### Scenario: Back returns to Medicines
- **WHEN** the user is on the Add medicine destination and presses back or the back affordance
- **THEN** the Medicines destination is shown with the Medicines item selected and the bottom navigation bar visible again

#### Scenario: Placeholder title
- **WHEN** the Add medicine destination is shown before the add-medicine change is applied
- **THEN** the screen displays the title "Add medicine"
