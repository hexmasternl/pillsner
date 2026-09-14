package nl.hexmaster.pillsner.wear.ui.theme

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
}
