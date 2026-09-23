## 1. Preconditions

- [x] 1.1 Verify the scaffold and the `medicine-expiry-tracking` stock code exist (`StockBatch`, `consumeFefo`, `ProjectWeeklyUsage`, `stockState`, `AddStockBatch`, the Add stock form), and stop if they don't
- [x] 1.2 Confirm whether `medicine-expiry-tracking` has been archived yet, and record in this file that this change must be archived after it
  - Checked 2026-09-23: `medicine-expiry-tracking` is still active (tasks 8.12, 9.8, 10.11 and 11.9 open). **Archive this change only after that one**, since this delta modifies its `medicine-stock-tracking` requirements.

## 2. Whole-pill units in the domain model

- [x] 2.1 Add `DoseUnit.isWholePill` (true for `TABLET` and `CAPSULE`) with KDoc explaining why only pills round
- [x] 2.2 Add `StockBatch.usableRemaining` (floored to a whole number for whole-pill units) and make `remainingInDoseUnits` use it
- [x] 2.3 Unit tests for `isWholePill` and `usableRemaining`, including a legacy 49.92-tablet batch reading as 49

## 3. Deduction algorithm

- [x] 3.1 In `consumeFefo`, skip candidates with no usable remaining stock and round owed batch units **up** to a whole pill for whole-pill batches, keeping exact division for all other batches
- [x] 3.2 Add the exact-fit preference: when the FEFO-first batch would use part of a pill, draw the whole dose from the first batch (by expiry, then add order) that covers it without breaking a pill
- [x] 3.3 Write whole-pill batch updates from `usableRemaining - taken`, so legacy fractional rows heal on the next draw
- [x] 3.4 Update the KDoc on `consumeFefo`, `CONVERSION_SCALE` and `StockConsumption`: `consumed` now reports the full worth of the pills used, and `firstDrawn` is the exact-fit batch when one is used
- [x] 3.5 Update `FefoConsumptionTest`: replace the 1.25-tablet expectation with 2 tablets, and add the issue #67 case (40 mg from 50 × 500 mg leaves 49), 0.5-tablet dose leaves 29, per-batch round-up across two batches, exact-fit preferred, no exact fit falls back to FEFO, exact fit without enough stock is not preferred, the 10 mg-tablet + mg-batch FEFO scenario unchanged, liquid 300 → 285 ml unchanged, and legacy 49.92 → 48

## 4. Projection and low-stock check

- [x] 4.1 Change `ProjectWeeklyUsage.forMedication` to take the medicine's batches and simulate the week's doses through `consumeFefo` over an in-memory copy, adding any uncovered amount at face value, and never writing to a batch
- [x] 4.2 Pass the batches through from `stockState` and `EvaluateStockWarning` (and any other caller found by searching for `ProjectWeeklyUsage`)
- [x] 4.3 Update `ProjectWeeklyUsageTest` and `StockStateTest`: twice-daily is 14 tablets and weekday-only is 3 tablets (both unchanged), 40 mg daily from 500 mg tablets projects 3,500 mg, 6 × 500 mg tablets against 40 mg daily is low, and 100 × 5 mg against 490 mg is not low

## 5. Add stock validation

- [x] 5.1 Reject a non-integer quantity when the chosen unit is a whole-pill unit, in the Add stock form's validation, using a new string resource `medicine_stock_error_quantity_not_whole` (named to match the existing `medicine_stock_error_quantity_*` strings rather than the name first planned here) (added to every supported language's `strings.xml`)
- [x] 5.2 Add the matching `require` guard in `AddStockBatch` at the domain boundary
- [x] 5.3 Unit tests: "20.5" tablets rejected, "20.5" capsules rejected, "150.5" millilitres accepted, and the `AddStockBatch` guard
- [x] 5.4 Run the `pillsner-ui-review` skill on the changed form code

## 6. Verification

- [x] 6.1 Run the unit tests (`./gradlew testDebugUnitTest` from `src`) and report any failures verbatim
- [x] 6.2 Run lint (`./gradlew lintDebug` from `src`)
- [x] 6.3 Run the instrumented tests, including `StockBatchDaoTest`, on a device or emulator
  - Run on 2026-09-23 on the `Pixel_10` AVD (API 37). **app**: every stock-related suite passed, including `StockBatchDaoTest` (8/8), `PillsnerDatabaseMigrationTest`, `MedicationDaoTest` and `MedicationFormFlowTest`. There were 4 failures in the full run, all in reminder and clock code this change doesn't touch:
    - `ReminderAlarmReconcileTest.aDeactivatedMedicineLeavesNoAlarmAtAll` and `TrustedClockStoreTest.withNothingEverWrittenThereIsNoPriorSample` pass when their classes run alone. They fail only in the full run, most likely because device state carries over from other tests.
    - `ReminderWakeTest.anEditThatDropsADose_withdrawsItAndTakesItsReminderDown` passed 3 out of 3 reruns. It is flaky, because it reads the live notification list straight after a cancel.
    - `ReminderRecoveryTest.aStepThatThrows_isRetriedLikeATimeout` also fails on `development`, so it was already failing before this change.
  - **wear**: all 9 tests failed because they ran on a phone emulator: `The Wearable API needs a Wear OS device or a paired phone`, and `NoSuchMethodException: android.hardware.input.InputManager.getInstance`. They need a Wear OS emulator.
- [x] 6.4 Manually verify on the device: add 50 × 500 mg tablets to a medicine with a 40 mg daily dose, take a dose, and confirm the Stock section shows 49 tablets and the low-stock warning uses whole tablets
  - Marked done on 2026-09-23 at the user's request; not run by the agent.
