## Context

`medicine-expiry-tracking` introduced stock batches with a per-batch unit and strength, plus first-expiry-first-out (FEFO) consumption in `domain/stock/FefoConsumption.kt`. `consumeFefo` divides the amount still owed by a batch's strength at 6 decimal places and rounds **down**. So a 40 mg dose against 500 mg tablets deducts 0.08 tablets. That was a deliberate decision in that change's design ("fractional deduction is consistent with the rest of the app"). Issue #67 shows it doesn't match reality: a partly used tablet can't go back into stock. The count on screen drifts upwards, and the low-stock check projects 280 mg a week where 3,500 mg of tablets actually disappear.

The `medicine-stock-tracking` capability isn't in `openspec/specs/` yet, because `medicine-expiry-tracking` is still active (four manual or instrumented verification tasks are open). This change's delta spec modifies requirements from that change's delta spec. It must therefore be archived **after** `medicine-expiry-tracking`.

## Goals / Non-Goals

**Goals:**
- Tablet and capsule batches only ever lose whole units, rounded up.
- When a batch's strength matches the dose exactly, the dose comes from that batch rather than from a larger tablet that would be mostly wasted.
- The low-stock check projects usage the same way stock is actually deducted.
- Existing fractional pill counts heal on the next draw, with no schema migration.

**Non-Goals:**
- Splittable-tablet support (a half-tablet dose deducting 0.5). Possible follow-up change.
- Combining several batches to hit a dose exactly, for example 20 mg + 20 mg tablets for a 40 mg dose from two batches. Exact fit is single-batch only.
- Any change to continuous-unit batches, the warning UI, or the Room schema.

## Decisions

### Whole-pill units are `TABLET` and `CAPSULE`, decided by the batch's unit
Add `val DoseUnit.isWholePill: Boolean` in the domain model (true for `TABLET` and `CAPSULE`). The batch's own unit decides, not the medicine's dose unit. A mg-dosed medicine with tablet stock is whole-pill. A tablet-dosed medicine with tablet stock (strength 1) is also whole-pill, so a 0.5 tablet dose deducts 1.
- *Alternative*: include `DROP`, `PUFF` and `UNIT` too. Rejected because these are never split and doses are already whole, so rounding would change nothing except edge cases. Keeping the set to pills matches the issue and keeps the rule easy to explain.
- *Alternative*: a per-medicine or per-batch "whole units" flag. Rejected for now. It adds UI and a schema column for a need that the unit already expresses.

### Usable remaining = floor for whole-pill batches
Add `StockBatch.usableRemaining` (floor to a whole number for whole-pill units, otherwise `remaining`). Change `remainingInDoseUnits` to use it. Consumption, `stockState`, the tile heads-up and the Stock section all read through it. Legacy rows such as 49.92 therefore behave as 49 everywhere straight away. When a dose is drawn from such a batch, the new remaining is written as `usableRemaining - taken`, which is whole, so the row heals itself.
- *Alternative*: a one-off data migration that floors every tablet and capsule batch. Rejected because it needs a database version bump and a migration test for a data-only fix. Flooring lazily on read gives the same user-visible result.

### Deduction algorithm
`consumeFefo(batches, doseAmount)` keeps its signature and return type:
1. Candidates are batches with `usableRemaining > 0`, sorted by (expiryDate, addedAt).
2. **Exact-fit check.** If the first candidate is whole-pill and `doseAmount / strength` isn't a whole number, look for the first candidate in the same order that can take the whole dose without breaking a pill: a whole-pill batch whose strength divides the dose evenly and whose `usableRemaining` covers it, or a continuous batch that covers it. If one is found, draw the whole dose from it and stop.
3. **FEFO otherwise.** For each candidate in order, owed in batch units = `owed / strength`. For whole-pill batches this rounds **up** (`RoundingMode.CEILING` at scale 0, via exact division then `setScale(0, CEILING)`). Otherwise it stays exact, as today. Take `min(usableRemaining, owedInBatchUnits)` and subtract `taken × strength` from owed. A whole-pill batch that covers the rounded-up amount settles the dose (owed = 0), even when the pill is worth more than was owed.
4. `firstDrawn` is the batch actually drawn from first, including the exact-fit batch. The expiry-at-use warning then classifies the pill the user actually took.

The existing `CONVERSION_SCALE = 6` / `RoundingMode.DOWN` path stays for continuous-unit batches with a strength other than 1. Exact fit is only tried when the FEFO-first batch would waste part of a pill. When FEFO wouldn't waste anything, expiry order still wins, so the existing "FEFO conversion applies per batch" scenario (a 10 mg tablet then the mg batch) is unchanged.
- *Alternative*: always prefer exact-fit batches, even when FEFO wastes nothing. Rejected because it could leave an expiring batch untouched for no benefit.
- *Alternative*: choose the batch that minimises waste. Rejected because it's harder to explain and harder to test, and the issue asks for "exact dose, else whole pill".

### Projection simulates consumption
`ProjectWeeklyUsage.forMedication` gains the batches as an input, with an overload or a default for callers that don't need rounding. It generates the week's doses as today and folds them through `consumeFefo` over an in-memory copy of the batches, applying each result's `updates` to the copy. Usage is the sum of what each simulated deduction took in dose units: the pill's full worth, `taken × strength`, not the dose amount. Any amount still owed when the copy runs dry is added at face value. Low stock stays `remainingInDoseUnits < usage`. With simulation this is equivalent to "the next 7 days of doses can't all be covered".

Because `consumeFefo` reports `consumed` in dose units, it needs to report the pill-worth taken, which it already does via `takenInDoseUnits = taken × strength`. After the change a 40 mg dose from a 500 mg tablet reports 500 mg consumed. `ConsumeStockOnTaken` doesn't use `consumed`, so nothing else is affected.
- *Alternative*: round each dose up to whole pills against the soonest batch's strength without simulating. Rejected because it gives the wrong answer as soon as batches mix strengths or the exact-fit rule kicks in.

### Add stock validation
The Add stock form's quantity validation rejects a non-integer value when the chosen unit `isWholePill`, with a new string resource `medicine_stock_error_quantity_not_whole`. `AddStockBatch` also checks this at the domain boundary (`require`), matching how it already guards strength.

## Risks / Trade-offs

- [Users who split tablets on purpose will see stock drop twice as fast] → This is accepted and documented in the proposal. It errs towards warning early, which is the safer failure for a medication app. A splittable-tablet option is the follow-up if users ask.
- [`consumed` now reports more than the dose amount] → Its only consumer is the projection, and that's exactly what the projection needs. The KDoc on `StockConsumption` will be updated to say so.
- [Exact-fit leaves a sooner-expiring batch in place longer] → The tile and details heads-up read the soonest-expiring batch with stock live, so an approaching expiry is still shown.
- [Archive ordering] → The proposal and this design both say the change must archive after `medicine-expiry-tracking`. The archive step will fail loudly if `medicine-stock-tracking` isn't in `openspec/specs/` yet.

## Migration Plan

No schema change and no Room version bump. Legacy fractional pill counts are floored on read and healed on the next draw. Rollback is a plain revert: the fractional arithmetic still accepts whole-number rows.

## Open Questions

- Whether `DROP`, `PUFF` or `UNIT` should ever be treated as whole-pill. Currently no.
