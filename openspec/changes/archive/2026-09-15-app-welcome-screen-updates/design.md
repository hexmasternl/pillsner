## Context

`RefreshPlannedDoses` is the single point where stored doses are made to agree with the medicines' schedules. It runs on app start, on every reminder wake, at the day rollover and — through `ReminderCoordinator.start()` collecting `medicationRepository.observeAll()` — whenever a medicine or schedule is written. That wiring already works: saving an edit does wake the coordinator and does re-run the refresh. What the refresh then does with the result is where the reported behaviour comes from.

Two mechanisms are responsible.

**The snapshot never moves.** `DoseDao.insertIgnore` uses `OnConflictStrategy.IGNORE` against the unique index on `(medication_id, scheduled_at)`. A re-plan of a moment that is already stored is therefore a no-op, by design — that is what makes the refresh idempotent. But it also means the row's `medication_name`, `amount_value` and `amount_unit` keep the values they were written with. Renaming a medicine leaves every already-planned dose reading the old name, and the Welcome screen (`DoseTile` renders `dose.medicationName`), the notification (`ReminderNotifier` sets it as the content title) and the watch all read that column. `dose-records` blesses this outright: *"Changing or deleting the medicine afterwards MUST NOT change existing doses."* The rule is right for a dose the user has answered and wrong for one they have not.

**Withdrawal is not keyed on the medicine.** `deletePlannedNotIn(from, to, keep)` deletes pending, un-reminded doses in the window whose `scheduled_at` is not in `keep`, and `keep` is the flat list of every planned moment across every medicine. Move medicine A from 08:00 to 09:00 while medicine B still has 08:00 and A's stale 08:00 dose is kept, because 08:00 is somewhere in the list. With a single medicine the bug is invisible, which is why it survived; with two it produces exactly the "the old schedule still fires" report.

There is a third, quieter part. The delete is also guarded by `first_reminded_at IS NULL`, so a dose whose reminder has already been shown is never withdrawn. That guard is deliberate and `reminder-scheduling` depends on it: on a clock or time-zone change, *"Doses already reminded MUST keep their scheduled instant and MUST NOT be reminded again."* But the same guard means that editing a schedule at 10:00 leaves this morning's 08:00 reminder sitting in the shade for a time the user has just deleted. The guard is right for one caller and wrong for the other, and the refresh currently cannot tell them apart.

No schema change is involved in any of this. The columns and indices are already what the fix needs.

## Goals / Non-Goals

**Goals:**

- A pending dose reads the medicine's current name and amount, everywhere it is shown.
- An answered dose keeps the name and amount it was recorded with, permanently.
- Changing a medicine's schedules withdraws exactly that medicine's doses that the new schedules no longer call for, and no other medicine's.
- A user's own edit withdraws a dose it has already reminded about, and takes that notification down with it.
- A clock, time-zone or reboot-driven refresh keeps every already-reminded dose, as it does today.
- The refresh stays idempotent and stays inside the broadcast-receiver budget.

**Non-Goals:**

- Changing the two-day window, the missed rule, the snooze rule or how the next wake is computed.
- Any database schema change or migration.
- Deactivating a medicine while one of its reminders is on screen. Same shape, separate decision, not reported.
- Rewriting the amount on a dose the user has answered, for any reason.

## Decisions

### D1. The snapshot protects answered doses, not pending ones

The dose table keeps its snapshot columns. What changes is when they are allowed to move: a row with `outcome IS NULL` is refreshed from the medicine on every refresh; a row with an outcome is never touched.

The refresh gains a second write alongside the insert — an update of `medication_name`, `amount_value` and `amount_unit` for pending doses of a medicine, driven by the same planned list the insert uses. Because the planner already produces the name and amount per planned dose, the update needs no extra read.

Considered and rejected: dropping the snapshot columns for pending doses and joining to `medications` at read time. That is the more normalised answer, but it forces the read path to cope with a medicine row that may be gone, splits "where does the name come from" across two cases in every consumer, and would need a migration. Keeping one column and moving it is smaller and leaves history untouched.

Considered and rejected: `OnConflictStrategy.REPLACE` on the insert. It would rewrite the whole row, including `first_reminded_at`, `snoozed_until` and the primary key, silently destroying the state the refresh is supposed to preserve.

A dose whose reminder is already showing is pending, so its snapshot moves too. The notification is re-posted on the same wake — the coordinator posts for every due dose after the refresh — so the user sees the new name rather than a notification that disagrees with the app. This overturns the current `medicine-details` scenario "Reminded dose untouched by an edit", which is listed as a spec change in the proposal.

### D2. Withdrawal matches on the medicine and the moment together

`deletePlannedNotIn(from, to, keep: Collection<Instant>)` is replaced by a withdrawal that takes the planned `(medicationId, scheduledAt)` pairs. Room cannot bind a list of pairs to a `NOT IN`, so the implementation is: for each medicine that has planned doses, delete its pending doses in the window whose moment is not among that medicine's planned moments; and delete every pending dose in the window belonging to a medicine that planned nothing at all, which is what covers a deactivated medicine or one whose last schedule was removed.

That is one statement per medicine plus one sweep. With a handful of medicines it is well inside the wake budget, and it keeps every bound a simple list of instants that Room can bind. Doses whose `medication_id` is null — the medicine was deleted from the database — are pending history of something that no longer exists and are left alone by both statements, as they are today.

Considered and rejected: deleting everything pending in the window and re-inserting. It would lose `first_reminded_at` and `snoozed_until` on doses that survive the edit, and it would churn primary keys that notifications hold in their pending intents.

### D3. The refresh is told why it is running

`RefreshPlannedDoses.invoke` takes a parameter saying whether this refresh follows a change the user made to a medicine. Only then may it withdraw a dose that has already been reminded; otherwise the `first_reminded_at IS NULL` guard applies exactly as now.

`ReminderCoordinator` already has this information: `WakeReason.MEDICATIONS_CHANGED` is the edit case, and every other reason — `ALARM`, `BOOT`, `TIME_CHANGED`, `APP_START`, `APP_UPDATED`, `ACTION`, `PERMISSION_CHANGED` — is not. The parameter is derived from the reason the coordinator already passes around, so nothing new has to be plumbed from the UI.

Considered and rejected: always withdrawing reminded doses. It breaks the `reminder-scheduling` requirement on clock and time-zone changes, which is agreed behaviour with its own scenarios.

Considered and rejected: comparing each medicine against a previously stored plan to detect what actually changed. It needs state the app does not keep, and the wake reason answers the question well enough.

**Refinement found during implementation.** The withdrawal range carries a day of slack on each side of the plan window, so that a dose planned under an old time zone cannot survive just outside the new window. Applying the edit-mode withdrawal over that same slack range would also take an unanswered dose from yesterday evening that the user has been reminded about — not because they stopped taking it, but because the two-day window simply does not reach back that far. That would lose a reminder the user still owes an answer to. So the refresh withdraws twice: once over the slack range with reminded doses protected, exactly as before, and then, only after a user edit, once more over the plan window alone with reminded doses included. Both passes are idempotent and the second only ever finds reminded doses, since the first has already taken the rest.

### D4. A withdrawn dose loses its notification

`RefreshPlannedDoses` returns the ids of the doses it withdrew. `ReminderCoordinator` cancels a notification for each, in the same step it already cancels for doses that have just gone missed. Without this, D3 would take the row away and leave the notification on screen, with an action that resolves to a dose that no longer exists.

Cancelling a dose that never had a notification is harmless — the platform ignores an unknown id — so the coordinator cancels for every withdrawn dose rather than checking first.

To return the ids, the withdrawal reads them before deleting rather than after: `SELECT id` with the same predicate, then `DELETE ... WHERE id IN (...)`. Both run inside the refresh, which the coordinator already serialises behind its mutex, so nothing can slip in between.

### D5. Ordering inside the wake is unchanged

The coordinator's sequence stays: mark missed, refresh, post what is due, compute the next wake in a `finally`. The refresh's new work — the snapshot update and the withdrawal — happens where the old withdrawal happened, so a dose withdrawn by an edit is gone before `dueDoses()` runs and can never be posted on the way out.

## Risks / Trade-offs

- **A user reads the medicine name off a reminder and the name changes under them** → This is the intended correction: the name on a pending dose is the medicine's name, and a stale one is worse. Answered doses, which are the record, never move.
- **Per-medicine withdrawal runs a statement per medicine instead of one for all** → Bounded by the number of medicines a person takes, on an indexed `(medication_id, scheduled_at)` column, inside a 9-second budget. If it ever matters it can become a single statement with a composite predicate.
- **The wake reason becomes load-bearing: a future caller passing the wrong one silently withdraws, or fails to withdraw, reminded doses** → The parameter is named for the behaviour rather than the reason, documented at the call site, and covered by tests for both directions.
- **Existing tests encode the old rules and will fail** → That is the signal that the behaviour changed. `DoseDaoTest`, `RefreshPlannedDosesTest` and the medicine-details tests are updated as part of the work, not worked around.
- **Two refreshes racing could cancel a notification for a dose another wake is re-posting** → The coordinator's mutex already admits one wake at a time, and the refresh runs entirely inside it.
