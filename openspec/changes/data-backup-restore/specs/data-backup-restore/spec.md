## ADDED Requirements

### Requirement: Settings offers Export data and Import data actions
The Settings screen SHALL show an "Export data" action and an "Import data" action, placed above the existing Danger zone section. Both MUST be reachable without leaving Settings for anything other than the platform's file picker and, for export, a passphrase entry step.

#### Scenario: Actions are present
- **WHEN** the user opens Settings
- **THEN** an "Export data" action and an "Import data" action are shown above the Danger zone section

### Requirement: Export writes a passphrase-encrypted file chosen by the user
Tapping "Export data" SHALL first ask the user to enter and confirm a passphrase, then open the Storage Access Framework's document creation picker so the user chooses the file's name and location. The app MUST NOT write the file anywhere the user did not explicitly choose, and MUST NOT proceed to write a file if the user cancels the picker.

#### Scenario: Passphrase required before the picker opens
- **WHEN** the user taps "Export data" and leaves the passphrase field empty
- **THEN** the export cannot proceed and no file picker is shown

#### Scenario: Passphrase and confirmation must match
- **WHEN** the user enters a passphrase and a different confirmation value
- **THEN** the export cannot proceed and a mismatch is shown

#### Scenario: Cancelling the picker writes nothing
- **WHEN** the user completes passphrase entry and then cancels the Storage Access Framework picker
- **THEN** no file is created and no data is modified

### Requirement: The exported file contains user data only, excluding device-only state
The exported file SHALL contain every medication (active and inactive), every schedule, every recorded intake with its outcome and timestamp, and current stock/refill state. It MUST NOT contain the app lock PIN verifier or biometric-unlock setting, the accepted-legal-document timestamps, or any Wear OS pairing state.

#### Scenario: User data is included
- **WHEN** a dataset with medicines, schedules and a mix of taken, skipped and missed doses is exported
- **THEN** the resulting file, once decrypted, contains all of that medicine, schedule and intake data

#### Scenario: Device-only state is excluded
- **WHEN** an export is produced on a device with an app lock PIN enabled and a paired watch
- **THEN** the resulting file, once decrypted, contains neither the PIN verifier, the biometric setting, nor any watch pairing state

### Requirement: The exported file is encrypted and carries a plaintext format version
The app SHALL derive an AES-256-GCM key from the export passphrase using PBKDF2-HMAC-SHA256 with a random per-export salt, and SHALL encrypt the entire data payload with that key and a random per-export nonce. The salt, the KDF iteration count, the nonce and an explicit integer format version SHALL be stored in an unencrypted header; none of these fields is treated as secret.

#### Scenario: Header is readable without the passphrase
- **WHEN** an exported file is inspected without its passphrase
- **THEN** its format version, salt, iteration count and nonce can be read, but the data payload cannot be decrypted

#### Scenario: Same passphrase, different files
- **WHEN** the same passphrase is used to export twice
- **THEN** the two resulting files use different salts and different nonces

### Requirement: Import requires the export passphrase and fails loudly on mismatch or corruption
Tapping "Import data" SHALL open the Storage Access Framework's document picker to choose a `.pill` file, then ask for the passphrase used to create it. A wrong passphrase, a corrupted file, or a file whose authentication check fails SHALL be reported as a single clear failure before any existing data is touched, and MUST NOT partially apply any part of the file.

#### Scenario: Wrong passphrase is rejected
- **WHEN** the user selects a valid `.pill` file and enters a passphrase other than the one used to export it
- **THEN** the import fails with a clear message and no on-device data changes

#### Scenario: Corrupted file is rejected
- **WHEN** the selected file's contents have been altered after export
- **THEN** decryption's authentication check fails, the import fails with a clear message, and no on-device data changes

### Requirement: Import refuses a newer, unrecognised format version
If the selected file's format version is newer than every format version the running app version supports, the import SHALL fail with a message that names the version mismatch, and MUST NOT attempt a partial or best-effort read of the payload.

#### Scenario: Newer format version is refused
- **WHEN** a `.pill` file with a format version higher than any this app version recognises is selected, with the correct passphrase
- **THEN** the import fails with a message naming the version mismatch, and no on-device data changes

### Requirement: A successful import is confirmed and then replaces the entire dataset
After the passphrase and format checks pass, the app SHALL show a confirmation dialog naming that every existing medicine, schedule and intake record will be replaced by the contents of the chosen file, styled after the existing reset confirmation. Only on explicit confirmation SHALL the app remove every existing medication, schedule and dose row and replace them with the file's contents, in a single transaction: either the replacement fully succeeds or the on-device dataset is left exactly as it was before the import.

#### Scenario: Confirmation names what will be replaced
- **WHEN** a valid file and correct passphrase are supplied
- **THEN** a confirmation dialog states that existing medicines, schedules and intake history will be replaced by the file's contents

#### Scenario: Cancelling the confirmation changes nothing
- **WHEN** the user cancels the import confirmation dialog
- **THEN** no existing data is removed or replaced

#### Scenario: A successful import replaces everything
- **WHEN** the user confirms an import
- **THEN** the on-device medications, schedules and doses tables contain exactly what the imported file described, and nothing from before the import remains

#### Scenario: A failure partway through the transaction changes nothing
- **WHEN** the replacement transaction cannot complete
- **THEN** the on-device dataset is left exactly as it was before the import was confirmed, and the app reports the failure

### Requirement: A completed import reschedules reminders and re-syncs the watch
Immediately after a successful import, the app SHALL bring the reminder schedule up to date against the newly-imported data the same way it already does after a reset, cancelling any alarm that no longer matches and scheduling every alarm the imported schedules imply, and SHALL publish the imported doses to a paired watch, replacing whatever it held before.

#### Scenario: Alarms match the imported data
- **WHEN** an import completes
- **THEN** every alarm pending afterward corresponds to a dose implied by the imported schedules, and no alarm from before the import remains

#### Scenario: The watch reflects the imported data
- **WHEN** an import completes with a watch paired
- **THEN** the watch is sent the doses implied by the imported data, replacing what it held before

### Requirement: Export and import require the app to be unlocked
When an app lock is enabled, both "Export data" and "Import data" SHALL be reachable only while the app is unlocked, the same as any other screen that shows medication data.

#### Scenario: Locked app cannot export or import
- **WHEN** an app lock is enabled and the app is locked
- **THEN** neither "Export data" nor "Import data" is reachable until the app is unlocked

### Requirement: Backup and restore text is translated and free of medical detail in logs
Every piece of text the export and import flows show SHALL come from a string resource with both an English and a Dutch translation. No medication name, dosage, or other medical detail SHALL be logged at info level or above in a release build during export or import.

#### Scenario: Dutch is complete
- **WHEN** the app runs in Dutch
- **THEN** the Export data and Import data actions, their passphrase dialogs, the import confirmation, and every success and failure message are all in Dutch

#### Scenario: No medical detail is logged
- **WHEN** a release build performs an export or an import
- **THEN** no log line at info level or above contains a medication name or dosage
