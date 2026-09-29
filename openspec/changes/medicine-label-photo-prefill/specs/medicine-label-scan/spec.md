## ADDED Requirements

### Requirement: Scan a label from the Add medicine form
The Add medicine form SHALL offer a "Scan a label" action in add mode only. Tapping it SHALL present two options: scan with the camera, and choose an existing photo with the system photo picker. The scan-with-camera option SHALL be offered only when the device reports a camera feature. The action MUST NOT appear in edit mode. All text MUST come from string resources.

#### Scenario: Options on a device with a camera
- **WHEN** the user opens the Add medicine form on a device with a camera and taps "Scan a label"
- **THEN** a sheet offers "Scan with camera" and "Choose a photo"

#### Scenario: Options on a device without a camera
- **WHEN** the user taps "Scan a label" on a device that reports no camera feature
- **THEN** the sheet offers only "Choose a photo"

#### Scenario: Not offered when editing
- **WHEN** the user opens an existing medicine's details
- **THEN** no "Scan a label" action is shown

### Requirement: Camera permission is requested in context and declining costs nothing
The app SHALL declare the `CAMERA` permission and the `android.hardware.camera.any` feature as not required. The permission SHALL be requested only when the user chooses "Scan with camera" and it is not yet granted, and only after an in-app rationale stating that the camera is used solely to read the label, that nothing is saved and that nothing is sent anywhere. The permission MUST NOT be requested at install, at app start, or on opening the form. When the permission is denied, the form SHALL remain unchanged and a message SHALL say the camera is unavailable and that a photo can still be chosen; when it is permanently denied, that message SHALL offer to open the app's system settings. No other permission MAY be added: `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`, `INTERNET` and `ACCESS_NETWORK_STATE` MUST stay undeclared.

#### Scenario: First scan with the camera
- **WHEN** the user chooses "Scan with camera" and the permission has never been requested
- **THEN** the rationale is shown, "Continue" shows the system permission prompt, and granting it opens the scanning screen

#### Scenario: Rationale declined
- **WHEN** the rationale is shown and the user taps "Not now"
- **THEN** no system prompt is shown and the form is unchanged

#### Scenario: Permission denied
- **WHEN** the user denies the system prompt
- **THEN** the form is unchanged and a message says the camera is unavailable and that a photo can still be chosen

#### Scenario: Permission permanently denied
- **WHEN** the user chooses "Scan with camera" after having permanently denied the permission
- **THEN** the message offers an action that opens the app's system settings page

#### Scenario: Already granted
- **WHEN** the permission is already granted and the user chooses "Scan with camera"
- **THEN** the scanning screen opens with no prompt

#### Scenario: Declared permissions match the allow-list
- **WHEN** the merged manifest of any build variant is inspected
- **THEN** its `uses-permission` set is exactly the previous seven permissions plus `CAMERA`, and the camera feature is declared as not required

#### Scenario: Installs on a camera-less device
- **WHEN** the app is installed on a device with no camera
- **THEN** installation succeeds and the Add medicine form offers "Choose a photo" only

### Requirement: Live scanning screen
The scanning screen SHALL show the camera preview with a framing guide, an instruction, a shutter button, a torch toggle when the camera has a flash, and a cancel action. Frames SHALL be analysed continuously in memory while the screen is visible. The scan SHALL accept itself when two consecutive frames each yield an interpretation with a name and at least one of a default dose or a schedule, and both yield the same name; the later interpretation SHALL be used. On acceptance the screen SHALL vibrate briefly, announce that the label was read, and return to the form. The shutter SHALL return the most recent frame's interpretation, whether or not it is complete. After 8 seconds without an accepted frame the instruction SHALL change to advise moving closer, adding light or holding still. Cancel SHALL return to the form unchanged. Leaving the screen SHALL release the camera.

#### Scenario: Automatic acceptance
- **WHEN** two consecutive frames read a name of "Metoprolol" and a strength of 50 mg
- **THEN** the screen vibrates, announces that the label was read, and the form is pre-filled with that interpretation

#### Scenario: Single misread is not accepted
- **WHEN** one frame reads a name and the next frame reads a different name
- **THEN** the scan continues and nothing is returned to the form

#### Scenario: Shutter with a partial read
- **WHEN** the latest frame yielded only a name and the user taps the shutter
- **THEN** the form is pre-filled with the name only and the banner is shown

#### Scenario: Shutter before any read
- **WHEN** no frame has yielded anything and the user taps the shutter
- **THEN** the form is unchanged and a message says nothing readable was found

#### Scenario: Guidance after eight seconds
- **WHEN** eight seconds pass without an accepted frame
- **THEN** the instruction changes to advise moving closer, adding light or holding still

#### Scenario: Cancel
- **WHEN** the user taps Cancel on the scanning screen
- **THEN** the form is shown unchanged and the camera is released

#### Scenario: App goes to the background
- **WHEN** the scanning screen is open and the app goes to the background
- **THEN** the camera is released and, when the app lock is enabled, the unlock screen is shown on return

### Requirement: Recognition runs entirely inside the app
Text recognition SHALL run on the device with a recogniser and trained data that ship inside the app package. It MUST work with no network connection from the first launch, MUST NOT download any model or component, and MUST NOT use Google Play services, Firebase or ML Kit, including the CameraX ML Kit bridge. The trained data SHALL be copied from the app's assets to the app's private files directory before first use and that directory SHALL be excluded from backup and device transfer. Recognition SHALL run off the main thread, SHALL never stall the preview, and SHALL be cancellable.

#### Scenario: Airplane mode from first launch
- **WHEN** the app is installed and first opened with all network disabled, and the user scans a label
- **THEN** recognition completes and the form is pre-filled without any network access

#### Scenario: Frames dropped while busy
- **WHEN** a frame arrives while the previous one is still being recognised
- **THEN** it is dropped, the preview keeps running, and the next frame after recognition finishes is analysed

#### Scenario: Trained data replaced on update
- **WHEN** the app is updated to a version whose bundled trained data differs
- **THEN** the next scan uses the new data

### Requirement: Recognised text is interpreted into form fields
The domain layer SHALL provide a pure function, with no Android, camera or recogniser dependency, that turns the recognised lines into a label interpretation holding an optional name, an optional default dose, zero or more schedules, a used-since date and an optional use-until date. Unit, frequency, duration and "until" words SHALL be matched from one vocabulary covering English, Dutch, German, French, Spanish and Portuguese, case- and accent-insensitively, regardless of the app's language. A field the function cannot determine SHALL be absent, never guessed.

#### Scenario: Name and strength on one line
- **WHEN** the recognised lines include "METOPROLOL 50 MG TABLET" and nothing else that looks like a name
- **THEN** the name is "METOPROLOL", the default dose is 50 mg

#### Scenario: Name without strength
- **WHEN** the recognised lines include a candidate line with no dose token
- **THEN** the name is that line with dose and pack-size tokens removed, and the default dose is absent unless an instruction line carries a count such as "1 tablet"

#### Scenario: Noise lines are skipped
- **WHEN** the lines before the medicine name are a pharmacy name, a salutation with a person's name, a phone number and a date
- **THEN** none of them is chosen as the name

#### Scenario: Nothing usable
- **WHEN** no line yields a name, a dose or a frequency
- **THEN** the interpretation has no name, no default dose, no schedules, used since today and no use until

### Requirement: Prescribed dose becomes a schedule
When the interpretation finds a frequency and an amount, it SHALL produce schedules using the app's existing schedule shapes. The amount SHALL be the count token in the instruction line when present (such as "1 tablet"), otherwise the default dose. N times a day for N of 1 to 4 SHALL become an every-day schedule at fixed default times (1: 08:00; 2: 08:00 and 20:00; 3: 08:00, 14:00 and 20:00; 4: 08:00, 12:00, 16:00 and 20:00). Every N hours SHALL become an every-N-hours schedule from 08:00 when N is one of 1, 2, 3, 4, 6, 8, 12 or 24. Every other day SHALL become an every-two-days schedule at 08:00. Day-part words and the box notation `a-b-c[-d]` SHALL map to the slots morning 08:00, noon 13:00, evening 18:00 and night 22:00. Any frequency outside these SHALL produce no schedule. The weekdays shape MUST NOT be produced. Schedules the domain constructors reject MUST be dropped.

#### Scenario: Twice a day in Dutch
- **WHEN** an instruction line reads "2x daags 1 tablet"
- **THEN** one schedule of 1 tablet every day at 08:00 and 20:00 is produced

#### Scenario: Every eight hours in Spanish
- **WHEN** an instruction line reads "1 comprimido cada 8 horas"
- **THEN** one schedule of 1 tablet every 8 hours from 08:00 is produced

#### Scenario: Box notation with equal digits
- **WHEN** a line reads "1-0-1" and the default dose is 50 mg with no count token
- **THEN** one schedule of 50 mg every day at 08:00 and 18:00 is produced

#### Scenario: Box notation with different digits
- **WHEN** a line reads "2-0-1" and an instruction line names the unit tablet
- **THEN** two schedules are produced: 2 tablets every day at 08:00, and 1 tablet every day at 18:00

#### Scenario: Frequency without any amount
- **WHEN** an instruction line reads "twice daily" and no strength or count token exists anywhere
- **THEN** no schedule is produced

#### Scenario: Unsupported frequency
- **WHEN** an instruction line reads "5 times a day"
- **THEN** no schedule is produced

### Requirement: Dates are interpreted conservatively
Used since SHALL be the most recent numeric date on the label that is on or before today and at most 365 days before it; when there is none, used since SHALL be today. A month-and-year form SHALL never be a date candidate. A date after today SHALL never become used since. Use until SHALL be, in priority order: a date after an "until" word that is after used since; otherwise used since plus N days minus one for a course of N days, or plus 7·N days minus one for N weeks; otherwise absent. A use until before used since SHALL be discarded.

#### Scenario: Dispense date becomes used since
- **WHEN** today is 29 September 2026 and the label carries "27-09-2026" and "EXP 03/2028"
- **THEN** used since is 27 September 2026 and the expiry is ignored

#### Scenario: No date on the label
- **WHEN** the label carries no numeric date
- **THEN** used since is today

#### Scenario: Old date is ignored
- **WHEN** today is 29 September 2026 and the only date on the label is 1 March 2025
- **THEN** used since is today

#### Scenario: Seven-day course
- **WHEN** used since is 29 September 2026 and an instruction line reads "gedurende 7 dagen"
- **THEN** use until is 5 October 2026

#### Scenario: Two-week course in Portuguese
- **WHEN** used since is 29 September 2026 and an instruction line reads "durante 2 semanas"
- **THEN** use until is 12 October 2026

#### Scenario: Explicit until date
- **WHEN** used since is 29 September 2026 and a line reads "tot 15-10-2026"
- **THEN** use until is 15 October 2026

### Requirement: Interpretation pre-fills the form for review
Applying an interpretation SHALL set only the fields it carries (name, default dose amount and unit, schedules, used since, use until) and SHALL leave every other field, including prescriber, unchanged. When the draft is untouched the interpretation SHALL be applied at once; when the draft has edits the form SHALL ask whether to replace them, and only "Replace" applies it. After applying, the form SHALL show an attention banner stating the fields were filled from a scan and must be checked, with an action that shows the raw recognised text and a dismiss action. Pre-filled values SHALL be editable and SHALL pass through the form's ordinary validation on Save. When the interpretation is empty, the form SHALL be unchanged and a message SHALL say nothing readable was found. When decoding a picked photo or recognition fails, a different message SHALL say the photo could not be read. Neither message MAY quote recognised text.

#### Scenario: Untouched form is filled
- **WHEN** the form has no edits and a scan yields a name, 50 mg, one schedule and a used-since date
- **THEN** the name, dose, unit, schedule row and used since show those values, prescriber is unchanged, and the banner is shown

#### Scenario: Edited form asks first
- **WHEN** the user has typed a name and then completes a scan
- **THEN** a dialog asks whether to replace what was entered; "Keep" leaves the form as typed and "Replace" applies the interpretation

#### Scenario: Show recognised text
- **WHEN** the banner is shown and the user taps "Show text"
- **THEN** a sheet shows the recognised text and nothing else

#### Scenario: Banner survives rotation until dismissed
- **WHEN** the banner is shown and the device rotates
- **THEN** the banner is still shown; once dismissed it does not return on a later rotation

#### Scenario: Nothing readable
- **WHEN** a scan yields an empty interpretation
- **THEN** the form is unchanged and a message says nothing readable was found

#### Scenario: Pre-filled value fails validation
- **WHEN** a scan pre-fills a dose amount that the form's validation rejects and the user taps Save
- **THEN** the medicine is not saved and the dose field shows the same error it shows for a typed value

### Requirement: Nothing is retained
Camera frames SHALL be analysed in memory only and MUST NOT be encoded to an image file or written to any storage. A picked photo SHALL be read once and never copied. The recogniser SHALL be released when the scanning screen closes. The recognised text SHALL be kept only in the form's draft state and MUST NOT be logged.

#### Scenario: No file after a camera scan
- **WHEN** a scan with the camera completes, is cancelled or fails
- **THEN** no new file exists under the app's cache or files directories other than the trained data

#### Scenario: Picked photo not copied
- **WHEN** a picked photo is recognised
- **THEN** no copy of it exists under the app's cache or files directories

#### Scenario: No text in logs
- **WHEN** a scan runs in a release build
- **THEN** no log line contains the recognised text, the name or any amount

### Requirement: Scanning is accessible
The "Scan a label" action, the option sheet, the rationale, the scanning screen's shutter, torch and cancel controls, the banner and its actions SHALL each have a spoken label. Opening the scanning screen SHALL announce the instruction, acceptance SHALL announce that the label was read, the guidance change SHALL be announced, and the result messages SHALL be announced. The form SHALL remain fully usable without ever using the action, including with TalkBack and at the largest font scale.

#### Scenario: Screen reader on the scanning screen
- **WHEN** a screen reader user opens the scanning screen
- **THEN** the instruction is announced and the shutter, torch and cancel controls are reachable and labelled

#### Scenario: Acceptance announced
- **WHEN** the scan accepts itself
- **THEN** "Label read" is announced before the form is shown

#### Scenario: Largest font scale with the banner
- **WHEN** the system font scale is at maximum and the banner is shown
- **THEN** the banner, its actions and every field are reachable by scrolling with no clipped text
