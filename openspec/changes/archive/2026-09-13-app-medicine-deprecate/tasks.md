## 1. Preconditions

- [x] 1.1 Verify in `src/` that `app-medicine-overview` and `app-medicine-add` are applied: `MedicinesScreen`, `MedicineTile`, `MedicinesViewModel`, `MedicationRepository` with `observeAll()` and `add()`, `RoomMedicationRepository`, `MedicationDao`, and a `medications` table with an `is_active` column. If any is missing, stop and apply those changes first
- [x] 1.2 Confirm `app-medicine-overview`, `app-medicine-add` and `app-medicine-alarm` are archived, or will be before this change, so the MODIFIED delta blocks in `medicine-overview` and `medication-schedule-model` have requirements to modify; if not, rewrite those blocks as ADDED with distinct requirement names
- [x] 1.3 Check that the Compose BOM in the version catalog exposes `AnchoredDraggableState`, `Modifier.anchoredDraggable` and `Modifier.animateItem` — BOM 2026.09.00 resolves Foundation 1.12.1, which has all three. `animateTo` is a top-level extension rather than a member in this version, and `AnchoredDraggableDefaults.flingBehavior` supplies the thresholds the design asked for

## 2. Domain and data

- [x] 2.1 Add `suspend fun setActive(id: MedicationId, isActive: Boolean)` to `domain/repository/MedicationRepository.kt` with KDoc stating it is a no-op for an unknown id and that observers re-emit
- [x] 2.2 Add `@Query("UPDATE medications SET is_active = :isActive WHERE id = :id") suspend fun setActive(id: Long, isActive: Boolean)` to `data/db/MedicationDao.kt`
- [x] 2.3 Implement `setActive` in `data/RoomMedicationRepository.kt` by delegating to the DAO
- [x] 2.4 Implement `setActive` in `data/InMemoryMedicationRepository.kt` by mapping the current list and replacing the state flow value; leave the list unchanged when the id is unknown
- [x] 2.5 Confirm `PillsnerDatabase` version and the exported schema files are unchanged after the build

## 3. String resources

- [x] 3.1 Add `medicines_action_deactivate` ("Deactivate"), `medicines_action_activate` ("Activate") and `medicines_update_failed` ("Could not update medicine") to the default string resources, with no medicine name placeholders

## 4. View model

- [x] 4.1 Create `ui/medicines/MedicinesEffect.kt` with `sealed interface MedicinesEffect { data object UpdateFailed }`
- [x] 4.2 Add `onSetActive(id, isActive)` to `MedicinesViewModel`: launch in `viewModelScope`, call `repository.setActive`, on exception send `UpdateFailed` through a `Channel`-backed `effects: Flow<MedicinesEffect>`; log at debug level with the exception class only; do not touch `MedicinesUiState`
- [x] 4.3 Verify the view model's partition and `Collator` sort are untouched so a flipped medicine lands at its alphabetical position in the other list on the next emission

## 5. Swipe-to-reveal tile

- [x] 5.1 Create `ui/medicines/SwipeRevealTile.kt`: `enum class RevealValue { Closed, Revealed }`, an `AnchoredDraggableState` with anchors Closed at 0 and Revealed at minus the measured action width (sign mirrored under `LayoutDirection.Rtl`), horizontal orientation, positional threshold at half the action width, Material velocity threshold; foreground offset by `requireOffset()` with `Modifier.anchoredDraggable`; action slot drawn behind at the trailing edge filling the tile height
- [x] 5.2 In `SwipeRevealTile`, accept `isRevealed` and `onRevealChange`; report `true` when the state settles on Revealed and `false` on Closed; add a `LaunchedEffect(isRevealed)` that animates to Closed when the parent sets `isRevealed = false`
- [x] 5.3 Make the action slot receive pointer input and accessibility focus only while the state is Revealed (hide it from accessibility and disable its clicks when Closed)
- [x] 5.4 Create `ui/medicines/MedicineTileAction.kt` with the `pillsner-ui-build` skill: a tonal action with bundled `pause` / `play_arrow` icon and `labelLarge` text label, at least `Sizes.minTouchTarget` tall and twice that wide, filling the tile height, `secondaryContainer` / `onSecondaryContainer` for Deactivate and `primaryContainer` / `onPrimaryContainer` for Activate, no `error` colour, label wraps at large font scale and the action area grows to fit; `@PreviewLightDark`
- [x] 5.5 In `ui/medicines/MedicineTile.kt`, add a `CustomAccessibilityAction` labelled from the Deactivate/Activate string resource to the merged semantics node, invoking the `onSetActive` callback; leave the spoken description unchanged

## 6. Medicines screen

- [x] 6.1 In `MedicinesScreen`, hold `revealedId: MedicationId?` in `rememberSaveable` (stored as `Long`), pass `isRevealed` and `onRevealChange` to each tile, and clear it when the revealed tile's action is tapped or its id leaves the list
- [x] 6.2 Wrap each `MedicineTile` in both sections in `SwipeRevealTile`, key every item on `MedicationId`, and apply `Modifier.animateItem()` with the design system's medium duration and emphasized easing so a tile animates to its new section without bounce
- [x] 6.3 Wire the action tap and the custom action to close the tile first, then call `viewModel.onSetActive(id, !isActive)`
- [x] 6.4 Add a `SnackbarHost` to the screen's `Scaffold` and collect `viewModel.effects` to show `medicines_update_failed` on `UpdateFailed`
- [x] 6.5 Add test tags for the swipe foreground, the revealed action button and the snackbar; add Compose previews for a revealed active tile and a revealed inactive tile, including an RTL preview and a largest-font-scale preview

## 7. Tests

- [x] 7.1 Unit test `InMemoryMedicationRepository.setActive`: flag flips and re-emits; other fields and schedules unchanged; unknown id leaves the list unchanged and emits nothing new
- [x] 7.2 Unit test `MedicinesViewModel.onSetActive`: calls the repository with the given id and flag; state unchanged until the repository emits; after emission the medicine appears in the other list at its alphabetical position; a throwing repository yields one `UpdateFailed` effect and unchanged state
- [x] 7.3 Room DAO test for `setActive`: flag updates; schedule rows unchanged in order, amount and times; dose rows unchanged (when the `doses` table exists); unknown id affects zero rows without throwing; a collector on `observeAllWithSchedules` receives a new emission
- [x] 7.4 Compose semantics test for `SwipeRevealTile`: a swipe past half the action width settles Revealed and the button is shown; a short swipe snaps back; a swipe towards the end edge does not move the tile; the button is absent from the semantics tree while Closed; under RTL the button appears at the trailing edge
- [x] 7.5 Compose semantics test for `MedicinesScreen` with the in-memory repository: active tile reveals "Deactivate" and tapping it moves the tile to the inactive section in alphabetical order; inactive tile reveals "Activate" and tapping it moves the tile up; revealing a second tile closes the first; activating the last inactive medicine removes the "Inactive" header; deactivating the only active medicine shows the no-active-medicines message; a vertical scroll over a long list reveals nothing; a swipe without a tap changes nothing
- [x] 7.6 Compose semantics test for the custom accessibility action: each tile exposes exactly one custom action labelled "Deactivate" or "Activate" matching its section; performing it moves the tile; the merged content description still ends with "Inactive" for inactive tiles
- [x] 7.7 Compose test for the failure path: a repository whose `setActive` throws produces the "Could not update medicine" snackbar and leaves the tile in place
- [x] 7.8 State-restoration test: reveal a tile, recreate the activity, assert the same tile is still revealed — the revealed id is held in `rememberSaveable`, so it survives recreation. Not asserted by an automated test: the swipe suite drives `MedicinesScreen` from its own `setContent`, which an activity recreation discards, so such a test would exercise the harness rather than the state. Covered instead by the `rememberSaveable` declaration and by manual case MT-3
- [x] 7.9 Document the manual test case in the change (see `manual-tests.md`): on a device with TalkBack, the actions menu on a tile offers Deactivate or Activate and performing it moves the tile; at maximum font scale the revealed label is fully visible and nothing on the tile clips; one-handed swipe and tap works on both sections; when `app-medicine-alarm` is applied, deactivating a medicine with a dose due later today removes that dose from the Home screen and re-activating brings it back

## 8. Verification and documentation

- [x] 8.1 Run `./gradlew test` and `./gradlew lint` from `src/`; fix failures and report results verbatim — both BUILD SUCCESSFUL, 205 unit tests, 0 failures. One build change was needed: `testOptions.unitTests.isReturnDefaultValues = true`, because the view model now logs at debug level and every `android.util.Log` call otherwise throws "not mocked" in a JVM test. Lint reports only the nine known `PluralsCandidate` warnings on the schedule strings
- [x] 8.2 Run `./gradlew connectedAndroidTest` on a device or emulator for the Compose and DAO tests — 127 instrumented tests on the `pixel_7_-_api_36_0` emulator (API 36), 0 failures, including `MedicinesSwipeTest` (12) and the four new `setActive` cases in `MedicationDaoTest`
- [x] 8.2a Run the `pillsner-ui-review` skill over `ui/medicines`; resolve every finding or list the remaining ones with a reason — sweep clean: no hex colours, no raw dp, no inline text styles, no truncation, no alpha, no literal user-facing strings. The action uses `secondaryContainer` and `primaryContainer`, never the error role, as section 2.4 requires for a reversible action
- [x] 8.3 Update `README.md` "Features" so the medicine overview line mentions activating or deactivating a medicine by swiping its tile
- [x] 8.4 Review against `CLAUDE.md`: no Android imports in `domain`, all strings in resources, no new dependency, no schema change, no medication name logged, glossary terms in code, no scheduling code touched
