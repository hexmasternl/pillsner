## Context

`docs/todo.md` (2026-09-16 optimization scan) flags three independent Compose recomposition/state-write inefficiencies:

- `MedicinesScreen.kt:196,230` computes `tile.schedules.map { formatter.describe(...) }` inline inside the `items(...)` lambda, on every recomposition of that tile. The screen keeps a single `revealedId: String?` state read by every tile's `isRevealed = revealedId == tile.id.value` check, so revealing/hiding the swipe action on any one tile recomposes *every* visible tile, re-running the formatter for tiles whose data did not change.
- `MedicationFormViewModel.kt:242-252` (`updateDraft`) unconditionally calls `DraftSaver.saveInitial(savedStateHandle, initialDraft)` on every draft mutation. `saveInitial` re-runs `ScheduleCodec.encode` over every schedule and writes 9 `SavedStateHandle` entries, but `initialDraft` is only ever assigned inside `load()` — every other call is a no-op write repeated on every keystroke.
- `ReminderDiagnosticsScreen.kt:159` keys its `LazyColumn` rows with `itemsIndexed(entries, key = { index, _ -> index })`. `entries` is newest-first and grows at the front, so every existing row's index — and therefore its key — shifts by one each time a new entry arrives, forcing Compose to treat every visible row as changed instead of recognizing it as the same row moved down one slot.

All three are the same class of bug: a value that is stable across most recompositions is either recomputed or rewritten as if it changed, because the code doesn't tell Compose (or `SavedStateHandle`) it's actually stable.

## Goals / Non-Goals

**Goals:**
- Make each of the three call sites do work proportional to what actually changed (the tile whose schedule changed, the load that actually reset the draft, the row that's actually new).
- Preserve exact current behaviour and visual output — this is a performance-only change.

**Non-Goals:**
- No change to `MedicinesScreen`, medication form, or diagnostics screen layout, copy, or interaction model.
- No broader refactor of `SwipeableMedicineTile`, `DraftSaver`, or the diagnostics list beyond the specific fix.
- Not introducing a spec for the reminder diagnostics screen — none exists today and this change doesn't add one.

## Decisions

1. **`MedicinesScreen`: `remember(tile.schedules) { tile.schedules.map { formatter.describe(it.summary, it.amount) } }`** at each of the two call sites (active and inactive lists), keyed on `tile.schedules`. Alternative considered: hoist `descriptions` computation into the tile's UI-state model (compute once when the list is built) — rejected for this change because it would touch the ViewModel/UI-state shape, which is a larger surface than the proposal's stated scope; the `remember` fix is a pure call-site change with no data-model impact.
2. **`MedicationFormViewModel`: move the `DraftSaver.saveInitial(...)` call out of `updateDraft` and into `load()`**, where `initialDraft` is actually assigned, so it runs once per load instead of once per keystroke. Alternative considered: keep the call in `updateDraft` but guard it with `if (draft !== initialDraft)` — rejected because `load()` is the single source of truth for when `initialDraft` changes, and moving the call there is simpler and removes the check entirely rather than adding one.
3. **`ReminderDiagnosticsScreen`: key on `entry.at.toEpochMilli()`** (the entry's timestamp) instead of list index. Assumes diagnostic entries have distinct timestamps at the granularity recorded; if two entries can share a timestamp, combine with a stable per-entry identifier if one exists on the entry model — verify during implementation by reading the entry data class before assuming `at` alone is unique.

## Risks / Trade-offs

- [Moving `saveInitial` into `load()` could miss a case where `initialDraft` is mutated outside `load()`] → Grep all assignments to `initialDraft` in `MedicationFormViewModel.kt` before moving the call, to confirm `load()` is the only site.
- [Diagnostic entry timestamps might collide, causing a Compose duplicate-key crash] → Read the entry model's fields during implementation; if timestamps aren't guaranteed unique, key on a combination that is (e.g. timestamp + outcome) or on an existing stable id if the entry has one.
- [Any of the three changes could subtly alter recomposition-dependent behaviour, e.g. animation timing tied to recomposition] → Manually exercise the medicines list swipe interaction, the medication form, and the diagnostics screen after the change, in addition to running `pillsner-ui-review` and the existing Compose UI tests.

## Migration Plan

No data migration. Roll out as a normal code change; rollback is a plain revert since no schema, spec, or persisted-state format changes.

## Open Questions

- Does the reminder diagnostics entry model guarantee unique timestamps? Resolve by reading the entry data class before implementing the key change.
