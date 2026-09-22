## ADDED Requirements

### Requirement: Stock batch model
The domain layer SHALL define a `StockBatch` with an identifier, the medication it belongs to, a remaining amount as a `Quantity`, an expiry date, and the moment it was added. Domain types MUST NOT depend on Android framework classes. A stock batch's `Quantity` unit SHALL match its medication's default dose unit; the Add stock form fixes the unit to the medication's current default dose unit rather than letting the user choose it, so no part of this capability needs to convert between units.

#### Scenario: Batch belongs to one medicine
- **WHEN** a stock batch is created for a medicine
- **THEN** it carries that medicine's identifier and no other medicine can reference it

#### Scenario: Unit matches the medicine
- **WHEN** a medicine's default dose unit is millilitre and the user adds a stock batch
- **THEN** the batch's amount is recorded in millilitres, with no unit choice offered

### Requirement: A medicine with no stock recorded is unaffected
A medicine with zero stock batches SHALL behave exactly as it did before this capability existed: no consumption, no low-stock check, no expiry-at-use check, and no stock indicator anywhere. The capability activates for a medicine the moment its first stock batch is added, and only for that medicine.

#### Scenario: No batches, no effect
- **WHEN** a medicine has never had a stock batch added and a dose of it is recorded taken
- **THEN** no consumption occurs, no stock warning is shown, and no stock indicator appears anywhere for that medicine

#### Scenario: Activates on first batch
- **WHEN** the user adds the first stock batch to a medicine that previously had none
- **THEN** the next dose of that medicine recorded taken is deducted from that batch and the medicine becomes subject to the low-stock and expiry-at-use checks

### Requirement: Add stock form
The Medicine details screen's Stock section SHALL offer an "Add stock" form with two fields: a quantity, a decimal number greater than zero parsed using the device locale in the medicine's default dose unit (shown read-only), and an expiry date, required, chosen with a date picker. An expiry date already in the past SHALL be accepted with a non-blocking warning stating so, never rejected. Saving a valid batch SHALL add it to the medicine's stock in a single write and SHALL clear the medicine's low-stock acknowledgement, regardless of whether the new batch actually restores sufficiency.

#### Scenario: Valid batch
- **WHEN** the user enters "30" and selects an expiry date six months away for a medicine dosed in tablets
- **THEN** a batch of 30 tablets with that expiry date is added to the medicine's stock

#### Scenario: Zero or negative quantity rejected
- **WHEN** the user enters "0" and attempts to save
- **THEN** the batch is not saved and the quantity field shows an error stating the amount must be greater than zero

#### Scenario: Past expiry date accepted with a warning
- **WHEN** the user selects an expiry date that has already passed
- **THEN** the form shows a non-blocking warning stating the date is in the past, and saving still succeeds

#### Scenario: Adding stock clears a standing acknowledgement
- **WHEN** a medicine's low-stock warning was previously suppressed with "I ordered new" and the user adds a new stock batch, even one too small to restore sufficiency
- **THEN** the medicine's acknowledgement is cleared and the next insufficient take warns again

### Requirement: First-expiry-first-out consumption
When a dose is recorded as taken, for a medicine with at least one stock batch, the dose's amount SHALL be deducted from that medicine's stock in the same transaction as the intake write, always drawing first from the batch with the earliest expiry date, then the next earliest, and so on, breaking a tie between batches with the same expiry date by the order they were added. A batch already at zero remaining SHALL be skipped, never taken below zero. If total remaining stock is less than the dose's amount, the deduction SHALL floor at zero across all batches rather than go negative. A batch that reaches zero remaining SHALL be kept, not deleted, so the medicine continues to count as having stock recorded.

#### Scenario: Single batch covers the dose
- **WHEN** a medicine has one batch of 30 tablets and a dose of 1 tablet is recorded taken
- **THEN** the batch's remaining amount becomes 29 tablets

#### Scenario: Consumption spans two batches
- **WHEN** a medicine has a batch expiring sooner with 1 tablet remaining and a batch expiring later with 30 tablets remaining, and a dose of 2 tablets is recorded taken
- **THEN** the sooner-expiring batch is exhausted to 0 and the later-expiring batch's remaining amount becomes 29 tablets

#### Scenario: Exhausted batch is kept, not removed
- **WHEN** a batch is consumed down to exactly 0 remaining
- **THEN** its row still exists with 0 remaining, and the medicine still counts as having stock batches recorded

#### Scenario: Tie broken by add order
- **WHEN** a medicine has two batches with the same expiry date, added at different times, and a dose is recorded taken
- **THEN** the batch that was added first is deducted from before the other

#### Scenario: Insufficient total stock floors at zero
- **WHEN** a medicine's total remaining stock is 1 tablet and a dose of 2 tablets is recorded taken
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
After consumption, the domain layer SHALL compare the medicine's total remaining stock across all batches to its projected weekly usage. When remaining stock is less than projected weekly usage and the medicine has a projected weekly usage at all, stock for that medicine SHALL be considered low. When a taken dose leaves stock low and the medicine's low-stock acknowledgement is unset, the app SHALL show a low-stock warning offering exactly two responses: "OK" and "I ordered new". Choosing "OK" SHALL leave the acknowledgement unset, so the next taken dose that still leaves stock low warns again. Choosing "I ordered new" SHALL set the acknowledgement, suppressing the warning for that medicine until a new stock batch is added, which clears it per the Add stock form's own requirement. The acknowledgement MUST NOT affect whether stock is considered low, only whether the warning is shown.

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
