## Context

Settings is a `LazyColumn` of self-contained sections in `ui/settings/SettingsScreen.kt` — Language, Security, Legal, About — each one composable with its own state, added by appending an `item` to the list. The screen already owns a `SnackbarHostState` and already collects a one-shot effect flow (`SecurityEffect`) to drive it, so a section that needs to say "done" has somewhere to say it.

Everything the app knows about medicines lives in one Room database, `pillsner.db`, with three tables: `medications`, `schedules` (cascade-deleted from medications) and `doses`. Settings live elsewhere, in three separate Preferences DataStore files — `settings` (language, legal acceptance), `applock` (PIN verifier, biometric flag; excluded from backup) and `reminders` (whether the notification permission has been asked for).

Three things hang off the database and will not correct themselves when it empties:

- **The alarm.** `ReminderAlarmScheduler` holds one `PendingIntent` for "the next thing that has to happen". `ReminderCoordinator.rescheduleNextWake()` cancels it when `ComputeNextWake` returns null, which it will once there are no doses — but only if something runs a wake.
- **Posted notifications.** `ReminderNotifier` cancels a notification per dose id, and only for doses it can still see. Doses erased out from under it are never cancelled, so a reminder for a medicine that no longer exists would sit in the shade offering "Took it" for a row that is gone.
- **The watch.** `DoseSyncPublisher` publishes the pending doses to the Wear data layer. Nothing takes that payload down on its own.

Constraints from `CLAUDE.md`: the domain layer has no Android dependencies; user-facing text is a string resource with a Dutch translation; every visual decision is a theme token from `docs/design-system.md`; no new dependency and nothing leaves the device. And one standing requirement points the other way — `medication-persistence` says *Medications are never removed*, enforced by a guard unit test that fails the build when a medication delete appears in the DAO.

## Goals / Non-Goals

**Goals:**

- One honest way to empty the app, stated in words that match exactly what happens.
- Impossible to trigger by accident, and impossible to trigger by anything but a person on the Settings screen: two deliberate taps with a checkbox between them.
- Atomic: after a reset either every table is empty or none is, never half a history.
- No orphans: no pending alarm, no posted reminder, no stale payload on the watch.
- The app stays usable and configured afterwards — it is empty, not uninstalled.
- The existing "medicines are never removed" guard keeps standing, unweakened, for every other code path.

**Non-Goals:**

- Wiping settings. Language, the app lock and legal acceptance survive; a factory reset of those is a separate proposal.
- Export, backup or a way to undo a reset. The dialog is the undo, and it comes before.
- Deleting a single medicine. That remains impossible by design; deactivating one is how a medicine goes away.
- A reset from a notification, a deep link, a shortcut or an intent. There is exactly one entry point.

## Decisions

### D1. What a reset erases, and what it deliberately does not

Erased: every row of `medications`, `schedules` and `doses` — every medicine, every schedule, the whole intake history including taken, skipped and missed outcomes.

Kept: the chosen language, the app lock (PIN verifier and biometric flag) and the recorded acceptance of the legal documents, along with the notification-permission flag. None of these is data the user entered about their health; they are how the app is set up, and losing them would lock someone out of an app they had just asked to keep, or ask them to re-accept documents they have already accepted.

This is the line the dialog copy has to draw, so the dialog names what goes rather than gesturing at "everything". The checkbox keeps its plain wording — *"I understand all data will be erased permanently"* — because in this app "data" is the medicines and the history; the body text above it says so in full.

Alternative considered: a true factory reset that also clears the three DataStore files and sends the user back through the legal acceptance flow. Rejected for now: it turns one destructive action into two unrelated ones, and someone resetting a phone they are keeping would be silently stripped of their app lock. It is the obvious follow-up proposal if it is ever wanted.

### D2. A domain port, `AppDataEraser`, implemented over `clearAllTables()`

`domain/reset/AppDataEraser.kt` — a single-method interface, `suspend fun eraseAll()`, with no Android import. `data/reset/RoomAppDataEraser.kt` implements it as `withContext(Dispatchers.IO) { database.clearAllTables() }`.

`clearAllTables()` is the right instrument for exactly this job: it empties every table in one transaction, resets the auto-increment sequences, and fires the invalidation tracker so every Room-backed `Flow` in the app re-emits. It is also declared `@WorkerThread`, hence the explicit dispatcher.

Alternatives considered:

- *A `deleteAll()` on each DAO.* Rejected: three deletes are three chances to leave the database half-erased, and it means adding a medication delete to the DAO — the one thing the `medication-persistence` guard test exists to prevent.
- *Deleting the database file.* Rejected: it needs the database closed, invalidates every open DAO and `Flow` in the process, and would take the schema version with it. Clearing rows keeps the schema and the migration history exactly where they are.

The reset auto-increment sequences are worth a note: dose ids restart at 1, and notification ids are derived from dose ids. That is safe only because D4 takes every posted notification down first; it would otherwise be possible for a new dose to inherit a stale notification.

### D3. `EraseAllData`: one use case, one fixed order

`domain/reset/EraseAllData.kt` holds the whole operation and its order, so the view model has one thing to call and the order is testable without a device:

1. `eraser.eraseAll()` — the store is emptied first, so nothing that runs afterwards can see a dose that is about to vanish.
2. `notifications.cancelAll()` — every posted reminder comes down. Doing this *after* the erase means a reminder that arrives in the same second is also caught; doing it before would leave a window.
3. `reminders.requestWake(MEDICATIONS_CHANGED)` — the coordinator recomputes against an empty database: `ComputeNextWake` returns null, the alarm is cancelled, and `DoseSyncPublisher.publishNow()` pushes an empty list to the watch. No new code path is needed for either; the existing wake cycle already does exactly the right thing when there is nothing to do.

Steps 2 and 3 are behind narrow domain ports (`ReminderTeardown`, `ReminderRefresh` — each one method) so the use case stays Android-free and a unit test can assert the order with fakes. `AppContainer` binds them to `ReminderNotifier::cancelAll` and `ReminderCoordinator::requestWake`.

Note that `ReminderCoordinator.start()` already collects `medicationRepository.observeAll()` and wakes on every change, so the clear will provoke a wake on its own. Step 3 is still explicit: relying on an observer to run a cancellation is a race, and the coordinator's mutex makes a second wake harmless.

### D4. `ReminderNotifier.cancelAll()`

One new method: `notificationManager.cancelAll()`. Pillsner posts reminders and nothing else, so cancelling everything the app has posted is precisely cancelling every reminder, and it needs no record of which dose ids were ever shown — which is what makes it correct after the rows are gone. Its KDoc states that dependency, so a later change that posts a second kind of notification is told to narrow it to the reminder group.

Alternative considered: reading `NotificationManager.activeNotifications` and cancelling those in the `reminders` group. Rejected: it is API-gated, returns nothing for a process without the right permission state, and buys nothing while the app has one kind of notification.

### D5. The medication-persistence requirement is qualified, not weakened

*Medications are never removed* becomes: no production code path deletes a medication row **except** the single, fully-confirmed, user-initiated reset of the entire database, and that reset performs a database-wide clear rather than a DAO delete. The DAO still declares no delete; the guard test still fails the build if one appears; every other path is as forbidden as it was. The delta spec says this explicitly so the two requirements cannot be read as contradicting each other.

### D6. The Danger zone section

`ui/settings/reset/DangerZoneSection.kt`, appended as the last `item` of the Settings `LazyColumn`, after About.

- A `headlineSmall` heading "Danger zone", marked `heading()`, in `colorScheme.error` — one of the cases `docs/design-system.md` section 4 reserves red for.
- A `bodyMedium` line in `onSurfaceVariant` saying what the button does, so the destructive action is explained before it is offered.
- A **Reset app** `Button` with `containerColor = colorScheme.error` and `contentColor = colorScheme.onError`, `Sizes.primaryActionHeight`, full width — the "Destructive" button of design system section 8.2, which that section permits only in a confirmation context. Here it opens the confirmation rather than performing anything, which is the whole point of the section.
- The section is separated from About by a `HorizontalDivider` above the heading, so the boundary is visible and not only chromatic. Colour is never the only signal.

The section is last because a destructive action should take a deliberate scroll to reach, and because the reading order "here is your app, and here is how to empty it" is the honest one.

### D7. The confirmation dialog

`ui/settings/reset/ResetAppDialog.kt` — an `AlertDialog`, `shapes.large`, per design system 8.12:

- Title, `headlineMedium`: "Reset app?"
- Body, `bodyLarge`: names exactly what goes — every medicine, every schedule, the complete intake history — and, in a second `bodyMedium` line, what stays: the language, the app lock and the accepted documents. Two paragraphs, because a list of casualties without a list of survivors reads as "everything" and would be a lie.
- A `Row` with a `Checkbox` and its label, **"I understand all data will be erased permanently"**, the whole row `toggleable(role = Role.Checkbox)` with `Sizes.minTouchTarget` minimum height so the label is part of the target. The checkbox carries no separate content description: the label is the accessible name, and a screen reader announces the ticked state from the role.
- `confirmButton`: **Erase everything**, an error-container `Button`, `enabled = accepted`. Disabled is the gate, not a validation message — nothing is wrong yet, the user simply has not confirmed.
- `dismissButton`: **Cancel**, a `TextButton`.
- `onDismissRequest` is Cancel. Tapping outside, pressing back and tapping Cancel are the same thing and all three erase nothing.

The checkbox state lives in the view model, not in the dialog, so a configuration change cannot silently untick it — or worse, leave it ticked over a dialog that was recreated. The dialog is a pure function of the state it is given.

### D8. `ResetViewModel`

State: `dialogVisible`, `confirmationAccepted`, `inProgress`. Events: `onResetTapped`, `onDismiss`, `onConfirmationToggled`, `onConfirmed`. A one-shot `Channel`-backed `Flow<ResetEffect>` with a single member, `Erased`, which the Settings screen turns into a snackbar — the same shape `SecurityEffect` already uses, so the screen gains one more `collect` in the effect it already has.

`onConfirmed` guards on `confirmationAccepted` and on `inProgress` before doing anything: a double tap, or a call from a restored state, erases once or not at all. It closes the dialog and resets `confirmationAccepted` to false before emitting, so reopening the dialog always starts unticked.

### D9. Staying on Settings is safe

The user stays where they are and gets a snackbar. Nothing has to be torn down:

- `HomeViewModel` observes `RoomUpcomingDosesRepository`, `MedicinesViewModel` observes `RoomMedicationRepository` — both Room-backed `Flow`s, both re-emitted by `clearAllTables()`, both already having an empty state to fall into.
- The Medicines back stack may hold a detail, form or history destination keyed by a medicine id that no longer exists. `MedicineHistoryViewModel` already handles a medicine it cannot read with a `MedicineHistoryEffect.OpenFailed` that closes the screen. The equivalent path on the other id-keyed destinations is verified as part of this change rather than assumed — returning to a dead route must close it, never crash.

### D10. Strings

Nine new strings, English and Dutch: the section heading, its explanation, the section button, the dialog title, the two body paragraphs, the checkbox label, the confirm button and the snackbar message. Nothing is composed from fragments at runtime, so Dutch can phrase each one freely.

## Risks / Trade-offs

- **A user erases a real medication history by accident** → The gate is a scroll to the bottom of Settings, a tap, a checkbox that must be ticked, and a second tap on a button that is disabled until it is. The dialog names what goes before it goes. There is no undo, and the copy does not pretend there is.
- **The erase succeeds but a side effect fails — a notification survives, or the alarm is left set** → The order in D3 puts the irreversible step first and the corrections after, so a failure never leaves data half-erased. A surviving alarm is self-correcting: the wake it triggers finds an empty database and cancels itself. A surviving notification answers into `RecordIntake` for a dose id that no longer exists, which is already a no-op path the reminder code handles.
- **`clearAllTables()` blocks while a large history is deleted** → It runs on `Dispatchers.IO` behind `inProgress`, and the confirm button is disabled while it runs. The volume is a few thousand rows at most on a device that generates two days of doses at a time.
- **Dose ids restart at 1 and could collide with a stale notification id** → Every notification is cancelled in the same operation (D4), so there is nothing left to collide with. This is the reason step 2 is not optional.
- **The requirement change reads as permission to delete medicines** → The delta states the exception in one sentence, names the mechanism (database-wide clear, not a DAO delete) and keeps the guard test, so the narrow reading is the only available one.
- **The watch is out of reach when the phone resets** → `DoseSyncPublisher` writes the empty payload to the data layer, which the watch picks up when it next connects. Until then it shows doses that no longer exist. That is the existing behaviour of every change to the data on the phone, not something this change introduces.
