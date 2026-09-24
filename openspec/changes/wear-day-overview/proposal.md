## Why

**GitHub Issue:** #71 (https://github.com/hexmasternl/pillsner/issues/71)

The watch shows the doses due in the next six hours. Six hours is often too short to answer the question the watch is raised to answer: *what am I taking today?* At half past eight in the morning the screen may hold one dose, with nothing said about the evening. And a dose on the watch is three lines of text with nowhere to go: there is no way to see what the medicine normally is, how often it is taken, or whether there is enough of it left.

## What Changes

- The watch's single screen becomes an **agenda of today and tomorrow**: a *Today* heading, then the doses still to be taken, grouped under the time they are due; then a *Tomorrow* heading with the same for the next day. The list scrolls vertically, by touch and by rotary. The phone already plans two days ahead, so nothing new has to be computed to fill it.
- Overdue doses the user has not answered stay at the top of *Today*. Taken, skipped and missed doses stay hidden, as now.
- The empty state is reworded from the six-hour phrasing to "No medicines scheduled for today or tomorrow".
- **Tapping a dose opens a read-only details screen** for the medicine behind it: the amount and time of that dose, the medicine's default dose, its schedule, and its remaining stock where the medicine records stock. The watch's own back gesture returns to the agenda.
- **Nothing on the watch edits anything.** The details screen has no field, button or gesture that changes a medicine, a schedule, a stock level or a dose, and the agenda still has none either.
- The sync payload gains, per dose, the medicine's default dose, its schedule lines and its stock text, all written out by the phone in the phone app's language. The addition is optional with defaults, so an older watch build reads a newer phone's payload unchanged and a newer watch shows a dose from an older phone without details.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `wearable-app`: the six-hour list becomes a two-day agenda grouped by time; entry content moves the time to a group heading; the empty state is reworded; a read-only dose details screen is added, reached by tapping a dose, and the "single screen" and "no write actions" requirements are restated to cover it.
- `wearable-sync`: the payload carries the medicine behind each dose — default dose, schedule lines and stock — additively, and the phone publishes them.

## Impact

- `src/shared/src/main/kotlin/nl/hexmaster/pillsner/shared/wear/SyncedDoses.kt` — `SyncedMedicineDetails`, and `SyncedDose.details`.
- `src/app/src/main/java/nl/hexmaster/pillsner/data/wear/` — `WearMedicineDetails` (new) and `DoseSyncPublisher`.
- `src/app/src/main/java/nl/hexmaster/pillsner/di/AppContainer.kt` — wiring.
- `src/wear/src/main/kotlin/nl/hexmaster/pillsner/wear/` — `domain/DayAgenda` replaces `domain/UpcomingWindowFilter`; the agenda screen, the dose card, the new details screen, the state and the view model; strings in all six languages.
- `README.md` and `PRIVACY.md` — what the watch shows and what is sent to it.
- No new dependency, no network access, no database change, no change to reminders or to how a dose is answered.
