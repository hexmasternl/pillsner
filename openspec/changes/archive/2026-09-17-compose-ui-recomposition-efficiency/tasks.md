## 1. Medicines list tile descriptions

- [x] 1.1 Wrap the `tile.schedules.map { formatter.describe(it.summary, it.amount) }` call in `remember(tile.schedules) { ... }` at both call sites in `ui/medicines/MedicinesScreen.kt` (active list and inactive list).
- [x] 1.2 Manually verify swipe-reveal on one tile no longer causes sibling tiles to recompute (e.g. with the Compose layout inspector or a recomposition-count log), and that descriptions still update when a tile's schedules actually change.

## 2. Medication form draft saving

- [x] 2.1 In `ui/medicines/form/MedicationFormViewModel.kt`, confirm `initialDraft` is assigned only inside `load()` (grep all assignments in the file).
- [x] 2.2 Remove the unconditional `DraftSaver.saveInitial(savedStateHandle, initialDraft)` call from `updateDraft`, and call it once from `load()` instead, at the point `initialDraft` is assigned.
- [x] 2.3 Manually verify: opening the form still restores correctly after process death (saved state still has the initial draft), and editing fields no longer triggers `ScheduleCodec.encode` / `SavedStateHandle` writes on every keystroke.

## 3. Reminder diagnostics list keys

- [x] 3.1 Read the diagnostics entry model used by `ReminderDiagnosticsScreen.kt` to confirm which field(s) are guaranteed unique across entries (e.g. timestamp, or timestamp + outcome).
- [x] 3.2 Replace `itemsIndexed(entries, key = { index, _ -> index })` with a stable per-entry key derived from that field (e.g. `entry.at.toEpochMilli()`, or a composite key if timestamps can collide).
- [x] 3.3 Manually verify: adding a new diagnostic entry does not visibly recompose/flicker existing rows, and no duplicate-key crash occurs.

## 4. Verification

- [x] 4.1 Run the `pillsner-ui-review` skill against `MedicinesScreen.kt`, `MedicationFormViewModel.kt`, and `ReminderDiagnosticsScreen.kt` to confirm no visual or design-system regression was introduced.
- [x] 4.2 Run unit tests (`./gradlew testDebugUnitTest` from `src/`) and confirm they pass.
- [x] 4.3 Run lint (`./gradlew lintDebug` from `src/`) and confirm it passes.
- [x] 4.4 Run or update Compose UI tests covering the medicines list, medication form, and reminder diagnostics screens; confirm they pass unchanged (no behaviour change expected).
