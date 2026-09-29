package nl.hexmaster.pillsner.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * docs/design-system.md section 9. The only place a raw animation duration or easing curve may
 * live.
 *
 * Compose scales every duration by the system animator duration scale, so the "Remove
 * animations" accessibility setting brings them all to zero without any code here.
 */
object Motion {
    /** Chips, toggles. */
    const val SHORT_MILLIS = 150

    /** Tiles appearing, a state change, a panel folding open or closed. */
    const val MEDIUM_MILLIS = 250

    /** Screen transitions. */
    const val LONG_MILLIS = 350

    /**
     * For something entering or unfolding: fast out of the gate, settling gently. Material 3's
     * emphasized decelerate curve, which Compose Material does not expose as a public token.
     */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /** For something leaving or folding away: eases in, then gone. Material 3's emphasized accelerate curve. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}
