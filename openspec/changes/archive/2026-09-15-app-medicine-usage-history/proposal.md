## Why

Pillsner records every dose outcome — taken, skipped, missed — and then never shows it back to the user. Someone who wants to answer "have I actually been taking this?" before a GP appointment, or who suspects they have been forgetting their evening dose, has no way to look. The data is already on the device; it is simply invisible.

## What Changes

- Add a **three-dots overflow menu** to the top app bar of the Medicine details screen with a single item, **Usage history**, that opens the history of that medicine. The menu offers no delete, remove or archive action, in keeping with the existing "medicines are never removed" rule.
- Add a **Medicine usage history** screen: a read-only secondary destination, reached only from the details screen, showing one medicine's adherence over a chosen period.
- The screen offers three periods as a **segmented button row**: 1 week, 1 month, 3 months. The selection is the window the screen looks back over, ending now.
- For the chosen period the screen shows:
  - how many doses were **scheduled** (doses whose moment has passed) and how many were **taken**;
  - the **adherence** percentage those two numbers give, as the headline figure;
  - a **breakdown** of the remainder into skipped, missed and still-unanswered, each with its own icon and label so colour is never the only carrier;
  - a **usage chart**: one bar per day for the week and month periods, one per week for the three-month period, each bar stacked from taken, skipped/unanswered and missed portions.
- The screen is honest about how far the records reach: when the medicine's oldest stored dose is later than the start of the chosen period, the screen says which date the records start from.
- Add read-only history queries to the dose repository and its DAO. **No schema change and no migration**: the existing `doses` table already keeps every past dose forever, and its `(medication_id, scheduled_at)` index already serves the query.
- Add a domain summariser that turns the stored doses into the counts and buckets above. It has no Android dependencies, so the date-boundary behaviour (midnight, month ends, leap days, daylight saving, time zone moves) is unit-testable.
- All new text ships as English and Dutch string resources. Two new Material Symbols Rounded drawables: `ic_more_vert` and `ic_history`.
- No new permission, no network, no third-party dependency, no writes: the screen only reads.

## Capabilities

### New Capabilities
- `medicine-usage-history`: what the usage history screen shows, how a period is chosen, how scheduled, taken, skipped, missed and unanswered doses are counted and bucketed, how the chart and its accessibility work, what the empty and partial-record states say, and that the screen never writes.

### Modified Capabilities
- `medicine-details`: the details screen's top bar gains an overflow menu whose only item opens the usage history; the existing requirement that no delete, remove or archive action exists anywhere on the screen now explicitly covers this menu.
- `app-navigation`: the navigation host gains a non-top-level Medicine usage history destination, reached from the medicine form flow and returning to it with the edited draft intact.

## Impact

- New `domain/model/UsageHistory.kt` (period, counts, buckets) and `domain/history/SummariseUsageHistory.kt` — no Android imports.
- Changed `domain/repository/DoseRepository.kt` and `data/db/DoseDao.kt`: two new read queries, `observeHistoryFor(medicationId, from, to)` and `earliestScheduledAt(medicationId)`. Implemented in `RoomDoseRepository` and `InMemoryDoseRepository`. No `DoseEntity` change, no database version bump.
- New `ui/medicines/history/` package: `MedicineHistoryScreen`, `MedicineHistoryViewModel`, `MedicineHistoryUiState`, `UsageChart`, `UsageBreakdown`.
- Changed: `ui/medicines/form/MedicationFormScreen.kt` (overflow menu in the top bar), `ui/medicines/form/MedicationFormNavigation.kt` (the new destination inside the flow graph), `ui/navigation/Routes.kt` (one more route), `di/AppContainer.kt` (expose the summariser and the clock the screen needs).
- New strings in `values/strings.xml` and `values-nl/strings.xml`; two new drawables.
- Tests: unit tests for the summariser (bucketing, boundaries, DST, empty and partial records) and the view model (period switching); Compose semantics tests for the menu, the segmented buttons, the counts and the chart's descriptions; a navigation test for open and back; a DAO test for the history query.
