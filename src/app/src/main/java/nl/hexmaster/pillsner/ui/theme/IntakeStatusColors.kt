package nl.hexmaster.pillsner.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import nl.hexmaster.pillsner.R

/**
 * Presentation-level status of a dose (docs/design-system.md section 2.3).
 * Later changes map the domain intake outcome plus timing onto this in the view model.
 */
enum class IntakeStatus { Due, Taken, Snoozed, Skipped, Overdue, Missed }

/** Container colour, on-container colour and Material Symbols Rounded icon for one [IntakeStatus]. */
data class StatusColors(
    val container: Color,
    val onContainer: Color,
    @DrawableRes val icon: Int,
)

/**
 * docs/design-system.md section 2.3, derived from the active colour scheme so both themes work.
 * Icons are bundled vector drawables (section 7): filled when the state is settled or alarming,
 * outlined otherwise. Red appears only for Overdue and Missed.
 */
@Composable
fun intakeStatusColors(status: IntakeStatus): StatusColors {
    val c = MaterialTheme.colorScheme
    return when (status) {
        IntakeStatus.Due -> StatusColors(c.secondaryContainer, c.onSecondaryContainer, R.drawable.ic_schedule)
        IntakeStatus.Taken -> StatusColors(c.primaryContainer, c.onPrimaryContainer, R.drawable.ic_check_circle_filled)
        IntakeStatus.Snoozed -> StatusColors(c.tertiaryContainer, c.onTertiaryContainer, R.drawable.ic_snooze)
        IntakeStatus.Skipped -> StatusColors(c.surfaceContainerHighest, c.onSurfaceVariant, R.drawable.ic_do_not_disturb_on)
        IntakeStatus.Overdue -> StatusColors(c.errorContainer, c.onErrorContainer, R.drawable.ic_error_filled)
        IntakeStatus.Missed -> StatusColors(c.errorContainer, c.onErrorContainer, R.drawable.ic_cancel_filled)
    }
}

/** Dose and medicine tile container (section 6): pure white in light, a lighter tier in dark. */
@Composable
fun tileContainerColor(): Color =
    if (LocalPillsnerDarkTheme.current) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
