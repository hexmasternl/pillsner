## Context

GitHub issue [#57](https://github.com/hexmasternl/pillsner/issues/57). The details screen is the medicine form in edit mode (`MedicationFormScreen`). Its top bar carries a single overflow action, rendered by the private `OverflowMenu` composable, shown only when `uiState.showsActiveSwitch` is true — that is, only for a saved medicine. The menu holds one `DropdownMenuItem`, "Usage history".

The two actions being shortcut already exist on the same screen and are already wired through the same composable's parameters:

- `onAddSchedule` — the `FilledTonalButton` under the schedules list; navigation opens the schedule editor.
- `onAddStockClicked` — the "Add stock" button in the Stock section added by `medicine-expiry-tracking`; the view model opens the add-stock dialog.

The issue was originally filed as blocked on stock tracking (#52). That groundwork has since landed, so both shortcuts are implementable together.

## Goals / Non-Goals

**Goals:**
- Reach "Add schedule" and "Add stock" from the top bar without scrolling.
- Reuse the existing callbacks and string resources exactly, so the shortcuts cannot drift from the buttons they mirror.

**Non-Goals:**
- Changing what "Usage history", "Add schedule" or "Add stock" do.
- Removing either full-width button from the form: the menu is a shortcut, not a replacement.
- Any menu on the add form. An unsaved medicine has no history and can hold no stock.

## Decisions

- **Reuse the existing strings.** The items read `medicine_add_schedule` ("Add schedule") and `medicine_stock_add` ("Add stock"), the same resources the buttons use, so the shortcut and the button always say the same thing in every locale and no translation work is needed.
- **Pass the screen's own callbacks straight through.** `OverflowMenu` takes `onAddSchedule` and `onAddStock` and invokes the same lambdas the buttons do, after closing the menu. No new view-model event, no duplicated navigation logic.
- **A `HorizontalDivider` separates the two shortcuts from "Usage history"**, because the first two act on the medicine in place while the third navigates away — the Material grouping convention for a menu that mixes the two.
- **Nothing destructive joins the menu.** The `medicine-details` spec forbids a delete, remove or archive action anywhere on this screen, and `NoMedicineDeletionTest` keeps guarding it.

## Risks / Trade-offs

- **Two entry points per action** could look like duplication. Accepted: the buttons stay where they are for discoverability while scrolling the form, and the menu serves the person who already knows what they want.
- **A menu of three items plus a divider** is still comfortably short, so no scrolling or grouping problem arises at the largest font scale.
