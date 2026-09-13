package nl.hexmaster.pillsner.ui.medicines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import nl.hexmaster.pillsner.ui.theme.Sizes

/** Whether a tile is sitting flat or pulled aside to show its action. */
enum class RevealValue { Closed, Revealed }

/** The width the action keeps even when its label is short, so it is never a sliver. */
internal val MinimumActionWidth = Sizes.minTouchTarget * 2

/**
 * A tile that can be dragged sideways to uncover one action behind it.
 *
 * The drag goes one way only, towards the start edge, so there is a single gesture to learn and a
 * swipe the other way simply does nothing. The action is drawn at the trailing edge and mirrors
 * under a right-to-left layout, so it is always on the side the tile moves away from.
 *
 * Built on Compose Foundation's anchored dragging, which is what Material's own swipe components
 * use: it brings the thresholds, the fling and the settle animation, and it does not fight the
 * vertical list for touch because the orientations differ.
 *
 * Swiping is not something a screen-reader or switch-access user can do, so the tile itself also
 * offers the action as an accessibility custom action; see [MedicineTile].
 *
 * @param isRevealed whether this tile is the one currently open. The screen owns this, so only one
 *   tile can be open at a time.
 * @param onRevealChange reports that this tile settled open or closed.
 */
@Composable
fun SwipeRevealTile(
    isRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    action: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val actionWidth = remember { mutableIntStateOf(0) }
    val tileHeight = remember { mutableIntStateOf(0) }
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val state = remember { AnchoredDraggableState(initialValue = RevealValue.Closed) }

    val isRtl = layoutDirection == LayoutDirection.Rtl

    // The anchors cannot exist until the action has been measured. They are expressed in the
    // reading direction — "revealed" is always minus the action width — and the two places that
    // touch the screen mirror that for a right-to-left layout: the gesture through
    // `reverseDirection`, the drawing through the sign of the offset below.
    LaunchedEffect(actionWidth.intValue) {
        if (actionWidth.intValue <= 0) return@LaunchedEffect
        state.updateAnchors(
            DraggableAnchors {
                RevealValue.Closed at 0f
                RevealValue.Revealed at -actionWidth.intValue.toFloat()
            },
        )
    }

    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }
            .collect { settled -> onRevealChange(settled == RevealValue.Revealed) }
    }

    // The screen closes every other tile by setting isRevealed to false.
    LaunchedEffect(isRevealed) {
        if (!isRevealed && state.settledValue == RevealValue.Revealed) {
            state.animateTo(RevealValue.Closed)
        }
    }

    val open = state.settledValue == RevealValue.Revealed

    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .height(with(density) { tileHeight.intValue.toDp() })
                .heightIn(min = Sizes.minTouchTarget)
                .onSizeChanged { actionWidth.intValue = it.width }
                // The action stays composed so it can be measured — the anchor is its width — but
                // while the tile is closed it is behind the tile and out of the semantics tree, so
                // as far as the user, a screen reader and a test are concerned it is not there.
                .then(if (open) Modifier else Modifier.clearAndSetSemantics {}),
            contentAlignment = Alignment.Center,
            content = action,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { tileHeight.intValue = it.height }
                // Read during layout, not composition, so a drag does not recompose the tile.
                .offset {
                    val x = if (state.anchors.size > 0) state.requireOffset().roundToInt() else 0
                    IntOffset(if (isRtl) -x else x, 0)
                }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    reverseDirection = isRtl,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(state),
                )
                // A finger that has just swiped is resting on the tile; a tap there should put the
                // tile back rather than navigate somewhere unexpected. Compose's touch slop is what
                // keeps this tap apart from the drag above.
                .clickable { if (open) onRevealChange(false) else onClick() },
            content = { content() },
        )
    }
}
