## Why

Pillsner keeps everything on the device and never removes a medicine, so a user who wants to start over — the app was set up for someone else, a trial run filled it with test medicines, the phone is being handed on — has no way out but to uninstall it. The one honest escape hatch is a deliberate, hard-to-hit reset that says plainly what it destroys and does exactly that.

## What Changes

- Add a **Danger zone** section to the Settings screen, as its last section, visually separated from the rest and carrying a single destructive action: **Reset app**.
- Tapping **Reset app** opens a confirmation dialog that names exactly what will be erased and cannot be confirmed until the user ticks a checkbox reading **"I understand all data will be erased permanently"**. The destructive button stays disabled while the box is unticked. Cancelling, dismissing or leaving the dialog erases nothing.
- On confirmation, the app erases every row of the on-device database — all medicines, all schedules and the whole intake history — in one transaction, and settles everything that hung off that data: the pending reminder alarm is cancelled, every posted reminder notification is taken down, and the watch is told there is nothing left to show.
- Settings survive the reset: the chosen language, the app lock (PIN and biometric unlock) and the recorded acceptance of the legal documents are untouched. The app stays configured — it is simply empty. The dialog says so, so the promise and the copy agree.
- The user stays on the Settings screen. A snackbar confirms the reset; Home and Medicines fall back to their empty states on their own because they observe the database.
- The reset is not reachable by accident: it takes a scroll to the bottom of Settings, a tap, a tick and a second tap, and no notification action, deep link or back-stack restoration can trigger it.
- All new text ships as English and Dutch string resources.
- No new permissions, no new dependency, no network access.

## Capabilities

### New Capabilities
- `app-reset`: the danger zone on the Settings screen, the confirmation contract (checkbox gating, cancel semantics), exactly what a reset erases and what it leaves alone, the side effects on alarms, notifications and the watch, and how the app behaves afterwards.

### Modified Capabilities
- `medication-persistence`: the standing requirement *Medications are never removed* forbids any production path that deletes a medication row. It is qualified so that one user-initiated, fully-confirmed reset of the entire database is the single permitted exception, performed as a database-wide clear rather than through a DAO delete — so the existing guard test against a medication delete in the DAO keeps standing unchanged.

## Impact

- New `domain/reset/` — an `AppDataEraser` port with no Android dependency, and an `EraseAllData` use case that clears the store and then settles reminders.
- New `data/reset/RoomAppDataEraser.kt` implementing the port against `PillsnerDatabase`.
- New `ui/settings/reset/` package: the danger zone section, the confirmation dialog and a view model.
- Changed: `ui/settings/SettingsScreen.kt` (one more section, one more snackbar message), `di/AppContainer.kt` (wire the eraser, the use case and the view model), `data/reminders/ReminderNotifier.kt` (a way to take down every reminder at once).
- New strings in `values/strings.xml` and `values-nl/strings.xml`.
- Tests: unit tests for the use case and the view model's gating, a Room test that the clear empties every table and leaves the schema version alone, Compose semantics tests for the section and the dialog (including the disabled destructive button and cancel-erases-nothing), and an instrumented check that the pending alarm and posted notifications are gone afterwards.
- Documentation: `README.md` gains the reset in its feature list if that list names settings features.
