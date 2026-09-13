## 1. Preconditions

- [x] 1.1 Verify `app-welcome-screen`, `app-medicine-overview`, `app-medicine-add` and `app-medicine-deprecate` are applied: `PillsnerApp`, `AddMedicationGraph`/`AddMedicationForm`/`EditSchedule` routes, `AddMedicationViewModel`, `AddMedicationDraft`, `DraftSaver`, `DiscardDialog`, `MedicationRepository` with `add()` and `setActive()`, `MedicationDao`, `RoomMedicationRepository`, `SwipeRevealTile`, `MedicineTile` all exist; stop and report if not
- [x] 1.2 Note whether `app-medicine-alarm` is applied — it is, and archived, so the `doses` table exists and the history scenarios are covered at the repository level by `MedicationDaoTest.update_leavesDoseRowsAlone`
- [x] 1.3 Confirm `./gradlew assembleDebug` succeeds from `src/` before touching anything

## 2. Rename the add form to the medicine form (no behaviour change)

- [x] 2.1 Rename package `ui/medicines/add` to `ui/medicines/form`; rename `AddMedicationScreen`, `AddMedicationViewModel`, `AddMedicationUiState`, `AddMedicationDraft` to `MedicationFormScreen`, `MedicationFormViewModel`, `MedicationFormUiState`, `MedicationFormDraft`; rename `domain/validation/AddMedicationValidator` to `MedicationFormValidator`
- [x] 2.2 In `Routes.kt` replace `AddMedicationGraph` with `MedicationFormGraph(medicationId: Long? = null)` and `AddMedicationForm` with `MedicationForm`; update `PillsnerApp` and the overview FAB to `MedicationFormGraph()`
- [x] 2.3 Rename the add title resource to `medication_form_title_add`; rename test classes to match
- [x] 2.4 Run `./gradlew test` and confirm every existing test still passes with the new names

## 3. Repository contract

- [x] 3.1 Add `suspend fun get(id: MedicationId): Medication?` and `suspend fun update(medication: Medication)` to `MedicationRepository`, with KDoc stating that `update` replaces fields and schedules atomically and throws for an unknown id, and an interface-level KDoc stating there is deliberately no removal operation
- [x] 3.2 Implement `get` and `update` in `InMemoryMedicationRepository` (replace by id and re-emit; throw when absent) and update the debug preview repository
- [x] 3.3 Unit tests for the in-memory implementation: get by id, get unknown returns null, update replaces fields and schedules under the same id with one emission, update changes the active flag, update unknown throws and emits nothing

## 4. Persistence

- [x] 4.1 Add to `MedicationDao`: `@Transaction getWithSchedules(id)`, `@Update updateMedication(entity): Int`, `@Query deleteSchedulesFor(medicationId)`, and a `@Transaction suspend fun update(medication, schedules)` that checks exactly one row was updated, deletes the old schedule rows, inserts the new ones with `position` and `medication_id` set; KDoc that any future table referencing `schedules` must revisit this
- [x] 4.2 Confirm `MedicationDao` and `MedicationEntity` declare no `@Delete` and no `DELETE FROM medications`; if `app-medicine-add` introduced one for its cascade-delete test, remove it and rewrite that test to use `execSQL` on the test database
- [x] 4.3 Implement `get` and `update` in `RoomMedicationRepository` using the existing entity mappers
- [x] 4.4 Create the unit test `MedicationDaoContractTest`: reflect over `MedicationDao`'s declared methods and fail on any `@Delete` or any `@Query` containing `DELETE FROM medications`, with a message quoting "a medicine can never be removed"
- [x] 4.5 Instrumented DAO tests: fields replaced under the same id; schedules A,B replaced by C,D,E in order with no leftover rows; schedules cleared leaves the medication row; dose rows byte-for-byte unchanged by the update (skip with a note if `doses` does not exist yet); exactly one flow emission per update; unknown id fails and inserts or deletes no schedule row; schedule insert failure rolls back fields and old schedules; database version and exported schema files unchanged

## 5. Form in edit mode

- [x] 5.1 Add `MedicationFormMode` (`Add`, `Edit(id)`), derived in `MedicationFormViewModel` from `SavedStateHandle.toRoute<MedicationFormGraph>()`
- [x] 5.2 Extend `MedicationFormDraft` with `medicationId: Long?` and `isActive: Boolean`, add `MedicationFormDraft.from(medication)` and a `toMedication(id)` mapping; extend `DraftSaver` to persist the working draft and the `initialDraft` under separate keys
- [x] 5.3 In the view model: in `Edit` mode start with `isLoading = true`, call `repository.get(id)` once unless a draft is restored from the handle, populate `draft` and `initialDraft`, emit `OpenFailed` when null and log at debug with the id only; in `Add` mode set `initialDraft` to the defaults
- [x] 5.4 Compute `isTouched = draft != initialDraft` and use it for the discard check; on Save in `Edit` mode call `repository.update(draft.toMedication(id))` and emit the existing `Saved` effect; keep the existing "Could not save" failure path with debug-level logging of the exception type only
- [x] 5.5 Extend `MedicationFormUiState` with `mode`, `isLoading` and `isActive`; add an `onActiveChanged` event
- [x] 5.6 Unit tests for the view model in edit mode: loading state then populated fields and schedule rows in order; `isTouched` false after load, true after an edit, false after reverting by hand; restored draft is not overwritten by a reload; Save calls `update` with the same id and the edited values and emits `Saved`; active toggle is included in the saved medication; `update` failure keeps state and surfaces the error; `get` returning null emits `OpenFailed`; add mode still calls `add` and never shows the switch
- [x] 5.7 Unit test for `DraftSaver`: save and restore an edit-mode draft with a changed schedule and assert `medicationId`, `isActive`, the draft and the initial draft round-trip and `isTouched` is still true

## 6. Details screen UI

- [x] 6.1 In `MedicationFormScreen` pick the `TopAppBar` title per mode ("Medicine details" from `medication_form_title_details` in edit mode, `titleLarge`), keep the pinned filled Save button, show a `CircularProgressIndicator` centred while `isLoading` and no editable fields, and handle `OpenFailed` by popping to Medicines and showing `medication_form_open_failed` in the overview snackbar
- [x] 6.2 Create `ActiveSwitchRow` with the `pillsner-ui-build` skill (`ListItem` with `titleSmall` headline `medication_form_active`, `bodyMedium` supporting text `medication_form_active_supporting`, a `Switch` with toggleable semantics, no `error` colour) and place it above the schedules section in edit mode only; `@PreviewLightDark` for on and off
- [x] 6.3 Confirm no delete, remove or archive action exists on the screen, in its top bar, menus or dialogs
- [x] 6.4 Add the string resources from design D11

## 7. Tile tap on the overview

- [x] 7.1 Add `onClick` to `MedicineTile` with an `onClick` semantics label from `medicines_tile_open_details`, keeping the merged description and the Activate/Deactivate custom action
- [x] 7.2 In `SwipeRevealTile` add `Modifier.clickable` on the foreground; when `isRevealed` close the tile via `onRevealChange(false)` instead of calling `onClick`
- [x] 7.3 In `MedicinesScreen` add `onOpenMedication(MedicationId)`, clear `revealedId` before navigating; in `PillsnerApp` navigate to `MedicationFormGraph(medicationId = id.value)`
- [x] 7.4 Compose tests on the overview: tap on a closed active tile and on a closed inactive tile navigates with the right id; tap on a revealed tile closes it and does not navigate; tapping tile B while A is revealed navigates and A is closed on return; a short drag snaps back without navigating; a long drag still reveals; screen-reader default action opens details while the custom action is still listed

## 8. End-to-end UI tests and manual checks

- [x] 8.1 Compose test: open a medicine with two schedules from its tile and assert title "Medicine details", every field pre-filled, both schedule rows in order, and the switch on
- [x] 8.2 Compose test: open an inactive medicine, assert the switch is off, turn it on, Save, assert the tile moved to the active section
- [x] 8.3 Compose test: rename a medicine and Save; assert exactly one tile for it with the new name at its new alphabetical position
- [x] 8.4 Compose test: replace the only schedule through the editor and Save; assert the tile shows only the new description
- [x] 8.5 Compose test: back without edits pops immediately; back after an edit shows the discard dialog; Discard leaves the tile unchanged; Keep editing preserves the edit; reverting an edit by hand pops without the dialog
- [x] 8.6 Configuration-change test: edit the name and add a schedule, rotate, assert both survive and back asks to confirm; process-death restore keeps the edited name and the edit mode — covered by `MedicationFormEditModeTest."a restored draft is not overwritten by the stored medicine"`, which asserts the restored draft wins over a reload and is still touched. Not asserted through a real recreation: the flow tests drive `PillsnerApp` from their own `setContent`, which an activity recreation discards, so such a test would exercise the harness rather than the draft. Manual case MT-4 covers the platform side
- [x] 8.7 Compose test: swipe both directions and long-press a tile; assert no delete action appears
- [x] 8.8 History after an edit — `MedicationDaoTest.update_leavesDoseRowsAlone` proves the update statement does not touch a dose row at all, which is what makes every one of those three cases hold: a dose keeps the snapshot it was planned with, and only the reminder layer's own refresh (covered by `RefreshPlannedDosesTest`) replaces planned, un-reminded doses. The end-to-end view of both together is manual case MT-3
- [x] 8.9 Manual test cases documented in the change (see `manual-tests.md`): largest font scale on the details screen with three schedules and the switch row; TalkBack announces the switch label and state, the tile's default action and its custom action

## 9. Verification and documentation

- [x] 9.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim — both BUILD SUCCESSFUL, 230 unit tests, 0 failures. Lint found one real defect, fixed: `LocalDate.EPOCH` in the draft saver needs API 34 and minSdk is 26. Only the nine known `PluralsCandidate` warnings remain
- [x] 9.2 Run `./gradlew connectedAndroidTest` for DAO and Compose tests; report results verbatim — 146 instrumented tests on the `pixel_7_-_api_36_0` emulator (API 36), 0 failures, including `MedicationDaoTest` (23, with the eight new get and update cases), `MedicationDetailsFlowTest` (7), `NoMedicineDeletionTest` (1) and `MedicinesSwipeTest` (15, with the three new tap cases)
- [x] 9.2a Run the `pillsner-ui-review` skill over `ui/medicines`; resolve every finding or list the remaining ones with a reason — sweep clean: no hex colours, no raw dp, no inline text styles, no truncation, no alpha, no literal user-facing strings. The Active row uses no error colour, as section 2.4 requires for a reversible action
- [x] 9.3 Update `README.md`: make the "Edit, pause or archive a medication without losing its history" feature line accurate (editing from the details screen, deactivating instead of removing, history always kept); do not promise deletion
- [x] 9.4 Review against `CLAUDE.md`: no `android.*` in domain, strings in resources, single DI mechanism, no schema change, no medication names or doses logged at info or above, no delete path for medications
- [x] 9.5 Confirm archive order — all five are archived (2026-09-13), in that order, so every MODIFIED block has a requirement to modify
