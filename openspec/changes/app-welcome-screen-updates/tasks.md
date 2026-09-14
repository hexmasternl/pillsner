## 1. Verify the ground

- [x] 1.1 Confirm the scaffold exists: `src/` is a Gradle project with an `app` module, `PillsnerDatabase`, `DoseDao`, `RefreshPlannedDoses`, `ReminderCoordinator` and `AppContainer` all present. Stop and report if any is missing — this change creates none of them.
- [x] 1.2 Confirm no schema change is needed: `doses` already has `medication_id`, `medication_name`, `amount_value`, `amount_unit`, `outcome` and `first_reminded_at`, and the unique index on `(medication_id, scheduled_at)`. Record that no migration is part of this change.
- [x] 1.3 Run `./gradlew testDebugUnitTest lint` from `src/` and note the current pass state, so later failures are attributable to this change.

## 2. Dose data layer: per-medicine withdrawal and snapshot refresh

- [x] 2.1 In `DoseDao`, add a query that returns the ids of pending doses of one medicine, inside a window, whose moment is not among a given list. *Built as `plannedNoLongerScheduled`.*
- [x] 2.2 Carry the reminded guard as an `includeReminded` parameter on that query rather than as a second copy of it (design D3). *Deviation from the original task, which asked for two near-identical queries; one parameter says the same thing with half the SQL and half the test surface.*
- [x] 2.3 In `DoseDao`, add the matching query for pending doses of medicines that planned nothing at all: ids inside the window whose `medication_id` is in a given list, with the same `includeReminded` parameter. Doses with a null `medication_id` MUST NOT be selected. *Built as `plannedForUnscheduledMedications`.*
- [x] 2.4 In `DoseDao`, add `deleteByIds(ids: Collection<Long>)`.
- [x] 2.5 In `DoseDao`, add an update that sets `medication_name`, `amount_value` and `amount_unit` for one pending dose identified by `(medication_id, scheduled_at)`, and does nothing when the row has an outcome. *Also skips rows whose values already match, so an unchanged refresh does not make the dose stream re-emit.*
- [x] 2.6 Remove `deletePlannedNotIn` from `DoseDao` once nothing calls it.
- [x] 2.7 Extend `DoseDaoTest` (instrumented): withdrawal picks only the named medicine's stale doses; a second medicine's dose at the same instant survives; a dose with an outcome survives both variants; a reminded dose survives the guarded variant and is withdrawn by the unguarded one; a null-medication dose survives both; the snapshot update changes a pending row and leaves an answered row alone.

## 3. Dose repository contract

- [x] 3.1 In `DoseRepository`, replace `deletePlannedNotIn(from, to, keep)` with a withdrawal that takes the window, the planned doses grouped by medication, and whether reminded doses may be withdrawn, and returns the withdrawn `DoseId`s. Document the two modes in KDoc.
- [x] 3.2 In `DoseRepository`, add the operation that refreshes the name and amount of pending doses from a list of planned doses.
- [x] 3.3 Implement both in `RoomDoseRepository` using the new DAO queries: read ids, delete by id, return them.
- [x] 3.4 Implement both in `InMemoryDoseRepository` with the same semantics, including the null-medication and answered-dose exclusions.

## 4. RefreshPlannedDoses

- [x] 4.1 Change `invoke` to take a parameter saying whether this refresh follows a change the user made to a medicine, and to return the withdrawn `DoseId`s.
- [x] 4.2 Insert the planned doses as now, then refresh the snapshot of pending doses from the same planned list (design D1), then withdraw per medicine (design D2), passing the flag through.
- [x] 4.3 Cover medicines that plan nothing — inactive, no schedules, or outside their used-since/use-until range — so their pending doses in the window are withdrawn too.
- [x] 4.4 Keep the one-day slack on each side of the window and keep the whole operation idempotent.
- [x] 4.5 Update the class KDoc to state the new snapshot rule and the edit-versus-clock-change distinction, with the reason for each.
- [x] 4.6 Extend `RefreshPlannedDosesTest`: rename reaches a pending dose; amount change reaches a pending dose; an answered dose keeps its snapshot; two medicines sharing 08:00 and one moving to 09:00 leaves the other alone; an edit withdraws a reminded dose; a non-edit refresh does not; withdrawn ids are returned; running twice changes nothing.

## 5. ReminderCoordinator

- [x] 5.1 Derive the "user changed a medicine" flag from `WakeReason` — true only for `MEDICATIONS_CHANGED` — and pass it to the refresh.
- [x] 5.2 Cancel a notification for every dose the refresh reports as withdrawn, immediately after the refresh and before `dueDoses()`.
- [x] 5.3 Keep the existing order and the `finally` that sets the next alarm, and keep the whole wake inside `WAKE_TIMEOUT_MILLIS`.
- [x] 5.4 Verify nothing added here logs a medicine name or amount at any level.
- [x] 5.5 Add tests for the coordinator: a `MEDICATIONS_CHANGED` wake cancels the withdrawn doses' notifications; a `TIME_CHANGED` wake withdraws and cancels nothing that was reminded; the next alarm is still set when the refresh throws. *Written in `ReminderWakeTest` (instrumented), not as JVM unit tests: `ReminderNotifier` needs a `Context`, and asserting the notification is actually gone needs the real `NotificationManager`. Written and compiling; not yet executed — see 7.3.*

## 6. Read paths

- [x] 6.1 Confirm `RoomUpcomingDosesRepository`, `ReminderNotifier`, `DoseTile` and `DoseSyncPublisher` all read the dose's stored name and amount, so they inherit the fix with no change. Note in the task any place that does not.
- [x] 6.2 Confirm the Welcome screen re-renders on the dose flow emission after an edit, without the user leaving the screen; add a `HomeViewModel` test if that is not already covered.
- [x] 6.3 Update `MedicineHistoryScreen`'s expectations only if a test asserts the old snapshot rule for a pending dose; answered doses must be unaffected.

## 7. Verification

- [x] 7.1 Run `./gradlew testDebugUnitTest` from `src/` and fix failures. Where a test asserted the old snapshot or withdrawal rule, update the test to the new rule rather than weakening the assertion. Report any remaining failure verbatim.
- [x] 7.2 Run `./gradlew lint` from `src/` and clear new findings.
- [ ] 7.3 Run `./gradlew connectedDebugAndroidTest` from `src/` — the database and scheduling changed, so the instrumented suite is required. **Not run.** The new instrumented tests compile but have never been executed; an emulator (`emulator-5554`) is up, so this is one command away. The DAO queries and the notification cancellation have no other coverage, so this is the gap that matters most.
- [ ] 7.4 By hand on a device: two medicines with a dose at the same time, move one, confirm the other's dose stays and the moved one's old dose is gone.
- [ ] 7.5 By hand on a device: rename a medicine with a dose due today and confirm the Welcome screen tile and the reminder notification both show the new name.
- [ ] 7.6 By hand on a device: with a reminder showing, edit the schedule so that dose is no longer planned, and confirm the notification disappears.
- [ ] 7.7 By hand on a device: change the system time zone with a reminded dose outstanding and confirm it is neither withdrawn nor re-reminded.
- [ ] 7.8 Run `openspec validate app-welcome-screen-updates --strict` and confirm every task above is ticked before archiving.
