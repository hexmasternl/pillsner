**GitHub Issue:** #57 (https://github.com/hexmasternl/pillsner/issues/57)

## Why

Adding another schedule to a medicine is common enough that it deserves a shortcut from the top of the Medicine details screen, not just the "Add schedule" button at the bottom of the form. Today the overflow (three-dot) menu holds a single item, "Usage history"; reaching "Add schedule" means scrolling past every field first. Issue #57 also asks for an "Add stock" shortcut in the same menu, but Pillsner has no stock/remaining-quantity concept anywhere yet (confirmed against `medicine-add`, `medicine-details` and the `medicine-expiry-tracking` change, which explicitly scopes stock tracking out as "a separate future proposal"). That part is tracked separately by issue #52 and is out of scope here; issue #57 is marked blocked-by #52 for that reason. This change delivers only the part that's implementable now.

## What Changes

- Add an **"Add schedule"** item to the Medicine details screen's overflow menu, above a divider, above the existing "Usage history" item. It triggers the exact same action as the existing "Add schedule" button already on the form (opens the schedule editor) — no new behaviour, a second entry point to something the form already does.
- Add a **divider** between "Add schedule" and "Usage history" in the overflow menu.
- **BREAKING** (spec-level, not user-facing breakage): the `medicine-details` requirement stating the overflow menu "holds exactly one item" changes to describe the new two-item-plus-divider menu and its order.
- Out of scope: an "Add stock" menu item. No stock/remaining-quantity concept exists in Pillsner today; adding one is tracked by issue #52. Once that lands, a follow-up change adds "Add stock" to this same menu.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `medicine-details`: the overflow menu's "Details screen title and actions" requirement changes from a single "Usage history" item to "Add schedule", a divider, then "Usage history", with "Add schedule" performing the same action as the form's existing "Add schedule" button.

## Impact

- **UI**: `MedicationFormScreen.kt`'s `OverflowMenu` composable gains a second `DropdownMenuItem` and a `HorizontalDivider`, wired to the existing `onAddSchedule` callback already passed into `MedicationFormScreen`. No new screens, no new callbacks on the public composable signature. Goes through `pillsner-ui-review` per CLAUDE.md.
- **Strings**: reuses the existing `medicine_add_schedule` string resource (already used by the bottom button) for the new menu item's label, rather than adding a duplicate resource.
- **Tests**: existing overflow-menu semantics tests extend to cover the new item and divider; no new screens or navigation destinations to test.
- No data, domain or scheduling changes; no new dependency.
