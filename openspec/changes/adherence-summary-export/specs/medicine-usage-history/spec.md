## ADDED Requirements

### Requirement: Share action on the Usage history screen
The Usage history screen's top app bar SHALL carry a "Share with your doctor" action, shown whenever figures (not the empty state) are on screen. Choosing it SHALL open the adherence export flow for the medicine and period currently displayed. The action MUST NOT be shown while the empty state is displayed, since there is nothing to export.

#### Scenario: Action available with figures on screen
- **WHEN** the Usage history screen shows figures for a period with at least one scheduled dose
- **THEN** the top app bar offers a "Share with your doctor" action

#### Scenario: Action hidden on the empty state
- **WHEN** the chosen period holds no scheduled dose and the empty state is shown
- **THEN** no "Share with your doctor" action is present in the top app bar

#### Scenario: Screen remains read-only
- **WHEN** the "Share with your doctor" action is used
- **THEN** no intake, dose or medicine record is created, changed or removed as a result
