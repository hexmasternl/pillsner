## Why

**GitHub Issue:** #67 (https://github.com/hexmasternl/pillsner/issues/67)
**Pull Request:** #68 (https://github.com/hexmasternl/pillsner/pull/68)

When a dose is smaller than the tablet it's taken from, stock loses only a fraction of a tablet, so the count on screen drifts away from what's actually in the box. For example, a 40 mg dose from a batch of 500 mg tablets deducts 0.08 tablets, so 50 tablets become 49.92 when the user really has 49. A tablet or capsule can't be put back once any part of it has been used, so any part of a pill used should count as the whole pill (issue #67). Because the count is too high, the low-stock warning also fires far too late.

## What Changes

- **BREAKING (behaviour)**: batches counted in tablets or capsules only ever lose whole units. Each batch gives up the dose divided by its strength, **rounded up** to a whole pill. Today the spec asks for the exact fraction (25 mg against 20 mg tablets deducts 1.25), and that scenario is replaced. Batches in continuous units (mg, g, µg, ml) and in counted units that are never split (drop, puff, unit) keep the exact deduction they have today.
- Exact-fit preference: when the batch first-expiry-first-out would draw from first would use only part of a pill, the deduction takes the whole dose from a batch that fits it exactly instead, if there is one. A batch fits exactly when its strength divides the dose evenly and it holds enough to cover it. If several batches fit, the one expiring soonest wins, and a tie goes to the batch added first. If none fits, or if no pill would be broken, first-expiry-first-out with whole-pill rounding applies.
- The weekly usage projection and the low-stock check run the next 7 days of doses through the same deduction rule over a copy of the batches. Rounded-up whole pills then count towards projected usage, so a 40 mg daily dose from 500 mg tablets projects 7 tablets a week, not 280 mg.
- A tablet or capsule batch that already holds a fraction, left over from the old behaviour, is rounded down to a whole number the next time a dose is drawn from it. There is no schema change.
- The Add stock form accepts only a whole number when the chosen unit is tablet or capsule.
- Out of scope: marking a tablet as intentionally splittable, so a half-tablet dose deducts half. Each half-tablet dose now deducts a whole tablet, so the stock count errs towards warning early. That option can be a separate change if it's needed.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `medicine-stock-tracking`: the "Stock unit conversion (strength)" and "First-expiry-first-out consumption" requirements change to whole-pill rounding and exact-fit batch preference. "Weekly usage projection" and "Low-stock warning and its acknowledgement" change to project through the same deduction. "Add stock form" changes to require whole numbers for tablet and capsule amounts. This capability is introduced by the still-active `medicine-expiry-tracking` change, so this change must be archived after that one.

## Impact

- Domain: `domain/stock/FefoConsumption.kt` (the deduction algorithm), `domain/stock/ProjectWeeklyUsage.kt` and `domain/stock/StockState.kt` (projection and low-stock check), `domain/stock/EvaluateStockWarning.kt`, and possibly a small helper on `DoseUnit` for "counted in whole pills". `ConsumeStockOnTaken` keeps its signature.
- UI: the quantity validation in the Add stock form (`MedicationFormValidator` or the stock form's own validation), plus a new error string resource.
- Tests: unit tests for the new deduction, projection and validation rules. No new Room migration, because the schema is unchanged.
- No new dependencies, permissions or network access.
