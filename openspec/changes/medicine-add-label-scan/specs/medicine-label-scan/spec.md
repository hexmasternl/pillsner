## ADDED Requirements

### Requirement: Scan label entry point on the Add medicine form
The Add medicine form SHALL show a "Scan label" action, only in add mode, that lets the user capture a photo with the camera or choose an existing photo. The action SHALL be optional: the form SHALL remain fully usable by typing alone whether or not the user ever taps it.

#### Scenario: Action is present in add mode
- **WHEN** the user opens the Add medicine form
- **THEN** a "Scan label" action is shown alongside the existing fields

#### Scenario: Action is absent in edit mode
- **WHEN** the user opens an existing medicine to edit it
- **THEN** no "Scan label" action is shown

#### Scenario: Form works without ever scanning
- **WHEN** the user fills in every field by hand without tapping "Scan label"
- **THEN** the form behaves exactly as it did before this capability existed, and saving works normally

### Requirement: Camera permission is requested only on demand, with a working fallback
Tapping "Scan label" SHALL request camera permission only at that moment, not on form open. Denying it, or having no camera, SHALL fall back to picking an existing photo. If neither capture nor picking is possible or the user cancels, the form SHALL remain exactly as it was, with manual entry unaffected.

#### Scenario: First scan requests permission
- **WHEN** the user taps "Scan label" for the first time and camera permission has not yet been decided
- **THEN** the system camera permission dialog is shown

#### Scenario: Permission denied falls back to the photo picker
- **WHEN** the user denies camera permission
- **THEN** the photo picker opens so the user can choose an existing photo instead

#### Scenario: Cancelling leaves the form untouched
- **WHEN** the user cancels the camera or the photo picker without selecting an image
- **THEN** the Add medicine form is shown exactly as it was before "Scan label" was tapped

### Requirement: Recognized text prefills fields but never saves automatically
Given a photo, the system SHALL recognize text from it entirely on-device and use it to prefill the name field and, when an amount and a recognizable unit are both found, the default dose amount and unit fields. The user MUST review and MAY correct every prefilled field before the medicine can be saved; a scan SHALL NOT save the medicine by itself.

#### Scenario: Name is recognized
- **WHEN** a scanned photo yields a clear medicine name and no clear dose
- **THEN** the name field is prefilled with that name, the dose fields are left as they were, and the user must still tap Save to store anything

#### Scenario: Name and dose are both recognized
- **WHEN** a scanned photo yields a name and a recognizable amount with a known unit
- **THEN** the name, amount and unit fields are all prefilled, and every one of them remains editable

#### Scenario: Nothing usable is recognized
- **WHEN** a scanned photo yields no recognizable name or dose
- **THEN** the form's fields are left exactly as they were, and the user is told the photo could not be read

#### Scenario: Prefilled dose still goes through normal validation
- **WHEN** a prefilled amount or unit would fail the existing dose validation (for example, an unrecognizable amount)
- **THEN** the same "fix this field" error the form already shows for a typed invalid dose is shown, and Save does not proceed

### Requirement: The scanned photo is never retained
The photo used for a scan SHALL exist only for the duration of recognizing its text and SHALL NOT be written to any storage controlled by Pillsner, nor become part of the saved medicine or its draft.

#### Scenario: Photo is discarded after prefill
- **WHEN** a scan completes and the form has been prefilled (or not, if nothing was recognized)
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
