# wearable-app Specification

## Purpose
TBD - created by archiving change app-wearable-support. Update Purpose after archive.
## Requirements
### Requirement: Wear OS companion app
Pillsner SHALL ship a Wear OS application with the same application id and signing as the phone app, declared as a companion that requires the phone app. It SHALL consist of a single screen and MUST NOT offer any action that creates, changes, confirms, snoozes or skips medicines or doses.

#### Scenario: Watch app opens to the list
- **WHEN** the user opens Pillsner on the watch
- **THEN** the upcoming doses screen is shown and there is no other destination

#### Scenario: No write actions
- **WHEN** the watch screen is inspected
- **THEN** it contains no button, gesture or menu that changes any dose or medicine

### Requirement: Six-hour list
The watch screen SHALL list the pending doses scheduled from now until six hours from now, ordered by scheduled time ascending. Pending doses whose scheduled time has already passed and that have not been answered SHALL be included, before the future ones. Taken, skipped and missed doses MUST NOT be shown.

#### Scenario: Doses inside the window
- **WHEN** it is 12:00 and pending doses exist at 13:00, 17:30 and 18:30
- **THEN** the list shows 13:00 and 17:30 and does not show 18:30

#### Scenario: Window boundary
- **WHEN** it is 12:00 and a pending dose is scheduled at exactly 18:00
- **THEN** the dose is shown

#### Scenario: Overdue pending dose
- **WHEN** it is 12:00 and a pending unanswered dose from 11:40 exists alongside one at 14:00
- **THEN** the 11:40 dose is listed first, then the 14:00 dose

#### Scenario: Answered dose disappears
- **WHEN** a listed dose is recorded as taken on the phone and the watch receives the update
- **THEN** the dose is no longer listed

#### Scenario: Dose enters the window with time
- **WHEN** it is 11:59 and a dose is scheduled at 18:00
- **THEN** at 12:00 the dose appears in the list without any user action

### Requirement: Entry content
Each list entry SHALL show the medicine name, the dose amount as formatted by the phone, and the scheduled time formatted for the display language in the watch's time zone. When the scheduled date is not today the entry MUST indicate that it is tomorrow. A screen reader MUST read each entry as one item containing name, amount and time.

#### Scenario: Entry today
- **WHEN** a dose of 40 mg of "Ibuprofen" is scheduled at 14:00 today
- **THEN** the entry shows "Ibuprofen", "40 mg" and "14:00"

#### Scenario: Entry tomorrow
- **WHEN** it is 22:00 and a dose is scheduled at 02:00 the next day
- **THEN** the entry shows the time with an indication that it is tomorrow

#### Scenario: Screen reader
- **WHEN** a screen reader focuses an entry
- **THEN** it announces the name, the amount and the time together

### Requirement: Empty state
When no pending dose falls in the six-hour window, the screen SHALL show the text "No medicines scheduled for the upcoming 6 hours" from a string resource instead of the list.

#### Scenario: Nothing in the window
- **WHEN** the next pending dose is more than six hours away
- **THEN** the screen shows "No medicines scheduled for the upcoming 6 hours" and no entries

#### Scenario: No doses at all
- **WHEN** the phone has published an empty list
- **THEN** the screen shows "No medicines scheduled for the upcoming 6 hours"

#### Scenario: Empty state gives way to an entry
- **WHEN** the empty state is shown and a dose enters the window as time passes
- **THEN** the empty state is replaced by that entry

### Requirement: Phone connection footer
When no phone is connected to the watch, the screen SHALL show a footer stating that the phone is not connected, below the list or the empty state. When no data has ever been received and no phone is connected, the footer SHALL instead tell the user to open Pillsner on the phone to sync. The footer MUST disappear when the phone reconnects.

#### Scenario: Phone out of range
- **WHEN** the paired phone is out of Bluetooth range and not on the same network
- **THEN** the screen shows the last received list and the footer "Phone not connected"

#### Scenario: Never synced
- **WHEN** the watch app is opened before any data has arrived and the phone is not connected
- **THEN** the screen shows the empty state and the footer asking to open Pillsner on the phone

#### Scenario: Reconnected
- **WHEN** the phone reconnects
- **THEN** the footer disappears within one minute

### Requirement: Language follows the phone app
The watch SHALL render its text and time formatting in the language published by the phone app. When no data has been received, it SHALL use the watch's system language. All watch strings MUST have English and Dutch translations, and a missing translation MUST fail the build.

#### Scenario: Dutch phone app
- **WHEN** the phone app is set to Dutch and publishes data
- **THEN** the watch header, empty state and footer are shown in Dutch and amounts read as published

#### Scenario: Language changed on the phone
- **WHEN** the user switches the phone app from Dutch to English and the phone republishes
- **THEN** the watch renders in English on the next update

#### Scenario: Missing translation
- **WHEN** a watch string has no Dutch translation
- **THEN** the build fails

### Requirement: Wear form factor and accessibility
The screen SHALL be usable on round and square watch screens, support rotary and touch scrolling, respect the watch font size setting without clipping text, and keep the system time visible at the top.

#### Scenario: Round screen
- **WHEN** the app runs on a round watch
- **THEN** no text is cut off by the screen edge and the list scrolls to reveal every entry

#### Scenario: Large font
- **WHEN** the watch font size is at its largest
- **THEN** every entry's name, amount and time remain fully visible, wrapping where needed

#### Scenario: Rotary scrolling
- **WHEN** the user turns the rotary input
- **THEN** the list scrolls

### Requirement: No sensitive logging on the watch
The watch app MUST NOT log medicine names or amounts at info level or above.

#### Scenario: Log inspection
- **WHEN** the watch module is inspected by lint
- **THEN** no info, warning or error log statement includes a medicine name or amount

