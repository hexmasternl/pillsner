package nl.hexmaster.pillsner.ui.insets

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max
import kotlin.math.roundToInt

/** Insets on each side of the window, in dp, for one kind of system UI. */
data class SideInsets(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp,
) {
    fun toPx(density: Float): PxInsets = PxInsets(
        left = left.toPx(density),
        top = top.toPx(density),
        right = right.toPx(density),
        bottom = bottom.toPx(density),
    )
}

/** Insets on each side of the window, in whole pixels, as the platform dispatches them. */
data class PxInsets(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0) {

    /** The larger of the two on each side, which is how Compose combines insets with `union`. */
    infix fun union(other: PxInsets) = PxInsets(
        left = max(left, other.left),
        top = max(top, other.top),
        right = max(right, other.right),
        bottom = max(bottom, other.bottom),
    )

    fun toInsets(): Insets = Insets.of(left, top, right, bottom)
}

/**
 * A named set of system insets a test dispatches in place of the device's own (design D2), so an
 * inset bug shows the same way on any emulator, whatever its navigation mode, cutout or keyboard.
 *
 * The values are the common phone ones: a 24 dp status bar, a 24 dp gesture handle or a 48 dp
 * 3-button bar, and a keyboard of about 300 dp. In landscape the 3-button bar moves to the side
 * and the camera cutout sits on the opposite edge.
 *
 * @property landscape whether the host has to be rotated before it launches for these insets to
 *   describe the window.
 */
enum class InsetPreset(
    val statusBars: SideInsets,
    val navigationBars: SideInsets,
    val displayCutout: SideInsets = SideInsets(),
    val ime: Dp = 0.dp,
    val landscape: Boolean = false,
) {
    GESTURE(
        statusBars = SideInsets(top = 24.dp),
        navigationBars = SideInsets(bottom = 24.dp),
    ),
    THREE_BUTTON(
        statusBars = SideInsets(top = 24.dp),
        navigationBars = SideInsets(bottom = 48.dp),
    ),
    LANDSCAPE_CUTOUT(
        statusBars = SideInsets(top = 24.dp),
        navigationBars = SideInsets(right = 48.dp),
        displayCutout = SideInsets(left = 32.dp),
        landscape = true,
    ),

    /** The keyboard over a 3-button bar; its inset is measured from the window's bottom edge. */
    KEYBOARD_OPEN(
        statusBars = SideInsets(top = 24.dp),
        navigationBars = SideInsets(bottom = 48.dp),
        ime = 300.dp,
    ),
    ;

    /**
     * What `WindowInsets.safeDrawing` must read in composition once this preset is applied: the
     * system bars, the cutout and the keyboard, combined side by side.
     */
    fun safeDrawingPx(density: Float): PxInsets =
        statusBars.toPx(density) union
            navigationBars.toPx(density) union
            displayCutout.toPx(density) union
            PxInsets(bottom = ime.toPx(density))

    /** The preset as the platform would dispatch it to the window's views. */
    fun toWindowInsets(density: Float): WindowInsetsCompat {
        val builder = WindowInsetsCompat.Builder()
        builder.setType(WindowInsetsCompat.Type.statusBars(), statusBars.toPx(density))
        builder.setType(WindowInsetsCompat.Type.navigationBars(), navigationBars.toPx(density))
        builder.setType(WindowInsetsCompat.Type.displayCutout(), displayCutout.toPx(density))
        builder.setType(WindowInsetsCompat.Type.ime(), PxInsets(bottom = ime.toPx(density)))
        return builder.build()
    }
}

private fun WindowInsetsCompat.Builder.setType(type: Int, insets: PxInsets) {
    setInsets(type, insets.toInsets())
    setVisible(type, insets != PxInsets())
}

private fun Dp.toPx(density: Float): Int = (value * density).roundToInt()
