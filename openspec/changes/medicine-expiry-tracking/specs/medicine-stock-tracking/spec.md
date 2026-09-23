## ADDED Requirements

### Requirement: Stock batch model
The domain layer SHALL define a `StockBatch` with an identifier, the medication it belongs to, a remaining amount, a unit, a strength (how much of the medication's default dose unit one unit of this batch is worth), an expiry date, and the moment it was added. Domain types MUST NOT depend on Android framework classes. A batch's unit MAY differ from its medication's default dose unit; when it matches, the batch's strength SHALL be exactly 1.

#### Scenario: Batch belongs to one medicine
- **WHEN** a stock batch is created for a medicine
- **THEN** it carries that medicine's identifier and no other medicine can reference it

#### Scenario: Unit matches the medicine
- **WHEN** a medicine's default dose unit is millilitre and the user adds a stock batch in millilitres
- **THEN** the batch's amount is recorded in millilitres with a strength of 1

#### Scenario: Unit differs from the medicine
- **WHEN** a medicine's default dose unit is milligram and the user adds a stock batch of 20 tablets, each worth 20 mg
- **THEN** the batch's amount is recorded as 20 tablets with a strength of 20

### Requirement: A medicine with no stock recorded is unaffected
A medicine with zero stock batches SHALL behave exactly as it did before this capability existed: no consumption, no low-stock check, no expiry-at-use check, and no stock indicator anywhere. The capability activates for a medicine the moment its first stock batch is added, and only for that medicine.

#### Scenario: No batches, no effect
- **WHEN** a medicine has never had a stock batch added and a dose of it is recorded taken
- **THEN** no consumption occurs, no stock warning is shown, and no stock indicator appears anywhere for that medicine

#### Scenario: Activates on first batch
- **WHEN** the user adds the first stock batch to a medicine that previously had none
- **THEN** the next dose of that medicine recorded taken is deducted from that batch and the medicine becomes subject to the low-stock and expiry-at-use checks

### Requirement: Add stock form
The Medicine details screen's Stock section SHALL offer an "Add stock" form with: a quantity, a decimal number greater than zero parsed using the device locale; a unit, chosen from the same fixed unit list the rest of the app uses, defaulting to the medicine's own default dose unit; a strength field, shown only when the chosen unit differs from the medicine's default dose unit, a decimal number greater than zero, labelled with both units so the direction of the conversion is unambiguous (for example "mg per tablet"); and an expiry date, required, chosen with a date picker. When the chosen unit matches the medicine's default dose unit, the strength field SHALL NOT be shown and the batch's strength SHALL be recorded as 1. An expiry date already in the past SHALL be accepted with a non-blocking warning stating so, never rejected. Saving a valid batch SHALL add it to the medicine's stock in a single write and SHALL clear the medicine's low-stock acknowledgement, regardless of whether the new batch actually restores sufficiency.

#### Scenario: Valid batch, unit matches the dose
- **WHEN** the user enters "300" and unit "millilitre" for a medicine dosed in millilitres, with no strength field shown, and selects an expiry date six months away
- **THEN** a batch of 300 millilitres with a strength of 1 and that expiry date is added to the medicine's stock

#### Scenario: Valid batch, unit differs from the dose
- **WHEN** a medicine is dosed in milligrams and the user enters quantity "20", unit "tablet", strength "20" (mg per tablet), and an expiry date six months away
- **THEN** a batch of 20 tablets with a strength of 20 and that expiry date is added to the medicine's stock

#### Scenario: Strength field appears only when needed
- **WHEN** the user opens the Add stock form for a medicine dosed in milligrams and changes the unit from milligram to tablet
- **THEN** the strength field appears, labelled "mg per tablet"; changing the unit back to milligram hides it again

#### Scenario: Zero or negative quantity rejected
- **WHEN** the user enters "0" and attempts to save
- **THEN** the batch is not saved and the quantity field shows an error stating the amount must be greater than zero

#### Scenario: Zero or negative strength rejected
- **WHEN** the chosen unit differs from the medicine's default dose unit and the user enters "0" for strength and attempts to save
- **THEN** the batch is not saved and the strength field shows an error stating the amount must be greater than zero

#### Scenario: Past expiry date accepted with a warning
- **WHEN** the user selects an expiry date that has already passed
- **THEN** the form shows a non-blocking warning stating the date is in the past, and saving still succeeds

#### Scenario: Adding stock clears a standing acknowledgement
- **WHEN** a medicine's low-stock warning was previously suppressed with "I ordered new" and the user adds a new stock batch, even one too small to restore sufficiency
- **THEN** the medicine's acknowledgement is cleared and the next insufficient take warns again

### Requirement: Removing a stock batch
Every batch row in the Stock section SHALL carry a delete affordance. Tapping it SHALL ask for confirmation before anything is removed, presented as an `AlertDialog` naming the batch and stating that removing it cannot be undone, with a confirm action and a way to cancel. Confirming SHALL remove exactly that batch from the medicine's stock; cancelling, or dismissing the dialog any other way, SHALL leave every batch unchanged. This is the only way a stock batch's own fields can be corrected once saved — editing a batch in place is out of scope, so a mistaken entry is removed and re-added instead. Removing a medicine's last remaining batch SHALL take that medicine out of stock tracking entirely, exactly as if it had never had a batch added.

#### Scenario: Confirming removes the batch
- **WHEN** the user taps the delete affordance on a batch row and confirms
- **THEN** that batch no longer appears in the Stock section and its consumption and warnings no longer consider it

#### Scenario: Cancelling leaves the batch untouched
- **WHEN** the user taps the delete affordance on a batch row and cancels, or dismisses the dialog without confirming
- **THEN** the batch is unchanged and still appears in the Stock section

#### Scenario: Removing the last batch disables the feature again
- **WHEN** a medicine has exactly one stock batch and the user removes it, confirming
- **THEN** the medicine has no stock batches left and behaves exactly as a medicine that never had stock recorded: no consumption, no low-stock check, no expiry-at-use check, and no stock indicator

### Requirement: Stock unit conversion (strength)
Wherever the stock system compares or totals quantities across batches — the low-stock check, the tile and details-screen heads-up, and first-expiry-first-out consumption itself — it SHALL work in the medicine's default dose unit, converting each batch's `remaining` by multiplying it by that batch's `strengthPerUnit`. A batch whose unit already matches the medicine's default dose unit (strength 1) needs no conversion, so a medicine whose stock and doses share one unit throughout SHALL behave exactly as plain subtraction and comparison in that unit, unchanged by this requirement's existence.

#### Scenario: Same unit throughout needs no conversion
- **WHEN** a medicine is dosed in millilitres, has 300 ml of stock and a 15 ml dose is recorded taken
- **THEN** the batch's remaining amount becomes 285 ml, computed as plain subtraction

#### Scenario: Tablets converted against a dose in milligrams
- **WHEN** a medicine is dosed in milligrams with a 40 mg schedule, has one batch of 20 tablets each worth 20 mg, and a dose is recorded taken
- **THEN** 2 tablets are deducted from the batch, leaving 18, because 40 mg ÷ 20 mg-per-tablet is 2 tablets

#### Scenario: A dose that does not divide evenly still deducts exactly
- **WHEN** a medicine is dosed in milligrams with a 25 mg schedule and has one batch of tablets each worth 20 mg
- **THEN** 1.25 tablets are deducted, since the domain already allows a fractional amount of a unit (the same way a dose can be half a tablet)

#### Scenario: Low-stock comparison converts every batch
- **WHEN** a medicine dosed in milligrams has one batch of 100 tablets worth 5 mg each (500 mg total) and a projected weekly usage of 490 mg
- **THEN** its stock is not considered low, because 500 mg of remaining stock, once converted, covers the projected 490 mg

#### Scenario: FEFO conversion applies per batch it draws from
- **WHEN** a medicine is dosed in milligrams, has a batch expiring sooner worth 10 mg per tablet with 1 tablet remaining and a batch expiring later in the medicine's own milligram unit with 30 mg remaining, and a 20 mg dose is recorded taken
- **THEN** the sooner-expiring batch is exhausted (its one 10 mg tablet fully consumed) and the remaining 10 mg owed is drawn from the later batch, leaving it at 20 mg

#### Scenario: A conversion that does not divide exactly never over-deducts
- **WHEN** a medicine is dosed in milligrams with a 2 mg dose and has one batch of tablets each worth 3 mg
- **THEN** the batch gives up at most 2 mg worth of tablets, rounded down at the conversion precision, and any negligible remainder is not drawn from a further batch

#### Scenario: Default dose unit is locked while stock is recorded
- **WHEN** a medicine has at least one stock batch and the user changes its default dose unit on the Medicine details form
- **THEN** the form shows an error on the dose field straight away and does not save until the unit is changed back or every stock batch is removed, since each batch's strength is relative to that unit

### Requirement: First-expiry-first-out consumption
When a dose is recorded as taken, for a medicine with at least one stock batch, the dose's amount (in the medicine's default dose unit) SHALL be deducted from that medicine's stock in the same transaction as the intake write, always drawing first from the batch with the earliest expiry date, then the next earliest, and so on, breaking a tie between batches with the same expiry date by the order they were added, converting through each batch's own strength per the "Stock unit conversion" requirement. A batch already at zero remaining SHALL be skipped, never taken below zero. If total remaining stock, converted to the medicine's default dose unit, is less than the dose's amount, the deduction SHALL floor at zero across all batches rather than go negative. A batch that reaches zero remaining SHALL be kept, not deleted, so the medicine continues to count as having stock recorded.

#### Scenario: Single batch covers the dose
- **WHEN** a medicine has one batch of 30 tablets (strength 1, dosed in tablets) and a dose of 1 tablet is recorded taken
- **THEN** the batch's remaining amount becomes 29 tablets

#### Scenario: Consumption spans two batches
- **WHEN** a medicine has a batch expiring sooner with 1 tablet remaining and a batch expiring later with 30 tablets remaining (both strength 1, dosed in tablets), and a dose of 2 tablets is recorded taken
- **THEN** the sooner-expiring batch is exhausted to 0 and the later-expiring batch's remaining amount becomes 29 tablets

#### Scenario: Exhausted batch is kept, not removed
- **WHEN** a batch is consumed down to exactly 0 remaining
- **THEN** its row still exists with 0 remaining, and the medicine still counts as having stock batches recorded

#### Scenario: Tie broken by add order
- **WHEN** a medicine has two batches with the same expiry date, added at different times, and a dose is recorded taken
- **THEN** the batch that was added first is deducted from before the other

#### Scenario: Insufficient total stock floors at zero
- **WHEN** a medicine's total remaining stock, converted to its default dose unit, is 1 mg and a dose of 2 mg is recorded taken
- **THEN** every batch ends at 0 remaining and no batch or total goes negative

#### Scenario: Skip and missed never consume stock
- **WHEN** a dose is recorded skipped, or lapses as missed
- **THEN** no stock batch's remaining amount changes

#### Scenario: Same recording path regardless of surface
- **WHEN** a taken dose is recorded from the dose detail screen, from the reminder notification, or from a paired wearable
- **THEN** stock consumption happens identically in each case, since all three answer through the same recording path

### Requirement: Weekly usage projection
The domain layer SHALL define a pure function that projects a medicine's usage for the 7 calendar days starting today, in a given time zone, by summing the amounts of every dose the existing dose generator (`dose-records`) would produce for that medicine over that window. A medicine with no schedules (as-needed) SHALL have no projected weekly usage.

#### Scenario: Twice-daily medicine
- **WHEN** a medicine has one schedule of 1 tablet twice a day
- **THEN** its projected weekly usage is 14 tablets

#### Scenario: Weekday-only medicine
- **WHEN** a medicine has one schedule of 1 tablet on Monday, Wednesday and Friday, and the 7-day window contains exactly those three weekdays once each
- **THEN** its projected weekly usage is 3 tablets

#### Scenario: As-needed medicine has no projection
- **WHEN** a medicine has no schedules
- **THEN** it has no projected weekly usage and is exempt from the low-stock check

### Requirement: Low-stock warning and its acknowledgement
After consumption, the domain layer SHALL compare the medicine's total remaining stock across all batches — each converted to the medicine's default dose unit per the "Stock unit conversion" requirement — to its projected weekly usage. When remaining stock is less than projected weekly usage and the medicine has a projected weekly usage at all, stock for that medicine SHALL be considered low. When a taken dose leaves stock low and the medicine's low-stock acknowledgement is unset, the app SHALL show a low-stock warning offering exactly two responses: "OK" and "I ordered new". Choosing "OK" SHALL leave the acknowledgement unset, so the next taken dose that still leaves stock low warns again. Choosing "I ordered new" SHALL set the acknowledgement, suppressing the warning for that medicine until a new stock batch is added, which clears it per the Add stock form's own requirement. The acknowledgement MUST NOT affect whether stock is considered low, only whether the warning is shown.

#### Scenario: First low-stock take warns
- **WHEN** a medicine's remaining stock falls below its projected weekly usage for the first time and its acknowledgement is unset
- **THEN** the low-stock warning is shown with "OK" and "I ordered new"

#### Scenario: OK does not suppress
- **WHEN** the user taps "OK" on the low-stock warning and later takes another dose that still leaves stock low
- **THEN** the low-stock warning is shown again

#### Scenario: I ordered new suppresses
- **WHEN** the user taps "I ordered new" and later takes another dose that still leaves stock low
- **THEN** no low-stock warning is shown

#### Scenario: New stock clears the suppression
- **WHEN** the user has suppressed the low-stock warning with "I ordered new" and then adds a new stock batch
- **THEN** the next taken dose that leaves stock low shows the warning again

#### Scenario: Sufficient stock shows no warning
- **WHEN** a taken dose leaves remaining stock at or above the medicine's projected weekly usage
- **THEN** no low-stock warning is shown, and any previously set acknowledgement is left as-is

### Requirement: Expiry-at-use warning
After consumption, the domain layer SHALL classify the batch FEFO drew from first into one of `NONE`, `APPROACHING` (within 30 days of its expiry date) or `PAST` (its expiry date has passed), relative to the current date. When the classification is `APPROACHING` or `PAST`, the app SHALL show an expiry-at-use warning naming that state, every time it recurs, with a single dismissal and no suppression: this warning has no acknowledgement state, unlike the low-stock warning, since only exhausting or replacing that batch changes the underlying fact.

#### Scenario: Drawn batch approaching expiry
- **WHEN** a taken dose is drawn from a batch expiring in 10 days
- **THEN** the expiry-at-use warning is shown stating stock expires soon

#### Scenario: Drawn batch past expiry
- **WHEN** a taken dose is drawn from a batch whose expiry date has passed
- **THEN** the expiry-at-use warning is shown stating the stock used has expired

#### Scenario: Drawn batch far from expiry
- **WHEN** a taken dose is drawn from a batch expiring in 90 days
- **THEN** no expiry-at-use warning is shown

#### Scenario: Recurs every time, never suppressed
- **WHEN** the user dismisses the expiry-at-use warning and later takes another dose drawn from the same still-approaching-expiry batch
- **THEN** the expiry-at-use warning is shown again, with no way to suppress it

### Requirement: Combined warning presentation
When a single taken dose triggers the low-stock warning, the expiry-at-use warning, or both, the app SHALL present them in one dialog rather than two, since both stem from the same take. When the app is in the foreground at the moment the dose is recorded, the dialog SHALL be shown immediately. When it is not — the dose was answered from the notification or from a wearable while the app was backgrounded — the warning SHALL be deferred and shown the next time the app is opened, evaluated fresh against the medicine's stock state at that moment rather than replayed from the original event. Multiple deferred takes for the same medicine SHALL collapse into a single warning reflecting current state when the app is next opened.

#### Scenario: Both warnings from one take
- **WHEN** a taken dose both leaves stock low and is drawn from an expiring batch
- **THEN** one dialog states both, with "OK" and "I ordered new" for the low-stock part and a single acknowledgement for the expiry part

#### Scenario: Foreground take warns immediately
- **WHEN** the app is open and the user answers "I took it" on the dose detail screen, triggering a warning
- **THEN** the warning dialog appears without leaving the current flow

#### Scenario: Backgrounded take defers the warning
- **WHEN** the user answers "I took it" from the reminder notification while the app is not open, triggering a warning
- **THEN** no dialog appears at that moment, and the warning appears the next time the app is opened

#### Scenario: Multiple deferred takes collapse
- **WHEN** two doses of the same medicine are answered "I took it" from notifications while the app is backgrounded, both leaving stock low
- **THEN** only one low-stock warning is shown the next time the app opens, reflecting the medicine's current state

### Requirement: No sensitive logging on the stock path
Stock consumption, the low-stock check and the expiry-at-use check MUST NOT log medicine names, dose amounts or stock quantities at info level or above in release builds.

#### Scenario: Log inspection
- **WHEN** the stock tracking package is inspected
- **THEN** no info, warning or error log statement includes a medicine name, a dose amount or a stock quantity

### Requirement: Stock tracking accessibility
Every element the Add stock form, the Stock section and the warning dialog introduce SHALL have a spoken label, every validation error and every advisory warning SHALL be announced when it appears, and each SHALL remain usable at the largest system font scale with no clipped or truncated text.

#### Scenario: Screen reader on the low-stock warning
- **WHEN** a screen reader focuses the low-stock warning dialog
- **THEN** it announces the warning text and both response labels, "OK" and "I ordered new"

#### Scenario: Largest font scale on the Add stock form
- **WHEN** the system font scale is at its largest setting and the Add stock form shows a past-expiry warning
- **THEN** every field, the warning and the save action are reachable by scrolling with no clipped text
