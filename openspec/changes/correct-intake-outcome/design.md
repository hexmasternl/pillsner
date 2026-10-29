# Design

## Context

See proposal.md for the motivation. The code is built on the first answer being final:

- **`AnswerDose`** (`domain/intake/AnswerDose.kt`) refuses any dose that is not pending ("an answer is not an edit, and the first one wins"). A taken answer writes the intake and deducts stock in one `TransactionRunner` transaction, then evaluates the stock warning after the commit.
- **`DoseDao.setIntake`** has no SQL guard. Overwriting is prevented only by the use case.
- **`ConsumeStockOnTaken.deduct`** runs `consumeFefo` and writes the new absolute `remaining` per batch (`StockBatchDao.applyConsumption`). It discards how much each batch gave and any shortfall. Nothing links a dose to the batches it drew from, and a removed batch is deleted outright.
- **The Usage history screen** (`ui/medicines/history/`) shows aggregates only, in a `verticalScroll` column. It reads `doseRepository.observeHistoryFor(medId, from, now)`, a Flow, so an outcome change re-emits by itself.
- **`MarkMissedDoses`** only selects pending doses (`outcome IS NULL`).
- **`DoseSyncPublisher`** publishes pending doses only.
- **The database** is at version 6.
- **The backup payload** is `PayloadV1`, which is never edited. A new format gets its own file.

## Goals / Non-Goals

**Goals:**
- Correct a past dose's outcome in a few taps, with stock and figures following exactly.
- Keep skipped and missed distinct, and keep the timing-accuracy chart honest.
- Leave the reminder path (`AnswerDose`, notifications, alarms, the watch) behaviourally unchanged, apart from recording stock draws.

**Non-Goals:**
- **Changing the dose detail screen.** A "Change answer" button there is a natural follow-up, and it can reuse the same use case and sheet.
- **Undo right after answering a reminder.** That is a separate follow-up.
- **Correcting to "missed" or back to "not answered".**
- **Choosing a taken time.**
- **Correcting doses older than 14 days.**
- **Bulk correction.**
- **Adding a menu item** to the medicine details overflow menu, which the in-flight `medicine-details-menu-shortcuts` change is editing.
- **A backup format version 2.**

## Decisions

### D1. The entry point is a Recent doses section on the Usage history screen
The issue asks for a button on the medicine details screen that opens a 14-day list.

- **Chosen:** a section on the existing Usage history screen, which is already the per-medicine place for looking back.
- **Why:** it adds no new destination and no new overflow item, so it doesn't collide with `medicine-details-menu-shortcuts`. It also puts a correction right next to the figures it changes.
- **Fixed window:** the list always covers 14 days, whatever period is selected. A 3-month list would be long, and corrections are about recent mistakes.
- **Alternatives:** a separate "Recent doses" screen behind a new overflow item adds navigation for no gain. Listing the selected period's doses would make the list up to about 360 rows long.
- **No per-row medicine name:** the issue asks for one on each row, but every row is the same medicine, already shown as the screen heading.

### D2. "I didn't take it" records skipped (open for review)
- **Two choices for the user:** the user picks between two plain answers, while the domain keeps three outcomes. A deliberate "I didn't take it" is a skip. Missed means the dose lapsed with nobody answering, which is untrue once the user has answered, even late.
- **Why skipped:** CLAUDE.md requires skipped and missed to stay distinct.
- **Alternative:** a third option, "I missed it". It was rejected because it asks the user to understand the difference on a small sheet.

### D3. "I took it" uses the scheduled moment (per the issue), and corrected doses are marked
- **Taken moment:** the issue asks for the scheduled moment. That is a guess, though, so a 0-minute deviation would flatter the timing chart.
- **New column:** `doses.corrected_at` (nullable instant), set on any correction that changes the outcome and kept on later corrections.
- **Effect on the timing chart:** `SummariseTimeDeviation` skips doses with `correctedAt != null` and outcome TAKEN.
- **Effect on the list:** the row says "Corrected" instead of a time and a difference.
- **Skipped moment:** for skipped, `recorded_at` becomes the correction moment, matching what "recorded" means for a skip.
- **Domain model:** `Intake` gains `correctedAt: Instant?`. This leaves all existing constructors source-compatible through a default of null.

### D4. Exact stock return via a `stock_draws` ledger
**The new table:**
- Columns: `stock_draws(dose_id, batch_id, amount_value TEXT, amount_unit TEXT)`, with primary key `(dose_id, batch_id)`.
- Both foreign keys cascade on delete, so purging a dose or removing a batch drops its draws.
- `amount_value` and `amount_unit` are stored in the batch's own unit, as decimal strings like `stock_batches.remaining_value`.

**What changes:**
- `consumeFefo` already knows what each batch gave. `StockConsumption` exposes it, including whole-pill round-up.
- `ConsumeStockOnTaken.deduct(doseId, …)` writes the draws in the same transaction.
- **Return:** the correction adds each draw back to its batch (`StockBatchDao.addToRemaining(batchId, amount)`, converting nothing because units match by construction), then deletes the draws.
- **Missing draws:** a draw whose batch is gone was already removed by the cascade. A dose with no draws returns nothing.

**Alternatives considered:**
- *Reverse FEFO without a ledger:* add the dose amount back to the batch that FEFO would draw from next. This is wrong whenever the exact-fit rule, whole-pill rounding, a spanning dose or a shortfall applied, and it can put stock into a batch the dose never touched.
- *No stock return at all:* this leaves the user with a wrong count and no way to fix it, because the app has no stock-edit action.

**Doses taken before this change:** they have no draws. Within 14 days of the update, every correctable taken dose is covered.

### D5. A guarded correction use case, separate from `AnswerDose`
`CorrectIntake(doseId, choice: TOOK_IT | DID_NOT_TAKE_IT, expected: IntakeOutcome)` lives in `domain/intake/`.

**Inside one transaction it:**
1. Re-reads the dose. It refuses with `Changed` if the dose is pending, outside the 14-day window, or its outcome is no longer `expected`. It returns `Unchanged` if the choice maps to the current outcome.
2. Calls `DoseRepository.correctIntake(id, expected, outcome, at, correctedAt)`. This is backed by `UPDATE doses SET outcome=?, recorded_at=?, corrected_at=? WHERE id=? AND outcome=?`, which returns the number of rows changed. If zero rows changed, the use case returns `Changed`.
3. Adjusts stock:
   - from taken: return the draws;
   - to taken: `consumeStockOnTaken.deduct`.

**After the commit,** for a correction to taken, it calls `flagWarning` as `AnswerDose` does.

**Why separate:** `AnswerDose`'s "first answer wins" is the right rule for reminders, the watch bridge and the future widget, and must stay so. Correction is a deliberate, guarded edit with its own rules.

**Side effects:** no `onAnswered`, no reminder wake and no notification cancel. A dose with an outcome has no notification, alarm or wear presence.

### D6. UI shape
- **List:** the Recent doses section is a plain `Column` of rows inside the screen's existing `verticalScroll`. At most a few dozen rows, so no `LazyColumn` nesting is needed.
- **Chip:** each row reuses the intake state colour roles and icons from the breakdown legend.
- **Sheet:** the correction sheet follows the label-scan `ModalBottomSheet` styling (`surfaceContainerHigh`). It uses a `selectableGroup` of two radio rows and a filled "Save" button.
- **State:** the view model holds the sheet in its state as `CorrectionSheet(doseId, expected, choice)?`. A `SavedStateHandle` keeps the open sheet's dose id and choice across process death.
- **Results:** `Changed` shows a snackbar ("This dose was updated in the meantime") and closes the sheet, and the list re-reads by itself.
- **Data source:** the view model reads the 14-day window with a second `observeHistoryFor` collection, independent of the period.
- **Design system:** all UI goes through `pillsner-ui-build` and is checked with `pillsner-ui-review`.

### D7. Backup stays at format 1
- **Draws:** they are device-only bookkeeping, and the backup has no batch ids to point them at. After an import, earlier doses correct without returning stock, the same as doses taken before this change.
- **`corrected_at`:** it is not exported either. After an import, a dose corrected to taken counts in the timing chart again.
- **Why accept this:** it is minor and rare, and adding the field means cutting backup format 2. That is better done when the backup next needs a format change.

## Risks / Trade-offs

- **[Risk] Every take now writes draw rows, so the migration and this extra write touch the core reminder path.** → Mitigation: same transaction as today, `AnswerDoseTest` and `ConsumeStockOnTakenTest` extended, the migration test proves existing data survives, and the safety-set emulator tests run.
- **[Risk] The correction races a reminder answer or the missed check.** → Mitigation: only non-pending doses can be corrected, so `AnswerDose` and `MarkMissedDoses` never compete for the same row. The compare-and-set update rejects a stale sheet.
- **[Risk] A deduction on correction-to-taken happens days after the real intake, so FEFO picks today's batches rather than the batches of that day.** → Mitigation: this is accepted. The physical stock was never deducted, and FEFO today is the best estimate.
- **[Trade-off] Doses taken before the update, or restored from a backup, don't return stock.** This is stated in the spec.
- **[Trade-off] The dose detail screen still says the outcome is final.** The follow-up can reuse `CorrectIntake`.

## Migration Plan

- **`MIGRATION_6_7`:**
  - `ALTER TABLE doses ADD COLUMN corrected_at INTEGER` (nullable; the same instant encoding as `recorded_at`).
  - `CREATE TABLE stock_draws (...)` with both foreign keys and an index on `batch_id`.
- **Supporting changes:** add the migration to `Migrations.ALL`, bump `PillsnerDatabase.VERSION` to 7, and commit `schemas/.../7.json`.
- **Test:** a `PillsnerDatabaseMigrationTest` case from version 6 with taken, skipped and missed doses and batches. Everything survives, `corrected_at` is null and `stock_draws` is empty.
- **Reset and purge:**
  - App reset deletes doses and batches, and the draws cascade.
  - The history purge deletes doses, and their draws cascade.
- **Rollback:** an older app cannot open version 7, as with every schema bump. That is the normal forward-only policy.
