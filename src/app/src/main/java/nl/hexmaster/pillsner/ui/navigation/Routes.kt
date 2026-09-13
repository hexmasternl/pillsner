package nl.hexmaster.pillsner.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.ui.PinSetupMode

/** The welcome screen; start destination. */
@Serializable
data object Home

/** The medicine overview: active and inactive medicines with their schedule descriptions. */
@Serializable
data object Medicines

/**
 * The medicine form flow (app-medicine-add design D4, app-medicine-details D1). A nested graph
 * rather than a single destination so the form and the schedule editor can share one draft: both
 * take their view model from this graph's back-stack entry.
 *
 * @property medicationId the medicine to open, or null to add a new one. One flow, two entrances:
 * the add button opens it empty, a tile opens it filled.
 */
@Serializable
data class MedicationFormGraph(val medicationId: Long? = null)

/** The medicine form; start destination of [MedicationFormGraph]. */
@Serializable
data object MedicationForm

/**
 * The schedule editor inside the add-medicine flow.
 *
 * @property index the position of the schedule being edited in the draft, or null to add one.
 */
@Serializable
data class EditSchedule(val index: Int? = null)

/** Placeholder until the settings changes land. */
@Serializable
data object Settings

/**
 * The PIN flow, reached from the Security section (app-login design D10). Not top-level.
 *
 * @property mode whether it sets the first PIN or replaces the current one (app-settings-security
 * D2). One screen, two entrances: the "Protect with PIN" switch opens it to set up, the
 * "Change PIN" row opens it to change, once the identity check has passed.
 */
@Serializable
data class PinSetup(val mode: PinSetupMode = PinSetupMode.SET_UP)

/**
 * One item of the bottom navigation bar or rail (docs/design-system.md section 8.6).
 *
 * @property route the type-safe route object passed to `navigate`.
 * @property label navigation label; always shown.
 * @property outlinedIcon icon when not selected.
 * @property filledIcon icon when selected.
 * @property testTag stable tag for semantics tests.
 */
data class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    @DrawableRes val outlinedIcon: Int,
    @DrawableRes val filledIcon: Int,
    val testTag: String,
)

/** Home, Medicines, Settings: fixed order, fixed count. */
val topLevelDestinations: List<TopLevelDestination> = listOf(
    TopLevelDestination(Home, R.string.nav_home, R.drawable.ic_home, R.drawable.ic_home_filled, NavigationTestTags.HOME),
    TopLevelDestination(Medicines, R.string.nav_medicines, R.drawable.ic_medication, R.drawable.ic_medication_filled, NavigationTestTags.MEDICINES),
    TopLevelDestination(Settings, R.string.nav_settings, R.drawable.ic_settings, R.drawable.ic_settings_filled, NavigationTestTags.SETTINGS),
)

/** Test tags for the navigation items and placeholder screens. */
object NavigationTestTags {
    const val HOME = "nav_home"
    const val MEDICINES = "nav_medicines"
    const val SETTINGS = "nav_settings"
    const val MEDICINES_TITLE = "medicines_title"
    const val SETTINGS_TITLE = "settings_title"
}
