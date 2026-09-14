package nl.hexmaster.pillsner.ui.theme

import androidx.compose.ui.unit.dp

/** docs/design-system.md section 5. The only place a raw dp value for spacing may live. */
object Spacing {
    /** Explicit zero: removes a component's default padding without introducing a new magnitude. */
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Horizontal screen padding on compact widths. */
    val screenEdge = lg

    /** Horizontal screen padding at medium width and above. */
    val screenEdgeWide = xl

    /** Content column cap on wide screens; also the width at which "wide" begins. */
    val contentMaxWidth = 600.dp
}

/** Touch targets and component sizes from docs/design-system.md sections 7, 8 and 10. */
object Sizes {
    val minTouchTarget = 48.dp
    val primaryActionHeight = 56.dp
    val tileMinHeight = 72.dp
    val stateStripeWidth = 4.dp
    val statusChipHeight = 28.dp
    val iconDefault = 24.dp

    /** Icon inside a status chip; the one size below iconDefault the design allows (section 8.3). */
    val iconChip = 18.dp
    val iconEmptyState = 64.dp
    val largeFab = 96.dp

    /** The product mark in the welcome header (section 8.7). */
    val logoHeader = 96.dp

    /** A masked PIN entry dot on the unlock and setup screens (design D10, app-login). */
    val pinDot = 16.dp

    /** Border width of an empty (not yet entered) PIN dot. */
    val pinDotStroke = 2.dp

    /**
     * The usage chart's fixed height (app-medicine-usage-history design D5). Tall enough for the
     * shortest bar of a fourteen-bar period to still read, short enough that the summary above it
     * and the legend below it stay on one screen at 100 % font scale.
     */
    val usageChartHeight = 96.dp

    /** The proportional outcome bar above the usage legend (design D5, D6 item 4). */
    val usageBreakdownBarHeight = 12.dp
}
