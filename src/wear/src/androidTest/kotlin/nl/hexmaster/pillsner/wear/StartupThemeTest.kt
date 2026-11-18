package nl.hexmaster.pillsner.wear

import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.core.splashscreen.R as SplashR

/**
 * The window behind the first frame and the startup screen are black, with the launcher icon
 * (`wearable-app` "Black watch background", change `wear-black-background` design D2). Google Play
 * rejects a Wear OS app that doesn't start on black with its icon (WO-V13, WO-V15).
 *
 * The frames from launch to the agenda are checked on a device in task 3.2; this test pins the
 * theme the platform and the splash library draw those frames from.
 */
@RunWith(AndroidJUnit4::class)
class StartupThemeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun runningActivity_windowBackgroundIsBlack() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val value = activity.theme.resolve(android.R.attr.windowBackground)
                assertColor("android:windowBackground", value)

                val background = activity.window.decorView.background
                assertTrue(
                    "Window background is ${background?.javaClass?.name}, expected a ColorDrawable",
                    background is ColorDrawable,
                )
                assertEquals(hex(Color.BLACK), hex((background as ColorDrawable).color))
            }
        }
    }

    @Test
    fun startingTheme_splashBackgroundIsBlack() {
        val value = startingTheme().resolve(SplashR.attr.windowSplashScreenBackground)
        assertColor("windowSplashScreenBackground", value)
    }

    @Test
    fun startingTheme_splashIconIsTheFlatLauncherArtwork() {
        // Not @mipmap/ic_launcher: the library draws only an adaptive icon's foreground below API 31
        // and draws it 60 dp across. splash_icon is the same artwork, flat and inset to 48 dp (D2).
        val value = startingTheme().resolve(SplashR.attr.windowSplashScreenAnimatedIcon)
        assertEquals(
            "windowSplashScreenAnimatedIcon is ${context.resources.nameOf(value.resourceId)}",
            R.drawable.splash_icon,
            value.resourceId,
        )
    }

    /** The theme the manifest gives MainActivity: what the platform draws the starting window from. */
    private fun startingTheme(): Resources.Theme {
        val info = context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)
        return ContextThemeWrapper(context, info.themeResource).theme
    }

    private fun Resources.Theme.resolve(attr: Int): TypedValue {
        val value = TypedValue()
        val found = resolveAttribute(attr, value, true)
        assertTrue("Theme does not set ${context.resources.nameOf(attr)}", found)
        return value
    }

    private fun assertColor(name: String, value: TypedValue) {
        assertTrue(
            "$name is not a colour (TypedValue type ${value.type})",
            value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT,
        )
        assertEquals(name, hex(Color.BLACK), hex(value.data))
    }

    private fun hex(color: Int) = "#%08X".format(color)

    private fun Resources.nameOf(id: Int): String =
        runCatching { getResourceName(id) }.getOrDefault("0x%08X".format(id))
}
