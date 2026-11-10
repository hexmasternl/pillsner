package nl.hexmaster.pillsner.ui.insets

import android.app.UiAutomation
import android.content.res.Configuration
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.Dp
import androidx.core.view.ViewCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Hosts content the way `MainActivity` does, edge-to-edge, under a synthetic [InsetPreset]
 * (design D1–D3).
 *
 * `createComposeRule()` hosts content in a window that fits the system windows, so Compose sees
 * no insets and an inset bug cannot show. This host launches [activityClass] itself, enables
 * edge-to-edge and replaces whatever insets the system dispatches with the preset's, on every
 * dispatch: a later real dispatch (a keyboard, a rotation settling) cannot overwrite them.
 *
 * One path bypasses it: while a real insets animation runs (the soft keyboard sliding in after a
 * text field takes focus), Compose reads the animation's progress values, and the preset comes
 * back only when the animation ends. [safeDrawingArea] waits for that rather than asserting on a
 * frame in between.
 *
 * Use it as the test's only rule and drive the content through [composeRule]:
 *
 * ```
 * @get:Rule val host = EdgeToEdgeHost(ComponentActivity::class.java)
 *
 * host.show(InsetPreset.THREE_BUTTON) { MyScreen() }
 * host.composeRule.onNodeWithTag(TAG).assertInsideSafeDrawingArea(host)
 * ```
 */
class EdgeToEdgeHost<A : ComponentActivity>(private val activityClass: Class<A>) : TestRule {

    val composeRule: ComposeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<A>? = null
    private var rotated = false
    private var autoRotateWasOn = false
    private var preset: InsetPreset? = null

    /** What `WindowInsets.safeDrawing` read in composition last, in pixels. */
    @Volatile
    private var observedSafeDrawing: PxInsets? = null

    lateinit var activity: A
        private set

    override fun apply(base: Statement, description: Description): Statement =
        composeRule.apply(
            object : Statement() {
                override fun evaluate() {
                    try {
                        base.evaluate()
                    } finally {
                        scenario?.close()
                        if (rotated) restoreRotation()
                    }
                }
            },
            description,
        )

    /**
     * Launches the host under [preset] without content yet, for a test that needs the activity to
     * build its dependencies first. Follow it with [setContent].
     */
    fun launch(preset: InsetPreset): A {
        check(scenario == null) { "The host has already been launched" }
        this.preset = preset
        if (preset.landscape) rotateToLandscape()

        val scenario = ActivityScenario.launch(activityClass).also { scenario = it }
        scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            val density = activity.resources.displayMetrics.density
            ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { _, _ ->
                preset.toWindowInsets(density)
            }
            this.activity = activity
        }
        if (preset.landscape) {
            check(activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                "The device did not rotate to landscape before the host launched"
            }
        }
        return activity
    }

    /** Sets the content of a [launch]ed host and waits until the preset's insets have reached it. */
    fun setContent(content: @Composable () -> Unit) {
        val scenario = checkNotNull(scenario) { "Launch the host before setting its content" }
        scenario.onActivity { activity ->
            activity.setContent {
                RecordSafeDrawing()
                content()
            }
            ViewCompat.requestApplyInsets(activity.window.decorView)
        }
        composeRule.waitForIdle()
    }

    /** [launch] and [setContent] in one, for content that needs nothing from the activity. */
    fun show(preset: InsetPreset, content: @Composable () -> Unit) {
        launch(preset)
        setContent(content)
    }

    /** One dp in pixels on this device. */
    val density: Float
        get() = activity.resources.displayMetrics.density

    /**
     * The window minus the preset's safe drawing insets, in window coordinates.
     *
     * Fails first if Compose did not read the preset's insets, so a test can never pass because
     * the device's own insets happened to be smaller than the preset's (design risk 2).
     */
    fun safeDrawingArea(): Rect {
        val expected = checkNotNull(preset) { "Launch the host first" }.safeDrawingPx(density)
        // A real keyboard animation can briefly put its own values in front of the preset's.
        try {
            composeRule.waitUntil(INSETS_SETTLE_TIMEOUT_MS) { observedSafeDrawing == expected }
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError(
                "WindowInsets.safeDrawing read $observedSafeDrawing in composition, not the $preset preset's " +
                    "$expected; the synthetic insets did not reach the content",
                timeout,
            )
        }
        var width = 0
        var height = 0
        composeRule.runOnIdle {
            width = activity.window.decorView.width
            height = activity.window.decorView.height
        }
        return Rect(
            left = expected.left.toFloat(),
            top = expected.top.toFloat(),
            right = (width - expected.right).toFloat(),
            bottom = (height - expected.bottom).toFloat(),
        )
    }

    /** The rounding allowance for every bounds comparison: half a dp (design D3). */
    val tolerancePx: Float
        get() = density * TOLERANCE_DP

    @Composable
    private fun RecordSafeDrawing() {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val insets = WindowInsets.safeDrawing
        val values = PxInsets(
            left = insets.getLeft(density, layoutDirection),
            top = insets.getTop(density),
            right = insets.getRight(density, layoutDirection),
            bottom = insets.getBottom(density),
        )
        SideEffect { observedSafeDrawing = values }
    }

    private fun rotateToLandscape() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        autoRotateWasOn = Settings.System.getInt(
            instrumentation.targetContext.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0,
        ) == 1
        instrumentation.uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90)
        rotated = true
        instrumentation.waitForIdleSync()
    }

    /** Back to portrait, and auto-rotate back to how the device had it, so later tests see neither. */
    private fun restoreRotation() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_0)
        if (autoRotateWasOn) uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
    }

    private companion object {
        const val TOLERANCE_DP = 0.5f
        const val INSETS_SETTLE_TIMEOUT_MS = 5_000L
    }
}

/** The node's bounds in window coordinates, before any parent clips them. */
fun SemanticsNodeInteraction.unclippedBoundsInWindow(): Rect {
    val node = fetchSemanticsNode()
    val position = node.positionInWindow
    return Rect(
        left = position.x,
        top = position.y,
        right = position.x + node.size.width,
        bottom = position.y + node.size.height,
    )
}

/**
 * Asserts that the whole node lies inside the window minus the [host]'s safe drawing insets:
 * nothing of it under a system bar, the cutout or the keyboard.
 */
fun SemanticsNodeInteraction.assertInsideSafeDrawingArea(
    host: EdgeToEdgeHost<*>,
): SemanticsNodeInteraction {
    val safe = host.safeDrawingArea()
    val bounds = unclippedBoundsInWindow()
    val tolerance = host.tolerancePx
    val inside = bounds.left >= safe.left - tolerance &&
        bounds.top >= safe.top - tolerance &&
        bounds.right <= safe.right + tolerance &&
        bounds.bottom <= safe.bottom + tolerance
    if (!inside) {
        throw AssertionError("Node bounds $bounds reach outside the safe drawing area $safe")
    }
    return this
}

/**
 * Asserts that no more than [maxGap] separates the node's bottom edge from the safe drawing
 * area's: a bar pinned above the keyboard or the navigation bar must sit right on it. A larger gap
 * means an inset was applied twice.
 */
fun SemanticsNodeInteraction.assertBottomGapToSafeAreaAtMost(
    host: EdgeToEdgeHost<*>,
    maxGap: Dp,
): SemanticsNodeInteraction {
    val gap = host.safeDrawingArea().bottom - unclippedBoundsInWindow().bottom
    val allowed = maxGap.value * host.density + host.tolerancePx
    if (gap > allowed) {
        throw AssertionError(
            "Node ends ${gap / host.density} dp above the safe drawing area, more than $maxGap: an inset applied twice?",
        )
    }
    return this
}
