## 1. Preparation

- [ ] 1.1 Verify the scaffold and the current form: `MedicationFormScreen.kt`, `MedicationFormUiState.kt`, `MedicationFormViewModel.kt` and `MedicationFormDraft.kt` exist under `src/app/src/main/java/nl/hexmaster/pillsner/ui/medicines/form/`; stop if not
- [ ] 1.2 Add `ic_expand_more.xml` to `src/app/src/main/res/drawable/` in the same Material Symbols Rounded style as `ic_chevron_right.xml`
- [ ] 1.3 Add string resources `medicine_details_more` ("More details"), `medicine_details_less` ("Less details"), `medicine_details_expanded` and `medicine_details_collapsed` (state descriptions) to `values/strings.xml` and translate them in `values-nl`, `values-de`, `values-fr`, `values-es` and `values-pt`

## 2. State and view model

- [ ] 2.1 Add `secondaryDetailsExpanded: Boolean = false` and a derived `showsSecondaryDetailsToggle` (true in edit mode only) to `MedicationFormUiState`
- [ ] 2.2 Add `onSecondaryDetailsToggled()` to `MedicationFormViewModel`, persisting the flag in the `SavedStateHandle` under its own key and restoring it on construction, without touching `MedicationFormDraft` or `hasEdits`
- [ ] 2.3 In `save()`, when `showErrors` is set and `canSave` is false, set `secondaryDetailsExpanded = true` if any secondary-field error (currently `useUntilError`) is non-null
- [ ] 2.4 Unit tests in `MedicationFormViewModelTest`: collapsed by default in edit mode; toggle flips the flag; toggling leaves `hasEdits` false; save with a use-until error expands the panel; save with only a name error does not; the flag is restored from a saved state handle

## 3. Screen

- [ ] 3.1 Through the `pillsner-designer` agent (or the `pillsner-ui-build` skill), regroup the field column in `MedicationFormScreen`: name and dose first; in edit mode a full-width `TextButton` toggle with a rotating `ic_expand_more` trailing icon and an `AnimatedVisibility` panel holding used since, use until, prescribed by and `ActiveSwitchRow`; in add mode the four fields inline as today; then the Schedules and Stock sections unchanged
- [ ] 3.2 Give the toggle `Modifier.heightIn(min = Sizes.minTouchTarget)`, a `labelLarge` label from the new strings, a `stateDescription` (expanded/collapsed) from the new strings, and the test tag `MedicationFormTestTags.SECONDARY_DETAILS_TOGGLE`; give the panel `MedicationFormTestTags.SECONDARY_DETAILS_PANEL`
- [ ] 3.3 Use the medium motion duration token for expand/shrink and confirm Compose honours the system animator scale
- [ ] 3.4 Update the details preview to show the collapsed state and add an expanded-state preview; keep the add-form preview unchanged
- [ ] 3.5 Run `pillsner-ui-review` on the changed screen and fix every finding

## 4. Instrumented tests

- [ ] 4.1 In `MedicationFormFlowTest`, make the edit-mode assertions on used since, use until and prescribed by tap the toggle first, and add a case asserting those fields are absent before the tap and the toggle reads "More details"
- [ ] 4.2 In `MedicationDetailsFlowTest`, tap the toggle before asserting on or clicking the active switch, and add cases for: collapse hides the fields again; toggling then pressing back shows no discard dialog; a use-until error on Save expands the panel; expand, edit, collapse, save keeps the edit
- [ ] 4.3 Add a configuration-change case that the expanded state survives recreation
- [ ] 4.4 Confirm the add-mode assertions still pass without changes

## 5. Verification

- [ ] 5.1 From `src`, run the unit test task and the lint task (including the translation completeness check) and record the results here
- [ ] 5.2 Run the instrumented test task for the medicine form tests and record the results here
- [ ] 5.3 Manual accessibility check with TalkBack at 200 % font scale: toggle announces label and state, newly shown fields follow it in traversal, Save reachable in both states; record the outcome here
