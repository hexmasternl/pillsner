## Context

The Welcome screen (`ui/home/`) renders up to five `UpcomingDose` tiles from `HomeViewModel`, which reads `UpcomingDosesRepository.observeUpcoming(limit)`. `DoseTile` is deliberately informational: its KDoc says "no tap target, no actions", and the whole card is one merged semantics node. The design system's dose tile note (section 8.1) anticipated the gap — "When intake actions arrive, the tile grows a 56 dp filled 'Taken' button along its bottom edge; the tile itself does not become the tap target" — but that sentence was written for an inline action, not for opening a detail screen. This change takes the other road: the tile becomes the tap target and the actions live on the screen it opens. The design system's section 8.8 already describes that screen's layout under "Full-screen reminder (if a change adds one)".

Answering a dose today happens in exactly one place: `ReminderActionReceiver`. It loads the dose, calls `AppContainer.recordIntake` or `AppContainer.snoozeDose`, cancels the notification through `ReminderNotifier.cancel(dose)`, and then calls `ReminderCoordinator.onWake(WakeReason.ACTION)`, which settles lapsed doses, refreshes the planning window, shows what is due, reschedules the single next-wake alarm and publishes to the watch. That five-step sequence is the whole correctness of an answer, and it currently lives inline in a broadcast receiver.

`DoseRepository` exposes `get(id)` and several streams, but nothing reactive for a single dose. `Dose` carries everything the screen needs: name, amount, `scheduledAt`, `intake`, `snoozedUntil`, `firstRemindedAt`.

Constraints from the repository: the domain layer takes no Android dependency; user-facing text lives in string resources with a Dutch translation; every visual decision comes from a theme token; reminders must survive the answer; nothing about a medicine is logged above debug level.

## Goals / Non-Goals

**Goals:**

- A dose the user can see on Home is a dose the user can answer, without waiting for a notification.
- The three answers mean exactly what they mean on the notification — one implementation, not two that drift.
- The list and the next alarm are correct before the user is back on Home.
- Early and late are stated plainly and change nothing: every button stays live, whatever the clock says.
- Legible at 200 % font scale, operable with TalkBack, primary action in the bottom third.

**Non-Goals:**

- Editing the dose — its time, its amount, its medicine. That is the medicine form.
- A fourth answer, an undo, or a way to record a dose at a time other than now. "Taken" is taken now; correcting a time is a separate proposal.
- Reaching this screen from anywhere but a Welcome screen tile: not from the notification body (which still opens Home), not from the medicine form, not from the watch.
- Changing the notification, its actions, its text, or the watch bridge.
- Adherence, stock or refill consequences of an answer.

## Decisions

### D1. `AnswerDose`: one use case for every answer, wherever it comes from

A new `domain/intake/AnswerDose.kt` holds the whole sequence an answer implies, and both the notification receiver and the new view model call it and nothing else:

```kotlin
enum class DoseAnswer { TAKEN, SNOOZE, SKIP }

class AnswerDose(
    private val doseRepository: DoseRepository,
    private val recordIntake: RecordIntake,
    private val snoozeDose: SnoozeDose,
    private val onAnswered: suspend (Dose) -> Unit,
) {
    suspend operator fun invoke(id: DoseId, answer: DoseAnswer)
}
```

It loads the dose, applies the answer (`RecordIntake` with `TAKEN`/`SKIPPED`, or `SnoozeDose`), then calls `onAnswered(dose)`. `AppContainer` supplies `onAnswered` as the Android-side pair of effects that must follow every answer: `reminderNotifier.cancel(dose)` and `reminderCoordinator.onWake(WakeReason.ACTION)`. Cancelling a notification that was never shown is a no-op, so the screen does not need to know whether one was on display.

`ReminderActionReceiver` keeps its own concerns — parsing the intent, `goAsync`, the ten-second budget, the debug log — and its body becomes `container.answerDose(doseId, answer)`. `ReminderAction` maps onto `DoseAnswer` one-to-one.

Alternative considered: letting the view model call `recordIntake` / `snoozeDose` and then the coordinator itself, mirroring the receiver. Rejected: it duplicates a five-step sequence whose whole value is that it is never partly done, in a layer that has no business knowing about notifications. The next surface that answers a dose — a widget, a tile, the watch app — would copy it a third time.

Alternative considered: putting `onAnswered` inside `ReminderCoordinator` and having `AnswerDose` call the coordinator directly. Rejected: `ReminderCoordinator` is in `data/reminders` and holds `ReminderNotifier`, an Android class; a domain use case cannot depend on it. The lambda keeps the direction of the dependency right.

### D2. Per-dose observation on `DoseRepository`

`DoseRepository` gains:

```kotlin
/** One dose as a stream: re-emits when it is answered, snoozed or withdrawn, null once it is gone. */
fun observe(id: DoseId): Flow<Dose?>
```

`DoseDao` gets a `@Query` on the primary key returning `Flow<DoseEntity?>`, mapped the way `observePending` already maps. `InMemoryDoseRepository` derives it from the map it already keeps.

The screen needs this, not a one-shot `get`: a dose can be answered from the notification shade or the watch while the detail screen is in the foreground, and an early/late warning computed once would also go stale as the hour boundary passes. A `null` emission is the dose being withdrawn by a refresh, which the screen treats as "gone" (D6).

No schema change — it is a new read of existing columns — so no migration.

### D3. Route and destination

`@Serializable data class DoseDetail(val doseId: Long)` in `ui/navigation/Routes.kt`, registered as `composable<DoseDetail>` in the `NavHost` in `PillsnerApp.kt`. Not one of `topLevelDestinations`, so the existing `onTopLevelDestination` check sets `NavigationSuiteType.None` and hides the bar or rail for free — the same mechanism About and the legal screens use.

The id travels in the route rather than in a shared view model, so process death restores the right dose with nothing to rebuild. `DoseDetailViewModel` reads it from `createSavedStateHandle()`, as `MedicineHistoryViewModel` does.

`Long` rather than `DoseId` in the route: `DoseId` is a `@JvmInline value class` and the route is a `kotlinx.serialization` surface. The view model wraps it back into `DoseId` immediately. This is the convention `MedicationFormGraph` and `MedicineHistory` already follow.

Home navigates with `navController.navigate(DoseDetail(doseId.value))`; Close, the back arrow and the system back all `popBackStack()`.

### D4. Timing: one domain function, three states

The early/late decision is pure arithmetic on two instants, so it belongs in the domain and is unit-testable without a device. `domain/intake/DoseTiming.kt`:

```kotlin
enum class DoseTiming { EARLY, ON_TIME, LATE }

/** EARLY when the dose is due in an hour or more; LATE when it is an hour or more overdue. */
fun doseTiming(scheduledAt: Instant, now: Instant): DoseTiming
```

Boundaries, stated once so the tests and the spec agree: exactly one hour before is `EARLY`, one second less is `ON_TIME`; exactly one hour after is `LATE`, one second less is `ON_TIME`. In other words `EARLY` is `now <= scheduledAt - 1h` and `LATE` is `now >= scheduledAt + 1h`. The threshold is a single `Duration` constant, `WARNING_MARGIN = Duration.ofHours(1)`, not a setting.

The view model recomputes the timing whenever the dose emits and whenever the clock ticks past a boundary. It does not poll: it combines the dose stream with a flow that emits once at the next boundary crossing (`delay` until `scheduledAt ∓ 1h`, whichever is still ahead), so a screen left open crosses from early to on time to late on its own without a timer running every second.

Alternative considered: computing the timing in the composable from `Instant.now()`. Rejected: it makes the warning untestable without Compose, and recomposition is not a clock — the banner would change only when something else happened to redraw.

### D5. The screen

`ui/dose/DoseDetailScreen.kt`, a `Scaffold` with a `TopAppBar` (title from `R.string.dose_detail_title`, `ic_arrow_back` navigation icon), matching `AboutScreen` and design system 8.7. Content is one `Column` with `verticalScroll`, capped at `Spacing.contentMaxWidth`, with `Spacing.screenEdge` / `screenEdgeWide` side padding, and the bottom-aligned Close button below it.

Layout follows design system 8.8's full-screen reminder, top to bottom:

1. **Time** — `displayMedium`, tabular figures, the locale-formatted scheduled time from the existing `UpcomingDoseTimeFormatter`; the day label beneath it in `bodyMedium` / `onSurfaceVariant` when the dose is not today, as the tile does.
2. **Medicine name** — `headlineMedium`.
3. **Amount** — `bodyLarge`, through `rememberQuantityFormatter()`, the same formatter the tile and the overview use.
4. **Status chip** — the existing `IntakeStatusChip` with the dose's `IntakeStatus`, so the state reads here the way it reads on the tile.
5. **Timing banner** — present only when the timing is `EARLY` or `LATE` (D7).
6. **Answers** — three full-width stacked buttons in the fixed notification order: `Button` at `Sizes.primaryActionHeight` for "I took it", `FilledTonalButton` for "Not yet", `OutlinedButton` for "Not going to", each at least `Sizes.minTouchTarget`, `Spacing.md` apart. Labels reuse `R.string.reminder_action_took_it`, `reminder_action_not_yet`, `reminder_action_not_going_to` — the same strings the notification builds its actions from, so the wording cannot drift between the two surfaces.
7. **Close** — a `TextButton` (design system 8.4: low-emphasis) pinned below the scrolling content so it is reachable without scrolling past the answers, full width, at least `Sizes.minTouchTarget`.

Nothing sets `maxLines`. At 200 % scale the content column scrolls and Close stays put.

Alternative considered: putting Close in the top app bar as an `ic_close` action and dropping the button. Rejected: the proposal asks for a bottom-aligned Close, and the bottom third is where the design system says the actions a one-handed user reaches for belong.

### D6. The four states the screen can be in

`DoseDetailUiState` is a sealed interface, because "loading", "answerable" and "settled" are not the same screen with a flag:

- `Loading` — the first emission has not arrived. Renders the app bar and an empty surface, so nothing flashes.
- `Answerable(dose, time, status, timing)` — the dose is pending. The layout above, with all three answers enabled.
- `Settled(dose, time, status, outcome, recordedAt)` — the dose has an intake. The answers are replaced by one `bodyLarge` line stating the recorded outcome and when ("Taken at 08:12", "Skipped", "Missed"), the status chip shows it, and Close is the only control. This is what the user sees when the dose was answered from the shade or the watch while this screen was opening or open.

  *Corrected during implementation.* This state first said `outcomeText: String`. Building that string needs `stringResource` and a locale, which would have put a `Context` in the view model and a formatted, localised sentence in the UI state — something no other state in this app does, and something that would then be wrong after an in-app language change until the view model re-emitted. The state carries the `IntakeOutcome` and the `Instant` instead, and the composable turns them into the sentence, reading the locale from `LocalConfiguration` so a language change recomposes it. `Answerable` already worked this way with `amount`, so the two now agree.
- `Gone` — the dose is not there any more, because a refresh withdrew it after the user's own edit. An empty state (design system 8.10) saying the dose is no longer scheduled, with Close.

Answering is fire-and-forget from the screen's point of view: the view model launches `AnswerDose` and emits a one-shot `DoseDetailEffect.Close` through a `Channel`, which the screen collects and turns into `onClose()`. It does not wait for the coordinator's wake to finish before navigating — the wake takes a lock and can take seconds, and the record is already written by then. The Welcome screen's list is a `Flow` off the same database, so by the time the pop animation finishes the tile is already gone.

Alternative considered: a single data class with `isSettled` and nullable fields. Rejected: three of the four states have genuinely different content, and the nullable-field version makes it possible to render answer buttons for an answered dose.

### D7. The timing banner: not red, not a blocker

A `ui/dose/DoseTimingBanner.kt` built on the attention-banner shape (design system 8.9) — `Surface`, `large` shape, an icon, one `bodyLarge` sentence — but **without** its error colours and without an action button:

| Timing | Container / content | Icon | Message |
| --- | --- | --- | --- |
| `EARLY` | `secondaryContainer` / `onSecondaryContainer` | `ic_info` | "This dose is not due yet. You can still record it." |
| `LATE` | `errorContainer` / `onErrorContainer` | `ic_schedule` | "This dose is overdue. You can still record it." |

Early is blue because design system 2.4 reserves red for danger and 2.3 gives blue to a dose that is pending and not yet at its time — being early is information, not a problem. Late reuses the `errorContainer` pair that 2.3 already assigns to Overdue, so the screen agrees with the tile the user just tapped. Both sentences end by saying the dose can still be recorded, because the whole point is that the warning does not take anything away; no button is ever disabled by timing.

The banner carries no `contentDescription` of its own and no `liveRegion`: it is ordinary text in reading order, announced when TalkBack reaches it, and the icon is decorative (`contentDescription = null`).

### D8. The tile becomes the tap target

`DoseTile` gains `onClick: () -> Unit`. The click goes on the same node that already carries the merged semantics, via `Modifier.clickable(role = Role.Button)` — not `Card(onClick = …)`, so the existing `semantics(mergeDescendants = true)` block stays the single source of the node's description and nothing else about the card changes.

The node gains `onClick(label = …)` semantics so TalkBack announces what the tap does ("Open dose"), and the existing `contentDescription` is unchanged, keeping the "one tile, one node, complete description" contract from design system 8.1 and the `welcome-screen` spec.

`WelcomeScreen` takes `onOpenDose: (DoseId) -> Unit` and passes `{ onOpenDose(dose.doseId) }` per tile. Its default stays a no-op so every existing preview and test keeps compiling.

`WelcomeScreenTest` currently asserts the tile has no click action; that assertion is inverted to assert it has one and reports the dose it opens.

### D9. Wiring

`AppContainer`:

- `private val answerDoseUseCase = AnswerDose(doseRepository, recordIntakeUseCase, snoozeDoseUseCase) { dose -> reminderNotifier.cancel(dose); reminderCoordinator.onWake(WakeReason.ACTION) }`, declared after `reminderCoordinator` so the lambda closes over an initialised field.
- `suspend fun answerDose(id: DoseId, answer: DoseAnswer)` replaces the `recordIntake` / `snoozeDose` pair the receiver calls. The two existing methods go, since `AnswerDose` is now the only correct way in.
- An `initializer { DoseDetailViewModel(doseRepository, answerDoseUseCase, createSavedStateHandle(), clock) }` in `viewModelFactory`.

Construction stays cheap and side-effect free, as that class's KDoc requires: `AnswerDose` holds three references and allocates nothing.

### D10. Strings

New keys in `values/strings.xml` with Dutch in `values-nl/strings.xml`:

| Key | English |
| --- | --- |
| `dose_detail_title` | Dose |
| `dose_detail_close` | Close |
| `dose_detail_early` | This dose is not due yet. You can still record it. |
| `dose_detail_late` | This dose is overdue. You can still record it. |
| `dose_detail_taken_at` | Taken at %1$s |
| `dose_detail_skipped` | You chose not to take this dose |
| `dose_detail_missed` | This dose was not taken |
| `dose_detail_gone_title` | This dose is no longer scheduled |
| `dose_detail_gone_hint` | Its medicine or schedule changed. |
| `dose_tile_open` | Open dose |

The three answer labels are reused, not duplicated. Nothing here names a medicine, so nothing here is logged.

### D11. Package layout

```
ui/dose/
  DoseDetailScreen.kt      screen, previews
  DoseDetailUiState.kt     sealed state, effect, IntakeStatus mapping
  DoseDetailViewModel.kt
  DoseTimingBanner.kt
domain/intake/
  AnswerDose.kt            DoseAnswer, the use case
  DoseTiming.kt            DoseTiming, doseTiming()
```

A new `ui/dose` package rather than adding to `ui/home`: the screen is not part of Home, it only starts there, and `ui/home` already holds nine files.

## Risks / Trade-offs

**Two ways to answer a dose can disagree** → D1 makes that structurally impossible: the receiver and the view model call the same use case, and the effects that must follow an answer are one lambda configured in one place. The `AnswerDose` unit test covers all three answers; the existing `ReminderActionReceiverTest` still covers the broadcast path end to end.

**Navigating away before the coordinator finishes** (D6) means the user is back on Home while the wake is still running → acceptable and already the norm: a notification answer returns control to the system the same way. The record is committed before the pop; the reschedule runs in the coordinator's own scope under its lock, and its `finally` block reschedules the next alarm even if the rest of the wake fails, so a reminder is never lost by leaving the screen.

**"Not yet" on the detail screen can silently do nothing** — `SnoozeDose` returns null when the dose has already lapsed, and the answer still closes the screen → the dose is `MISSED` by then, so the list on Home will show it gone and the user is not told why. Mitigated by the boundary the spec fixes: the snooze is bounded by the lapse moment exactly as the notification's is, the behaviour matches what tapping "Not yet" on the notification does in the same situation, and the `Settled` state catches the case where the lapse is already recorded when the screen renders. Telling the user "too late to postpone" is a message the notification does not have either; adding one to both surfaces is a separate proposal.

**The tile becomes clickable, so a mis-tap while scrolling opens a screen** → the screen records nothing by itself and Close is in the bottom third, so a mis-tap costs one tap to undo. Preferable to an inline "Taken" button on the tile, where a mis-tap writes a record the user cannot undo at all.

**A stale timing warning on a screen left open for hours** → D4's boundary-crossing flow re-emits at the crossing, and the dose stream re-emits on any change, so the banner is correct without polling. The worst case is a device asleep across the boundary, where the next resume recomposes with the current instant.

**Deviation from design system 8.1's note** that the tile does not become the tap target → stated here deliberately rather than quietly: that sentence was written for an inline action button, and this change puts the actions on their own screen instead. The `welcome-screen` delta records the tile as a tap target, and the design system's dose tile note is left for a design change to reword if inline actions ever arrive.
