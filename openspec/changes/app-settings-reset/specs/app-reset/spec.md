## ADDED Requirements

### Requirement: Danger zone is the last section of Settings
The Settings screen SHALL show a section headed "Danger zone" as its last section, after About, separated from the section above it by a visible divider. The heading MUST use the error colour role and MUST be marked as a heading for assistive technology. The section MUST contain one line of explanatory text saying what the action does, and one destructive button labelled "Reset app". Colour MUST NOT be the only thing that marks the section as destructive: the divider, the heading text and the explanatory line each carry the meaning on their own.

#### Scenario: Section is present and last
- **WHEN** the user opens Settings and scrolls to the bottom
- **THEN** the Danger zone heading, its explanatory line and the Reset app button are shown below the About section, with a divider above the heading

#### Scenario: Heading is announced as a heading
- **WHEN** a screen reader user navigates the Settings screen by heading
- **THEN** "Danger zone" is reachable as a heading, and the Reset app button is announced as a button

#### Scenario: Readable at large font scale
- **WHEN** the Settings screen is shown at 200 % font scale
- **THEN** the heading, the explanatory line and the button label are fully readable, and no text is truncated

### Requirement: Reset app opens a confirmation and never erases directly
Tapping the Reset app button SHALL only open the reset confirmation dialog. It MUST NOT erase anything, MUST NOT change any stored data, and MUST NOT cancel any alarm or notification.

#### Scenario: Tapping Reset app opens the dialog
- **WHEN** the user taps the Reset app button
- **THEN** the reset confirmation dialog is shown and the database still holds every medicine, schedule and dose it held before

### Requirement: The confirmation names exactly what is erased and what survives
The confirmation dialog SHALL state that every medicine, every schedule and the complete intake history will be removed, and SHALL separately state that the chosen language, the app lock and the accepted legal documents are kept. The dialog title MUST be a question. The dialog MUST NOT claim that the reset can be undone, and MUST NOT offer any export, backup or undo.

#### Scenario: Both halves of the truth are shown
- **WHEN** the reset confirmation dialog is shown
- **THEN** it names medicines, schedules and the intake history as what will be erased, and names the language, the app lock and the accepted documents as what will be kept

### Requirement: Erasing is gated by an acknowledgement checkbox
The confirmation dialog SHALL contain a checkbox labelled "I understand all data will be erased permanently". The destructive confirm button MUST be disabled while the checkbox is unticked and enabled only while it is ticked. The whole checkbox row, label included, MUST be one toggle target of at least the minimum touch target height, exposed with the checkbox role so its ticked state is announced.

#### Scenario: Confirm is disabled until the box is ticked
- **WHEN** the reset confirmation dialog is first shown
- **THEN** the checkbox is unticked and the confirm button is disabled

#### Scenario: Ticking the box enables confirm
- **WHEN** the user ticks the acknowledgement checkbox
- **THEN** the confirm button becomes enabled

#### Scenario: Unticking the box disables confirm again
- **WHEN** the user ticks the acknowledgement checkbox and then unticks it
- **THEN** the confirm button is disabled again

#### Scenario: The label is part of the target
- **WHEN** the user taps the checkbox label rather than the box itself
- **THEN** the checkbox toggles

#### Scenario: Ticked state is announced
- **WHEN** a screen reader user focuses the checkbox row
- **THEN** it is announced with the checkbox role, its label as its name, and its ticked or unticked state

### Requirement: Dismissing the confirmation erases nothing
Cancelling the dialog, pressing system back while it is shown, and tapping outside it SHALL all close the dialog and erase nothing. Reopening the dialog afterwards MUST show the acknowledgement checkbox unticked and the confirm button disabled, whatever state it was left in.

#### Scenario: Cancel erases nothing
- **WHEN** the user ticks the checkbox and then taps Cancel
- **THEN** the dialog closes and every medicine, schedule and dose is still stored

#### Scenario: Back erases nothing
- **WHEN** the user ticks the checkbox and then presses system back
- **THEN** the dialog closes and every medicine, schedule and dose is still stored

#### Scenario: Reopening starts unticked
- **WHEN** the user ticks the checkbox, cancels, and taps Reset app again
- **THEN** the checkbox is unticked and the confirm button is disabled

#### Scenario: Rotation does not lose or invent the acknowledgement
- **WHEN** the user ticks the checkbox and the device is rotated
- **THEN** the dialog is still shown with the checkbox still ticked and the confirm button still enabled

### Requirement: A confirmed reset erases every stored medicine, schedule and dose
On confirmation the app SHALL remove every row from the medications, schedules and doses tables in a single transaction. Either all three tables end empty or none is changed; a partially erased state MUST NOT be observable. The database schema version MUST be unchanged by the reset, and the database MUST remain open and usable without restarting the app.

#### Scenario: Everything is gone
- **WHEN** a database holding medicines, schedules and taken, skipped and missed doses is reset
- **THEN** the medications, schedules and doses tables are all empty

#### Scenario: Schema survives
- **WHEN** the reset completes
- **THEN** the database schema version is the same as before the reset and no migration has run

#### Scenario: The app keeps working
- **WHEN** the user adds a medicine immediately after a reset without restarting the app
- **THEN** the medicine is stored and appears in the medicines list

### Requirement: Settings survive a reset
A reset SHALL NOT change the chosen app language, the app lock (whether it is enabled, the stored PIN verifier or the biometric unlock setting), the recorded acceptance of the legal documents, or the record of whether the notification permission has already been requested.

#### Scenario: Language is kept
- **WHEN** the app language is Dutch and the app is reset
- **THEN** the app is still in Dutch

#### Scenario: The app lock is kept
- **WHEN** a PIN lock is enabled and the app is reset
- **THEN** the lock is still enabled and the same PIN still unlocks the app

#### Scenario: Legal acceptance is kept
- **WHEN** the legal documents have been accepted and the app is reset
- **THEN** the Legal section still reports them as accepted, on the same date, and the user is not asked to accept them again

### Requirement: A reset leaves no pending alarm, notification or watch payload
As part of the same reset operation the app SHALL take down every reminder notification it has posted, and SHALL bring the reminder schedule up to date against the now-empty database so that no alarm remains pending. The watch, when one is paired, SHALL be sent an empty set of doses. These steps MUST run after the data is erased, and a failure in any of them MUST NOT leave the data half-erased.

#### Scenario: Posted reminders are taken down
- **WHEN** a reminder notification is showing for a dose and the app is reset
- **THEN** no reminder notification remains posted

#### Scenario: The pending alarm is cancelled
- **WHEN** an alarm was scheduled for the next dose and the app is reset
- **THEN** no alarm remains scheduled

#### Scenario: The watch is emptied
- **WHEN** doses had been published to a paired watch and the app is reset
- **THEN** an empty set of doses is published

#### Scenario: A stray alarm is harmless
- **WHEN** an alarm fires after a reset because it was already pending
- **THEN** the wake finds no doses, shows no notification and cancels itself

### Requirement: The app stays on Settings and confirms the reset
After a confirmed reset the app SHALL close the dialog, remain on the Settings screen and show a snackbar confirming that the app has been reset. It MUST NOT navigate anywhere, restart, or close itself.

#### Scenario: Confirmation is shown in place
- **WHEN** the reset completes
- **THEN** the dialog is closed, the Settings screen is still shown and a snackbar confirms the reset

### Requirement: Screens showing erased data fall back safely
After a reset, every screen that observes stored data SHALL show its empty state without the user reloading it, and any destination that was opened for a medicine that no longer exists SHALL close itself and return to the medicines list rather than crash or show empty fields.

#### Scenario: Home falls back to its empty state
- **WHEN** the user returns to Home after a reset
- **THEN** the Home screen shows its empty state and lists no doses

#### Scenario: Medicines falls back to its empty state
- **WHEN** the user returns to Medicines after a reset
- **THEN** the medicines list shows its empty state

#### Scenario: A detail screen for an erased medicine closes
- **WHEN** the user returns by back navigation to a destination opened for a medicine that the reset removed
- **THEN** that destination closes and the medicines list is shown, and the app does not crash

### Requirement: The reset has exactly one entry point
The reset SHALL be reachable only by a person acting on the Settings screen. No notification action, notification answer, deep link, app shortcut, broadcast, external intent or restored navigation state SHALL be able to start or complete a reset.

#### Scenario: No route reaches it
- **WHEN** the navigation graph and every registered intent filter, notification action and shortcut are inspected
- **THEN** none of them addresses the reset

#### Scenario: Restoring state does not erase
- **WHEN** the process is recreated from saved state while the reset dialog was open
- **THEN** the dialog is restored but nothing is erased until the user ticks the checkbox and confirms again

### Requirement: Confirming twice erases once
The confirm action SHALL be guarded so that a repeated tap, or a confirm that arrives while an erase is already running, does not start a second erase. The confirm button MUST be unavailable while the erase is in progress.

#### Scenario: Double tap
- **WHEN** the user taps the confirm button twice in quick succession
- **THEN** the erase runs exactly once

### Requirement: Reset text is translated
Every piece of text the danger zone and its confirmation show SHALL come from a string resource with both an English and a Dutch translation. No part of it MAY be composed from fragments at runtime, and no medicine name or amount MAY appear in it.

#### Scenario: Dutch is complete
- **WHEN** the app runs in Dutch
- **THEN** the heading, the explanation, the button, the dialog title, both body paragraphs, the checkbox label, the confirm button and the snackbar are all in Dutch

#### Scenario: No medical detail in the copy
- **WHEN** the danger zone and its dialog are shown for a user with medicines stored
- **THEN** no medicine name, amount or schedule appears anywhere in them
