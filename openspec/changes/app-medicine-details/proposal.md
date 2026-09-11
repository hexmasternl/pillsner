## Why

After `app-medicine-add` a medicine can be created, and after `app-medicine-deprecate` it can be switched between active and inactive, but nothing lets the user open a medicine and correct it: a typo in the name, a dose the doctor changed, a schedule that moved from 08:00 to 09:00. Today the only remedy is to deactivate the old entry and add a new one, which fragments the medicine's history across two records. Tapping a tile is also the most natural gesture on the Medicines screen and it currently does nothing.

## What Changes

- Tapping a medicine tile on the Medicines screen opens a **Medicine details screen** for that medicine. The screen is the Add medicine form in a second mode: the same fields (name, default dose, used since, use until, prescribed by, schedules), the same schedule editor, the same validation and the same discard confirmation, but **pre-populated** with the medicine's saved values and titled "Medicine details".
- The user can change **any** detail: rename the medicine, change the default dose, move the dates, change the prescriber, add, edit or remove schedules, and switch the medicine between **active** and **inactive** with a switch on the screen. Save writes the whole medicine back in one transaction and returns to the Medicines screen, where the tile reflects the new values immediately.
- The add-medicine flow becomes a **medicine form flow** with two entry points: the + button opens it empty in add mode, a tile opens it filled in edit mode. Both modes share one screen, one view model and one draft. The route, screen and view model are renamed from "AddMedication" to "MedicationForm" so the names stop lying.
- The repository contract gains a way to **load one medication by identifier** and to **update a medication with its schedules**. Updating replaces the medicine's schedule rows in the same transaction as the field update. No schema change is needed.
- **A medicine can never be removed.** This change states that rule explicitly in the specs: the repository exposes no removal operation, no production code path deletes a `medications` row, and neither the details screen nor the overview offers a delete action. Deactivating is the only way to stop a medicine, and its record, its schedules and its dose history stay on the device. The dose snapshots and the nullable medication reference that `app-medicine-alarm` designed for "deletion in a later change" stay as defensive storage, but the path that would exercise them is closed by contract.
- Editing a medicine **preserves its history**: recorded intakes are never altered, and reminded doses are never rewritten. Only planned doses that have not yet been reminded are regenerated, through the refresh rule that `app-medicine-alarm` already defines for "schedule edited". This change adds no scheduling logic; it guarantees that every edit goes through the repository so that rule fires.
- On the Medicines screen, tapping a tile whose activation action is revealed (from `app-medicine-deprecate`) **closes the tile** instead of opening details, so a finger that just swiped never lands on the wrong screen.

## Capabilities

### New Capabilities
- `medicine-details`: Opening a medicine from its tile; the details screen as the form in edit mode: pre-population, the title, the active switch, saving as an in-place update, discard confirmation relative to the loaded values, draft survival, the "never removed" rule at the UI level, history preservation, and accessibility.

### Modified Capabilities
- `medicine-overview`: A new requirement makes every tile tappable and opens the details screen; the "Swiping a tile reveals one activation action" requirement from `app-medicine-deprecate` gains the rule that tapping a revealed tile closes it. This spec is a delta inside the active `app-medicine-deprecate` change, which MUST be archived before this one.
- `medication-schedule-model`: The "Medication repository contract" requirement gains a load-by-identifier operation and an update operation that replaces a medication's fields and schedules, and states that the contract has no removal operation. This spec is a delta inside the active `app-medicine-deprecate` change.
- `medication-persistence`: New requirements for the in-place update (one transaction, schedules replaced, doses untouched, stream emits, no schema change) and for the absence of any medication delete path in production code. The cascade delete that schema version 1 already declares on `schedules` is what makes replacing schedules cheap; it is never used to remove a medicine.
- `app-navigation`: The "Add medicine destination" requirement becomes the medicine form flow with two entry points and a route parameter for the medicine identifier; the "Schedule editor destination" requirement is reworded for the renamed flow. These are deltas inside the active `app-medicine-add` change.

## Impact

- **Application code (`src/`)**: `ui/navigation/Routes.kt` replaces `AddMedicationGraph`/`AddMedicationForm` with `MedicationFormGraph(medicationId: Long?)`/`MedicationForm`; `PillsnerApp` passes the id into the graph and the overview tile gains an `onClick`. `ui/medicines/add` is renamed to `ui/medicines/form`: `MedicationFormScreen`, `MedicationFormViewModel`, `MedicationFormUiState`, `MedicationFormDraft` gain a mode, a loading state, an identifier, the active flag and an initial-draft snapshot for the discard check. `MedicineTile` and `SwipeRevealTile` gain tap handling. The domain gains `MedicationRepository.get(id)` and `update(medication)`; `RoomMedicationRepository` and `InMemoryMedicationRepository` implement both; `MedicationDao` gains a single-medication query, an update statement, a delete-schedules-by-medication statement and a `@Transaction` update.
- **Dependencies**: none added. No network, no telemetry, no schema change.
- **Depends on**: `app-welcome-screen`, `app-medicine-overview`, `app-medicine-add` and `app-medicine-deprecate` applied and archived first, in that order. `app-medicine-alarm` is not a hard dependency of the code, but the "history preserved on edit" scenarios can only be exercised end to end once it is applied; until then they are covered at the repository level (dose rows untouched by the update statement).
- **Privacy**: medication names and doses are never logged at info level or above; the update failure message is generic.
- **Tests**: unit tests for the view model in edit mode (load, pre-population, touched detection, save calls update with the same id, active toggle, failure path) and for the draft saver with an identifier; Room DAO tests for the update (fields replaced, schedules replaced in order, dose rows untouched, stream emits, transaction rolls back on schedule insert failure, no delete method on the medication DAO); Compose tests for tile tap opening the pre-filled form, tap on a revealed tile closing it, and the edit-then-save round trip on the overview; a manual test at the largest font scale with TalkBack.
- **README**: the "Edit, pause or archive a medication without losing its history" feature line becomes accurate for editing; the wording is adjusted so it does not promise deletion.

## Non-goals

- Deleting, archiving or hiding a medicine. Never, by product rule. Inactive is the end state.
- Editing individual doses or intake history from the details screen.
- Showing the medicine's dose history or adherence on the details screen. That is a separate screen and a separate change.
- Undo after saving an edit.
- Concurrent-edit reconciliation. The app is single-activity and nothing else writes medication fields while the form is open, so the last save wins.
