## Context

After `app-medicine-add` the Medicines screen's + button opens a nested `AddMedicationGraph` whose two destinations, `AddMedicationForm` and `EditSchedule(index)`, share one `AddMedicationViewModel` scoped to the graph entry. The view model owns an `AddMedicationDraft` (all fields plus `List<Schedule>`), persisted through `SavedStateHandle` by a `DraftSaver`, validated by pure `AddMedicationValidator` and `ScheduleDraftValidator`, and saved through `MedicationRepository.add(NewMedication)`. `RoomMedicationRepository` sits on a `MedicationDao` over `medications` and `schedules` (one-to-many, cascade delete, schema version 1). `app-medicine-deprecate` adds `setActive(id, isActive)`, wraps every `MedicineTile` in a `SwipeRevealTile` built on `anchoredDraggable`, and keeps one `revealedId` on the screen. `app-medicine-alarm` adds a `doses` table whose rows reference `medications` only (never `schedules`), snapshot the name and amount, and are re-planned by `RefreshPlannedDoses` whenever the medication stream emits: pending, un-reminded doses that no longer match a schedule are deleted and missing ones are inserted; reminded doses and doses with an intake are never touched.

Tiles are not tappable. There is no way to load a single medication, no way to update one, and, by design of the earlier changes, no way to delete one. The product owner has now made the last point a rule: a medicine can never be removed from the system, so that its history is never lost.

Constraints from `CLAUDE.md`: domain layer without Android imports, strings in resources, Material 3 accessibility, single DI mechanism, never log medication names or dosages at info level or above, no destructive migration, no broad refactors bundled into a feature, every visual decision from `docs/design-system.md` tokens checked with `pillsner-ui-review`. The form inherits its layout from `app-medicine-add` (secondary screen with a `TopAppBar`, fields per section 8.11, the filled Save button pinned to the bottom); this change adds one row and a title and changes nothing else visually.

## Goals / Non-Goals

**Goals:**
- Tap a tile, land on a form that already holds that medicine, change anything, save, and see the tile update.
- One form implementation for add and edit. Every rule the add form has (fields, validation, schedule editor, discard confirmation, draft survival) applies unchanged in edit mode.
- An update path in the repository and database that writes the medicine and its schedules atomically, leaves dose rows alone, and lets the existing refresh rule adjust future reminders.
- The "never removed" rule written into the contract and the specs, with a guard test, so no later change adds a delete by accident.
- No new dependency, no schema change, no scheduling logic.

**Non-Goals:**
- Delete, archive or hide a medicine in any form.
- Dose history, adherence or intake editing on the details screen.
- Live-updating the open form when the medication changes underneath it.
- Undo after saving.

## Decisions

### D1. One form, two modes, driven by a nullable route argument

```kotlin
@Serializable data class MedicationFormGraph(val medicationId: Long? = null)   // null = add
@Serializable data object MedicationForm
@Serializable data class EditSchedule(val index: Int? = null)                   // unchanged

sealed interface MedicationFormMode {
    data object Add : MedicationFormMode
    data class Edit(val id: MedicationId) : MedicationFormMode
}
```

`AddMedicationGraph`, `AddMedicationForm`, `AddMedicationScreen`, `AddMedicationViewModel`, `AddMedicationUiState`, `AddMedicationDraft` and `AddMedicationValidator` are renamed to `MedicationFormGraph`, `MedicationForm`, `MedicationFormScreen`, `MedicationFormViewModel`, `MedicationFormUiState`, `MedicationFormDraft` and `MedicationFormValidator`, and the package `ui/medicines/add` becomes `ui/medicines/form`. The overview FAB navigates to `MedicationFormGraph()`; a tile navigates to `MedicationFormGraph(medicationId = id.value)`. The view model derives its `mode` from the graph entry's `SavedStateHandle` via `toRoute<MedicationFormGraph>()`.

*Why a second mode rather than a second screen:* the product asked for "very similar to the add medicine screen", and every field, validator, picker and the whole schedule editor would be duplicated otherwise. A mode flag changes three things: the title, whether the draft is loaded before editing, and which repository operation Save calls. Everything else is shared, so a validation fix lands in both places at once.

*Why rename rather than keep "AddMedication" names:* a class called `AddMedicationViewModel` that updates medicines misleads the 2 a.m. reader `CLAUDE.md` cares about. The rename is mechanical, confined to the form package and the routes file, and listed as its own task so the diff stays reviewable. If `app-medicine-add` has not been applied yet when this change is, the names are simply created correct.

*Alternative considered:* a `MedicationDetailsScreen` that renders the medicine read-only with an Edit button leading to the form. Rejected: it adds a screen and a tap for no product reason; the owner asked for an editable details screen.

### D2. Loading and pre-population

In `Edit` mode the view model starts in `isLoading = true`, calls `repository.get(id)` once, builds the draft with `MedicationFormDraft.from(medication)` and keeps a copy as `initialDraft`. In `Add` mode `initialDraft` is the default draft. If a draft is already present in the `SavedStateHandle` (rotation or process death), the load is skipped and both the draft and its `initialDraft` are restored from the handle, so a half-edited form never snaps back to the saved medicine.

`get` is a one-shot suspend call, not a `Flow`. Nothing else edits medication fields while the form is open (single activity; the reminder receivers only write dose rows and the swipe action lives on the overview, which is off screen), so observing would only add the question of what to do when the source changes under the user's fingers. The form owns the draft from load to save.

If `get` returns null the view model emits a one-shot `OpenFailed` effect, the screen pops back to Medicines and a snackbar "Could not open medicine" is shown. Because medicines are never removed this can only be a defect, so the failure is also logged at debug level with the id (never the name).

### D3. Repository contract

```kotlin
interface MedicationRepository {
    fun observeAll(): Flow<List<Medication>>
    /** One medication with its schedules, or null when no medication has [id]. */
    suspend fun get(id: MedicationId): Medication?
    suspend fun add(medication: NewMedication): MedicationId
    /** Replaces every field and every schedule of the medication with [medication]'s id, atomically. Throws when the id is unknown. */
    suspend fun update(medication: Medication)
    suspend fun setActive(id: MedicationId, isActive: Boolean)
    // There is deliberately no remove(). Medicines are never deleted; deactivate instead.
}
```

*Why `update(Medication)` and not `update(id, NewMedication)`:* `Medication` already carries the id and `isActive`, both of which the details screen needs to write, and its `init` block validates the whole object, so an invalid medicine cannot reach the database. `NewMedication` stays as the shape for unsaved medicines only.

*Why throw on an unknown id where `setActive` is a no-op:* `setActive` fires from a tile that came from the live stream and a lost race is harmless. `update` writes a form the user has spent time on; silently dropping it would violate "confirm in one tap, keep the data". The existing "Could not save" path handles the exception. In practice the id is always known because rows are never deleted.

*Why no `remove` and a comment saying so:* the KDoc is the first thing a future author reads. The spec, a guard test (D5) and this design say the same thing in three places on purpose.

### D4. Room update: replace fields, replace schedules, in one transaction

```kotlin
@Dao interface MedicationDao {
    @Transaction @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getWithSchedules(id: Long): MedicationWithSchedules?
    @Update suspend fun updateMedication(medication: MedicationEntity): Int
    @Query("DELETE FROM schedules WHERE medication_id = :medicationId")
    suspend fun deleteSchedulesFor(medicationId: Long)
    @Transaction
    suspend fun update(medication: MedicationEntity, schedules: List<ScheduleEntity>) {
        check(updateMedication(medication) == 1) { "No medication row with id ${medication.id}" }
        deleteSchedulesFor(medication.id)
        insertSchedules(schedules)          // existing, with position and medication_id set
    }
}
```

*Why delete-and-reinsert schedules rather than diff:* schedule rows have no external references. `doses.medication_id` points at `medications`, never at `schedules`, so replacing schedule rows cannot orphan or alter a dose. A diff by position would need identity for schedules that the domain model does not have (a `Schedule` is a value) and would produce the same table state with more code. The whole thing is one Room transaction, so a collector sees exactly one emission with the finished state, and a failed schedule insert rolls back the field update too.

*Why no schema change:* every column already exists. The database stays at its current version and the exported schema set is unchanged; a DAO test asserts the version number.

`RoomMedicationRepository.update` maps `Medication` to the entities, calls `dao.update`, and lets Room's invalidation tracker do the rest: the overview re-partitions, and `ReminderCoordinator` (from `app-medicine-alarm`, when applied) runs `RefreshPlannedDoses`, which is the only code that touches doses. `InMemoryMedicationRepository.update` replaces the element with the matching id and re-emits, or throws when absent.

### D5. No delete path, guarded

Nothing in production code deletes a `medications` row: no `@Delete` on `MedicationEntity`, no `DELETE FROM medications` query, no repository method. The `Cascade delete` scenario that `app-medicine-add` wrote for the schema is exercised in the instrumented test through `SupportSQLiteDatabase.execSQL` on the test database, not through a DAO method. If `app-medicine-add` was applied with a DAO delete for that test, this change moves the test to raw SQL and removes the method.

A small reflection test, `MedicationDaoContractTest`, walks the declared methods of `MedicationDao` and fails if any carries `@Delete` or a `@Query` whose SQL contains `DELETE FROM medications`. It runs as a plain unit test (annotations are on the interface, no Room runtime needed), so a future change that adds a delete fails the build before it reaches a device. The test's message quotes the product rule.

*Why a test and not just a code review note:* review notes fade; the CLAUDE.md line "a medicine can never be removed" will be read by future assistants, but a red test is read by everyone.

### D6. Tile tap, and how it coexists with the swipe

`MedicineTile` gains `onClick: () -> Unit`. `SwipeRevealTile` places `Modifier.clickable` on the foreground content together with the existing `anchoredDraggable`. Compose's pointer system already separates a tap from a horizontal drag through touch slop, so no custom gesture code is required. The tap handler branches on the tile's reveal state: when `isRevealed` is true the tile closes (`onRevealChange(false)`) and nothing navigates; when closed, `onClick` fires and the screen navigates to `MedicationFormGraph(medicationId)`. Navigation also clears `revealedId` so no tile is left open behind the form.

Semantics: the tile's merged node gains `onClick` with the label "Open medicine details" (string resource). The custom accessibility action for Activate/Deactivate from `app-medicine-deprecate` stays in the actions menu. A screen-reader double tap therefore opens details, which is the expected default action for a list item, while the activation action remains one menu away.

*Why close-on-tap rather than navigate-on-tap for a revealed tile:* a finger that just swiped is resting on the tile; navigating from there is a surprise, and closing is what every mail and messaging app does. It also gives a fourth way to close a tile that is easier than dragging.

### D7. The active switch

In `Edit` mode the form shows, above the schedules section, a `ListItem` with headline "Active" in `titleSmall`, supporting text "Inactive medicines are not reminded" in `bodyMedium` `onSurfaceVariant`, and a `Switch` bound to `draft.isActive`, with a `Sizes.minTouchTarget` row height. It is absent in `Add` mode because new medicines are always active (add spec). The value is written as part of `update`, so a deactivation from the details screen and one from the swipe both end in the same repository stream emission, and the reminder refresh treats them identically. The switch has a spoken label and the standard toggleable semantics. Deactivating is reversible, so the row uses no `error` colour.

*Why include it:* the request says the user can change "everything", and the active flag is the one remaining field on `Medication`. Leaving it out would make the details screen the only place where a medicine cannot be stopped.

### D8. Touched detection and Save

`isTouched = draft != initialDraft`. Back with `isTouched` shows the existing `DiscardDialog`; otherwise it pops. Save is enabled whenever the draft is valid, in both modes, as the add spec states; saving an unchanged medicine performs the update anyway and the refresh finds nothing to change. Disabling Save for an untouched form is listed as an open question rather than done, because it introduces a rule the add form does not have.

On Save in `Edit` mode the view model builds `Medication(id, ...)` from the draft, calls `repository.update`, and emits the existing `Saved` effect; `PillsnerApp` pops `MedicationFormGraph` and Medicines shows the updated tile from the stream. Failures surface as the existing "Could not save" snackbar and are logged at debug level with the exception type only.

### D9. History survives every edit, without new code here

- **Rename or dose change**: dose rows snapshot name and amount at generation. Taken, skipped, missed and already-reminded doses keep the old text for ever. Planned, un-reminded doses are deleted and regenerated with the new values by `RefreshPlannedDoses` on the next emission.
- **Schedule or date change**: the same refresh removes planned doses that no longer match and inserts the new ones. A dose already on the notification shade is left alone, exactly as `app-medicine-deprecate` D6 describes for deactivation.
- **Active switch**: identical to the swipe path.

This change adds nothing to the scheduling layer. The `medicine-details` spec states the observable outcome (history unchanged, future reminders follow the edit) and the tasks test it at the repository level (dose rows untouched by the update statement) plus end to end once `app-medicine-alarm` is present.

### D10. Draft persistence

`DraftSaver` gains `medicationId: Long?`, `isActive: Boolean`, and stores the `initialDraft` alongside the working draft under a second key. Both are small string maps, so doubling them costs nothing measurable and makes restore deterministic without a second repository read. The unit test saves and restores an edit-mode draft with a changed schedule and asserts `isTouched` is still true afterwards.

### D11. Strings

New resources: `medication_form_title_details` ("Medicine details"), `medication_form_active` ("Active"), `medication_form_active_supporting` ("Inactive medicines are not reminded"), `medication_form_open_failed` ("Could not open medicine"), `medicines_tile_open_details` ("Open medicine details"). The add title resource is renamed to `medication_form_title_add`. No medicine name is interpolated into any new string.

### D12. Package layout

```
domain/repository/MedicationRepository.kt       + get(), + update(), KDoc stating no removal
domain/validation/MedicationFormValidator.kt     renamed from AddMedicationValidator
data/db/MedicationDao.kt                         + getWithSchedules(), updateMedication(), deleteSchedulesFor(), update()
data/RoomMedicationRepository.kt                 + get(), + update()
data/InMemoryMedicationRepository.kt             + get(), + update()
ui/navigation/Routes.kt                          MedicationFormGraph(medicationId), MedicationForm
ui/navigation/PillsnerApp.kt                     graph takes the id; tile navigation; clear revealedId
ui/medicines/MedicineTile.kt                     + onClick, onClick semantics label
ui/medicines/SwipeRevealTile.kt                  + clickable on foreground, close-on-tap when revealed
ui/medicines/MedicinesScreen.kt                  + onOpenMedication callback
ui/medicines/form/MedicationFormScreen.kt        renamed; title per mode, active switch, loading state
ui/medicines/form/MedicationFormViewModel.kt     renamed; mode, load, update, OpenFailed effect
ui/medicines/form/MedicationFormUiState.kt       renamed; + isLoading, + isActive, + mode
ui/medicines/form/MedicationFormDraft.kt         renamed; + medicationId, + isActive, from(Medication)
ui/medicines/form/DraftSaver.kt                  + initial draft, + new fields
ui/medicines/form/ActiveSwitchRow.kt             new
test/.../MedicationDaoContractTest.kt            new guard test
```

## Risks / Trade-offs

- [Four unarchived changes with chained MODIFIED deltas: add, alarm, deprecate, then this] → Archive order is stated in the proposal and repeated in the tasks. If it cannot be honoured, the MODIFIED blocks are rewritten as ADDED with distinct names before archiving.
- [Renaming the add form's classes after `app-medicine-add` is applied] → Mechanical rename, isolated in one task, no behaviour change in that task. If add is not yet applied, the names are created correct from the start.
- [Tap and horizontal drag on the same node] → Compose's touch-slop disambiguation handles it; the Compose test performs a tap, a short drag and a long drag on the same tile and asserts navigate, snap-back and reveal respectively.
- [Last write wins if something else edits a medication while the form is open] → Nothing else writes medication fields while the form is on screen (single activity, receivers only write doses). Documented as a non-goal; revisit if a widget or wearable input ever writes medications.
- [A rename changes the name on planned, un-reminded doses but not on reminded ones] → Intended: history is a fact, plans are not. The Home screen may briefly show the old name on an overdue dose next to the new name on the next one. Accepted; the alternative rewrites history.
- [Delete-and-reinsert schedules loses nothing today but would break a future table that references `schedules`] → Any such table must be introduced through a proposal, which will have to revisit D4. The KDoc on `MedicationDao.update` says so.
- [Reflection guard test can be deleted by the same author who adds a delete] → It cannot stop a determined change; it stops an accidental one, which is the realistic risk. The product rule also lives in the spec and in `CLAUDE.md`.
- [Details screen at large font gains one more row] → The form already scrolls; the manual test at maximum font scale covers the switch row.

## Migration Plan

Additive. No schema change, no data migration; the database version and exported schemas are unchanged, which a test asserts. Rollback is removing `get`, `update` and their DAO statements, the tile tap, the active switch and the mode handling, and pointing the FAB back at a parameterless graph. Data written by this change is an ordinary medication row with ordinary schedule rows and is valid for every earlier version of the code.

## Open Questions

- Should Save be disabled when nothing changed in edit mode? Recommended: no for now, to keep one rule for both modes; revisit after a usability pass.
- Should a notification currently showing for a medicine be re-posted with the new name or amount after an edit? Same owner and same recommendation as the deprecate change's question: `app-medicine-alarm`'s `ReminderNotifier`.
- Should the details screen show a read-only summary of recent intakes below the form? Recommended: a separate history screen in its own change, reached from the details screen.
- Should the README's feature line say "pause" or "deactivate"? Recommended: use the glossary word once the deprecate change is archived.
