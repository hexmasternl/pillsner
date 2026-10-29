## Why

**GitHub Issue:** #57 (https://github.com/hexmasternl/pillsner/issues/57)

The Medicine details screen's overflow menu holds exactly one item, "Usage history". The two most common secondary tasks on that screen — adding another schedule and topping up stock — both live far down the form: "Add schedule" sits under the schedules list and "Add stock" under the Stock section that `medicine-expiry-tracking` added. On a medicine with several schedules and a few batches, reaching either means scrolling past the whole form.

## What Changes

- The details screen's overflow menu gains two shortcut items above a divider, with "Usage history" staying below it:
  1. **Add schedule** — opens the same schedule editor as the existing "Add schedule" button further down the form.
  2. **Add stock** — opens the same add-stock form as the "Add stock" button in the Stock section.
  3. *(divider)*
  4. **Usage history** — unchanged.
- Both shortcuts are second entry points to existing behaviour: no new screen, no new state, no new string resource. Like the menu itself they only exist in edit mode, where a saved medicine can hold schedules and stock.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `medicine-details`: the requirement that the overflow menu holds a single item becomes a requirement that it holds these three items in this order, separated by a divider, each opening something the screen already offers.

## Impact

- `src/app/src/main/java/nl/hexmaster/pillsner/ui/medicines/form/MedicationFormScreen.kt` — the `OverflowMenu` composable and two new test tags.
- `src/app/src/androidTest/java/nl/hexmaster/pillsner/ui/medicines/form/MedicineHistoryMenuTest.kt` — covers the new items.
- No domain, data, scheduling or reminder change. No new dependency, no network access, no new string resource.
