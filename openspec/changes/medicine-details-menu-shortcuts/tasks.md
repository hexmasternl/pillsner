## 1. Overflow menu shortcuts

- [x] 1.1 Add `MENU_ADD_SCHEDULE` and `MENU_ADD_STOCK` test tags to `MedicationFormTestTags`
- [x] 1.2 Give `OverflowMenu` `onAddSchedule` and `onAddStock` parameters, and render "Add schedule" (`medicine_add_schedule`) and "Add stock" (`medicine_stock_add`) above a `HorizontalDivider`, with "Usage history" below it, each item closing the menu before invoking its callback
- [x] 1.3 Pass the form's existing `onAddSchedule` and `onAddStockClicked` callbacks into `OverflowMenu` from the top bar, still only in edit mode

## 2. Tests

- [x] 2.1 Extend `MedicineHistoryMenuTest` to assert the menu holds all three items in edit mode
- [x] 2.2 Add tests that the "Add schedule" and "Add stock" items invoke the form's corresponding callbacks
- [x] 2.3 Confirm `NoMedicineDeletionTest` still passes: the menu gains nothing destructive

## 3. Verification

- [ ] 3.1 Run the unit tests and lint from `src/`
- [ ] 3.2 Run the medicine form instrumented tests
- [ ] 3.3 Check the menu against `docs/design-system.md`: theme tokens only, string resources only, minimum touch targets, readable at the largest font scale
