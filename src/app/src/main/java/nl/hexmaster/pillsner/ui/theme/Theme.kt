package nl.hexmaster.pillsner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Whether [PillsnerTheme] is currently rendering the dark scheme. Lets [tileContainerColor] and
 * previews agree with the theme instead of re-reading the system setting.
 */
val LocalPillsnerDarkTheme = staticCompositionLocalOf { false }

/**
 * Pillsner's Material 3 theme. Renders the scheme it is given and never uses dynamic colour
 * (docs/design-system.md, principles 4 and 5). Wrap the whole app in it once, in MainActivity,
 * after `enableEdgeToEdge()`.
 *
 * @param darkTheme whether to render the dark scheme. MainActivity passes the user's stored theme
 *   choice resolved against the phone (app-theme-setting design D6); the default follows the phone,
 *   which is right for every other caller — previews and tests, which have no choice to hand.
 */
@Composable
fun PillsnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalPillsnerDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PillsnerTypography,
            shapes = PillsnerShapes,
            content = content,
        )
    }
}
