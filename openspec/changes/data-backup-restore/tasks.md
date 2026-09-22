## 1. Domain: backup snapshot model and use cases

- [ ] 1.1 Define a domain-layer `BackupSnapshot` model covering every medication, schedule, intake record and stock/refill value, independent of Room entities.
- [ ] 1.2 Implement an `ExportBackupUseCase` that reads the current repository state into a `BackupSnapshot`.
- [ ] 1.3 Implement an `ImportBackupUseCase` that replaces the repository state from a `BackupSnapshot` in a single transaction, rolling back entirely on any failure.
- [ ] 1.4 Add unit tests for both use cases covering an empty dataset, a populated dataset, and a transaction failure during import leaving prior data intact.

## 2. Data: file format, versioning and encryption

- [ ] 2.1 Define the `.pill` file header (format version, salt, KDF iteration count, nonce) and a format-version-1 JSON payload schema for `BackupSnapshot`.
- [ ] 2.2 Implement PBKDF2-HMAC-SHA256 key derivation and AES-256-GCM encrypt/decrypt using `javax.crypto`, with a random salt and nonce generated per export.
- [ ] 2.3 Implement the format-version-1 serialiser/deserialiser for `BackupSnapshot`.
- [ ] 2.4 Implement version-mismatch handling: fail with a clear, distinguishable error when the file's format version is newer than any this app version deserialises.
- [ ] 2.5 Implement wrong-passphrase and corrupted-file handling: any GCM authentication failure surfaces as one clear "could not be read" error, never a partial result.
- [ ] 2.6 Add unit tests for encrypt/decrypt round-trip, wrong passphrase, corrupted ciphertext, and an unrecognised format version.

## 3. Platform: Storage Access Framework integration

- [ ] 3.1 Wire "Export data" to `ACTION_CREATE_DOCUMENT` and write the encrypted file to the chosen `Uri`.
- [ ] 3.2 Wire "Import data" to `ACTION_OPEN_DOCUMENT` and read the encrypted file from the chosen `Uri`.
- [ ] 3.3 Run both file operations off the main thread via a coroutine, with a progress state exposed to the UI.
- [ ] 3.4 Verify no broad storage permission is requested — only the per-file SAF grant.

## 4. UI: Settings entry points and dialogs

- [ ] 4.1 Add "Export data" and "Import data" actions to the Settings screen, above the Danger zone section, following `docs/design-system.md`.
- [ ] 4.2 Build the passphrase entry + confirmation dialog for export, disabling proceed until the two values match and neither is empty.
- [ ] 4.3 Build the passphrase entry dialog for import.
- [ ] 4.4 Build the import confirmation dialog, styled after the existing reset confirmation, naming that existing medicines, schedules and intake history will be replaced.
- [ ] 4.5 Surface export/import success and each distinct failure (wrong passphrase, corrupted file, unrecognised version, cancelled picker) as clear, non-technical messages.
- [ ] 4.6 Gate both actions behind the app lock, consistent with how other screens showing medication data are gated.
- [ ] 4.7 Add all new strings to both `values/strings.xml` and the Dutch translation; no medical detail in any string.
- [ ] 4.8 Run `pillsner-ui-review` against the new Settings actions and dialogs.

## 5. Post-import reconciliation

- [ ] 5.1 After a successful import, rebuild the reminder/alarm schedule from the imported data, cancelling stale alarms and scheduling new ones, reusing the existing post-reset rebuild path.
- [ ] 5.2 After a successful import, publish the imported doses to a paired watch, replacing whatever it held before, reusing the existing post-reset watch sync path.
- [ ] 5.3 Add an instrumented test confirming alarms and watch state match the imported data after import.

## 6. Verification

- [ ] 6.1 Run unit tests and lint; fix any failures.
- [ ] 6.2 Run instrumented tests covering export/import, alarm scheduling and watch sync.
- [ ] 6.3 Manually verify: export with a passphrase, uninstall and reinstall (or reset), import the file, and confirm medicines, schedules and history match exactly.
- [ ] 6.4 Manually verify a wrong passphrase, a corrupted file, and cancelling each picker all leave existing data untouched.
