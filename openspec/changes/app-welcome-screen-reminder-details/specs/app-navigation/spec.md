## ADDED Requirements

### Requirement: Dose detail destination
The app shell SHALL include a Dose detail destination, reached only by tapping an upcoming dose tile on the Welcome screen, carrying the dose's identifier as a route argument. It MUST NOT be a top-level destination, and the bottom navigation bar and the navigation rail MUST remain hidden while it is shown. The identifier MUST survive configuration changes and process death as part of the route.

Back from the Dose detail destination — by the back arrow, by the system back gesture or by its "Close" control — SHALL return to the Welcome screen with the Home navigation item still selected. Answering a dose SHALL also leave the destination, taking it off the back stack, so back from the Welcome screen afterwards never returns to the dose that was just answered.

#### Scenario: Open from a dose tile
- **WHEN** the user taps an upcoming dose tile on the Welcome screen
- **THEN** the Dose detail destination is shown for that dose and the bottom navigation bar is not visible

#### Scenario: Back returns to Home
- **WHEN** the user presses back on the Dose detail destination
- **THEN** the Welcome screen is shown and the Home navigation item is selected

#### Scenario: Close returns to Home
- **WHEN** the user taps "Close" on the Dose detail destination
- **THEN** the Welcome screen is shown and the Home navigation item is selected

#### Scenario: Answering leaves the destination
- **WHEN** the user answers the dose and then presses back on the Welcome screen
- **THEN** the app does not return to the Dose detail destination

#### Scenario: Identifier survives process death
- **WHEN** the process is killed and restored while the Dose detail destination is open
- **THEN** the Dose detail destination is restored for the same dose

#### Scenario: Rotation on the Dose detail destination
- **WHEN** the device rotates while the Dose detail destination is open
- **THEN** the same dose is shown and the navigation bar is still hidden

#### Scenario: Not reachable from anywhere else
- **WHEN** the user is on the Medicines destination, the Settings destination, the medicine form or a reminder notification
- **THEN** there is no way to reach the Dose detail destination
