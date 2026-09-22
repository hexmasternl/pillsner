## 1. Domain and data layer

- [ ] 1.1 Add a `StockBatch` domain model (`id`, `medicationId`, `remaining: Quantity`, `expiryDate: LocalDate`, `addedAt: Instant`), no Android dependency
- [ ] 1.2 Add a nullable `lowStockAcknowledgement` field to the `Medication` domain model, default unset
- [ ] 1.3 Add a `stock_batches` Room entity (FK to `medications`, cascade delete) and a nullable `low_stock_acknowledgement` column on the medication entity
- [ ] 1.4 Write the Room migration adding the table and the column, defaulting existing rows to no acknowledgement
- [ ] 1.5 Write a migration test asserting the schema change and that existing medication rows survive with `low_stock_acknowledgement = NULL` and no stock batch rows
- [ ] 1.6 Extend the medicine repository to expose: observe a medicine's stock batches, add a batch, replace a medicine's batch set after consumption, set/clear the low-stock acknowledgement

## 2. Domain layer: consumption and checks

- [ ] 2.1 Add a pure FEFO consumption function: orders batches by expiry date then added-at, deducts an amount across them, floors total deducted at available stock, reports the deducted amount and the first batch drawn from
- [ ] 2.2 Unit test FEFO consumption: single batch, spanning two batches, exact exhaustion, insufficient total stock, tie-broken by added-at, batches already at zero are skipped
- [ ] 2.3 Add a pure weekly-usage projection function that sums the existing dose generator's output for a medicine over the 7 calendar days starting today, in a given time zone
- [ ] 2.4 Unit test weekly-usage projection against every schedule shape (every-N-days, weekdays, every-N-hours) and against a medicine with no schedules (no usage, exempt)
- [ ] 2.5 Add a pure sufficiency function comparing total remaining stock to projected weekly usage
- [ ] 2.6 Add a pure batch expiry classification function (`NONE` / `APPROACHING` within 30 days / `PAST`), unit tested around the day boundary, the 30-day edge, month/year boundaries and leap days

## 3. Intake recording integration

- [ ] 3.1 In the shared "record taken" path used by the dose detail screen, the reminder notification action and the wearable action, after recording the intake: if the dose's medicine has at least one stock batch, run FEFO consumption for the dose's amount and persist the updated batches in the same transaction as the intake write
- [ ] 3.2 After consumption, evaluate sufficiency (skipping medicines with no schedule) and the drawn-from batch's expiry classification
- [ ] 3.3 When insufficient and the medicine's acknowledgement is unset, flag a low-stock warning to show; when the drawn-from batch is `APPROACHING` or `PAST`, flag an expiry-at-use warning to show, unconditionally
- [ ] 3.4 If the app is in the foreground, show the combined warning dialog immediately; otherwise persist a pending-warning marker per medicine to be evaluated fresh and shown the next time the app is opened
- [ ] 3.5 Collapse any number of pending markers for the same medicine into a single warning, evaluated against current state, not replayed per event
- [ ] 3.6 Confirm skip and missed outcomes never trigger consumption or any stock check

## 4. Warning dialog

- [ ] 4.1 Build the combined warning dialog: states the low-stock message with "OK" and "I ordered new" when sufficiency failed and unacknowledged; states the expiry-at-use message with a single acknowledgement when the drawn batch is approaching/past expiry; states both when both apply
- [ ] 4.2 Wire "OK" to leave the acknowledgement unset (so the next insufficient take warns again) and dismiss the dialog
- [ ] 4.3 Wire "I ordered new" to set the acknowledgement and dismiss the dialog
- [ ] 4.4 Wire the expiry-at-use acknowledgement to simply dismiss, with no persisted state
- [ ] 4.5 Add string resources for both messages and all three responses

## 5. Medicine details: Stock section

- [ ] 5.1 Add a Stock section to the Medicine details screen listing the medicine's batches ordered by expiry date ascending, each showing remaining amount and expiry date
- [ ] 5.2 Add an "Add stock" action opening the add-batch form
- [ ] 5.3 Build the Add stock form: quantity (decimal, greater than zero, unit fixed to the medicine's default dose unit, read-only) and expiry date (required date picker, non-blocking warning if already past)
- [ ] 5.4 Saving a batch clears the medicine's low-stock acknowledgement
- [ ] 5.5 Show an inline note of the medicine's current low-stock / expiry-at-use state on the details screen, recomputed live
- [ ] 5.6 Add string resources for the section, the form's fields and its validation message

## 6. Medicines screen: tile heads-up

- [ ] 6.1 Design the stock heads-up indicator (icon/badge, copy, colour-independent distinction between low-stock and expiry states) with the `pillsner-designer` agent against `docs/design-system.md`
- [ ] 6.2 Add the indicator to the medicine tile composable, driven live by current stock and schedule state, omitted entirely for a medicine with no stock batches
- [ ] 6.3 Ensure the indicator renders for both active and inactive tiles and combines correctly with the existing inactive distinction
- [ ] 6.4 Add the stock state to the tile's combined screen-reader announcement
- [ ] 6.5 Add previews for tile states: no stock tracked, sufficient stock, low stock, nearest batch approaching expiry, nearest batch past expiry, low stock + expiring

## 7. Verification

- [ ] 7.1 Run `pillsner-ui-review` against the changed and new composables
- [ ] 7.2 Run unit tests, Room migration tests and lint from the `src` Gradle project root
- [ ] 7.3 Manually verify the Stock section, Add stock form, tile heads-up and warning dialog with TalkBack and the largest system font scale
- [ ] 7.4 Manually verify: a dose taken from the notification while the app is backgrounded defers its warning to the next app open, and does not crash or silently drop it
