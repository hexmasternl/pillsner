# Spec Delta

## MODIFIED Requirements

### Requirement: Wear form factor and accessibility
Both watch screens SHALL be usable on round and square watch screens, support rotary and touch scrolling, respect the watch font size setting without clipping text, and keep the system time visible at the top.

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
- **WHEN** the watch font size is at its largest on a small round watch and the screen shows the empty state with or without the footer
- **THEN** every line that is on screen without scrolling lies entirely inside the circle of the display

#### Scenario: First lines clear the bezel
- **WHEN** the agenda or the details screen opens on a small round watch, scrolled to the top
- **THEN** the first heading or line of text lies entirely inside the circle of the display

#### Scenario: Large font
- **WHEN** the watch font size is at its largest
- **THEN** every entry's name and amount and every details line remain fully visible, wrapping where needed

#### Scenario: Rotary scrolling
- **WHEN** the user turns the rotary input
- **THEN** the list scrolls
