## REMOVED Requirements

### Requirement: Six-hour list

**Reason**: Six hours is too short a horizon to answer "what am I taking today?" on the wrist: at half past eight in the morning it says nothing about the evening. Replaced by the "Today and tomorrow agenda" requirement, which lists every pending dose of today and tomorrow grouped by time. The phone already plans two days ahead, so the longer horizon needs no new data. See design D1.

## MODIFIED Requirements

### Requirement: Wear OS companion app
Pillsner SHALL ship a Wear OS application with the same application id and signing as the phone app, declared as a companion that requires the phone app. It SHALL consist of the agenda screen and the read-only dose details screen reached from it, and MUST NOT offer any action that creates, changes, confirms, snoozes or skips medicines or doses.

#### Scenario: Watch app opens to the agenda
- **WHEN** the user opens Pillsner on the watch
- **THEN** the agenda of today and tomorrow is shown

#### Scenario: The only other destination is read-only details
- **WHEN** the user taps a dose on the agenda
- **THEN** the details of the medicine behind it are shown, and the watch's back gesture returns to the agenda

#### Scenario: No write actions
- **WHEN** either watch screen is inspected
- **THEN** it contains no button, gesture, field or menu that changes any dose, schedule, stock level or medicine

### Requirement: Entry content
Each list entry SHALL show the medicine name and the dose amount as formatted by the phone, under a time heading giving the scheduled time formatted for the display language in the watch's time zone. A screen reader MUST read each entry as one item containing name, amount and time.

#### Scenario: Entry today
- **WHEN** a dose of 40 mg of "Ibuprofen" is scheduled at 14:00 today
- **THEN** "Ibuprofen" and "40 mg" appear under a "14:00" heading in the "Today" section

#### Scenario: Entry tomorrow
- **WHEN** a dose is scheduled at 02:00 the next day
- **THEN** it appears under a "02:00" heading in the "Tomorrow" section

#### Scenario: Screen reader
- **WHEN** a screen reader focuses an entry
- **THEN** it announces the name, the amount and the time together

### Requirement: Empty state
When no pending dose falls on today or tomorrow, the screen SHALL show the text "No medicines scheduled for today or tomorrow" from a string resource instead of the list.

#### Scenario: Nothing on either day
- **WHEN** the next pending dose is the day after tomorrow
- **THEN** the screen shows "No medicines scheduled for today or tomorrow" and no entries

#### Scenario: No doses at all
- **WHEN** the phone has published an empty list
- **THEN** the screen shows "No medicines scheduled for today or tomorrow"

#### Scenario: Empty state gives way to an entry
- **WHEN** the empty state is shown and the phone publishes a dose due today
- **THEN** the empty state is replaced by that entry

### Requirement: Wear form factor and accessibility
Both watch screens SHALL be usable on round and square watch screens, support rotary and touch scrolling, respect the watch font size setting without clipping text, and keep the system time visible at the top.

#### Scenario: Round screen
- **WHEN** the app runs on a round watch
- **THEN** no text is cut off by the screen edge and the list scrolls to reveal every entry

#### Scenario: Large font
- **WHEN** the watch font size is at its largest
- **THEN** every entry's name and amount and every details line remain fully visible, wrapping where needed

#### Scenario: Rotary scrolling
- **WHEN** the user turns the rotary input
- **THEN** the list scrolls

### Requirement: Language follows the phone app
The watch SHALL render its text and time formatting in the language published by the phone app. When no data has been received, it SHALL use the watch's system language. All watch strings MUST have English, Dutch, German, French, Spanish and Portuguese translations, and a missing translation MUST fail the build.

#### Scenario: Dutch phone app
- **WHEN** the phone app is set to Dutch and publishes data
- **THEN** the watch headings, empty state, details labels and footer are shown in Dutch and amounts read as published

#### Scenario: New language phone app
- **WHEN** the phone app is set to Portuguese and publishes data
- **THEN** the watch headings, empty state, details labels and footer are shown in Portuguese and amounts read as published

#### Scenario: Language changed on the phone
- **WHEN** the user switches the phone app from Dutch to English and the phone republishes
- **THEN** the watch renders in English on the next update

#### Scenario: Missing translation
- **WHEN** a watch string has no translation for one of the supported languages
- **THEN** the build fails

## ADDED Requirements

### Requirement: Today and tomorrow agenda
The watch screen SHALL list every pending dose scheduled for today or tomorrow in the watch's own time zone, under a "Today" heading and a "Tomorrow" heading from string resources, in that order, and within each day grouped under the time the doses are due, times ascending. Doses due at the same time SHALL share one time heading. A day with no pending dose MUST NOT show its heading. Pending doses whose scheduled time has already passed and that have not been answered SHALL be included, before the future ones of that day. Taken, skipped and missed doses MUST NOT be shown. The list SHALL scroll vertically.

#### Scenario: The rest of today and all of tomorrow
- **WHEN** it is 12:00 and pending doses exist at 13:00 and 20:00 today and at 08:00 tomorrow
- **THEN** all three are listed, the first two under "Today" and the last under "Tomorrow"

#### Scenario: Beyond the six hours that used to be the limit
- **WHEN** it is 12:00 and a pending dose is scheduled at 20:00 the same day
- **THEN** the dose is shown

#### Scenario: Doses at the same time
- **WHEN** two medicines are both due at 08:00 tomorrow
- **THEN** they appear as two entries under one "08:00" heading

#### Scenario: Overdue pending dose
- **WHEN** it is 12:00 and a pending unanswered dose from 11:40 exists alongside one at 14:00
- **THEN** the 11:40 dose is listed first under "Today", then the 14:00 dose

#### Scenario: Nothing left today
- **WHEN** every dose of today has been answered and doses remain for tomorrow
- **THEN** only the "Tomorrow" heading and its doses are shown

#### Scenario: Answered dose disappears
- **WHEN** a listed dose is recorded as taken on the phone and the watch receives the update
- **THEN** the dose is no longer listed

#### Scenario: A longer list scrolls
- **WHEN** the two days hold more doses than fit on the screen
- **THEN** the user can reach every one of them by scrolling, by touch and by rotary

### Requirement: Read-only dose details
Tapping a dose on the agenda SHALL open a screen showing, from string-resource labels: the amount of that dose, the time it is due with an indication when it falls on the next day, the medicine's default dose, its schedule, and its remaining stock. The stock line SHALL be shown only for a medicine that records stock. Every value SHALL be the text the phone published, so it reads exactly as it does on the phone. When the phone published no details for the dose, the screen SHALL say there are none rather than showing an empty screen. The screen MUST be read-only: it MUST NOT contain any control that changes the dose, the medicine, its schedule or its stock. The watch's back gesture SHALL return to the agenda.

#### Scenario: Details of a dose
- **WHEN** the user taps a dose of "Ibuprofen" whose medicine has a default dose of 400 mg, a schedule of twice a day and 24 tablets in stock
- **THEN** the screen shows the medicine's name, this dose's amount and time, "400 mg", the schedule line and the stock line

#### Scenario: Medicine without stock
- **WHEN** the medicine behind the tapped dose records no stock
- **THEN** no stock line is shown

#### Scenario: Stock that has run out
- **WHEN** the medicine behind the tapped dose records stock and none of it is left
- **THEN** a stock line is shown reading zero

#### Scenario: A dose whose medicine is gone
- **WHEN** the phone published a dose without medicine details
- **THEN** the screen shows the dose's own amount and time and says there are no details for this medicine

#### Scenario: Nothing can be changed
- **WHEN** the details screen is inspected
- **THEN** it holds no field, button, menu or gesture that writes anything

#### Scenario: Back to the agenda
- **WHEN** the user performs the watch's back gesture on the details screen
- **THEN** the agenda is shown again

#### Scenario: The dose is answered on the phone
- **WHEN** the dose whose details are open is recorded as taken on the phone and the watch receives the update
- **THEN** the watch returns to the agenda rather than showing a dose that is no longer pending
