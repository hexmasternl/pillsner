package nl.hexmaster.pillsner.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The watch's spacing and sizes, in one place, the way `Spacing` and `Sizes` work on the phone.
 *
 * The phone's own scale is not reused: it is drawn for a screen many times this size. What does
 * carry over is the rule that no composable writes a raw dp value.
 */
object WearDimens {
    /** Nothing tappable or readable is smaller than this, as on the phone. */
    val minTouchTarget = 48.dp

    val cardPaddingHorizontal = 12.dp
    val cardPaddingVertical = 8.dp
    val iconSize = 20.dp
    val betweenCards = 6.dp
    val screenEdge = 8.dp

    /**
     * How far plain text — the empty state, the phone footer — stays in from each side, as a
     * fraction of the screen width, on top of the scaffold's own padding. The scaffold's 5.2% is
     * sized for cards, which the scroll transformation narrows near the edges; text is not narrowed,
     * and on a round screen it would otherwise run into the bezel (#107). A fraction, so it scales
     * from small to extra-large round watches the way the scaffold's padding does.
     */
    const val textInsetFraction = 0.12f
}

/** [WearDimens.textInsetFraction] of this watch's screen width. */
@Composable
fun roundTextInset(): Dp = (LocalConfiguration.current.screenWidthDp * WearDimens.textInsetFraction).dp
