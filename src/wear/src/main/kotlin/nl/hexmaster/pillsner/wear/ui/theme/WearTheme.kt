package nl.hexmaster.pillsner.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * The watch palette: the dark column of `docs/design-system.md` section 2.2, because a watch face
 * is always dark. The only file in this module with a hex literal, exactly as on the phone.
 *
 * Typography and shapes stay the Wear Material 3 defaults (design D6): the phone's Montserrat and
 * Raleway scale is drawn for phone screens, and another megabyte of fonts buys a watch nothing.
 */
private object WearPalette {
    val Primary = Color(0xFF8CD8B0)
    val OnPrimary = Color(0xFF00382A)
    val PrimaryContainer = Color(0xFF005E43)
    val OnPrimaryContainer = Color(0xFFA8F0CE)
    val Secondary = Color(0xFFA2C9FF)
    val OnSecondary = Color(0xFF003259)
    val SecondaryContainer = Color(0xFF1D4E7F)
    val OnSecondaryContainer = Color(0xFFD3E4FF)
    val Tertiary = Color(0xFF83D4E0)
    val OnTertiary = Color(0xFF00363D)
    val TertiaryContainer = Color(0xFF005A66)
    val OnTertiaryContainer = Color(0xFFB5EEF6)
    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)
    val Background = Color(0xFF101413)
    val OnBackground = Color(0xFFE1E3E0)
    val Surface = Color(0xFF1D211F)
    val OnSurface = Color(0xFFE1E3E0)
    val SurfaceVariant = Color(0xFF3F4945)
    val OnSurfaceVariant = Color(0xFFBFC9C4)
    val Outline = Color(0xFF89938E)
    val OutlineVariant = Color(0xFF3F4945)
}

private val PillsnerWearColorScheme = ColorScheme(
    primary = WearPalette.Primary,
    onPrimary = WearPalette.OnPrimary,
    primaryContainer = WearPalette.PrimaryContainer,
    onPrimaryContainer = WearPalette.OnPrimaryContainer,
    secondary = WearPalette.Secondary,
    onSecondary = WearPalette.OnSecondary,
    secondaryContainer = WearPalette.SecondaryContainer,
    onSecondaryContainer = WearPalette.OnSecondaryContainer,
    tertiary = WearPalette.Tertiary,
    onTertiary = WearPalette.OnTertiary,
    tertiaryContainer = WearPalette.TertiaryContainer,
    onTertiaryContainer = WearPalette.OnTertiaryContainer,
    error = WearPalette.Error,
    onError = WearPalette.OnError,
    errorContainer = WearPalette.ErrorContainer,
    onErrorContainer = WearPalette.OnErrorContainer,
    background = WearPalette.Background,
    onBackground = WearPalette.OnBackground,
    surfaceContainerLow = WearPalette.Background,
    surfaceContainer = WearPalette.Surface,
    surfaceContainerHigh = WearPalette.SurfaceVariant,
    onSurface = WearPalette.OnSurface,
    onSurfaceVariant = WearPalette.OnSurfaceVariant,
    outline = WearPalette.Outline,
    outlineVariant = WearPalette.OutlineVariant,
)

/** Wraps the watch app in Pillsner's colours. No dynamic colour, as on the phone. */
@Composable
fun PillsnerWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PillsnerWearColorScheme, content = content)
}
