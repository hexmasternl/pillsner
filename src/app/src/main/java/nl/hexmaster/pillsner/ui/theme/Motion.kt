package nl.hexmaster.pillsner.ui.theme

/**
 * docs/design-system.md section 9. The only place a raw animation duration may live.
 *
 * Compose scales every one of these by the system animator duration scale, so the "Remove
 * animations" accessibility setting brings them all to zero without any code here.
 */
object Motion {
    /** Chips, toggles. */
    const val SHORT_MILLIS = 150

    /** Tiles appearing, a state change, a panel folding open or closed. */
    const val MEDIUM_MILLIS = 250

    /** Screen transitions. */
    const val LONG_MILLIS = 350
}
