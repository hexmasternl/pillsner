**GitHub Issue:** #52 (https://github.com/hexmasternl/pillsner/issues/52)

## Why

A medicine can become unusable in two ways Pillsner does not yet track: the pack physically runs out before the next refill, or it passes its expiry date while stock still remains. Today the app has no concept of "how much is left" at all, so the person taking the medicine has to remember to check the box and the label themselves.

This proposal replaces an earlier, narrower draft of this change that added a single, informational expiry date per medicine with no stock tracking. That draft explicitly scoped stock tracking out as "a separate future proposal." Working through the request with the user surfaced that expiry and remaining stock are not separable here: new pack of the same medicine almost always carries a different expiry date than what is already open, and the thing worth warning about is stock running low *and* stock being used past, or close to, its expiry. This version supersedes that draft and treats stock — as one or more batches, each with its own expiry date — as the foundation both features sit on.

## What Changes

- Add **stock batches** to a medicine: each batch records a quantity (an amount and unit, e.g. "30 tablets" or "100 ml") and an expiry date. A medicine can hold any number of batches at once (e.g. a half-used older pack plus a fresh one).
- When a dose is recorded as **taken** — from the dose detail screen, the reminder notification, or a wearable, whichever surface recorded it — the app deducts the dose's amount from stock, always drawing from the batch that expires soonest first (first-expiry-first-out), spilling into the next batch if the first is exhausted.
- After every such deduction, the app projects the medicine's usage for the coming week from its schedules and compares it to what remains. If remaining stock would not cover a week, the user sees a **low-stock warning** with two responses: "OK" and "I ordered new". "OK" leaves the warning active, so it appears again the next time the medicine is taken while stock is still short. "I ordered new" suppresses it until the user adds a new batch of stock, which always clears the suppression.
- Independently, if the batch a dose was just drawn from is approaching (within 30 days) or past its expiry, the user sees an **expiry-at-use warning**, every time it applies, dismissed with a simple acknowledgement — this is never suppressed the way the low-stock warning can be, since only using up or discarding that batch changes the fact.
- The Medicines screen tile shows a live, passive heads-up (low stock and/or nearest-batch expiry) for any medicine that has at least one batch of stock recorded, recomputed from the medicine's current stock and schedules whenever the screen is shown — not only right after a dose is taken.
- The Medicine details screen gains a **Stock** section: the medicine's batches, ordered by expiry date, each showing its remaining amount and expiry date, with an "Add stock" action to record a new batch.
- **The entire feature is inert for a medicine with no stock recorded**: no consumption, no projection, no warning and no tile heads-up. It only switches on once the user adds a first batch, and it has no effect on reminders, scheduling or dose generation regardless.

## Capabilities

### New Capabilities
- `medicine-stock-tracking`: stock batches, first-expiry-first-out consumption tied to a taken dose, the weekly-sufficiency low-stock warning and its "I ordered new" suppression, the expiry-at-use warning, and the Add stock form.

### Modified Capabilities
- `medicine-details`: the details screen gains a Stock section listing batches and an "Add stock" action, plus an inline note of the medicine's current low-stock / expiry-at-use state.
- `medicine-overview`: medicine tiles gain a live stock heads-up (low stock and/or nearest-batch expiry), shown only for a medicine with at least one stock batch, visually and textually distinct from the existing inactive-tile treatment.
- `medication-persistence`: adds a `stock_batches` table and a nullable low-stock-acknowledgement column to `medications`, shipped with a migration and a migration test.

## Impact

- **Data**: new `stock_batches` table (medication reference, remaining quantity, expiry date, added-at moment) and a nullable acknowledgement column on `medications`, via a Room schema migration with its migration test.
- **UI**: Medicine details gains a Stock section and an Add stock form; medicine tiles gain a heads-up indicator; a warning dialog appears after a taken dose when it applies. No new top-level screens. Goes through the `pillsner-designer` agent / `pillsner-ui-build` + `pillsner-ui-review` skills per CLAUDE.md.
- **Domain**: pure, Android-free functions for first-expiry-first-out consumption, weekly usage projection (reusing the existing dose generator), stock sufficiency, and per-batch expiry classification — unit-testable, covering date-boundary edge cases per CLAUDE.md's testing expectations.
- **Reminders/scheduling**: untouched. Stock state is read only when a dose is recorded taken or when a screen renders; it never affects whether or when a dose is generated or reminded.
- No new dependency, no network access.
