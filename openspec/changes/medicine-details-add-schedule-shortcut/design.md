## Context

The Medicine details screen (`MedicationFormScreen.kt`) is the Add medicine form in edit mode. Its top bar shows an overflow action only in edit mode, opening a `DropdownMenu` (`OverflowMenu` composable) that today holds one `DropdownMenuItem`, "Usage history", wired to the `onOpenUsageHistory` callback. Further down the same form, a full-width `FilledTonalButton` labelled "Add schedule" is already wired to the `onAddSchedule` callback and opens the schedule editor. Both callbacks already exist on `MedicationFormScreen`'s public signature and are supplied by the view model layer; this change adds no new callback, no new screen and no new navigation destination.

## Goals / Non-Goals

**Goals:**
- Add an "Add schedule" shortcut to the overflow menu, above a divider, above "Usage history".
- Reuse the existing `onAddSchedule` callback and `medicine_add_schedule` string resource — no duplicated logic or strings.
- Update the `medicine-details` spec and the `OverflowMenu` composable's doc comment, both of which currently assert the menu holds exactly one item.

**Non-Goals:**
- No "Add stock" item — no stock concept exists yet (tracked by #52; out of scope per the proposal).
- No change to what "Usage history" does, or to the add-schedule flow itself (the schedule editor, its fields, its validation).
- No change to the overflow action's visibility rule (still edit-mode only).

## Decisions

- **Reuse, don't duplicate**: the new menu item calls the same `onAddSchedule` lambda the bottom button already calls, and reuses `R.string.medicine_add_schedule` for its label. This keeps the two entry points behaviourally identical by construction — there's only one code path to add a schedule, just two ways to reach it.
- **Divider placement**: a single `HorizontalDivider` sits between "Add schedule" and "Usage history", so the menu reads as two groups — a shortcut to an action already on the form, then a navigation item. Order top-to-bottom: Add schedule, divider, Usage history, matching issue #57's requested order.
- **New test tag**: `MedicationFormTestTags` gains `OVERFLOW_ADD_SCHEDULE` alongside the existing `USAGE_HISTORY` tag, following the same naming pattern, so semantics tests can target the new item without colliding with the bottom button's existing `ADD_SCHEDULE` tag.
- **Spec update, not a new capability**: this is a requirement-level change to `medicine-details`'s existing "Details screen title and actions" requirement (item count, item list and order), not a new capability — no new `specs/<name>/` directory.

## Risks / Trade-offs

- [Two entry points to the same action could confuse a screen reader user about which one to use] → Both are announced with their own distinct label ("Add schedule" menu item vs. the bottom button), matching how "Usage history" is already the menu's only distinguishing label; no new ambiguity is introduced beyond what a standard overflow-menu-plus-primary-button pattern already has.
- [Growing the menu changes an existing, tested invariant ("exactly one item")] → The affected tests and the composable doc comment are updated in the same change so the invariant they assert matches the new menu contents; no test is left describing stale behaviour.

## Migration Plan

1. Update `MedicationFormScreen.kt`'s `OverflowMenu` composable: add the new `DropdownMenuItem` and `HorizontalDivider`, add the new test tag.
2. Update `openspec/specs/medicine-details/spec.md` via this change's delta spec.
3. Update existing instrumented/semantics tests referencing the overflow menu's single-item assumption.
4. Run `pillsner-ui-review`, unit tests and lint before considering the change done.

No data migration, no rollback concerns beyond a standard UI change.

## Open Questions

None — the follow-up "Add stock" item is intentionally deferred to a separate change once issue #52 lands.
