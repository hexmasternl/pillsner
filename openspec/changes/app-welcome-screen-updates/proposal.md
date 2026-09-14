## Why

Editing a medicine does not fully take effect. Rename a medicine and the Welcome screen keeps showing the old name on today's doses, and so does the reminder notification when it fires. Change a schedule and the doses the old schedule produced can survive alongside the new ones, so the user is reminded at a time they have just removed. Part of this is a code defect and part of it is a rule in the spec that turns out to be wrong in practice: `dose-records` currently says a dose keeps the name and amount it was generated with, full stop, which was written to protect history but also freezes doses the user has not answered yet.

A medicine reminder that names the wrong medicine, or fires at a time the user has deleted, undermines the one thing Pillsner promises. This is a correctness problem in the core flow, not a polish item.

## What Changes

- **BREAKING (spec):** the name and amount snapshot on a dose protects **answered** history only. A dose that is still pending SHALL show the medicine's current name and amount, on the Welcome screen, in the notification and on the watch. A dose with an intake — taken, skipped or missed — keeps the snapshot it was recorded with, unchanged.
- Withdrawing planned doses becomes **per medicine**. Today the refresh keeps every dose whose moment appears anywhere in the new plan, so moving one medicine from 08:00 to 09:00 leaves its 08:00 dose behind whenever another medicine still has an 08:00 dose. Withdrawal will match on the medicine and the moment together.
- A user's own edit withdraws a dose it has already reminded about. When the user changes a medicine or its schedules, a pending dose the new schedules no longer call for is withdrawn even if its reminder has already been shown, and that notification is cancelled. A clock or time-zone change keeps the existing protection and withdraws nothing that has been reminded, exactly as `reminder-scheduling` requires.
- The distinction above is carried explicitly into the refresh, so the two cases can never be confused.

Out of scope, and deliberately so: deactivating a medicine while one of its reminders is on screen. That is the same shape of problem but a separate decision about what deactivation means, and it is not what was reported.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `dose-records`: "Snapshots protect history" narrows to answered doses and gains the rule that a pending dose follows the live medicine. "Rolling two-day window" gains per-medicine withdrawal and the edit-versus-clock-change distinction.
- `welcome-screen`: "Upcoming dose tile content" states that a pending dose tile shows the medicine's current name and amount.
- `medicine-details`: "Editing preserves history" is corrected — a rename reaches pending doses, and an edit that drops a dose withdraws it even when it has been reminded.
- `reminder-scheduling`: a withdrawn dose loses its notification, and the refresh is told why it is running.

## Impact

Code, all under `src/app/src/main/java/nl/hexmaster/pillsner`:

- `domain/scheduling/RefreshPlannedDoses.kt` — takes the reason it is running, withdraws per medicine, reports which doses it withdrew.
- `domain/repository/DoseRepository.kt`, `data/RoomDoseRepository.kt`, `data/InMemoryDoseRepository.kt` — withdrawal keyed on medicine and moment; snapshot refresh for pending doses.
- `data/db/DoseDao.kt` — replaces `deletePlannedNotIn` with a per-medicine query, adds the snapshot update for pending doses. No schema change, so no migration.
- `data/reminders/ReminderCoordinator.kt` — passes the wake reason through and cancels notifications for withdrawn doses.

Nothing here changes the database schema, adds a dependency, or touches the design system. Existing tests in `DoseDaoTest`, `RefreshPlannedDosesTest` and the medicine-details tests assert the old snapshot rule and will need updating with the behaviour change.
