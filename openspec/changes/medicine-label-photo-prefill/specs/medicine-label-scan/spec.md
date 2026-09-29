## ADDED Requirements

### Requirement: Scan a label from the Add medicine form
The Add medicine form SHALL offer a "Scan a label" action in add mode only. Tapping it SHALL present two options: take a photo with the device's camera app, and choose an existing photo with the system photo picker. The take-a-photo option SHALL be offered only when an activity resolves for the system image-capture intent. The action MUST NOT appear in edit mode. All text MUST come from string resources.

#### Scenario: Options on a device with a camera app
- **WHEN** the user opens the Add medicine form on a device where a camera app is installed and taps "Scan a label"
- **THEN** a sheet offers "Take a photo" and "Choose a photo"

#### Scenario: Options on a device without a camera app
- **WHEN** the user taps "Scan a label" on a device where no activity resolves for the image-capture intent
- **THEN** the sheet offers only "Choose a photo"

#### Scenario: Not offered when editing
- **WHEN** the user opens an existing medicine's details
- **THEN** no "Scan a label" action is shown

#### Scenario: Cancelled capture
- **WHEN** the user chooses "Take a photo" and leaves the camera app without taking one
- **THEN** the form is unchanged and no message is shown

### Requirement: No permission is declared or requested for scanning
Scanning SHALL use the system camera app through the image-capture activity result and the system photo picker. The app MUST NOT declare the `CAMERA`, `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` or `INTERNET` permission, MUST NOT declare a camera `uses-feature`, and MUST NOT show any runtime permission prompt of its own for scanning. The only manifest additions for scanning SHALL be a `queries` intent entry for the image-capture action and one non-exported `FileProvider` whose paths expose only the scan cache subdirectory.

#### Scenario: Declared permissions unchanged
- **WHEN** the merged manifest of any build variant is inspected
- **THEN** its set of `uses-permission` entries is exactly the set disclosed in the README's permission table, with no camera, media, storage or network permission

#### Scenario: Provider exposes only the scan cache
- **WHEN** the FileProvider's path configuration is inspected
- **THEN** it lists exactly one cache path, `label-scan/`, and the provider is not exported

#### Scenario: Installs on a camera-less device
- **WHEN** the app is installed on a device with no camera
- **THEN** installation succeeds and the Add medicine form offers "Choose a photo" only

### Requirement: Recognition runs entirely inside the app
Text recognition SHALL run on the device with a recogniser and trained data that ship inside the app package. It MUST work with no network connection from the first launch, MUST NOT download any model or component, and MUST NOT use Google Play services, Firebase or ML Kit. The trained data SHALL be copied from the app's assets to the app's private files directory before first use and that directory SHALL be excluded from backup and device transfer. Recognition SHALL run off the main thread and SHALL be cancellable.

#### Scenario: Airplane mode from first launch
- **WHEN** the app is installed and first opened with all network disabled, and the user scans a label
- **THEN** recognition completes and the form is pre-filled without any network access

#### Scenario: Cancel while reading
- **WHEN** recognition is in progress and the user taps Cancel
- **THEN** recognition stops, the form is unchanged and the progress state disappears

#### Scenario: Trained data replaced on update
- **WHEN** the app is updated to a version whose bundled trained data differs
- **THEN** the next scan uses the new data

### Requirement: Recognised text is interpreted into form fields
The domain layer SHALL provide a pure function, with no Android or recogniser dependency, that turns the recognised lines into a label interpretation holding an optional name, an optional default dose, zero or more schedules, a used-since date and an optional use-until date. Unit, frequency, duration and "until" words SHALL be matched from one vocabulary covering English, Dutch, German, French, Spanish and Portuguese, case- and accent-insensitively, regardless of the app's language. A field the function cannot determine SHALL be absent, never guessed.

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
Applying an interpretation SHALL set only the fields it carries (name, default dose amount and unit, schedules, used since, use until) and SHALL leave every other field, including prescriber, unchanged. When the draft is untouched the interpretation SHALL be applied at once; when the draft has edits the form SHALL ask whether to replace them, and only "Replace" applies it. After applying, the form SHALL show an attention banner stating the fields were filled from a photo and must be checked, with an action that shows the raw recognised text and a dismiss action. Pre-filled values SHALL be editable and SHALL pass through the form's ordinary validation on Save. When the interpretation carries nothing, the form SHALL be unchanged and a message SHALL say nothing readable was found. When decoding or recognition fails, a different message SHALL say the photo could not be read. Neither message MAY quote recognised text.

#### Scenario: Untouched form is filled
- **WHEN** the form has no edits and a scan yields a name, 50 mg, one schedule and a used-since date
- **THEN** the name, dose, unit, schedule row and used since show those values, prescriber is unchanged, and the banner is shown

#### Scenario: Edited form asks first
- **WHEN** the user has typed a name and then scans a label
- **THEN** a dialog asks whether to replace what was entered; "Keep" leaves the form as typed and "Replace" applies the interpretation

#### Scenario: Show recognised text
- **WHEN** the banner is shown and the user taps "Show text"
- **THEN** a sheet shows the recognised text and nothing else

#### Scenario: Banner survives rotation until dismissed
- **WHEN** the banner is shown and the device rotates
- **THEN** the banner is still shown; once dismissed it does not return on a later rotation

#### Scenario: Nothing readable
- **WHEN** a scan yields an empty interpretation
- **THEN** the form is unchanged and a message says nothing readable was found on the photo

#### Scenario: Pre-filled value fails validation
- **WHEN** a scan pre-fills a dose amount that the form's validation rejects and the user taps Save
- **THEN** the medicine is not saved and the dose field shows the same error it shows for a typed value

### Requirement: The photo is not retained
A captured photo SHALL be written only to a file under the app's `label-scan` cache subdirectory, SHALL never be registered with the media store, and SHALL be deleted when the scan ends, whatever the outcome. A picked photo SHALL be read once and never copied. The scan cache subdirectory SHALL be emptied at process start and at the start of every scan. The recognised text SHALL be kept only in the form's draft state and MUST NOT be logged.

#### Scenario: Capture file deleted after success
- **WHEN** a captured photo is recognised and the form is pre-filled
- **THEN** no file remains under the scan cache subdirectory

#### Scenario: Capture file deleted after failure
- **WHEN** recognition of a captured photo fails or is cancelled
- **THEN** no file remains under the scan cache subdirectory

#### Scenario: Leftover swept at start
- **WHEN** a file exists under the scan cache subdirectory when the process starts
- **THEN** it is deleted before any screen is shown

#### Scenario: No text in logs
- **WHEN** a scan runs in a release build
- **THEN** no log line contains the recognised text, the name or any amount

### Requirement: Scanning is accessible
The "Scan a label" action, the option sheet, the progress state, the banner and its actions SHALL each have a spoken label; the progress state and the result messages SHALL be announced; and the form SHALL remain fully usable without ever using the action, including with TalkBack and at the largest font scale.

#### Scenario: Screen reader on the action
- **WHEN** a screen reader focuses the "Scan a label" button
- **THEN** it announces the label and that it is a button

#### Scenario: Progress announced
- **WHEN** recognition starts
- **THEN** "Reading the label" is announced, and the result message is announced when it ends

#### Scenario: Largest font scale with the banner
- **WHEN** the system font scale is at maximum and the banner is shown
- **THEN** the banner, its actions and every field are reachable by scrolling with no clipped text
