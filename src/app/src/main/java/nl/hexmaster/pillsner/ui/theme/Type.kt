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

/**
 * Apply to times and quantities that line up in columns (section 3.3):
 * `style.copy(fontFeatureSettings = TabularNumbers)`.
 */
const val TabularNumbers = "tnum"
