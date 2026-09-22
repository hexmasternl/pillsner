## ADDED Requirements

### Requirement: Scan label to prefill name and dose
The Add medicine form SHALL offer an optional "Scan label" action that lets the user take a photo or pick an existing one, and attempts on-device text recognition against it. When recognition finds a likely medicine name and/or a dose amount/unit matching the form's fixed unit list, those fields SHALL be prefilled with the recognised values; any field not recognised SHALL remain as it was. Prefilled values MUST remain fully editable and are not saved until the user taps Save, exactly as with manually typed values. Recognition MUST NOT populate used since, use until, prescribed by, or schedules.

#### Scenario: Name and dose recognised
- **WHEN** the user taps "Scan label", captures a photo of a label reading "Metoprolol 40 mg", and recognition completes
- **THEN** the name field shows "Metoprolol", the dose amount field shows "40" and the unit field shows "mg", and all three remain editable

#### Scenario: Only name recognised
- **WHEN** recognition finds a name candidate but no text matching the dose unit list
- **THEN** the name field is prefilled and the dose fields remain exactly as they were before the scan

#### Scenario: Nothing recognised
- **WHEN** recognition completes with no usable name or dose candidate
- **THEN** the form is unchanged and the user can continue typing manually

#### Scenario: Recognised text still requires review before save
- **WHEN** the name field has been prefilled from a scan and the user taps Save without reviewing it
- **THEN** the medicine is saved with exactly the text currently shown in the field, following the same validation as a manually typed name

### Requirement: Scanned photo is not retained
Any photo captured or selected for label scanning SHALL be used only to run on-device recognition and MUST NOT be persisted by Pillsner beyond that recognition, whether recognition succeeds or fails.

#### Scenario: Photo discarded after successful scan
- **WHEN** recognition completes and the form fields are prefilled
- **THEN** the photo used for the scan is no longer retained anywhere in the app's storage

#### Scenario: Photo discarded after failed scan
- **WHEN** recognition fails or is cancelled partway through
- **THEN** no photo from that attempt is retained anywhere in the app's storage

### Requirement: Scan label degrades gracefully without a camera
"Scan label" SHALL remain available via photo picker on a device without a camera or where camera permission is declined, and the rest of the Add medicine form SHALL remain fully usable without it.

#### Scenario: No camera on device
- **WHEN** the user opens the Add medicine form on a device with no camera
- **THEN** "Scan label" still offers picking an existing photo, and every other field is usable exactly as on a device with a camera

#### Scenario: Camera permission declined
- **WHEN** the user taps "Scan label", is prompted for camera permission, and declines
- **THEN** a message explains the camera is unavailable, the photo-picker option remains offered, and the rest of the form is unaffected
