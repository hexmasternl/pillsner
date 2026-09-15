# Pillsner theme code

Reference implementation of `docs/design-system.md` sections 2 to 5 and 11. Package `nl.hexmaster.pillsner.ui.theme`. Copy each file as-is.

## Color.kt

```kotlin
package nl.hexmaster.pillsner.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Every colour in the app. This is the only file that may contain a hex literal.
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
```

## Type.kt

```kotlin
package nl.hexmaster.pillsner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import nl.hexmaster.pillsner.R

/** Raleway is used for display and headline roles only, never below 24 sp. */
val RalewayFamily = FontFamily(
    Font(R.font.raleway_extralight, FontWeight.ExtraLight),
    Font(R.font.raleway_light, FontWeight.Light),
)

/** Montserrat is used for every other role. bodyLarge (18 sp, 400) is the app default. */
val MontserratFamily = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
)

/** docs/design-system.md section 3.2. Sizes in sp, letter spacing in sp. */
val PillsnerTypography = Typography(
    displayLarge = TextStyle(fontFamily = RalewayFamily, fontWeight = FontWeight.ExtraLight, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = 0.sp),
    displayMedium = TextStyle(fontFamily = RalewayFamily, fontWeight = FontWeight.ExtraLight, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = RalewayFamily, fontWeight = FontWeight.ExtraLight, fontSize = 34.sp, lineHeight = 42.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = RalewayFamily, fontWeight = FontWeight.Light, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = RalewayFamily, fontWeight = FontWeight.Light, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = RalewayFamily, fontWeight = FontWeight.Light, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

/** Apply to times and quantities that line up in columns (section 3.3): `style.copy(fontFeatureSettings = TabularNumbers)`. */
const val TabularNumbers = "tnum"
```

## Shape.kt

```kotlin
package nl.hexmaster.pillsner.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** docs/design-system.md section 4. Buttons and chips use CircleShape (full) via their Material defaults. */
val PillsnerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
```

## Dimens.kt

```kotlin
package nl.hexmaster.pillsner.ui.theme

import androidx.compose.ui.unit.dp

/** docs/design-system.md section 5. The only place a raw dp value for spacing or sizing may live. */
object Spacing {
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
    /** Content column cap on wide screens. */
    val contentMaxWidth = 600.dp
}

object Sizes {
    val minTouchTarget = 48.dp
    val primaryActionHeight = 56.dp
    val tileMinHeight = 72.dp
    val stateStripeWidth = 4.dp
    val statusChipHeight = 28.dp
    val iconDefault = 24.dp
    val iconEmptyState = 64.dp
    val fab = 56.dp
}
```

## IntakeStatusColors.kt

```kotlin
package nl.hexmaster.pillsner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze

/** Presentation-level status of a dose. Map domain `IntakeOutcome` plus timing onto this in the view model. */
enum class IntakeStatus { Due, Taken, Snoozed, Skipped, Overdue, Missed }

data class StatusColors(
    val container: Color,
    val onContainer: Color,
    val icon: ImageVector,
)

/** docs/design-system.md section 2.3. Derived from the active colour scheme so both themes work. */
@Composable
fun intakeStatusColors(status: IntakeStatus): StatusColors {
    val c = MaterialTheme.colorScheme
    return when (status) {
        IntakeStatus.Due -> StatusColors(c.secondaryContainer, c.onSecondaryContainer, Icons.Outlined.Schedule)
        IntakeStatus.Taken -> StatusColors(c.primaryContainer, c.onPrimaryContainer, Icons.Filled.CheckCircle)
        IntakeStatus.Snoozed -> StatusColors(c.tertiaryContainer, c.onTertiaryContainer, Icons.Outlined.Snooze)
        IntakeStatus.Skipped -> StatusColors(c.surfaceContainerHighest, c.onSurfaceVariant, Icons.Outlined.DoNotDisturbOn)
        IntakeStatus.Overdue -> StatusColors(c.errorContainer, c.onErrorContainer, Icons.Filled.Error)
        IntakeStatus.Missed -> StatusColors(c.errorContainer, c.onErrorContainer, Icons.Filled.Cancel)
    }
}
```

`Icons.Outlined.Snooze`, `Schedule` and `DoNotDisturbOn` need `androidx.compose.material:material-icons-extended`. That artifact is first party but large; if APK size matters, copy the five vector assets into `res/drawable` from Material Symbols Rounded instead and use `painterResource`.

## Theme.kt

```kotlin
package nl.hexmaster.pillsner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Pillsner's Material 3 theme. Follows the system light/dark setting and never uses dynamic colour
 * (docs/design-system.md, principle 5). Wrap the whole app in it once, in MainActivity.
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
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PillsnerTypography,
        shapes = PillsnerShapes,
        content = content,
    )
}
```

`MainActivity` calls `enableEdgeToEdge()` before `setContent { PillsnerTheme { PillsnerApp() } }`.

## ThemePreview.kt (debug-only sanity preview)

```kotlin
package nl.hexmaster.pillsner.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark

@PreviewLightDark
@Composable
private fun ThemePreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg)) {
                Text("Today", style = MaterialTheme.typography.displayLarge)
                Text("Take 40 mg of your medicine 'Ibuprofen', on 08:00", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = {}) { Text("I took it") }
            }
        }
    }
}
```

Preview text may be inline; production text never is.
