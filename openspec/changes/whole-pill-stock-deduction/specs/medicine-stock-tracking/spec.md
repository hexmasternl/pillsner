## ADDED Requirements

### Requirement: Whole-pill units
The domain layer SHALL treat `TABLET` and `CAPSULE` as whole-pill units: a stock batch counted in one of them holds and loses only whole units, because a pill cannot be returned to stock once any part of it has been used. Every other unit (`MILLIGRAM`, `GRAM`, `MICROGRAM`, `MILLILITRE`, `DROP`, `PUFF`, `UNIT`) SHALL keep exact decimal arithmetic. A whole-pill batch's usable remaining amount SHALL be its recorded remaining amount rounded down to a whole number. Everything that reads a batch's remaining stock SHALL use the usable remaining amount: consumption, the low-stock check, the tile and details-screen heads-up, and the Stock section. A fractional amount left over in a whole-pill batch from before this requirement existed SHALL be rounded down and written back as a whole number the next time a dose is drawn from that batch, without a schema migration.

#### Scenario: Tablet batch is a whole-pill batch
- **WHEN** a stock batch is counted in tablets or capsules
- **THEN** its remaining amount only ever changes by whole units

#### Scenario: Liquid batch keeps exact arithmetic
- **WHEN** a medicine dosed in millilitres has a 300 ml batch and a 15 ml dose is recorded taken
- **THEN** the batch's remaining amount becomes exactly 285 ml

#### Scenario: Legacy fractional remainder is normalised on next draw
- **WHEN** a tablet batch holds 49.92 tablets, recorded before this requirement existed, and a dose that deducts 1 tablet is drawn from it
- **THEN** the batch's remaining amount becomes 48 tablets

#### Scenario: Legacy fractional remainder is displayed as whole pills
- **WHEN** a tablet batch holds 49.92 tablets, recorded before this requirement existed, and no dose has been drawn from it since
- **THEN** the Stock section and every stock comparison treat it as 49 tablets

## MODIFIED Requirements

### Requirement: Stock unit conversion (strength)
Wherever the stock system compares or totals quantities across batches, it SHALL work in the medicine's default dose unit, converting each batch's usable remaining amount by multiplying it by that batch's `strengthPerUnit`. This covers the low-stock check, the tile and details-screen heads-up, and first-expiry-first-out consumption itself. A batch whose unit already matches the medicine's default dose unit (strength 1) needs no conversion. A medicine whose stock and doses share one continuous unit throughout SHALL therefore behave exactly as plain subtraction and comparison in that unit. When a dose amount is converted into a whole-pill batch's own unit (see "Whole-pill units"), the result SHALL be rounded **up** to the next whole pill, because any part of a pill used counts as the whole pill used. For every other batch, the conversion SHALL stay exact decimal division.

#### Scenario: Same unit throughout needs no conversion
- **WHEN** a medicine is dosed in millilitres, has 300 ml of stock and a 15 ml dose is recorded taken
- **THEN** the batch's remaining amount becomes 285 ml, computed as plain subtraction

#### Scenario: Tablets converted against a dose in milligrams
- **WHEN** a medicine is dosed in milligrams with a 40 mg schedule, has one batch of 20 tablets each worth 20 mg, and a dose is recorded taken
- **THEN** 2 tablets are deducted from the batch, leaving 18, because 40 mg ÷ 20 mg-per-tablet is 2 tablets

#### Scenario: A dose smaller than one tablet uses the whole tablet
- **WHEN** a medicine is dosed in milligrams with a 40 mg schedule, has one batch of 50 tablets each worth 500 mg, and a dose is recorded taken
- **THEN** 1 tablet is deducted, leaving 49, because the partly used tablet counts as used

#### Scenario: A dose that does not divide evenly rounds up to whole tablets
- **WHEN** a medicine is dosed in milligrams with a 25 mg schedule and has one batch of tablets each worth 20 mg
- **THEN** 2 tablets are deducted, because 25 mg ÷ 20 mg-per-tablet is 1.25 tablets, rounded up

#### Scenario: A fractional tablet dose uses the whole tablet
- **WHEN** a medicine is dosed in tablets with a 0.5 tablet schedule, has one batch of 30 tablets, and a dose is recorded taken
- **THEN** 1 tablet is deducted, leaving 29

#### Scenario: Low-stock comparison converts every batch
- **WHEN** a medicine dosed in milligrams has one batch of 100 tablets worth 5 mg each (500 mg total) and a projected weekly usage of 490 mg
- **THEN** its stock is not considered low, because 500 mg of remaining stock, once converted, covers the projected 490 mg

#### Scenario: FEFO conversion applies per batch it draws from
- **WHEN** a medicine is dosed in milligrams and has two batches: one expiring sooner, worth 10 mg per tablet with 1 tablet remaining, and one expiring later, in the medicine's own milligram unit, with 30 mg remaining. A 20 mg dose is then recorded taken.
- **THEN** the sooner-expiring batch is exhausted (its one 10 mg tablet is fully used) and the remaining 10 mg owed is drawn from the later batch, leaving it at 20 mg

#### Scenario: Rounding up applies to what is still owed per batch
- **WHEN** a medicine is dosed in milligrams with a 25 mg schedule and has two batches of 20 mg tablets: one expiring sooner with 1 tablet remaining, and one expiring later with 10 tablets. A dose is then recorded taken.
- **THEN** the sooner batch is exhausted (20 mg), and the 5 mg still owed takes 1 whole tablet from the later batch, leaving it at 9

#### Scenario: Default dose unit is locked while stock is recorded
- **WHEN** a medicine has at least one stock batch and the user changes its default dose unit on the Medicine details form
- **THEN** the form shows an error on the dose field straight away and does not save until the unit is changed back or every stock batch is removed, since each batch's strength is relative to that unit

### Requirement: First-expiry-first-out consumption
When a dose is recorded as taken for a medicine with at least one stock batch, the dose's amount (in the medicine's default dose unit) SHALL be deducted from that medicine's stock in the same transaction as the intake write. Deduction SHALL always draw first from the batch with the earliest expiry date, then the next earliest, and so on. A tie between batches with the same expiry date SHALL be broken by the order they were added. Each batch's share SHALL be converted through that batch's own strength and rounding, per the "Stock unit conversion" requirement.

There is one exception, the exact-fit preference. It applies when the batch first-expiry-first-out would draw from first is a whole-pill batch, and covering the dose from it would use part of a pill, meaning the dose is not a whole multiple of its strength. In that case, if another batch with enough usable remaining stock can cover the whole dose without using part of a pill, the dose SHALL be drawn entirely from that batch. If more than one batch qualifies, the one with the earliest expiry date SHALL be used, and a tie SHALL go to the batch added first.

A batch with no usable remaining stock SHALL be skipped and never taken below zero. If total usable remaining stock, converted to the medicine's default dose unit, is less than the dose's amount, the deduction SHALL floor at zero across all batches rather than go negative. A batch that reaches zero remaining SHALL be kept, not deleted, so the medicine continues to count as having stock recorded.

#### Scenario: Single batch covers the dose
- **WHEN** a medicine has one batch of 30 tablets (strength 1, dosed in tablets) and a dose of 1 tablet is recorded taken
- **THEN** the batch's remaining amount becomes 29 tablets

#### Scenario: Consumption spans two batches
- **WHEN** a medicine has two batches, both strength 1 and dosed in tablets: one expiring sooner with 1 tablet remaining, and one expiring later with 30 tablets remaining. A dose of 2 tablets is then recorded taken.
- **THEN** the sooner-expiring batch is exhausted to 0 and the later-expiring batch's remaining amount becomes 29 tablets

#### Scenario: Exact-fit batch is preferred over wasting a pill
- **WHEN** a medicine is dosed in milligrams with a 40 mg schedule and has two batches: 50 tablets of 500 mg expiring sooner, and 30 tablets of 40 mg expiring later. A dose is then recorded taken.
- **THEN** 1 tablet is deducted from the 40 mg batch, leaving 29, and the 500 mg batch is left at 50

#### Scenario: No exact fit falls back to first-expiry-first-out
- **WHEN** a medicine is dosed in milligrams with a 40 mg schedule and has two batches: 50 tablets of 500 mg expiring sooner, and 30 tablets of 300 mg expiring later. A dose is then recorded taken.
- **THEN** 1 tablet is deducted from the sooner-expiring 500 mg batch, leaving 49, and the 300 mg batch is unchanged

#### Scenario: Exact-fit batch without enough stock is not preferred
- **WHEN** a medicine is dosed in milligrams with an 80 mg schedule and has two batches: 50 tablets of 500 mg expiring sooner, and 1 tablet of 40 mg expiring later. A dose is then recorded taken.
- **THEN** the 40 mg batch cannot cover the dose alone, so 1 tablet is deducted from the sooner-expiring 500 mg batch, leaving 49, and the 40 mg batch is unchanged

#### Scenario: Exhausted batch is kept, not removed
- **WHEN** a batch is consumed down to exactly 0 remaining
- **THEN** its row still exists with 0 remaining, and the medicine still counts as having stock batches recorded

#### Scenario: Tie broken by add order
- **WHEN** a medicine has two batches with the same expiry date, added at different times, and a dose is recorded taken
- **THEN** the batch that was added first is deducted from before the other

#### Scenario: Insufficient total stock floors at zero
- **WHEN** a medicine's total usable remaining stock, converted to its default dose unit, is 1 mg and a dose of 2 mg is recorded taken
- **THEN** every batch ends at 0 remaining and no batch or total goes negative

#### Scenario: Skip and missed never consume stock
- **WHEN** a dose is recorded skipped, or lapses as missed
- **THEN** no stock batch's remaining amount changes

#### Scenario: Same recording path regardless of surface
- **WHEN** a taken dose is recorded from the dose detail screen, from the reminder notification, or from a paired wearable
- **THEN** stock consumption happens identically in each case, since all three answer through the same recording path

### Requirement: Weekly usage projection
The domain layer SHALL define a pure function that projects a medicine's usage for the 7 calendar days starting today, in a given time zone. It SHALL take every dose the existing dose generator (`dose-records`) would produce for that medicine over that window and run them, in order, through the "First-expiry-first-out consumption" deduction against a copy of the medicine's stock batches. Projected weekly usage SHALL be the total those simulated deductions would take, in the medicine's default dose unit, including every whole pill the rounding uses. Any amount the copied batches could no longer cover SHALL be added at its full dose amount. The simulation SHALL NOT write to any stock batch. A medicine with no schedules (as-needed) SHALL have no projected weekly usage.

#### Scenario: Twice-daily medicine
- **WHEN** a medicine has one schedule of 1 tablet twice a day
- **THEN** its projected weekly usage is 14 tablets

#### Scenario: Weekday-only medicine
- **WHEN** a medicine has one schedule of 1 tablet on Monday, Wednesday and Friday, and the 7-day window contains exactly those three weekdays once each
- **THEN** its projected weekly usage is 3 tablets

#### Scenario: Whole-pill rounding counts towards the projection
- **WHEN** a medicine is dosed in milligrams with a 40 mg daily schedule and its only stock is a batch of 500 mg tablets
- **THEN** its projected weekly usage is 3,500 mg (7 tablets), not 280 mg

#### Scenario: As-needed medicine has no projection
- **WHEN** a medicine has no schedules
- **THEN** it has no projected weekly usage and is exempt from the low-stock check

### Requirement: Low-stock warning and its acknowledgement
After consumption, the domain layer SHALL compare the medicine's total usable remaining stock across all batches to its projected weekly usage. Each batch SHALL be converted to the medicine's default dose unit per the "Stock unit conversion" requirement, and the projection SHALL follow the "Weekly usage projection" requirement, including whole-pill rounding. Stock for a medicine SHALL be considered low when the medicine has a projected weekly usage and its remaining stock is less than that usage.

When a taken dose leaves stock low and the medicine's low-stock acknowledgement is unset, the app SHALL show a low-stock warning offering exactly two responses: "OK" and "I ordered new". Choosing "OK" SHALL leave the acknowledgement unset, so the next taken dose that still leaves stock low warns again. Choosing "I ordered new" SHALL set the acknowledgement. This suppresses the warning for that medicine until a new stock batch is added, which clears the acknowledgement per the Add stock form's own requirement. The acknowledgement MUST NOT affect whether stock is considered low, only whether the warning is shown.

#### Scenario: First low-stock take warns
- **WHEN** a medicine's remaining stock falls below its projected weekly usage for the first time and its acknowledgement is unset
- **THEN** the low-stock warning is shown with "OK" and "I ordered new"

#### Scenario: Whole-pill rounding makes stock low sooner
- **WHEN** a medicine is dosed in milligrams with a 40 mg daily schedule, its only stock is 6 tablets of 500 mg, and its acknowledgement is unset
- **THEN** its stock is considered low, because 6 tablets cannot cover the 7 tablets the next week's doses would use

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

### Requirement: Add stock form
The Medicine details screen's Stock section SHALL offer an "Add stock" form with four fields:

- **Quantity**: a decimal number greater than zero, parsed using the device locale. When the chosen unit is a whole-pill unit (see "Whole-pill units"), the quantity SHALL be a whole number.
- **Unit**: chosen from the same fixed unit list the rest of the app uses, defaulting to the medicine's own default dose unit.
- **Strength**: shown only when the chosen unit differs from the medicine's default dose unit. It is a decimal number greater than zero, labelled with both units so the direction of the conversion is unambiguous (for example "mg per tablet").
- **Expiry date**: required, chosen with a date picker.

When the chosen unit matches the medicine's default dose unit, the strength field SHALL NOT be shown and the batch's strength SHALL be recorded as 1. An expiry date already in the past SHALL be accepted with a non-blocking warning stating so, never rejected. Saving a valid batch SHALL add it to the medicine's stock in a single write. It SHALL also clear the medicine's low-stock acknowledgement, whether or not the new batch actually restores sufficiency.

#### Scenario: Valid batch, unit matches the dose
- **WHEN** the user enters "300" and unit "millilitre" for a medicine dosed in millilitres, with no strength field shown, and selects an expiry date six months away
- **THEN** a batch of 300 millilitres with a strength of 1 and that expiry date is added to the medicine's stock

#### Scenario: Valid batch, unit differs from the dose
- **WHEN** a medicine is dosed in milligrams and the user enters quantity "20", unit "tablet", strength "20" (mg per tablet), and an expiry date six months away
- **THEN** a batch of 20 tablets with a strength of 20 and that expiry date is added to the medicine's stock

#### Scenario: Fractional pill quantity rejected
- **WHEN** the user chooses unit "tablet" or "capsule", enters "20.5" and attempts to save
- **THEN** the batch is not saved and the quantity field shows an error stating the amount must be a whole number of tablets or capsules

#### Scenario: Fractional quantity accepted for a continuous unit
- **WHEN** the user chooses unit "millilitre", enters "150.5" and saves with a valid expiry date
- **THEN** a batch of 150.5 millilitres is added to the medicine's stock

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
