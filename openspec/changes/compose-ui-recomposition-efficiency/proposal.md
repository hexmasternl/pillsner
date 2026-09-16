GitHub issue: [#4](https://github.com/hexmasternl/pillsner/issues/4)

## Why

Three Compose screens do unnecessary work on every recomposition or keystroke: `MedicinesScreen` re-runs schedule-description formatting for every tile whenever any tile's swipe reveal state changes, `MedicationFormViewModel.updateDraft` re-encodes and rewrites the entire saved-state draft on every keystroke, and `ReminderDiagnosticsScreen` uses a shifting list index as its `LazyColumn` key so every visible row recomposes each time a new diagnostic entry arrives. None of these are bugs — output is correct today — but they waste CPU and battery on every interaction with the medicines list, the medication form, and the diagnostics screen. Fixing the shared "missing memoization / unstable key" pattern across all three keeps the app responsive as lists grow.

## What Changes

- `ui/medicines/MedicinesScreen.kt`: wrap the per-tile schedule description computation in `remember(tile.schedules) { ... }` so only the tile whose schedules actually changed recomputes, instead of every visible tile recomputing whenever the shared `revealedId` state changes.
- `ui/medicines/form/MedicationFormViewModel.kt`: only call `DraftSaver.saveInitial(...)` when `initialDraft` itself changes (i.e. inside `load()`), not on every `updateDraft` mutation, removing the redundant `ScheduleCodec.encode` + `SavedStateHandle` rewrite on every keystroke.
- `ui/settings/diagnostics/ReminderDiagnosticsScreen.kt`: replace the list-index `LazyColumn` key with a stable key derived from each entry (e.g. `entry.at.toEpochMilli()`), so only newly-inserted rows recompose.
- No visual, behavioural, or navigation changes. Output and on-screen appearance are identical before and after.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `medicine-overview`: the "Only one tile is revealed at a time" requirement gains an explicit guarantee that revealing or closing one tile never changes another tile's displayed schedule description — already true today, now stated as a contract so the `remember`-based fix has a scenario to be tested against.

The medication form draft-saving fix and the reminder diagnostics list-key fix have no corresponding capability in `openspec/specs/` (no spec documents the form's saved-state mechanics or the diagnostics screen), so they carry no spec delta — they remain pure internal efficiency fixes.

## Impact

- Affected files: `ui/medicines/MedicinesScreen.kt`, `ui/medicines/form/MedicationFormViewModel.kt`, `ui/settings/diagnostics/ReminderDiagnosticsScreen.kt`.
- No new dependencies, no schema changes, no navigation changes.
- Must be verified against `docs/design-system.md` via the `pillsner-ui-review` skill to confirm no visual regression was introduced, even though none is intended.
