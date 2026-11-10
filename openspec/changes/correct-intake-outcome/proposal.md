# Proposal

## Why

**GitHub Issue:** #112 (https://github.com/hexmasternl/pillsner/issues/112)

A reminder is answered with one tap, and a tap can land on the wrong button. Today the first answer is final: the dose detail screen stops offering answers once a dose has an outcome, and the Usage history screen is read-only. A mis-tap therefore leaves a record that contradicts what really happened, and it skews the adherence figures, the stock count and anything shared from them. A missed dose the user did take, but forgot to confirm, has the same problem.

## What Changes

- The Usage history screen gains a **Recent doses** section listing every dose of the medicine scheduled in the last 14 days, newest first. Each row shows the scheduled date and time, the amount, the outcome as a labelled chip (Taken, Skipped, Missed or Not answered yet) and, for a taken dose, when it was taken and how far that was from the scheduled time.
- Tapping a row with a recorded outcome opens a bottom sheet with two choices, **I took it** and **I didn't take it**, and a **Save** button. Saving corrects the dose's outcome. A dose that has not been answered yet is listed but cannot be corrected from here; it is still answered from its reminder or the dose detail screen.
- Correction rules:
  - "I took it" records taken, with the scheduled moment as the taken moment, and uses up stock exactly as a normal take does (including the low-stock and expiry warnings).
  - "I didn't take it" records **skipped**, not missed. Missed stays reserved for a dose that lapsed unanswered.
  - Correcting a taken dose returns to stock what that dose drew, batch by batch, where the app recorded it. Doses taken before this change shipped, and draws from a batch that has since been removed, leave stock unchanged.
  - A corrected dose is marked as corrected. A dose corrected to taken has no real taken time, so it is left out of the timing-accuracy chart and its row shows no time difference.
- Taking a dose now records which stock batches it drew from and how much. This is the bookkeeping that makes the stock return exact.
- **BREAKING (spec):** the Usage history screen is no longer read-only. The correction sheet is its one writing action.

## Capabilities

### New Capabilities

- `intake-correction`: the rules for changing a recorded intake outcome after the fact, covering which doses can be corrected, what each choice records, how stock is used up or returned, how the dose is marked as corrected, and what is not affected (reminders, the watch, the missed rule).

### Modified Capabilities

- `medicine-usage-history`:
  - The screen is no longer read-only.
  - It gains the Recent doses list and the correction sheet.
  - The timing-accuracy chart excludes doses corrected to taken.
  - The chart value-axis requirement no longer says the screen is read-only.

## Impact

- **Domain:**
  - a new correction use case next to `AnswerDose` in `domain/intake/`;
  - `ConsumeStockOnTaken` and `FefoConsumption` report what each batch gave up;
  - the dose model gains a corrected marker;
  - `SummariseTimeDeviation` skips corrected doses.
- **Data:**
  - Room schema version 6 → 7 adds a nullable `corrected_at` column to `doses` and a `stock_draws` table (dose → batch → amount), with a migration and a migration test.
  - `DoseDao` gains a guarded update that only changes a dose whose outcome is still what the user saw.
  - `StockBatchDao` gains a way to add an amount back to a batch.
- **UI:** `ui/medicines/history/`, covering the screen, view model and state, a new Recent doses section and the correction sheet. New strings go in all six locales.
- **Not affected:**
  - Reminders, alarms, notifications and the wear sync. A correction only ever touches a dose that is no longer pending, and the watch only shows pending doses.
  - The medicine details overflow menu, so there is no overlap with `medicine-details-menu-shortcuts`.
  - The dose detail screen.
  - The backup format. Draws and the corrected marker are device-only bookkeeping and are not exported, so the format stays at version 1.
- **No new dependency, no network access.**
