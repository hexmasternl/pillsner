## Context

`ReminderCoordinator.wake()` runs, in order: `markMissedDoses()`, `refreshPlannedDoses()`,
`dueDoses()`, then later `reconcileAlarms()` calls `computeWakeSchedule()`. Tracing the calls:

- `MarkMissedDoses.invoke()` calls `doseRepository.pending()` once, then `lapseAt(dose)` (which
  itself queries `nextScheduledAtAfter`) for every pending dose.
- `DueDoses.invoke()` calls `doseRepository.pending()` again, and for doses whose repeat might be
  due, calls `markMissedDoses.lapseAt(dose)` again — a second `nextScheduledAtAfter` query for the
  same dose.
- `ComputeWakeSchedule.invoke()` calls `doseRepository.pending()` a third time, calls
  `markMissedDoses.lapseAt(dose)` a third time for every pending dose, and separately collects
  `medicationRepository.observeAll().first()` even though `RefreshPlannedDoses` (called earlier in
  the same wake) already read the same list.

None of these three use cases are wrong; each was written to be usable on its own, which is also
why they independently re-fetch what an earlier step in the same wake already has. The cost is
multiplicative with the size of the pending-dose table, which only grows (doses are never deleted).

`RoomDoseRepository.refreshSnapshots` and `ReminderCoordinator.wake()`'s `due.forEach` loop have
the same shape: a suspend DAO call issued once per row, each its own Room transaction/commit,
instead of one transaction covering the whole batch. `withdrawPlanned` loops per medication instead
of resolving the whole window in one round trip. `ReminderDeliveryLog.append()` reads the entire
log file back on every write just to decide whether it has grown past the trim threshold.

## Goals / Non-Goals

**Goals:**
- Cut the wake cycle to one `pending()` read and one lapse-moment computation per dose, shared by
  all three use cases.
- Make each place that currently loops per-row over a suspend DAO call issue one transaction
  instead.
- Stop `ReminderDeliveryLog` from reading its file on every write.
- Add the missing `(outcome, scheduled_at)` index so `pending()`/`observePending()` stay a
  single index scan as the doses table grows.
- Preserve every existing scenario in `reminder-scheduling` and `reminder-delivery-resilience`
  exactly: same doses due, same lapse moments, same alarms armed, same log entries.

**Non-Goals:**
- Deleting or archiving old dose rows. The unbounded growth is accepted product behaviour (doses
  are the user's history); this change only stops query cost from scaling badly with it.
- Collapsing `withdrawPlanned` into a single literal SQL statement across all medications via
  dynamic raw SQL. The medication count is small (a handful, per `ComputeWakeSchedule`'s own
  comment that "the set stays in single digits"), so one transaction around the existing
  per-medication queries removes the actual redundant cost (separate coroutine/Room round trips)
  without the complexity and risk of hand-built dynamic SQL.
- Changing when a dose becomes due, lapses, or is withdrawn. This is an implementation change only.

## Decisions

### 1. A single wake-scoped snapshot feeds `DueDoses` and `ComputeWakeSchedule`

**Correction found during implementation** (recorded here per `CLAUDE.md`'s rule to fix the design
and say so rather than quietly diverge): the original wording above said the snapshot is built
"after `markMissedDoses()` has run" and is shared by all three use cases. Tracing the actual call
order in `ReminderCoordinator.wake()` shows that is unsafe in two ways:

- `refreshPlannedDoses()` runs *between* `markMissedDoses()` and `dueDoses()`, and it can both
  **insert** new doses (which can change an existing dose's `nextScheduledAtAfter` result, and
  therefore its `lapseAt`) and **withdraw** pending doses outright. A snapshot taken right after
  `markMissedDoses()` would hand `dueDoses()` doses that have since been deleted, and lapse moments
  that predate a schedule edit that should have shortened them.
- `computeWakeSchedule()` does not run inside `wake()` at all — it runs later, in
  `reconcileAlarms()`, after the `due.forEach` loop has already called `recordReminded(...)` /
  `setSnooze(...)` for the doses that were just reminded. A snapshot built before that loop would
  make `computeWakeSchedule` see stale `firstRemindedAt` / `lastRemindedAt` / `reminderCount` /
  `snoozedUntil` values for exactly the doses that just changed, which is precisely the state
  `ReminderRepeats.nextRepeatAt`/`nextUnannouncedRetryAt` use to decide the next wake moment.

The fix: build the snapshot **after `refreshPlannedDoses()` has run**, not right after
`markMissedDoses()`, and **update it in memory** to mirror the `due.forEach` loop's writes before
handing it to `computeWakeSchedule()`. `MarkMissedDoses` is unaffected by this change: it still runs
first, before `refreshPlannedDoses`, with its own independent `pending()` + `lapseAt` resolution,
exactly as today, since its timing requirement (decide what has lapsed *before* the window is
refreshed) is a real, order-dependent decision, not a redundant re-fetch.

Introduce `PendingSnapshot(doses: List<Dose>, lapseAt: Map<DoseId, Instant>)`. Build it once per
wake, right after `refreshPlannedDoses()`: one `pending()` call, one `nextScheduledAtAfter` query per
dose to resolve `lapseAt`. This is the snapshot `DueDoses` consumes.

Before calling `computeWakeSchedule()` in `reconcileAlarms()`, `wake()` derives an **updated**
snapshot from the one `dueDoses` used, by replacing each dose that was actually reminded (i.e.
`notifier.show(...)` returned true) with a copy carrying the same field changes
`recordReminded`/`setSnooze` make in the database:

```
firstRemindedAt = dose.firstRemindedAt ?: now
lastRemindedAt = now
reminderCount = dose.reminderCount + (if (countsAsRepeat) 1 else 0)
// then, only if the dose had a snooze pending:
snoozedUntil = null
reminderCount = 0   // setSnooze always resets the repeat count, per DoseDao.setSnooze
```

`lapseAt` values are untouched by this update, because `lapseAt` depends only on `scheduledAt` and
other doses' `scheduled_at` — never on outcome, reminder or snooze fields — so the map built after
`refreshPlannedDoses()` stays valid across the `due.forEach` loop.

`wake()` returns this updated snapshot (alongside the medication list from decision 2) so
`reconcileAlarms()` can pass it into `computeWakeSchedule(snapshot, medications)` without querying
the database again. Both `WakeOutcome.COMPLETED` and `reconcileAlarms()`'s "schedule failed" path
must carry this value; the cleanest shape is to have `runWakeBody`/`wake()` return a small
`WakeResult(snapshot, medications)` alongside the existing timeout/failure handling.

`DueDoses` and `ComputeWakeSchedule` change from resolving their own doses and lapse moments to
accepting a `PendingSnapshot` (and, for `ComputeWakeSchedule`, the medication list) as parameters,
with their public `invoke()` signatures updated accordingly. Their unit tests move from stubbing
`DoseRepository.pending()` to passing a snapshot directly, which also makes those tests faster (no
repository fake needed for the parts that don't touch it).

**Alternative considered**: cache `pending()` and `lapseAt()` results transparently on
`DoseRepository`/`MarkMissedDoses` themselves (e.g. a request-scoped cache keyed by wake). Rejected:
it hides a wake-lifetime concern behind an object that outlives a single wake, inviting stale-cache
bugs across wakes. An explicit snapshot passed down the existing call chain keeps the lifetime
obvious and needs no cache invalidation.

### 2. `ComputeWakeSchedule` receives the medication list instead of re-reading it

`RefreshPlannedDoses.invoke()` already reads `medicationRepository.observeAll().first()`. Change its
return type from `List<DoseId>` to a small `RefreshResult(withdrawn: List<DoseId>, medications:
List<Medication>)`, and have `ReminderCoordinator.wake()` pass `result.medications` into
`computeWakeSchedule(snapshot, medications)`. `ComputeWakeSchedule.anyMedicineProducesDoses()` is
replaced by a plain check over the passed-in list.

### 3. Batch writes become single-transaction DAO calls, not single SQL statements

For `refreshSnapshots` and the due-dose `recordReminded`/`setSnooze` loop in
`ReminderCoordinator.wake()`, add a `@Transaction`-annotated composite method on `DoseDao` (a Kotlin
DAO interface default/abstract method that calls the existing single-row `@Query` methods inside
one method Room wraps in a transaction). This removes the per-row commit and the per-row suspend
round trip from the repository/coordinator to the DAO, without hand-writing a batched `UPDATE ...
CASE` statement:

- `DoseDao.refreshSnapshots(rows: List<SnapshotUpdate>)` — one `@Transaction` method looping the
  existing `refreshSnapshot(...)` query.
- A new `DoseRepository.applyReminderOutcomes(updates: List<ReminderOutcomeUpdate>)` backed by a
  `@Transaction` DAO method that loops `recordReminded(...)` and, where the update carries a snooze
  clear, `setSnooze(id, null)`. `ReminderCoordinator.wake()` collects the per-dose updates while
  iterating `due` (as it does today) and applies them in one call after the loop instead of calling
  the repository inside the loop.

`withdrawPlanned` keeps its existing per-medication query shape (see Non-Goals) but the whole method
body moves inside a single `@Transaction` DAO-backed call, so the id-collection queries and the
final `deleteByIds` are one atomic unit instead of implicitly separate ones.

### 4. `ReminderDeliveryLog` tracks its line count in memory

Add a `private var lineCount: Int` initialised once (lazily, on the writer dispatcher, the first
time `append` or `trimIfNeeded` runs) by reading the file exactly once. Every `append()` increments
it instead of re-reading the file; `trimIfNeeded()` only runs its read-and-rewrite when `lineCount`
exceeds `MAX_ENTRIES + TRIM_SLACK`, and resets `lineCount` to `MAX_ENTRIES` after trimming. All
access already happens on the single-threaded `writer` dispatcher (`limitedParallelism(1)`), so the
counter needs no further synchronisation.

### 5. Room schema version 5: composite index on `(outcome, scheduled_at)`

**Correction found during implementation**: the database is already at version 4 in this repository
(`MIGRATION_1_2`, `MIGRATION_2_3` and `MIGRATION_3_4` already exist for unrelated earlier changes),
not version 2 as first assumed. This change adds version 5, not version 3.

Add `Index(value = ["outcome", "scheduled_at"])` to `DoseEntity`, bump the database version to 5,
add `MIGRATION_4_5` (`CREATE INDEX ...`), export the version 5 schema JSON, and add a migration test
to the existing harness (seed a version 4 database, migrate to 5, validate against the exported
schema). No column or data change, so the migration is additive and cannot lose data.

## Risks / Trade-offs

- **[Risk] Restructuring the three scheduling use cases' signatures touches the most fragile part
  of the app.** → Mitigation: no change to the lapse rule, the due rule, or the wake-schedule rule
  themselves — only where their inputs come from. Existing unit tests for each use case are ported
  to the new signatures rather than rewritten, so they keep asserting the same behaviour. Run the
  full unit, instrumented, and lint suite before this change is considered done, per CLAUDE.md.
- **[Risk] `@Transaction` composite DAO methods still issue N statements, just atomically.** → This
  is an accepted trade-off (see Non-Goals): it removes the redundant per-row Room/coroutine
  round-trip cost, which is what `docs/todo.md` actually flagged, without the complexity of dynamic
  SQL for a row count that stays small by design.
  **Mitigation**: DAO tests cover the batched methods with more than one row to confirm atomicity
  and correctness.
- **[Risk] Schema version 5 is a real migration.** → Mitigated by the migration test harness
  requirement already in `medication-persistence` and extended here; the change is index-only
  (additive), so there is no data transformation to get wrong.

## Migration Plan

Ship as one change on a short-lived branch, in the order tasks.md lays out (index/migration first
so DAO changes have a stable schema to build on, then the read-path snapshot sharing, then the
write-path batching, then the delivery log). No feature flag: this is an internal-only change with
no user-visible behaviour, consistent with `docs/todo.md`'s own framing. Rollback is a normal
revert; because the app is still in early development with no released schema history beyond what
is in this repository, there is no external migration-downgrade concern.

## Open Questions

- None outstanding. If, during implementation, batching `withdrawPlanned` into one transaction
  still shows up as a measurable cost (it should not, given the bounded medication count), revisit
  the single-raw-query option called out in Non-Goals as a follow-up change rather than expanding
  this one.
