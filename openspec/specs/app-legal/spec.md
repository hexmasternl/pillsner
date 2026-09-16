# app-legal Specification

## Purpose
TBD - created by archiving change app-legal-information. Update Purpose after archive.
## Requirements
### Requirement: Two legal documents ship with the app
The app SHALL hold exactly two legal documents: a Disclaimer and Terms of Service. Each document SHALL be defined in the domain layer as an ordered list of sections, each section a heading and one or more paragraphs, and SHALL carry an integer version starting at 1 and an effective date. The domain layer MUST NOT hold the prose itself: every heading and paragraph is a reference to a string resource, so the documents carry no Android dependency and the text is translated like all other user-facing text.

#### Scenario: Documents are available
- **WHEN** the current legal documents are read
- **THEN** there are exactly two, identified as Disclaimer and Terms of Service, each with a version, an effective date and at least one section

#### Scenario: Text comes from resources
- **WHEN** a document is rendered while the app language is Dutch
- **THEN** every heading and paragraph is shown in Dutch, and no legal text appears anywhere in Kotlin source

### Requirement: What the Disclaimer says
The Disclaimer SHALL state, in plain language:

- that Pillsner is an aid to remembering medication and is not a medical device, and gives no medical advice;
- that the user, together with the person who prescribed the medication, decides which medication is taken, the dose, the frequency and the schedule, and that Pillsner only repeats what the user entered;
- that a reminder may arrive late or may not arrive at all, for reasons outside the app's control such as the device being off, out of battery, silenced or restricted by the system, and that Pillsner MUST NOT be the only thing the user relies on to take medication;
- that neither the app, its publisher nor its developers can be held responsible for a dose that is missed, taken late, taken twice or taken wrongly, nor for any harm arising from use of the app;
- that questions about medication go to a doctor or pharmacist, and that in an emergency the user contacts the emergency services.

#### Scenario: Responsibility is stated
- **WHEN** the user reads the Disclaimer
- **THEN** it states that the schedule, the dose and the frequency are the user's own responsibility and that the app, its publisher and its developers carry none of it

#### Scenario: Reliability is stated
- **WHEN** the user reads the Disclaimer
- **THEN** it states that a reminder may be late or absent and that the app must not be relied on as the only reminder

### Requirement: What the Terms of Service say
The Terms of Service SHALL state: a personal, non-exclusive, revocable licence to use the app; that the app is provided "as is" without warranty of any kind; that liability is limited as far as the law permits, while noting that liability which cannot lawfully be excluded is not excluded; that all data is held on the device only, that backups are the user's own responsibility and that the publisher has no access to it; that the terms may be revised and that a revision is presented again before the next medicine is added; and that the terms are governed by the law of the Netherlands. The publisher and developer SHALL be named as Eduard Keilholz, and that name MUST NOT be translated.

#### Scenario: Terms are readable in full
- **WHEN** the user opens the Terms of Service
- **THEN** all of the above is present, the publisher is named, and the governing law is stated

#### Scenario: Data location is stated
- **WHEN** the user reads the Terms of Service
- **THEN** they state that data stays on the device and that the publisher has no access to it

### Requirement: Acceptance is recorded on the device
The app SHALL record an acceptance as the accepted Disclaimer version, the accepted Terms version and the moment of acceptance, written as one atomic record so a version can never be recorded without a time. The record SHALL be stored in the app's general settings store on the device, separate from the app lock's store. No acceptance record SHALL be created by anything other than the user activating the accept action. Nothing about an acceptance leaves the device.

#### Scenario: Fresh install
- **WHEN** the app is installed and has never been used
- **THEN** no acceptance record exists

#### Scenario: Acceptance is written
- **WHEN** the user accepts on the acceptance screen while the current versions are Disclaimer 1 and Terms 1
- **THEN** a record is stored holding disclaimer version 1, terms version 1 and the moment of acceptance

#### Scenario: Acceptance survives a restart
- **WHEN** the user accepted earlier and the app is started again
- **THEN** the stored record is read back with the same versions and moment

### Requirement: Accepted means both documents are at least as new as accepted
The app SHALL treat the legal documents as accepted when an acceptance record exists and both its stored versions are greater than or equal to the current versions of the corresponding documents. Any other state SHALL be treated as not accepted. This rule MUST be implemented without Android framework dependencies and MUST be unit-tested.

#### Scenario: No record
- **WHEN** no acceptance record exists
- **THEN** the documents are not accepted

#### Scenario: Both versions current
- **WHEN** the record holds disclaimer 1 and terms 1 and the current versions are 1 and 1
- **THEN** the documents are accepted

#### Scenario: One document revised
- **WHEN** the record holds disclaimer 1 and terms 1 and the Disclaimer is revised to version 2
- **THEN** the documents are not accepted

#### Scenario: Both documents revised
- **WHEN** the record holds disclaimer 1 and terms 1 and both documents are revised to version 2
- **THEN** the documents are not accepted

#### Scenario: Downgraded build
- **WHEN** the record holds disclaimer 2 and terms 2 and the installed build's current versions are 1 and 1
- **THEN** the documents are accepted and the user is not asked again

### Requirement: Acceptance is required before a medicine is added
Adding a medicine SHALL be possible only once the current legal documents are accepted. When the user starts to add a medicine while they are not accepted, the app SHALL show the acceptance screen instead of the add-medicine form. Accepting SHALL record the acceptance and continue immediately into the add-medicine form. Leaving the acceptance screen without accepting SHALL record nothing and SHALL add no medicine.

#### Scenario: First medicine, not yet accepted
- **WHEN** the user taps the add button on the Medicines screen and the documents have never been accepted
- **THEN** the acceptance screen is shown and the add-medicine form is not

#### Scenario: Accepting continues into the form
- **WHEN** the user accepts on the acceptance screen
- **THEN** the acceptance is recorded and the add-medicine form is shown, ready to fill in

#### Scenario: Backing out of the gate
- **WHEN** the user presses back or the back arrow on the acceptance screen without accepting
- **THEN** the Medicines screen is shown, no acceptance is recorded and no medicine was added

#### Scenario: Already accepted
- **WHEN** the user taps the add button and the current documents are accepted
- **THEN** the add-medicine form is shown directly, with no legal screen in between

#### Scenario: Documents revised after earlier acceptance
- **WHEN** the user accepted version 1, the Disclaimer is now version 2, and the user taps the add button
- **THEN** the acceptance screen is shown again with the version 2 text

#### Scenario: Back from the form after accepting
- **WHEN** the user accepted, reached the form, and presses back on the untouched form
- **THEN** the Medicines screen is shown and the acceptance screen is not shown again

### Requirement: Acceptance never blocks existing data or reminders
Acceptance SHALL gate only the adding of a new medicine. Viewing, editing, deactivating and deleting an existing medicine, receiving reminders, recording an intake, and every other screen SHALL remain fully usable whether or not the current documents are accepted.

#### Scenario: Editing an existing medicine after a revision
- **WHEN** the Disclaimer is revised to version 2, the user accepted only version 1, and the user taps an existing medicine tile
- **THEN** the medicine opens for editing with no legal screen in between

#### Scenario: Reminders after a revision
- **WHEN** the documents are revised and the user has not accepted the new versions
- **THEN** reminders for existing medicines are still scheduled and delivered, and a dose can still be confirmed, snoozed or skipped

### Requirement: The acceptance screen
The acceptance screen SHALL be a screen with a top app bar, a back affordance and no bottom navigation, and SHALL show the Disclaimer in full, a way to read the Terms of Service, and a single action that accepts both documents. The accept action SHALL be disabled until the Disclaimer has been scrolled to its end, and while disabled SHALL be accompanied by a line of text explaining why. When the Disclaimer fits on the screen without scrolling, the action SHALL be enabled immediately. There SHALL be no separate decline action: the back affordance is how the user leaves without accepting.

#### Scenario: Disclaimer shown in full
- **WHEN** the acceptance screen opens
- **THEN** the Disclaimer's sections are shown in order and the bottom navigation bar is not visible

#### Scenario: Accept is disabled until read
- **WHEN** the acceptance screen opens with a Disclaimer longer than the screen
- **THEN** the accept action is disabled and a line of text states that the disclaimer must be read to the end first

#### Scenario: Accept becomes enabled
- **WHEN** the user scrolls the Disclaimer to its end
- **THEN** the accept action becomes enabled and the explanatory line is gone

#### Scenario: Short disclaimer on a large screen
- **WHEN** the whole Disclaimer fits without scrolling
- **THEN** the accept action is enabled as soon as the screen appears

#### Scenario: Reading the terms from the gate
- **WHEN** the user taps the action that opens the Terms of Service and then goes back
- **THEN** the acceptance screen is shown again in the state it was left in, including its scroll position

#### Scenario: One action accepts both
- **WHEN** the user activates the accept action
- **THEN** both the Disclaimer and the Terms of Service are recorded as accepted at their current versions

### Requirement: The documents are readable from Settings
The Settings screen SHALL show a Legal section, after Security, with one row for the Disclaimer and one row for the Terms of Service, each opening that document on its own screen with a top app bar, a back affordance and no bottom navigation. The document screen SHALL show the document's version and effective date once, beneath its title. Settings SHALL NOT offer a way to accept or to withdraw acceptance.

#### Scenario: Legal section present
- **WHEN** the user opens Settings
- **THEN** a section headed "Legal" is shown after Security with a Disclaimer row and a Terms of Service row

#### Scenario: Open a document
- **WHEN** the user taps the Terms of Service row
- **THEN** the Terms of Service are shown on their own screen with a back affordance, their version and effective date, and no bottom navigation

#### Scenario: Back returns to Settings
- **WHEN** the user presses back on a document screen
- **THEN** the Settings screen is shown with the Settings navigation item still selected

#### Scenario: No accept or withdraw in Settings
- **WHEN** the user reads a document opened from Settings
- **THEN** there is no action to accept it and no action to withdraw an earlier acceptance

#### Scenario: Document survives process death
- **WHEN** the process is killed and restored while the Terms of Service are open
- **THEN** the Terms of Service are shown again, not the Disclaimer

### Requirement: Settings shows the state of acceptance
The Legal section SHALL show one line stating the state of acceptance: that the documents have not yet been accepted; or the date on which they were accepted; or, when either document has been revised since, the date of acceptance together with a statement that the documents have changed since. The date SHALL be formatted for the app language.

#### Scenario: Never accepted
- **WHEN** no acceptance record exists and the user opens Settings
- **THEN** the Legal section states that the documents have not yet been accepted

#### Scenario: Accepted and current
- **WHEN** the user accepted the current versions on 14 September 2026
- **THEN** the Legal section states that they were accepted on 14 September 2026

#### Scenario: Accepted but revised since
- **WHEN** the user accepted version 1 and the Disclaimer is now version 2
- **THEN** the Legal section states the acceptance date and that the documents have changed since

#### Scenario: Date follows the app language
- **WHEN** the app language is Dutch and an acceptance exists
- **THEN** the date in the Legal section is formatted for Dutch

### Requirement: Legal screens are accessible
Every section heading in a legal document SHALL be exposed as a heading to a screen reader. Every row and action SHALL announce its label and its role, and a disabled accept action SHALL announce why it is disabled. The accept action becoming enabled SHALL be announced without the user moving focus. At the largest system font scale every document SHALL scroll in full with no text clipped or truncated, and the accept action SHALL remain fully visible and reachable.

#### Scenario: Screen reader navigates by heading
- **WHEN** a screen reader user moves by heading through the Terms of Service
- **THEN** each section heading is reached in order

#### Scenario: Disabled accept is explained
- **WHEN** a screen reader focuses the accept action while it is disabled
- **THEN** it announces the label, that the action is disabled, and the reason

#### Scenario: Enabling is announced
- **WHEN** the user scrolls the Disclaimer to its end while a screen reader is active
- **THEN** the change is announced without the user moving focus

#### Scenario: Largest font scale
- **WHEN** the system font scale is at maximum and the acceptance screen is open
- **THEN** the whole Disclaimer is reachable by scrolling, no text is clipped, and the accept action is fully visible

### Requirement: The legal text is translated
Every string of both legal documents SHALL have an English, Dutch, German, French, Spanish and Portuguese version, and a missing or extra translation for any supported language MUST fail the project's lint task. The publisher's name and the product name MUST NOT be translated.

#### Scenario: Dutch user reads Dutch terms
- **WHEN** the app language is Dutch and the user opens the Terms of Service
- **THEN** the whole document is in Dutch

#### Scenario: New language user reads translated terms
- **WHEN** the app language is Spanish and the user opens the Terms of Service
- **THEN** the whole document is in Spanish

#### Scenario: Missing translation fails lint
- **WHEN** a legal string is added without a translation for every supported language and lint runs
- **THEN** lint fails with a missing-translation error

#### Scenario: Names are not translated
- **WHEN** the app language is Dutch
- **THEN** the publisher is still named "Eduard Keilholz" and the product is still named "Pillsner"

### Requirement: Legal information adds nothing that leaves the device
The legal documents SHALL ship with the build and SHALL NOT be fetched or updated over a network. This capability SHALL add no permission, no network access, no link out of the app and no third-party dependency.

#### Scenario: Offline
- **WHEN** the device has no network connection at all
- **THEN** both documents are fully readable and acceptance can be given and recorded

#### Scenario: No new permission
- **WHEN** the manifest is inspected after this change
- **THEN** no permission was added and no network access was declared

