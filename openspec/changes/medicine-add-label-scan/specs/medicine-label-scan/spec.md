## ADDED Requirements

### Requirement: Scan button on the Medicines screen
The Medicines screen SHALL show a button, next to the existing add button, with an icon indicating a camera and the content description "Scan medicine label". Tapping it SHALL start the capture flow described below. The Medicines screen itself SHALL remain fully usable, including the regular add button, whether or not the user ever taps this button.

#### Scenario: Scan button is present
- **WHEN** the Medicines screen is shown
- **THEN** a "Scan medicine label" button is shown alongside the existing add button

#### Scenario: Medicines screen works without ever scanning
- **WHEN** the user never taps the scan button
- **THEN** the Medicines screen and the regular add button behave exactly as they did before this capability existed

### Requirement: Camera permission is requested only on demand, with a working fallback
Tapping the scan button SHALL request camera permission only at that moment, not on screen load. Denying it, or the device reporting no camera, SHALL fall back to picking an existing photo. If neither capturing nor picking is possible, or the user cancels either, the Medicines screen SHALL remain exactly as it was, with no navigation and no message left behind.

#### Scenario: First scan requests permission
- **WHEN** the user taps the scan button for the first time and camera permission has not yet been decided
- **THEN** the system camera permission dialog is shown

#### Scenario: Permission denied falls back to the photo picker
- **WHEN** the user denies camera permission
- **THEN** the photo picker opens so the user can choose an existing photo instead

#### Scenario: Cancelling leaves the Medicines screen untouched
- **WHEN** the user cancels the camera or the photo picker without selecting an image
- **THEN** the Medicines screen is shown exactly as it was before the scan button was tapped, and no other screen is opened

### Requirement: A recognized photo opens a new Add medicine form prefilled from it
Given a photo, the system SHALL recognize text from it entirely on-device, then open the Add medicine form in add mode — never editing an existing medicine — with the name field, and, when a recognizable amount and unit are both found, the default dose amount and unit fields, prefilled from what was recognized. Every prefilled field remains an ordinary editable draft value; the user MUST review and MAY correct any of them before the medicine can be saved, and a scan SHALL NOT save a medicine by itself.

#### Scenario: Name is recognized
- **WHEN** a scanned photo yields a clear medicine name and no clear dose
- **THEN** the Add medicine form opens with the name field prefilled with that name, the dose fields at their normal empty defaults, and the user must still tap Save to store anything

#### Scenario: Name and dose are both recognized
- **WHEN** a scanned photo yields a name and a recognizable amount with a known unit
- **THEN** the Add medicine form opens with the name, amount and unit fields all prefilled, and every one of them remains editable

#### Scenario: Nothing usable is recognized
- **WHEN** a scanned photo yields no recognizable name or dose
- **THEN** the Add medicine form opens with every field at its normal empty default, and a message states the photo could not be read

#### Scenario: Prefilled dose still goes through normal validation
- **WHEN** a prefilled amount or unit would fail the existing dose validation (for example, an unrecognizable amount)
- **THEN** the same "fix this field" error the form already shows for a typed invalid dose is shown, and Save does not proceed

#### Scenario: Scanning always opens a fresh medicine
- **WHEN** a scan completes, whatever was or was not recognized
- **THEN** the Add medicine form opens in add mode, never editing an existing medicine

### Requirement: The scanned photo is never retained
The photo used for a scan SHALL exist only for the duration of recognizing its text and SHALL NOT be written to any storage controlled by Pillsner beyond a temporary file deleted immediately after recognition completes, and SHALL NOT become part of the opened form's draft or any saved medicine.

#### Scenario: Photo is discarded after recognition
- **WHEN** recognition completes, successfully or not
- **THEN** no copy of the photo remains accessible to Pillsner

### Requirement: Recognition works across every supported app language
The recognized-text-to-dose parsing SHALL recognize unit words for all languages Pillsner supports (English, Dutch, German, French, Spanish, Portuguese), matched against the app's current language setting.

#### Scenario: Unit word in the app's current language is recognized
- **WHEN** the app language is set to Dutch and a scanned label's text includes a recognizable Dutch unit word with an amount
- **THEN** the dose amount and unit fields are prefilled accordingly

### Requirement: No medical interpretation of recognized text
The system SHALL NOT attempt to identify a drug, check interactions, validate a dose against any medical reference, or otherwise interpret recognized text beyond extracting a name and an amount/unit pair.

#### Scenario: Recognized text is used only for name and dose
- **WHEN** a scanned label contains other information such as warnings, prescriber details or a barcode
- **THEN** none of that text is used for anything beyond what a human reading it would type into the name or dose fields; the form's prescriber field is never auto-filled from a scan
