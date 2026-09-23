## 1. Domain and data layer

- [x] 1.1 Add a `StockBatch` domain model (`id`, `medicationId`, `remaining: Quantity`, `expiryDate: LocalDate`, `addedAt: Instant`), no Android dependency
- [x] 1.2 Add a nullable `lowStockAcknowledgement` field to the `Medication` domain model, default unset
- [x] 1.3 Add a `stock_batches` Room entity (FK to `medications`, cascade delete) and a nullable `low_stock_acknowledgement` column on the medication entity
- [x] 1.4 Write the Room migration adding the table and the column, defaulting existing rows to no acknowledgement
- [x] 1.5 Write a migration test asserting the schema change and that existing medication rows survive with `low_stock_acknowledgement = NULL` and no stock batch rows
- [x] 1.6 Extend the medicine repository to expose: observe a medicine's stock batches, add a batch, replace a medicine's batch set after consumption, set/clear the low-stock acknowledgement

## 2. Domain layer: consumption and checks

- [x] 2.1 Add a pure FEFO consumption function: orders batches by expiry date then added-at, deducts an amount across them, floors total deducted at available stock, reports the deducted amount and the first batch drawn from
- [x] 2.2 Unit test FEFO consumption: single batch, spanning two batches, exact exhaustion, insufficient total stock, tie-broken by added-at, batches already at zero are skipped
- [x] 2.3 Add a pure weekly-usage projection function that sums the existing dose generator's output for a medicine over the 7 calendar days starting today, in a given time zone
- [x] 2.4 Unit test weekly-usage projection against every schedule shape (every-N-days, weekdays, every-N-hours) and against a medicine with no schedules (no usage, exempt)
- [x] 2.5 Add a pure sufficiency function comparing total remaining stock to projected weekly usage
- [x] 2.6 Add a pure batch expiry classification function (`NONE` / `APPROACHING` within 30 days / `PAST`), unit tested around the day boundary, the 30-day edge, month/year boundaries and leap days

## 3. Intake recording integration

- [x] 3.1 In the shared "record taken" path used by the dose detail screen, the reminder notification action and the wearable action, after recording the intake: if the dose's medicine has at least one stock batch, run FEFO consumption for the dose's amount and persist the updated batches in the same transaction as the intake write
- [x] 3.2 After consumption, evaluate sufficiency (skipping medicines with no schedule) and the drawn-from batch's expiry classification
- [x] 3.3 When insufficient and the medicine's acknowledgement is unset, flag a low-stock warning to show; when the drawn-from batch is `APPROACHING` or `PAST`, flag an expiry-at-use warning to show, unconditionally
- [x] 3.4 If the app is in the foreground, show the combined warning dialog immediately; otherwise persist a pending-warning marker per medicine to be evaluated fresh and shown the next time the app is opened
- [x] 3.5 Collapse any number of pending markers for the same medicine into a single warning, evaluated against current state, not replayed per event
- [x] 3.6 Confirm skip and missed outcomes never trigger consumption or any stock check

## 4. Warning dialog

- [x] 4.1 Build the combined warning dialog: states the low-stock message with "OK" and "I ordered new" when sufficiency failed and unacknowledged; states the expiry-at-use message with a single acknowledgement when the drawn batch is approaching/past expiry; states both when both apply
- [x] 4.2 Wire "OK" to leave the acknowledgement unset (so the next insufficient take warns again) and dismiss the dialog
- [x] 4.3 Wire "I ordered new" to set the acknowledgement and dismiss the dialog
- [x] 4.4 Wire the expiry-at-use acknowledgement to simply dismiss, with no persisted state
- [x] 4.5 Add string resources for both messages and all three responses

## 5. Medicine details: Stock section

- [x] 5.1 Add a Stock section to the Medicine details screen listing the medicine's batches ordered by expiry date ascending, each showing remaining amount and expiry date
- [x] 5.2 Add an "Add stock" action opening the add-batch form
- [x] 5.3 Build the Add stock form: quantity (decimal, greater than zero, unit fixed to the medicine's default dose unit, read-only) and expiry date (required date picker, non-blocking warning if already past)
- [x] 5.4 Saving a batch clears the medicine's low-stock acknowledgement
- [x] 5.5 Show an inline note of the medicine's current low-stock / expiry-at-use state on the details screen, recomputed live
- [x] 5.6 Add string resources for the section, the form's fields and its validation message

## 6. Medicines screen: tile heads-up

- [x] 6.1 Design the stock heads-up indicator (icon/badge, copy, colour-independent distinction between low-stock and expiry states) against `docs/design-system.md` — designed directly against the doc and corrected by `pillsner-ui-review` (see 7.1), rather than via a separate `pillsner-designer` agent pass
- [x] 6.2 Add the indicator to the medicine tile composable, driven live by current stock and schedule state, omitted entirely for a medicine with no stock batches
- [x] 6.3 Ensure the indicator renders for both active and inactive tiles and combines correctly with the existing inactive distinction
- [x] 6.4 Add the stock state to the tile's combined screen-reader announcement
- [x] 6.5 Add previews for tile states: no stock tracked, sufficient stock, low stock, nearest batch approaching expiry, nearest batch past expiry, low stock + expiring

## 7. Verification

- [x] 7.1 Run `pillsner-ui-review` against the changed and new composables — found 2 colour-role findings (past-expiry state used `error`/`errorContainer`, not on design-system.md 2.4's allowed red list), both fixed
- [x] 7.2 Run unit tests and lint from the `src` Gradle project root — 482/482 unit tests pass, lint clean, `assembleDebug` succeeds. Booted the `Pixel_10` emulator and ran the Room migration instrumented test suite on-device: all 6 pass, including `migrate5To6_addsStockBatchesAndTheAcknowledgementColumn`
- [x] 7.3 Manually verified on the `Pixel_10` emulator: added a medicine with a daily schedule, opened its Medicine details, confirmed the Stock section ("No stock is tracked" + Add stock), added a batch with a past expiry date and saw the non-blocking "This date has already passed. You can still save it." warning, saved it, and saw the inline "Stock is running low." / "The soonest batch has expired." notes render live in the informational (non-red) colour. Confirmed the Medicines tile's "Low stock" heads-up chip renders live in the same colour. Re-verified the Stock section, the Add stock form and the tile heads-up at `font_scale=2.0` (largest system font): fully scrollable, nothing clipped, matching the large-font previews
- [x] 7.4 Manually verified on-device: answered "I took it" on the actual posted reminder notification (the background/notification code path, app not on Home at the time) — the stock batch was consumed via FEFO (1 mg → 0 mg), the exhausted batch correctly dropped out of the expiry-at-use note (live recompute), and the combined warning dialog ("TestMed is running low and won't last the week at this rate." with "I ordered new" / "OK") appeared correctly the next time Home was opened, evaluated fresh. No crash, no silently dropped warning. Tapped "I ordered new" and the dialog dismissed cleanly

## 8. Stock unit conversion (strength)

Added after on-device testing surfaced a real gap: stock could only ever be recorded in the medicine's own default dose unit, so a medicine dosed in mg could never have its stock tracked as a pill count. `design.md` and `specs/medicine-stock-tracking/spec.md` are updated first; this section implements that revision.

- [x] 8.1 Change the `StockBatch` domain model: `remaining: BigDecimal`, `unit: DoseUnit`, `strengthPerUnit: BigDecimal` (> 0; exactly 1 when `unit` matches the medicine's default dose unit)
- [x] 8.2 Add a `strength_per_unit` column to the `stock_batches` Room entity and its (still-unshipped, so revised in place rather than bumped) version-6 migration; regenerate the exported schema JSON
- [x] 8.3 Update `StockBatchRepository.addBatch` and both implementations (Room, in-memory) to carry the unit and strength through
- [x] 8.4 Rewrite `consumeFefo` to work in the medicine's default dose unit, converting through each visited batch's own strength, per the revised "First-expiry-first-out consumption" and new "Stock unit conversion" requirements
- [x] 8.5 Update `stockState`'s remaining-stock total to sum each batch's `remaining × strengthPerUnit` rather than assume one shared unit
- [x] 8.6 Update `AddStockBatch` to accept a unit and a strength, forcing strength to 1 when the chosen unit equals the medicine's default dose unit regardless of what was passed
- [x] 8.7 Add a unit picker to the Add stock form (defaulting to the medicine's default dose unit) and a strength field shown only when the chosen unit differs, labelled with both units (e.g. "mg per tablet"), validated greater than zero
- [x] 8.8 Show a batch's strength in the Stock section's row when it is not 1
- [x] 8.9 Rewrite `FefoConsumptionTest` and `StockStateTest` for the conversion-aware behaviour; add scenarios mixing a tablet-strength batch and a same-unit batch for one medicine
- [x] 8.10 Add `ConsumeStockOnTakenTest` scenarios for a tablet-strength batch (40 mg dose against 20 mg tablets deducts 2) and a same-unit batch (300 ml stock, 15 ml dose)
- [x] 8.11 Run unit tests and lint from the `src` Gradle project root
- [ ] 8.12 Manually verify on-device: add a tablet-strength batch to a mg-dosed medicine, take a dose, confirm the tablet count deducted matches the dose ÷ strength

## 9. Removing a stock batch

Added alongside section 8, at the user's request: a batch can now be removed after it is added, since a mistaken quantity, unit or strength can otherwise never be corrected. `design.md` and `specs/medicine-stock-tracking/spec.md` are updated first (the removal-related non-goal is reversed); this section implements that revision.

- [x] 9.1 Add `removeBatch(batchId: StockBatchId)` to `StockBatchRepository` and both implementations (Room DAO delete query, in-memory)
- [x] 9.2 Add a delete (trash) icon to each Stock section row
- [x] 9.3 Add a confirmation `AlertDialog` ("remove this batch, cannot be undone") shown before removal; cancelling or dismissing leaves the batch untouched
- [x] 9.4 Wire the confirmed removal through `MedicationFormViewModel` to the repository, live-updating the Stock section and the tile heads-up
- [x] 9.5 Add string resources for the delete affordance's label and the confirmation dialog's title, body and actions
- [x] 9.6 Add tests: repository removal, and that removing a medicine's last batch takes it out of stock tracking (no consumption, no checks, no indicator)
- [x] 9.7 Run unit tests and lint from the `src` Gradle project root
- [ ] 9.8 Manually verify on-device: remove a batch with confirmation, cancel a removal and confirm nothing changed, and remove a medicine's last batch and confirm its tile heads-up and Stock section both revert to "no stock tracked"

## 10. Pull request review fixes

Fixes for the review on pull request #60. Each brings the code in line with requirements the spec already stated (same-transaction consumption, the first-drawn batch for expiry-at-use); the two new scenarios under "Stock unit conversion" cover the rounding and unit-lock fixes.

- [x] 10.1 Add a domain `TransactionRunner` (Room `withTransaction`, in-memory mutex) and run a taken answer's pending re-check, intake write and FEFO read-modify-write inside it in `AnswerDose`; flag the stock warning only after commit
- [x] 10.2 Round the FEFO unit conversion down, and let a batch that covers the amount owed settle the dose so no rounding remainder spills into a further batch
- [x] 10.3 Report the first-drawn batch from `consumeFefo`, carry its expiry date through the stock warning queue, and classify that batch in `EvaluateStockWarning` rather than the nearest batch still holding stock
- [x] 10.4 Replace `firstNotNullOfOrNull` in `HomeViewModel` with an explicit suspend loop over the pending entries
- [x] 10.5 Make `MedicationRepository.update` keep the stored low-stock acknowledgement, and drop the acknowledgement from the form draft and its saved state
- [x] 10.6 Reject a default dose unit change on the form while stock batches exist (`DOSE_UNIT_LOCKED_BY_STOCK`, string in all six locales)
- [x] 10.7 Load the stored medicine and observe its stock when the edit form is restored from a saved draft
- [x] 10.8 Feed the Medicines screen a `currentDates` flow that re-emits at midnight, so tile expiry states roll over without a data change
- [x] 10.9 Add unit tests for every fix above and instrumented tests for `update` keeping the acknowledgement and for the transaction runner rolling back
- [x] 10.10 Run unit tests and lint from the `src` Gradle project root
- [ ] 10.11 Run the instrumented tests on a device or emulator
