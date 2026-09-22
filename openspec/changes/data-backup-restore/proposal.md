**GitHub Issue:** #32 (https://github.com/hexmasternl/pillsner/issues/32)

## Why

Pillsner keeps every medicine, schedule and intake record on-device only, with no account and no cloud sync — that is the whole privacy pitch. But it also means an uninstall, a factory reset or a move to a new phone loses everything, with no way back. A local, user-initiated export/import closes that gap without weakening the no-network promise: the file is written and read only through the Storage Access Framework, at the user's explicit choice, and Pillsner itself never transmits it anywhere.

## What Changes

- Add an **Export data** action and an **Import data** action to Settings, above the existing "Danger zone" (Reset app) section, since these are safe, reversible-by-the-user actions rather than destructive ones.
- **Export** writes a single versioned, encrypted `.pill` file via the Storage Access Framework document picker, containing every medication (active and inactive), every schedule and every recorded intake (taken, skipped, missed). Device-only state (app lock PIN, accepted-legal-document timestamps, Wear OS pairing) is explicitly excluded, matching how `app-reset` already treats those as surviving separately from user data.
- The export is protected by a passphrase the user supplies at export time: PBKDF2-HMAC-SHA256 with a random per-export salt derives an AES-256-GCM key (both from the JDK's built-in `javax.crypto`, no new dependency). The salt, KDF iteration count and nonce are stored unencrypted in a small file header; GCM authentication means a wrong passphrase or a corrupted file fails loudly at import rather than producing garbage data. There is no passphrase recovery.
- The file header carries its own format/schema version, independent of the Room schema version, so a future app version can still read an older `.pill` file. Importing a file from a newer, unrecognised format version fails with a clear message; nothing is partially imported.
- **Import** replaces the entire on-device dataset — medications, schedules and intake history — after a confirmation dialog that, like the Reset app confirmation, states plainly what will be overwritten. There is no merge; merge semantics are out of scope for this change.
- Generating or restoring a backup requires the app to already be unlocked when an app lock is enabled, the same as any other screen showing medication data.
- Requires reading the file passphrase and running the export/import off the main thread; requires no new Android permission beyond the Storage Access Framework's implicit, per-file access grant (no broad storage permission).

## Capabilities

### New Capabilities
- `data-backup-restore`: local, passphrase-encrypted export of the full on-device dataset to a user-chosen file, and import that replaces the on-device dataset after confirmation; covers the Settings entry points, the `.pill` file format and versioning, the encryption scheme, and the replace-on-import behaviour.

### Modified Capabilities
(none — `app-reset`'s existing requirement that its own confirmation dialog "MUST NOT offer any export, backup or undo" is about that dialog specifically and is unaffected by a separate Settings action elsewhere; `medication-persistence`'s "no medication data leaves the device through this capability" is scoped to that capability and stays true — this change adds a distinct, user-initiated local file operation, not network transmission)

## Impact

- **UI**: new Settings section/actions (Export data, Import data), a passphrase entry dialog for each direction, and an import confirmation dialog styled after the existing reset confirmation.
- **Domain**: a new backup/restore use case that reads the full repository state into a serialisable snapshot and reconstructs it from one; no change to the `Medication`, `Schedule`, `Dose` or `Intake` models themselves.
- **Data**: no Room schema change; a new file format (versioned, encrypted JSON) that is independent of but tracks the Room schema version.
- **Platform APIs**: Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`) for file selection; `javax.crypto` (PBKDF2, AES-256-GCM) for encryption — both first-party, no new dependency.
- **Scheduling**: after a replacing import, reminders must be rescheduled against the newly-imported schedules exactly as `app-reset` already rebuilds the alarm state after erasing data, and the paired watch (if any) must be re-synced.
