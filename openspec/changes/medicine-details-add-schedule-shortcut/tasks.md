## 1. UI: overflow menu

- [ ] 1.1 Add `OVERFLOW_ADD_SCHEDULE` to `MedicationFormTestTags` in `MedicationFormScreen.kt`, alongside the existing `USAGE_HISTORY` tag
- [ ] 1.2 In `OverflowMenu`, add an `onAddSchedule: () -> Unit` parameter and a new `DropdownMenuItem` labelled with `R.string.medicine_add_schedule`, wired to it, placed above the existing "Usage history" item
- [ ] 1.3 Add a `HorizontalDivider` between the new "Add schedule" item and "Usage history"
- [ ] 1.4 Pass `onAddSchedule` from `MedicationFormScreen` into `OverflowMenu` (reusing the same lambda already passed to the bottom "Add schedule" button)
- [ ] 1.5 Update the `OverflowMenu` doc comment: it no longer holds "exactly one item"; describe the two-item-plus-divider menu and that no destructive action will ever join it

## 2. Strings and previews

- [ ] 2.1 Confirm `R.string.medicine_add_schedule` reads naturally as a menu item label (no change expected; add a new resource only if the existing one reads awkwardly in menu context)
- [ ] 2.2 Update `MedicationFormDetailsPreview` (and any other preview showing the overflow menu open) if needed to reflect the new menu contents

## 3. Spec sync

- [ ] 3.1 Confirm the delta spec at `specs/medicine-details/spec.md` matches the implemented menu order and behaviour exactly

## 4. Tests

- [ ] 4.1 Update `MedicineHistoryMenuTest.kt` and any other test asserting the overflow menu holds exactly one item, to assert the new two-item-plus-divider contents and order instead
- [ ] 4.2 Confirm `MedicineHistoryNavigationTest.kt` still passes unchanged (usage history navigation itself is untouched)
- [ ] 4.3 Confirm `NoMedicineDeletionTest.kt` still passes unchanged (no destructive action introduced)
- [ ] 4.4 Add an instrumented test that tapping "Add schedule" in the overflow menu opens the schedule editor and, on Done, adds the schedule to the list — same outcome as the bottom button

## 5. Verification

- [ ] 5.1 Run `pillsner-ui-review` against `MedicationFormScreen.kt`
- [ ] 5.2 Run unit tests, lint and the instrumented tests from the `src` Gradle project root
- [ ] 5.3 Manually verify the overflow menu with TalkBack and the largest system font scale
