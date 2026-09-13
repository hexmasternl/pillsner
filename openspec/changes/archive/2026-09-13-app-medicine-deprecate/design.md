## Context

After `app-medicine-overview` and `app-medicine-add` the Medicines screen is a single `LazyColumn` with an "Active" section and an "Inactive" section, each tile a `MedicineTile` `Card` per design system section 8.2 (active on the tile container tier, inactive on `surfaceContainerLow` with an `Inactive` chip) showing the name and one description per schedule. `MedicinesViewModel` maps `MedicationRepository.observeAll()` into `MedicinesUiState(active, inactive)`, partitioning on `isActive` and sorting each list with a locale `Collator`. `RoomMedicationRepository` implements `observeAll()` and `add()` over a `medications` table that already has an `is_active` column since schema version 1. Tiles are not interactive.

`app-medicine-alarm` adds a `ReminderCoordinator` that collects `observeAll()` in an application-scoped coroutine and, on every emission, re-plans the two-day dose window: planned doses of inactive medicines that have not been reminded yet are deleted, and the single next-wake alarm is recomputed. That change is the only place scheduling logic lives, and this design deliberately leans on it rather than duplicating anything.

Constraints from `CLAUDE.md` that shape this design: strings in resources, Material 3 accessibility (large fonts, TalkBack, one hand), no new dependencies without a named reason, domain layer without Android imports, never log medication names, single DI mechanism, every visual decision from `docs/design-system.md` tokens checked with `pillsner-ui-review`. The product promise "confirm in one tap, keep the data on the device" extends naturally to "stop a medicine in one gesture".

## Goals / Non-Goals

**Goals:**
- Swipe any tile sideways to reveal one action button: Deactivate on active tiles, Activate on inactive tiles.
- Tapping the button flips the active flag through the repository, so the tile moves to the other section at its alphabetical position and the reminder coordinator reacts on its own.
- At most one tile is revealed at a time; revealing another closes the previous one.
- The same action is reachable without a swipe gesture, for TalkBack, switch access and keyboard users.
- No new dependency, no schema change, no scheduling code.

**Non-Goals:**
- Delete, edit or detail navigation from a tile.
- Confirmation dialogs, undo snackbars, haptics.
- Dismissing a reminder notification already showing for the medicine being deactivated.
- Any change to sorting or partitioning; both remain as the overview change defines them.

## Decisions

### D1. Repository contract: `setActive` as a single targeted write

```kotlin
interface MedicationRepository {
    fun observeAll(): Flow<List<Medication>>
    suspend fun add(medication: NewMedication): MedicationId
    /** Sets the active flag of one medication. No-op when the id is unknown. Observers re-emit on change. */
    suspend fun setActive(id: MedicationId, isActive: Boolean)
}
```

`MedicationDao` gains `@Query("UPDATE medications SET is_active = :isActive WHERE id = :id") suspend fun setActive(id: Long, isActive: Boolean)`. Room's invalidation tracker makes `observeAllWithSchedules()` re-emit, so the overview and the reminder coordinator both see the change with no extra plumbing. `InMemoryMedicationRepository` maps the list and replaces the flow value.

*Why a targeted update rather than a general `update(medication)`:* the only field this change edits is the flag. A whole-medication update would have to reconcile the schedules table (delete and reinsert, or diff by position) for a case that never touches schedules, and would invite races with a future edit screen. The edit change can add `update` when it has a reason to; `setActive` stays as the cheap path.

*Why no-op on unknown id:* the tile the user swiped came from the same stream, so the id exists in practice. If the medicine was removed in between, doing nothing and letting the stream correct the screen is safer than throwing into the UI.

### D2. View model: one event, no optimistic state

`MedicinesViewModel` gains:

```kotlin
fun onSetActive(id: MedicationId, isActive: Boolean)
val effects: Flow<MedicinesEffect>          // Channel-backed, one-shot
sealed interface MedicinesEffect { data object UpdateFailed : MedicinesEffect }
```

`onSetActive` launches in `viewModelScope`, calls `repository.setActive`, and on any exception sends `UpdateFailed`, which the screen shows as a snackbar "Could not update medicine". The failure is logged at debug level with the exception type only, never the name.

*Why not update `MedicinesUiState` optimistically:* Room emits within milliseconds on the same device, so the tile moves visibly fast enough. Optimistic state would need a second source of truth and a rollback path for the failure case, for no perceptible gain. The view model stays a pure function of the repository stream plus one command.

### D3. Swipe-to-reveal with Compose Foundation's anchored draggable

`SwipeRevealTile` wraps each `MedicineTile`:

```kotlin
enum class RevealValue { Closed, Revealed }

@Composable
fun SwipeRevealTile(
    isRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    action: @Composable BoxScope.() -> Unit,     // the button, drawn behind the tile at the trailing edge
    content: @Composable () -> Unit,             // the MedicineTile
)
```

Internally an `AnchoredDraggableState<RevealValue>` with anchors `Closed → 0f` and `Revealed → -actionWidthPx` (sign flipped under `LayoutDirection.Rtl` so the action always sits at the trailing edge), `Orientation.Horizontal`, positional threshold at half the action width and the Material velocity threshold. The foreground carries `Modifier.anchoredDraggable(state, Orientation.Horizontal)` and is offset by `state.requireOffset()`. The action `Box` is the same height as the tile, at least 96 dp wide, and aligned to the trailing edge; it only receives pointer input and accessibility focus while the state is `Revealed`.

*Why the foreground only drags towards the start:* one direction, one action, one thing to learn. The anchors permit no positive offset, so a swipe the other way is a no-op rather than a second gesture to discover. In RTL the same rule mirrors so the button is still at the trailing edge.

*Why `anchoredDraggable` over Material 3 `SwipeToDismissBox`:* `SwipeToDismissBox` models an item leaving the list; its settled states are "dismissed" with the item fully off screen, and the product asked for a button that stays visible for a tap. `anchoredDraggable` is the primitive under `SwipeToDismissBox`, is already on the classpath, gives fling and snap animation, thresholds, and correct disambiguation from the vertical `LazyColumn` scroll because the orientations differ.

*Why not `detectHorizontalDragGestures` by hand:* it would re-implement thresholds, velocity handling and settle animations, and would fight the lazy list for touch slop. Not worth owning.

### D4. One revealed tile at a time, held by the screen

`MedicinesScreen` keeps `revealedId: MedicationId?` in `rememberSaveable` (saved as the `Long`). Each tile receives `isRevealed = tile.id == revealedId` and `onRevealChange`. When a tile settles on `Revealed` it reports `true` and the screen stores its id; every other tile now sees `isRevealed = false` and a `LaunchedEffect(isRevealed)` animates its state back to `Closed`. Tapping the action, swiping closed, or the tile disappearing from the list all clear the id.

`LazyColumn` items are keyed on `MedicationId` and use `Modifier.animateItem()`, so when the flag flips the tile slides from one section to its alphabetical position in the other rather than disappearing and reappearing. Because both sections live in the same list, the animation crosses the section boundary.

*Why the screen rather than the view model holds `revealedId`:* it is transient gesture state with no domain meaning. Putting it in `MedicinesUiState` would make the view model care about touch mechanics and the repository stream would have to be merged with it on every emission.

### D5. Accessibility: the action exists without the gesture

The merged semantics node of every tile gains one `CustomAccessibilityAction` labelled "Deactivate" or "Activate" (same string resources as the button) that calls `onSetActive`. TalkBack exposes it in the actions menu; switch access and keyboard users reach it through the same mechanism. The tile's spoken description is unchanged, so an inactive tile still ends with "Inactive".

The revealed button is a `FilledTonalButton`-styled action with a `labelLarge` text label and an icon (`pause` for Deactivate, `play_arrow` for Activate, Material Symbols Rounded bundled as vector drawables), fills the tile height, meets `Sizes.minTouchTarget`, and uses `secondaryContainer` / `onSecondaryContainer` for Deactivate (blue means information and place, section 2.4) and `primaryContainer` / `onPrimaryContainer` for Activate (green means go). Neither uses the `error` role: the action is reversible and not destructive, and section 2.4 reserves red for danger. Its corners follow the tile's `shapes.large` on the trailing side. The label wraps at large font scales; the action area grows to fit its text with a minimum width of twice `Sizes.minTouchTarget` so the label never clips.

*Why a custom action rather than a visible always-on button:* a permanent button on every tile clutters the list and competes with the product's one-tap dose confirmation for attention. The swipe keeps the list calm; the custom action keeps it usable for everyone.

### D6. Reminder consequences stay in `app-medicine-alarm`

Nothing in this change touches doses or alarms. `RoomMedicationRepository.setActive` writes the flag; Room emits; the reminder coordinator's collector runs `RefreshPlannedDoses`, which drops un-reminded pending doses of the now-inactive medicine and recomputes the single alarm. Reactivating a medicine inserts its planned doses for the current two-day window on the next refresh, in the same way that adding a new medicine does.

A reminder already posted for a dose of the deactivated medicine stays, because the alarm change never touches reminded doses. The user can still take, skip or let it lapse. This is called out as an open question rather than solved here.

### D7. Strings

New resources: `medicines_action_deactivate` ("Deactivate"), `medicines_action_activate` ("Activate"), `medicines_update_failed` ("Could not update medicine"). The custom accessibility actions reuse the first two. No medicine name is interpolated into any new string, so no new privacy surface.

### D8. Package layout

```
domain/repository/MedicationRepository.kt     + setActive()
data/db/MedicationDao.kt                       + setActive() update query
data/RoomMedicationRepository.kt               + setActive()
data/InMemoryMedicationRepository.kt           + setActive()
ui/medicines/MedicinesViewModel.kt             + onSetActive(), effects
ui/medicines/MedicinesEffect.kt
ui/medicines/SwipeRevealTile.kt                anchored draggable wrapper, RevealValue
ui/medicines/MedicineTileAction.kt             the Activate/Deactivate button
ui/medicines/MedicineTile.kt                   + customActions semantics
ui/medicines/MedicinesScreen.kt                revealedId state, keyed items, animateItem, snackbar host
```

## Risks / Trade-offs

- [Horizontal drag competes with vertical list scroll] → The two gestures have different orientations, so Compose's touch-slop logic assigns each drag to one of them. The Compose test drives both a horizontal swipe and a vertical scroll on the same list to confirm neither breaks the other.
- [Swipe is undiscoverable] → Every tile carries the custom accessibility action, and the first release notes it in the README features line. If users still do not find it, a one-time hint or a visible overflow menu can be proposed later without changing the data path.
- [Accidental deactivation] → The button must be tapped after the swipe settles; a swipe alone changes nothing. The action is reversible with the opposite swipe in the other section.
- [The tile moves while the user's finger is still near it] → The move is animated with `animateItem()`, and the revealed state is cleared before the write so no tile is left half-open in the new section.
- [`anchoredDraggable` API surface differs between Compose Foundation versions] → The version catalog pins Compose BOM 2026.08.00 (Foundation 1.12.0), in which `AnchoredDraggableState`, `Modifier.anchoredDraggable` and `Modifier.animateItem` are stable; the wrapper is isolated in one file so an API rename is a one-file fix.
- [The tile animates section changes] → Motion follows design system section 9: 250 ms medium duration with `EmphasizedDecelerate` in and `EmphasizedAccelerate` out, no bounce, and Compose honours the system animator scale so "Remove animations" is respected.
- [Reminder already showing for a deactivated medicine] → Left in place by design (D6). Listed as an open question for the alarm change.
- [MODIFIED delta blocks target requirements that only exist after earlier changes are archived] → Archive order is stated in the proposal and repeated in the tasks; fallback is rewriting those blocks as ADDED with distinct names.

## Migration Plan

Additive. No schema change, no data migration: the `is_active` column exists since version 1 and its meaning does not change. Rollback is removing the `SwipeRevealTile` wrapper, the action button, the custom action, the view model event and the `setActive` members. Data written by this change (a flipped flag) remains valid for every earlier version of the code.

## Open Questions

- Should deactivating a medicine cancel a reminder notification that is currently showing for it? Recommended: yes, but it belongs to `app-medicine-alarm` (its `ReminderNotifier` owns notification ids) and should be proposed there once this change exists.
- Should reactivating a medicine late in the day suppress reminders for doses earlier that day? Today the refresh treats it like a newly added medicine. Same owner as above.
- Should the swipe be discoverable through a first-run hint on the tile? Not until user feedback asks for it.
