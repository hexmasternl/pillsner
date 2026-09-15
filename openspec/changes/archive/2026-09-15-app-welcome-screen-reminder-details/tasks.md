## 1. Preflight

- [x] 1.1 Verify the project scaffold exists — `src/settings.gradle.kts`, `src/gradle/libs.versions.toml`, `src/app/build.gradle.kts`, `ui/theme/`, `di/AppContainer.kt` — and stop with a note rather than recreating any part of it if something is missing. The scaffold belongs to `app-welcome-screen` alone.
- [x] 1.2 Confirm the `welcome-screen` delta in this change is based on the current agreed text of "Upcoming dose tile content". `openspec/changes/app-welcome-screen-updates/` modifies the same requirement; this change's delta already carries that change's wording plus the tap-target rule, so whichever archives second keeps both. Re-check this if `app-welcome-screen-updates` is edited before this change is archived.
- [x] 1.3 Run `./gradlew :app:testDebugUnitTest :app:lintDebug` from `src` and record the starting state, so a failure introduced by this change is distinguishable from one already there.

## 2. Domain: timing

- [x] 2.1 Add `domain/intake/DoseTiming.kt` with `enum class DoseTiming { EARLY, ON_TIME, LATE }`, a `WARNING_MARGIN = Duration.ofHours(1)` constant and `fun doseTiming(scheduledAt: Instant, now: Instant): DoseTiming`. No Android imports. KDoc states the boundaries explicitly (design D4).
- [x] 2.2 Add `DoseTimingTest`: more than an hour early, exactly an hour early, one second inside the early boundary, at the scheduled moment, five minutes late, one second inside the late boundary, exactly an hour late, and hours late. Cover a dose scheduled across a daylight-saving transition to confirm the arithmetic is on instants and not on wall-clock times.

## 3. Domain: one path for every answer

- [x] 3.1 Add `fun observe(id: DoseId): Flow<Dose?>` to `domain/repository/DoseRepository.kt` with KDoc saying what a null emission means (design D2).
- [x] 3.2 Implement it in `data/db/DoseDao.kt` as a `@Query` on the primary key returning `Flow<DoseEntity?>`, and in `data/RoomDoseRepository.kt` mapping it the way `observePending` already maps. No schema change, so no migration.
- [x] 3.3 Implement it in `data/InMemoryDoseRepository.kt` from the store it already keeps.
- [x] 3.4 Add `domain/intake/AnswerDose.kt` with `enum class DoseAnswer { TAKEN, SNOOZE, SKIP }` and the use case that loads the dose, applies the answer through `RecordIntake` or `SnoozeDose`, then invokes its injected `onAnswered: suspend (Dose) -> Unit` (design D1). No Android imports; a dose that is absent or already answered is a no-op.
- [x] 3.5 Add `AnswerDoseTest` against `InMemoryDoseRepository`: each of the three answers records what the `dose-detail` spec says it records, `onAnswered` is invoked once with the dose in every case, a dose that is already answered changes nothing, and an absent dose changes nothing and does not throw.
- [x] 3.6 Add a `DoseDaoTest` case for `observe(id)`: it emits the dose, re-emits when an intake is recorded, and emits null after the row is withdrawn.

## 4. Wire the single answer path into the reminder side

- [x] 4.1 In `di/AppContainer.kt`, construct `AnswerDose` after `reminderCoordinator`, with `onAnswered` calling `reminderNotifier.cancel(dose)` and then `reminderCoordinator.onWake(WakeReason.ACTION)`. Expose `suspend fun answerDose(id: DoseId, answer: DoseAnswer)` and remove the now-unused `recordIntake` and `snoozeDose` methods (design D9). Construction stays free of side effects.
- [x] 4.2 Rewrite the body of `data/reminders/ReminderActionReceiver.kt` to map `ReminderAction` onto `DoseAnswer` and call `container.answerDose(...)`, keeping its intent parsing, `goAsync` handling, receiver budget and debug-level logging exactly as they are. No medicine name or amount is logged.
- [x] 4.3 Run `ReminderActionReceiverTest` and `ReminderWakeTest` unchanged; they are the regression net for the notification path and must still pass.

## 5. Dose detail state and view model

- [x] 5.1 Add `ui/dose/DoseDetailUiState.kt`: the sealed interface with `Loading`, `Answerable`, `Settled` and `Gone` (design D6), the mapping from a `Dose` plus `now` onto an `IntakeStatus`, and `DoseDetailEffect.Close`.
- [x] 5.2 Add `ui/dose/DoseDetailViewModel.kt`: reads the dose id from `SavedStateHandle`, combines `doseRepository.observe(id)` with the boundary-crossing flow from design D4 into `StateFlow<DoseDetailUiState>`, exposes a `Channel`-backed effect flow, and offers `onAnswer(DoseAnswer)` which launches `AnswerDose` and emits `Close`.
- [x] 5.3 Add `DoseDetailViewModelTest`: a pending dose becomes `Answerable` with the right timing; an answered dose becomes `Settled` with the recorded outcome and moment; a null emission becomes `Gone`; an answer recorded elsewhere while collecting moves `Answerable` to `Settled`; each answer calls through to `AnswerDose` with the right `DoseAnswer` and emits `Close`; the timing changes from `EARLY` to `ON_TIME` when the virtual clock crosses the boundary without the dose re-emitting.
- [x] 5.4 Register the view model in `AppContainer.viewModelFactory` with `createSavedStateHandle()`.

## 6. Dose detail screen

- [x] 6.1 Add the strings from design D10 to `res/values/strings.xml` and their Dutch translations to `res/values-nl/strings.xml`. Reuse `reminder_action_took_it`, `reminder_action_not_yet` and `reminder_action_not_going_to`; do not duplicate them.
- [x] 6.2 Add `ui/dose/DoseTimingBanner.kt` following design D7: the attention-banner shape without its action button, `secondaryContainer` for early and `errorContainer` for late, each with its icon and a `bodyLarge` sentence. Previews for both, light and dark, and at 200 % font scale.
- [x] 6.3 Add `ui/dose/DoseDetailScreen.kt` following design D5: `Scaffold` with a `TopAppBar` and back arrow, the scrolling content column capped at `Spacing.contentMaxWidth`, the time, name, amount, status chip, the timing banner, the three stacked full-width answer buttons in the fixed order, and the bottom-aligned "Close". Every value a theme token; nothing sets `maxLines`.
- [x] 6.4 Render the `Settled` and `Gone` states in the same screen: the outcome line instead of the answers for `Settled`, the empty state for `Gone`, and `Close` in both. `Loading` renders the app bar and an empty surface.
- [x] 6.5 Add the accessibility contract: decorative icons get `contentDescription = null`, the back arrow and every button have labels, the status chip carries a `stateDescription`, and the time, name, amount and warning are ordinary text in reading order.
- [x] 6.6 Add previews with `@PreviewLightDark` and `@Preview(fontScale = 2f)` for: on-time pending dose, early pending dose, late pending dose, taken dose, skipped dose, and the no-longer-scheduled state.
- [x] 6.7 Collect `DoseDetailEffect.Close` in the screen and turn it into `onClose()`.

## 7. Open it from the Welcome screen

- [x] 7.1 Add `onClick: () -> Unit` to `ui/home/DoseTile.kt` via `Modifier.clickable(role = Role.Button)` on the node that already carries the merged semantics, plus `onClick(label = …)` semantics from `R.string.dose_tile_open`. The existing `contentDescription` is unchanged (design D8).
- [x] 7.2 Add `onOpenDose: (DoseId) -> Unit = {}` to `ui/home/WelcomeScreen.kt` and pass it per tile.
- [x] 7.3 Add `@Serializable data class DoseDetail(val doseId: Long)` to `ui/navigation/Routes.kt`, with KDoc saying it is not a top-level destination and why the id travels in the route (design D3).
- [x] 7.4 Register `composable<DoseDetail>` in the `NavHost` in `ui/PillsnerApp.kt`, wire Home's `onOpenDose` to `navController.navigate(DoseDetail(it.value))`, and give the screen `onClose = { navController.popBackStack() }`. Confirm the existing `onTopLevelDestination` check hides the navigation suite on it with no extra code.

## 8. Tests for the screen and the flow

- [x] 8.1 Update `WelcomeScreenTest`: invert the assertion that the tile has no click action, and assert that tapping the third of three tiles reports the third dose's id.
- [x] 8.2 Add `ui/dose/DoseDetailScreenTest` (semantics): the four controls are present and labelled for a pending dose; the early and late warnings appear and disappear with the timing; every answer button stays enabled while a warning is shown; `Settled` shows the outcome and no answers; `Gone` shows the empty state and only `Close`; the screen renders without clipping at 200 % font scale.
- [x] 8.3 Add a navigation test alongside `PillsnerAppNavigationTest`: tapping a tile opens the destination with the navigation bar hidden; back returns to Home with Home selected; `Close` does the same; after an answer, back from Home does not return to the detail destination.
- [x] 8.4 Run `pillsner-ui-review` over `ui/dose/` and the changed files in `ui/home/`, and fix everything it reports before calling any UI task done.

## 9. Verification

- [x] 9.1 Run `./gradlew :app:testDebugUnitTest` from `src` and report failures verbatim.
- [x] 9.2 Run `./gradlew :app:lintDebug` from `src`; confirm no missing Dutch translation and no new warning.
- [x] 9.3 Run `./gradlew :app:connectedDebugAndroidTest` from `src` — database, notification and scheduling code changed, so the instrumented suite is required.
- [x] 9.4 Manual pass on a device, recorded in the change: answer a dose from the detail screen while its notification is showing and confirm the notification goes and the next reminder is for the next dose; answer the same dose from the notification while the detail screen is open and confirm the screen switches to the settled state; open a dose more than an hour early and more than an hour late and confirm both warnings and that both let the dose be recorded; leave the screen open across the one-hour boundary and confirm the warning changes on its own; rotate and kill the process with the screen open. — written up in `manual-tests.md`; needs a physical device, not yet run
- [x] 9.5 Re-read `proposal.md` against what was built. If the design turned out wrong anywhere, update `design.md` and say so rather than diverging quietly.
