package nl.hexmaster.pillsner.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.applock.ui.PinSetupMode
import nl.hexmaster.pillsner.domain.legal.LegalDocumentId
import nl.hexmaster.pillsner.domain.model.DoseUnit

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
 * @property medicationId the medicine to open, or null to add a new one. One flow, three entrances:
 * the add button opens it empty, a tile opens it filled, and the label-scan shortcut opens it
 * pre-filled from a photo (medicine-add-label-scan design D3).
 * @property scannedName a recognized medicine name to seed a fresh add-mode draft with, or null.
 * Ignored whenever [medicationId] is not null.
 * @property scannedDoseAmount a recognized dose amount, in the same free-text form the dose field
 * accepts, to seed a fresh add-mode draft with, or null. Ignored whenever [medicationId] is not
 * null.
 * @property scannedDoseUnit a recognized dose unit to seed a fresh add-mode draft with, or null.
 * Ignored whenever [medicationId] is not null.
 */
@Serializable
data class MedicationFormGraph(
    val medicationId: Long? = null,
    val scannedName: String? = null,
    val scannedDoseAmount: String? = null,
    val scannedDoseUnit: DoseUnit? = null,
)

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

/**
 * One medicine's usage history (app-medicine-usage-history design D7). Registered inside
 * [MedicationFormGraph] rather than beside it: the history only exists as something you opened
 * from a medicine you have open, so leaving the flow takes it off the back stack too, and back
 * returns to the form with its draft, unsaved edits included. Not a top-level destination.
 *
 * @property medicationId the medicine whose record this is. It travels in the route, so process
 * death restores the right history with nothing to rebuild.
 */
@Serializable
data class MedicineHistory(val medicationId: Long)

/**
 * One dose on its own screen, reached by tapping a tile on the welcome screen
 * (app-welcome-screen-reminder-details design D3). Not a top-level destination, so the navigation
 * suite hides itself while it is shown.
 *
 * @property doseId the dose being answered. It travels in the route rather than in a shared view
 * model, so process death restores the right dose with nothing to rebuild. A `Long` rather than a
 * `DoseId` because the route is a serialisation surface and `DoseId` is a value class; the view
 * model wraps it back immediately.
 */
@Serializable
data class DoseDetail(val doseId: Long)

/** Placeholder until the settings changes land. */
@Serializable
data object Settings

/**
 * The About screen (app-about-screen design D6). A secondary destination, reached only from the
 * About row on Settings, so it is not in [topLevelDestinations] and the navigation suite hides
 * itself while it is shown. It carries no arguments: its content is the same for every process,
 * so process death restores it with nothing to rebuild.
 */
@Serializable
data object About

/**
 * The reminder delivery log. A secondary destination reached only from the Reminders row on
 * Settings; like [About] it carries no arguments and is not in [topLevelDestinations].
 */
@Serializable
data object ReminderDiagnostics

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
 * One legal document on its own screen, reached from the Legal section on Settings and from
 * [AcceptLegal] (app-legal-information design D7). Not top-level.
 *
 * @property document which of the two to show. It travels in the route rather than in a view model,
 * so process death restores the document the user was reading with nothing to rebuild.
 */
@Serializable
data class LegalDocumentRoute(val document: LegalDocumentId)

/**
 * The legal acceptance screen, reached from the Medicines add button while the current documents
 * are not accepted (design D5, D6). Not top-level. Accepting continues into
 * [MedicationFormGraph] and takes this destination off the back stack; back returns to Medicines.
 */
@Serializable
data object AcceptLegal

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
