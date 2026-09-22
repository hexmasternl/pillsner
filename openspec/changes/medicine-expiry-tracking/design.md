## Context

Medicines are stored in Room with name, default dose, used since/until, prescriber and their schedules (`medicine-add`, `medicine-details`, `medication-schedule-model`). Doses are generated from schedules and recorded as taken, skipped or missed through one shared code path used identically by the dose detail screen, the reminder notification and a paired wearable (`dose-detail`, `medicine-reminders`, `dose-records`). There is no concept of remaining stock anywhere today.

This design supersedes an earlier draft of this same change that added a single, per-medicine, informational expiry date with explicit non-goals of "no stock/remaining-quantity tracking" and "no per-batch expiry." Both are reversed here at the user's direction: expiry only matters in relation to a specific batch of stock, and stock only matters if the app knows how much is used and how fast.

This design deliberately does not modify the `dose-detail`, `medicine-reminders` or `dose-records` specs. Those capabilities already fully own how an outcome is recorded; this change hooks into that existing, shared recording path as a side effect for a "taken" outcome, the same way `medicine-usage-history` reads dose data without modifying `dose-records`.

## Goals / Non-Goals

**Goals:**
- Let the user record one or more stock batches per medicine, each an amount + unit and an expiry date.
- Deduct a taken dose's amount from stock automatically, always from the batch expiring soonest first (FEFO).
- Warn when remaining stock would not cover the next 7 days of scheduled usage, every time a dose is taken while that holds, until either resolved by fresh stock or explicitly acknowledged as ordered.
- Warn, separately, when the batch actually drawn from is within 30 days of, or past, its expiry date.
- Do all of the above only for a medicine that has at least one stock batch; leave every other medicine's behaviour completely unchanged.

**Non-Goals:**
- No manual editing or removal of a stock batch once added — only automatic FEFO consumption changes a batch's remaining amount. Manual correction is left as a future proposal if requested.
- No configurable low-stock lead time (fixed at 7 days) or expiry lead time (fixed at 30 days, same constant the superseded draft chose) — avoids a new settings surface for v1.
- No unit conversion: a stock batch's unit must match the medicine's default dose unit; the app does not convert between units (e.g. ml to mg).
- No initial-stock fields on the Add medicine form. Stock is always added afterwards from the Medicine details screen's new Stock section, including for a medicine just created — this keeps the Add flow's atomic save exactly as it is today and puts all stock behaviour in one place.
- No barcode/label scanning to read a batch's expiry date (tracked separately as issue #33's OCR proposal).
- No medical judgement about whether an expired medicine is still safe to take.
- No change to how doses are generated, reminded or missed. Stock is read, never a scheduling input.
- No wearable-surfaced stock warning; the warning is a phone-app dialog only (see Decisions).

## Decisions

- **Data model**: add a `StockBatch` domain type (`id`, `medicationId`, `remaining: Quantity`, `expiryDate: LocalDate`, `addedAt: Instant`) and a Room `stock_batches` table (FK to `medications`, cascade delete — moot in practice since medications are never deleted outside the full app reset, but consistent with how `schedules` cascades). Add a nullable `lowStockAcknowledgement` enum column to `medications` (`ACKNOWLEDGED_ORDERED` or unset). Both ship in one schema version bump with one migration and one migration test, per `medication-persistence`.

- **Unit consistency enforced at entry, not at consumption**: the Add stock form fixes the batch's unit to the medicine's current default dose unit (shown read-only, not chosen by the user), so the consumption function can assume every batch of a medicine shares one unit and never needs to convert.

- **Feature gating**: a medicine is under stock tracking if and only if it has at least one `stock_batches` row, regardless of that row's remaining amount. A medicine with none behaves exactly as before this change in every respect — no consumption, no projection, no warning, no tile heads-up.

- **Batches are never deleted by consumption**: when FEFO consumption exhausts a batch to zero remaining, the row is kept at zero rather than removed. This is deliberate: if exhausted batches were deleted, a medicine that runs completely out (last batch consumed to zero) would revert to "no stock recorded" and silently stop warning the user at the exact moment the warning matters most. Keeping the zero row means the gate in the previous decision stays satisfied and the low-stock check keeps firing every time the medicine is taken with nothing left.

- **FEFO consumption**: a pure function orders a medicine's batches by `expiryDate` ascending, then `addedAt` ascending to break ties, and deducts the taken dose's amount across them in that order, never taking a batch below zero and skipping batches already at zero. It reports the total actually deducted (which may be less than requested if total stock is insufficient — deducted stock simply floors at zero across all batches, there is no negative stock) and which batch it drew from first (used for the expiry-at-use check below). This runs in the same transaction that records the intake, so a dose is never recorded taken without its stock effect, and never partially.

- **Weekly sufficiency**: after consumption, sum every batch's remaining amount for the medicine and compare it against a **projected weekly usage**: the total amount of doses the existing dose generator (`dose-records`' "Dose generation from schedules" requirement) produces for that medicine over the 7 calendar days starting today, in the device time zone. This reuses already-specified, already-tested schedule-expansion logic instead of inventing a second rate calculation, and it automatically handles every-N-days anchoring, weekday schedules and every-N-hours schedules the same way the rest of the app does. A medicine with no schedules (as-needed) has no projected weekly usage and is exempt from the low-stock check entirely — there is no reliable rate to project.

- **Low-stock acknowledgement is per medicine, not per event**: `lowStockAcknowledgement` is unset by default. Whenever a taken dose leaves stock insufficient for the week and the flag is unset, the low-stock warning is shown; tapping "OK" leaves the flag unset (so the same check will warn again next time); tapping "I ordered new" sets it. While set, the low-stock check still runs (so the tile heads-up and the details screen still reflect reality) but the warning dialog is not shown. Adding any new stock batch — regardless of amount or whether it actually restores sufficiency — always clears the flag back to unset, per the user's own description of the suppression.

- **Expiry-at-use is independent of the low-stock acknowledgement**: after consumption, the batch FEFO drew from first is classified `NONE` / `APPROACHING` (within 30 days) / `PAST` using the same three-state shape the superseded draft defined, now evaluated per batch instead of per medicine. `APPROACHING` or `PAST` always shows the expiry-at-use warning on that take, with no suppression mechanism — only using up or replacing that batch changes the underlying fact, so there is nothing sensible to acknowledge away.

- **Warning presentation**: when a taken dose triggers the low-stock warning, the expiry-at-use warning, or both, the app shows one dialog listing whichever apply, since both stem from the same take. When the app was not in the foreground at the moment of recording (the dose was answered from the notification, or from a wearable), the warning is not lost: it is shown the next time the app is opened, evaluated fresh against the medicine's stock state at that later moment rather than replayed as a stale event. If several such takes happen while the app is backgrounded, they collapse into a single warning per medicine when the app is next opened, since only the current state, not the history of events, is meaningful to the user.

- **Tile and details-screen heads-up is a live read, not an event**: unlike the warning dialog, the Medicines screen tile and the Medicine details screen's Stock section always reflect the medicine's *current* stock and expiry state, recomputed whenever they are shown, independent of whether or when a dose was last taken. This mirrors `medicine-overview`'s existing "Overview updates live" requirement.

- **Add stock form**: quantity (decimal, greater than zero, unit fixed to the medicine's dose unit) and expiry date (required, date picker). An expiry date already in the past is accepted with a non-blocking warning, consistent with the advisory-warning pattern `dose-detail` already uses elsewhere in the app, rather than being rejected outright — a user may be entering stock they already know is expired.

## Risks / Trade-offs

- [Keeping zero-remaining batches means a long-lived medicine can accumulate many exhausted rows] → Acceptable: rows are small and the count is bounded by how many times a user restocks a medicine over the app's lifetime, not by dose frequency.
- [A fixed 7-day and 30-day window may not suit every medicine] → Same trade-off the superseded draft accepted for its 30-day window; revisit if user feedback asks for configurability.
- [Deferring the warning until the app is foregrounded means a dose answered from the notification does not warn immediately] → Necessary: there is no reliable way to show an interactive, two-choice dialog without the app's UI. This keeps the reminder/notification path exactly as simple and reliable as CLAUDE.md requires, at the cost of a short delay before the warning is seen.
- [An as-needed medicine's stock can run out with no low-stock warning, since there is no schedule to project from] → Accepted as a Non-Goal; the expiry-at-use warning still applies to as-needed medicines with stock, so expiry is still covered.

## Migration Plan

1. Ship the Room schema migration (new `stock_batches` table, new nullable `lowStockAcknowledgement` column on `medications`) with its migration test.
2. Add the domain types and pure functions: `StockBatch`, FEFO consumption, weekly-usage projection (via the existing dose generator), sufficiency check, batch expiry classification.
3. Hook consumption + sufficiency + expiry-at-use evaluation into the existing shared "record taken" path, atomically with the intake write.
4. Add the warning dialog, including the deferred-display path for app-backgrounded takes.
5. Add the Medicine details Stock section and Add stock form.
6. Add the Medicines screen tile heads-up.

No rollback concerns beyond a standard additive schema migration: the new table and column are additive and nullable/empty by default, so a revert simply stops reading and writing them.

## Open Questions

- Whether the 7-day and 30-day windows should become user-configurable later.
- Whether manual batch correction (edit or remove a mistaken entry) should be a follow-up proposal.
