**GitHub Issue:** #52 (https://github.com/hexmasternl/pillsner/issues/52)

## Why

A medicine can become unusable while plenty of it still physically remains, simply because the pack has passed its expiry date. Nothing in Pillsner today records or surfaces that date, so the person taking the medicine has no on-device reminder that a pack is about to, or has already, expired — they have to remember and check the label themselves. This is a small, natural extension of the medicine record Pillsner already keeps (name, dose, used since/until, prescriber), not a new domain concept, and it stays entirely on-device.

Note on scope: the original request framed this as "separate from the existing low-stock refill warning," but no stock tracking or refill warning exists in the current specs (`medicine-add`, `medicine-details`, `medicine-overview`) — a medicine currently has no stock/remaining-quantity field at all. This change therefore introduces the expiry date and its heads-up as their own, self-contained feature. If remaining-stock tracking is proposed later, its heads-up should follow the same presentation pattern established here, kept visually and textually distinct so the two are never confused.

## What Changes

- Add an optional **expiry date** field to the medicine record, alongside the existing name, dose, used since/until and prescriber fields, settable and editable on the Add medicine and Medicine details forms.
- Show the expiry date on the Medicine details screen when the medicine has one, using the same date formatting already used for "used since" / "use until".
- Show an **expiry heads-up** on the Medicines screen for a medicine that is approaching or past its expiry date: a distinct visual indicator on that medicine's tile, with wording that makes clear it is about the pack expiring, not about running low.
- Expiry has **no effect on reminders, scheduling or dose generation**. A dose from an expired medicine still comes due, still reminds, and still needs an outcome recorded, exactly as before.
- Editing a medicine's expiry date behaves like editing any other field: it changes what's shown and warned about from that point on, and never alters doses or intake already recorded.

## Capabilities

### New Capabilities
(none — this extends existing medicine capabilities rather than introducing a new domain concept)

### Modified Capabilities
- `medicine-add`: the Add medicine form gains an optional expiry date field, with its own validation (must not be before "used since" when both are set) and default (empty).
- `medicine-details`: the details screen shows and lets the user edit the expiry date, applying the add form's validation rule, and displays the "approaching/past expiry" state inline.
- `medicine-overview`: medicine tiles gain an expiry heads-up indicator (approaching vs. past expiry) that is visually and textually distinct from any other tile state, shown for both active and inactive medicines.

## Impact

- **Data**: adds a nullable `expiryDate` column to the medicine entity in Room, shipped with a migration and a migration test (per CLAUDE.md, every schema change ships with a migration and a migration test).
- **UI**: Add medicine form, Medicine details form, and Medicine tile composables gain one field / one indicator each; no new screens. Goes through the `pillsner-designer` agent / `pillsner-ui-build` + `pillsner-ui-review` skills per CLAUDE.md.
- **Domain**: a small pure function to classify a medicine's expiry state (none / approaching / past) from its expiry date and the current date, unit-testable with no Android dependency, covering the midnight/date-boundary edge cases CLAUDE.md calls out.
- No new dependency, no network access, no change to reminder/alarm behaviour.
