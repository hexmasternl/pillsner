## ADDED Requirements

### Requirement: Black watch background
Every watch app screen, the window shown before the first frame, the startup screen and any tile the app offers SHALL use a pure black background (`#000000`), as the Wear OS app quality guidelines require (WO-V13, WO-V15). Cards and buttons MAY use container colours on top of the black. All text SHALL meet WCAG AA contrast against the surface it sits on.

#### Scenario: Agenda on black
- **WHEN** the agenda opens with doses for today or tomorrow
- **THEN** every pixel of the screen background outside the cards, the text and the system time is `#000000`

#### Scenario: Empty and unreachable states on black
- **WHEN** the agenda shows the empty state, with or without the phone connection footer
- **THEN** every pixel of the screen background outside the text is `#000000`

#### Scenario: Dose details on black
- **WHEN** the details screen for a dose opens
- **THEN** every pixel of the screen background outside the text is `#000000`

#### Scenario: Startup on black
- **WHEN** the watch app is launched from the app list while its process is not running
- **THEN** the screen is black from the first frame until the agenda appears, and the only element shown before the agenda is the launcher icon

#### Scenario: Cards stand out on black
- **WHEN** the agenda shows a due dose and an overdue dose
- **THEN** each card's container colour differs from black, and the card's text meets at least 4.5 : 1 contrast against that container colour

#### Scenario: Secondary text on black
- **WHEN** a screen shows secondary text directly on the background, such as the connection footer or the app mark and version
- **THEN** that text meets at least 4.5 : 1 contrast against `#000000`
