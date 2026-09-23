## 1. Preconditions

- [ ] 1.1 Verify the scaffold and the `medicine-expiry-tracking` stock code exist (`StockBatch`, `consumeFefo`, `ProjectWeeklyUsage`, `stockState`, `AddStockBatch`, the Add stock form), and stop if they don't
- [ ] 1.2 Confirm whether `medicine-expiry-tracking` has been archived yet, and record in this file that this change must be archived after it

## 2. Whole-pill units in the domain model

- [ ] 2.1 Add `DoseUnit.isWholePill` (true for `TABLET` and `CAPSULE`) with KDoc explaining why only pills round
- [ ] 2.2 Add `StockBatch.usableRemaining` (floored to a whole number for whole-pill units) and make `remainingInDoseUnits` use it
- [ ] 2.3 Unit tests for `isWholePill` and `usableRemaining`, including a legacy 49.92-tablet batch reading as 49

## 3. Deduction algorithm

- [ ] 3.1 In `consumeFefo`, skip candidates with no usable remaining stock and round owed batch units **up** to a whole pill for whole-pill batches, keeping exact division for all other batches
- [ ] 3.2 Add the exact-fit preference: when the FEFO-first batch would use part of a pill, draw the whole dose from the first batch (by expiry, then add order) that covers it without breaking a pill
- [ ] 3.3 Write whole-pill batch updates from `usableRemaining - taken`, so legacy fractional rows heal on the next draw
- [ ] 3.4 Update the KDoc on `consumeFefo`, `CONVERSION_SCALE` and `StockConsumption`: `consumed` now reports the full worth of the pills used, and `firstDrawn` is the exact-fit batch when one is used
- [ ] 3.5 Update `FefoConsumptionTest`: replace the 1.25-tablet expectation with 2 tablets, and add the issue #67 case (40 mg from 50 × 500 mg leaves 49), 0.5-tablet dose leaves 29, per-batch round-up across two batches, exact-fit preferred, no exact fit falls back to FEFO, exact fit without enough stock is not preferred, the 10 mg-tablet + mg-batch FEFO scenario unchanged, liquid 300 → 285 ml unchanged, and legacy 49.92 → 48

## 4. Projection and low-stock check

- [ ] 4.1 Change `ProjectWeeklyUsage.forMedication` to take the medicine's batches and simulate the week's doses through `consumeFefo` over an in-memory copy, adding any uncovered amount at face value, and never writing to a batch
- [ ] 4.2 Pass the batches through from `stockState` and `EvaluateStockWarning` (and any other caller found by searching for `ProjectWeeklyUsage`)
- [ ] 4.3 Update `ProjectWeeklyUsageTest` and `StockStateTest`: twice-daily is 14 tablets and weekday-only is 3 tablets (both unchanged), 40 mg daily from 500 mg tablets projects 3,500 mg, 6 × 500 mg tablets against 40 mg daily is low, and 100 × 5 mg against 490 mg is not low

## 5. Add stock validation

- [ ] 5.1 Reject a non-integer quantity when the chosen unit is a whole-pill unit, in the Add stock form's validation, using a new string resource `stock_quantity_whole_number_error` (added to every supported language's `strings.xml`)
- [ ] 5.2 Add the matching `require` guard in `AddStockBatch` at the domain boundary
- [ ] 5.3 Unit tests: "20.5" tablets rejected, "20.5" capsules rejected, "150.5" millilitres accepted, and the `AddStockBatch` guard
- [ ] 5.4 Run the `pillsner-ui-review` skill on the changed form code

## 6. Verification

- [ ] 6.1 Run the unit tests (`./gradlew testDebugUnitTest` from `src`) and report any failures verbatim
- [ ] 6.2 Run lint (`./gradlew lintDebug` from `src`)
- [ ] 6.3 Run the instrumented tests, including `StockBatchDaoTest`, on a device or emulator
- [ ] 6.4 Manually verify on the device: add 50 × 500 mg tablets to a medicine with a 40 mg daily dose, take a dose, and confirm the Stock section shows 49 tablets and the low-stock warning uses whole tablets
