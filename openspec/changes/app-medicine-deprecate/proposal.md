## Why

The Medicines screen from `app-medicine-overview` shows active and inactive medicines in two sections, and `app-medicine-add` lets the user create a medicine, but nothing lets the user move a medicine between the two sections. A medicine the user has stopped taking keeps producing doses and reminders until it is deleted, and a medicine the user starts taking again has to be re-entered. The active flag already exists on the model, in the database and in the dose generator; what is missing is the one-gesture way for the user to flip it.

## What Changes

- Every tile on the Medicines screen becomes **swipeable**. Dragging a tile sideways reveals a single action button behind it. On an active tile the button reads **Deactivate**; on an inactive tile it reads **Activate**. The tile stays revealed until the user taps the button, swipes it closed or reveals another tile.
- Tapping **Deactivate** sets the medicine's active flag to false. The tile moves from the active section to the inactive section, keeping the inactive section's alphabetical order. Tapping **Activate** does the reverse.
- Both sections remain **sorted alphabetically by name**, locale-aware and case-insensitive, exactly as the overview change already requires. A medicine that changes section is placed at its alphabetical position in its new section, not appended.
- Swiping is not discoverable for screen-reader, switch-access or keyboard users, so each tile also exposes the same **Activate**/**Deactivate** action as an accessibility custom action. The result is identical to tapping the revealed button.
- Deactivating a medicine stops it producing doses and reminders through the existing rule in `app-medicine-alarm`: the reminder coordinator observes the medication stream and removes un-reminded planned doses of inactive medicines. No new scheduling logic is added; this change only guarantees that flipping the flag goes through the repository so that rule fires.
- The `MedicationRepository` contract gains a **`setActive`** operation, implemented by the Room repository as an in-place update of the existing `is_active` column and by the in-memory repository for tests and previews. **No schema change**: the column exists since version 1.

## Capabilities

### New Capabilities

None. The behaviour extends the existing Medicines screen and repository contract.

### Modified Capabilities

- `medicine-overview`: adds requirements for the swipe-to-reveal action on every tile, for the activate/deactivate outcome (the tile changes section and keeps alphabetical order), for a single revealed tile at a time, and for an accessible non-gesture alternative. The "Medicines are provided through a domain contract" requirement is extended so that the active flag is changed only through the repository interface and the view model.
- `medication-schedule-model`: the "Medication repository contract" requirement gains a `setActive` operation that re-emits on the observation stream.
- `medication-persistence`: adds a requirement that the Room repository updates the active flag in place, in one statement, without touching schedules or doses, and that the change is emitted to observers. The schema stays at its current version.

Both `medicine-overview` and `medication-schedule-model` are currently deltas inside the active `app-medicine-overview` and `app-medicine-add` changes, and `medication-persistence` lives in `app-medicine-add` and `app-medicine-alarm`. Those changes MUST be archived before this one so the MODIFIED blocks have requirements to modify. If that order cannot be honoured, the MODIFIED blocks are rewritten as ADDED with distinct requirement names before archiving.

## Impact

- **Application code (`src/`)**: `ui/medicines` gains a `SwipeRevealTile` wrapper (built on Compose Foundation's anchored draggable, no new library), the revealed action button, and the `revealedMedicationId` screen state. `MedicinesViewModel` gains `onSetActive(id, isActive)` and a one-shot error effect. `domain/repository/MedicationRepository` gains `setActive`. `data/RoomMedicationRepository`, `MedicationDao` and `data/InMemoryMedicationRepository` implement it.
- **Dependencies**: none added. Compose Foundation's `anchoredDraggable` is already on the classpath through Material 3.
- **Depends on**: `app-medicine-overview` (screen, tiles, view model), `app-medicine-add` (Room repository, `is_active` column) being applied first. `app-medicine-alarm` is not a hard dependency for the UI, but without it a deactivated medicine merely moves sections; the reminder consequence arrives when that change is applied.
- **Persistence**: one new DAO update query. No migration, no new schema version, no new exported schema file.
- **Reminders**: none of the scheduling code changes. The existing "refresh on every medication change" rule in `app-medicine-alarm` handles the consequence. A reminder that is already showing for a medicine at the moment it is deactivated is left as is, in line with that change's rule of never touching reminded doses.
- **Tests**: unit tests for the view model event and for both repository implementations; Compose semantics tests for the reveal gesture, the button labels, the section move, the single-revealed-tile rule and the custom accessibility action; a DAO test for the update; a manual test case for TalkBack and large fonts.
- **README**: the "Features" line about the medicine overview gains "activate or deactivate a medicine by swiping its tile".

## Non-goals

- Deleting a medicine, editing it, or opening a detail screen from a tile.
- A confirmation dialog or an undo snackbar. The action is reversible with the opposite swipe, so neither is needed now.
- Automatically deactivating a medicine whose "use until" date has passed. That belongs to the scheduling change and is listed as an open question there.
- Dismissing a reminder notification that is already showing for a medicine being deactivated.
- Any change to sorting rules. The existing alphabetical ordering requirement is unchanged and simply continues to apply after a tile changes section.
