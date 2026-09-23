package nl.hexmaster.pillsner.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Every colour in the app. This is the only Kotlin file that may contain a hex literal.
 * Values come from docs/design-system.md section 2.2 and are contrast-checked there.
 */
internal object PillsnerPalette {
    // Light
    val LightPrimary = Color(0xFF1B7F5C)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFA8F0CE)
    val LightOnPrimaryContainer = Color(0xFF00381F)
    val LightSecondary = Color(0xFF2D6DA8)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFD3E4FF)
    val LightOnSecondaryContainer = Color(0xFF0B2F52)
    val LightTertiary = Color(0xFF0F7C8C)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFB5EEF6)
    val LightOnTertiaryContainer = Color(0xFF00363D)
    val LightError = Color(0xFFBA1A1A)
    val LightOnError = Color(0xFFFFFFFF)
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightOnErrorContainer = Color(0xFF410002)
    // Amber, used only for the medicine tile's "Stock low" indicator (design-system.md 2.2): the
    // one step between the green "In stock" and the red "Critical stock" reserved-red case.
    val LightWarningContainer = Color(0xFFFFDEA6)
    val LightOnWarningContainer = Color(0xFF271900)
    val LightSurface = Color(0xFFF9FBFA)
    val LightOnSurface = Color(0xFF191C1B)
    val LightSurfaceVariant = Color(0xFFDCE5E0)
    val LightOnSurfaceVariant = Color(0xFF3F4945)
    val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val LightSurfaceContainerLow = Color(0xFFF3F6F4)
    val LightSurfaceContainer = Color(0xFFEDF1EF)
    val LightSurfaceContainerHigh = Color(0xFFE7ECE9)
    val LightSurfaceContainerHighest = Color(0xFFE1E6E3)
    val LightOutline = Color(0xFF6F7975)
    val LightOutlineVariant = Color(0xFFBFC9C4)
    val LightInverseSurface = Color(0xFF2E312F)
    val LightInverseOnSurface = Color(0xFFEFF1EE)
    val LightInversePrimary = Color(0xFF8CD8B0)

    // Dark
    val DarkPrimary = Color(0xFF8CD8B0)
    val DarkOnPrimary = Color(0xFF00382A)
    val DarkPrimaryContainer = Color(0xFF005E43)
    val DarkOnPrimaryContainer = Color(0xFFA8F0CE)
    val DarkSecondary = Color(0xFFA2C9FF)
    val DarkOnSecondary = Color(0xFF003259)
    val DarkSecondaryContainer = Color(0xFF1D4E7F)
    val DarkOnSecondaryContainer = Color(0xFFD3E4FF)
    val DarkTertiary = Color(0xFF83D4E0)
    val DarkOnTertiary = Color(0xFF00363D)
    val DarkTertiaryContainer = Color(0xFF005A66)
    val DarkOnTertiaryContainer = Color(0xFFB5EEF6)
    val DarkError = Color(0xFFFFB4AB)
    val DarkOnError = Color(0xFF690005)
    val DarkErrorContainer = Color(0xFF93000A)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)
    val DarkWarningContainer = Color(0xFF5F4200)
    val DarkOnWarningContainer = Color(0xFFFFDEA6)
    val DarkSurface = Color(0xFF101413)
    val DarkOnSurface = Color(0xFFE1E3E0)
    val DarkSurfaceVariant = Color(0xFF3F4945)
    val DarkOnSurfaceVariant = Color(0xFFBFC9C4)
    val DarkSurfaceContainerLowest = Color(0xFF0B0F0E)
    val DarkSurfaceContainerLow = Color(0xFF191C1B)
    val DarkSurfaceContainer = Color(0xFF1D211F)
    val DarkSurfaceContainerHigh = Color(0xFF272B29)
    val DarkSurfaceContainerHighest = Color(0xFF323634)
    val DarkOutline = Color(0xFF89938E)
    val DarkOutlineVariant = Color(0xFF3F4945)
    val DarkInverseSurface = Color(0xFFE1E3E0)
    val DarkInverseOnSurface = Color(0xFF2E312F)
    val DarkInversePrimary = Color(0xFF1B7F5C)

    val Scrim = Color(0xFF000000)
}

internal val LightColorScheme = lightColorScheme(
    primary = PillsnerPalette.LightPrimary,
    onPrimary = PillsnerPalette.LightOnPrimary,
    primaryContainer = PillsnerPalette.LightPrimaryContainer,
    onPrimaryContainer = PillsnerPalette.LightOnPrimaryContainer,
    secondary = PillsnerPalette.LightSecondary,
    onSecondary = PillsnerPalette.LightOnSecondary,
    secondaryContainer = PillsnerPalette.LightSecondaryContainer,
    onSecondaryContainer = PillsnerPalette.LightOnSecondaryContainer,
    tertiary = PillsnerPalette.LightTertiary,
    onTertiary = PillsnerPalette.LightOnTertiary,
    tertiaryContainer = PillsnerPalette.LightTertiaryContainer,
    onTertiaryContainer = PillsnerPalette.LightOnTertiaryContainer,
    error = PillsnerPalette.LightError,
    onError = PillsnerPalette.LightOnError,
    errorContainer = PillsnerPalette.LightErrorContainer,
    onErrorContainer = PillsnerPalette.LightOnErrorContainer,
    background = PillsnerPalette.LightSurface,
    onBackground = PillsnerPalette.LightOnSurface,
    surface = PillsnerPalette.LightSurface,
    onSurface = PillsnerPalette.LightOnSurface,
    surfaceVariant = PillsnerPalette.LightSurfaceVariant,
    onSurfaceVariant = PillsnerPalette.LightOnSurfaceVariant,
    surfaceContainerLowest = PillsnerPalette.LightSurfaceContainerLowest,
    surfaceContainerLow = PillsnerPalette.LightSurfaceContainerLow,
    surfaceContainer = PillsnerPalette.LightSurfaceContainer,
    surfaceContainerHigh = PillsnerPalette.LightSurfaceContainerHigh,
    surfaceContainerHighest = PillsnerPalette.LightSurfaceContainerHighest,
    outline = PillsnerPalette.LightOutline,
    outlineVariant = PillsnerPalette.LightOutlineVariant,
    inverseSurface = PillsnerPalette.LightInverseSurface,
    inverseOnSurface = PillsnerPalette.LightInverseOnSurface,
    inversePrimary = PillsnerPalette.LightInversePrimary,
    scrim = PillsnerPalette.Scrim,
)

internal val DarkColorScheme = darkColorScheme(
    primary = PillsnerPalette.DarkPrimary,
    onPrimary = PillsnerPalette.DarkOnPrimary,
    primaryContainer = PillsnerPalette.DarkPrimaryContainer,
    onPrimaryContainer = PillsnerPalette.DarkOnPrimaryContainer,
    secondary = PillsnerPalette.DarkSecondary,
    onSecondary = PillsnerPalette.DarkOnSecondary,
    secondaryContainer = PillsnerPalette.DarkSecondaryContainer,
    onSecondaryContainer = PillsnerPalette.DarkOnSecondaryContainer,
    tertiary = PillsnerPalette.DarkTertiary,
    onTertiary = PillsnerPalette.DarkOnTertiary,
    tertiaryContainer = PillsnerPalette.DarkTertiaryContainer,
    onTertiaryContainer = PillsnerPalette.DarkOnTertiaryContainer,
    error = PillsnerPalette.DarkError,
    onError = PillsnerPalette.DarkOnError,
    errorContainer = PillsnerPalette.DarkErrorContainer,
    onErrorContainer = PillsnerPalette.DarkOnErrorContainer,
    background = PillsnerPalette.DarkSurface,
    onBackground = PillsnerPalette.DarkOnSurface,
    surface = PillsnerPalette.DarkSurface,
    onSurface = PillsnerPalette.DarkOnSurface,
    surfaceVariant = PillsnerPalette.DarkSurfaceVariant,
    onSurfaceVariant = PillsnerPalette.DarkOnSurfaceVariant,
    surfaceContainerLowest = PillsnerPalette.DarkSurfaceContainerLowest,
    surfaceContainerLow = PillsnerPalette.DarkSurfaceContainerLow,
    surfaceContainer = PillsnerPalette.DarkSurfaceContainer,
    surfaceContainerHigh = PillsnerPalette.DarkSurfaceContainerHigh,
    surfaceContainerHighest = PillsnerPalette.DarkSurfaceContainerHighest,
    outline = PillsnerPalette.DarkOutline,
    outlineVariant = PillsnerPalette.DarkOutlineVariant,
    inverseSurface = PillsnerPalette.DarkInverseSurface,
    inverseOnSurface = PillsnerPalette.DarkInverseOnSurface,
    inversePrimary = PillsnerPalette.DarkInversePrimary,
    scrim = PillsnerPalette.Scrim,
)

/**
 * Pillsner green as a packed colour int, for the one API that cannot read a Compose theme: the
 * notification accent (docs/design-system.md section 8.8). Equal to [PillsnerPalette.LightPrimary].
 */
val PillsnerNotificationColor: Int = PillsnerPalette.LightPrimary.toArgb()
