## Why

The Welcome screen lists what the user still has to take, but it is read-only. The only place a dose can be answered is the reminder notification, and a notification is a moment that passes: swipe it away, clear the shade, look at the phone an hour later, and the dose is stranded. The user can see "Ibuprofen, 1 tablet, 08:00, Overdue" on Home and has no way to say "I took it" — the one thing the app exists to record.

Opening a dose from the list closes that gap, and it is also the right place to say something a notification cannot: that the user is reaching for a dose well before it is due, or well after. The warning is information, not a block — Pillsner records what happened, it does not police it.

## What Changes

- The dose tile on the Welcome screen becomes a tap target that opens a new **dose detail** screen for that dose. The tile keeps its own shape and accessibility node; it gains a click action, not a button.
- A new full-screen destination shows the dose: the scheduled time, the medicine name, the amount, and its intake state — following the full-screen reminder layout the design system already defines (section 8.8).
- The screen offers the same three answers as the reminder notification, with the same labels and in the same fixed order: **I took it**, **Not yet**, **Not going to**. Each records exactly what its notification action records — taken now, snoozed for fifteen minutes bounded by the dose's lapse moment, or skipped.
- A bottom-aligned **Close** button returns to the Welcome screen without recording anything, as does the back arrow and the system back gesture.
- Answering returns to the Welcome screen immediately. The answer settles the dose, cancels any reminder notification showing for it, and recomputes the planning window and the next alarm, so the list and the reminders the user gets are both correct before they see the list again.
- **Timing warnings.** When the dose is due an hour or more from now, the screen warns the user that they are early. When it is an hour or more overdue, the screen warns that they are late. Both are advisory: every button stays enabled and the user can always record the dose.
- Opening a dose that no longer exists or has already been answered — the last reminder was answered from the watch while the screen was opening, say — shows that it has been settled rather than offering answers that resolve to nothing.

Out of scope, deliberately: editing the dose's time or amount from this screen (that is the medicine form's job), adding an in-app answer path anywhere other than the dose list, and any change to what the notification itself offers.

## Capabilities

### New Capabilities

- `dose-detail`: the dose detail screen — what it shows for one dose, the three answers and what each records, the early and late warnings and their thresholds, Close and back, the settled-dose case, and its accessibility contract.

### Modified Capabilities

- `welcome-screen`: "Upcoming dose tile content" gains the rule that a tile is a tap target which opens that dose's detail; the tile is still one accessibility node, now with a click action and a label saying what tapping does.
- `app-navigation`: a Dose detail destination inside the app shell, reached only from a Welcome screen tile, carrying the dose identifier in the route, with the navigation bar hidden while it is shown.
- `reminder-scheduling`: "Processing on wake" is joined by the rule that an answer given inside the app is processed by the same path as a notification action — the outcome is recorded, that dose's notification is cancelled, and the next wake is recomputed.

## Impact

Code, all under `src/app/src/main/java/nl/hexmaster/pillsner`:

- `ui/home/DoseTile.kt` — gains an `onClick` and the click semantics; the tile itself becomes the target, as the design system's dose tile note allows.
- `ui/home/WelcomeScreen.kt` — passes the tap up as `onOpenDose(DoseId)`.
- `ui/dose/` (new) — `DoseDetailScreen.kt`, `DoseDetailUiState.kt`, `DoseDetailViewModel.kt`, and the warning banner composable.
- `ui/navigation/Routes.kt` — a `DoseDetail(doseId: Long)` route.
- `ui/PillsnerApp.kt` — registers the destination and wires Home's tile tap to it.
- `domain/intake/AnswerDose.kt` (new) — the one place that records an answer, cancels the dose's notification and triggers the reminder wake, so the notification receiver and the screen cannot drift apart. `data/reminders/ReminderActionReceiver.kt` is rewritten to call it.
- `domain/repository/DoseRepository.kt` gains a per-dose observation — it has `get(DoseId)` but nothing reactive for one dose — so the screen reflects an answer given from the notification or the watch while it is open. `data/RoomDoseRepository.kt`, `data/InMemoryDoseRepository.kt` and `data/db/DoseDao.kt` implement it as a query on the existing columns.
- `di/AppContainer.kt` — registers `AnswerDose` and the new view model.
- `res/values/strings.xml` and the translated variants — the screen title, the Close label, the two warning sentences, the settled-dose message and the tile's click label. The three answer labels already exist and are reused.

No schema change, so no migration. No new dependency, no new permission, no network access. The design system is not changed: the screen is built from the layout its section 8.8 already describes, and the warnings use the existing attention-banner shape with the intake state colours from section 2.3.

Tests in `WelcomeScreenTest` assert that the tile has no click action today and will need updating with the behaviour change.
