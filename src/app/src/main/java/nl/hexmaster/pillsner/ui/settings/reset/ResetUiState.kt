package nl.hexmaster.pillsner.ui.settings.reset

/**
 * What the danger zone and its dialog are showing (design D8).
 *
 * @property dialogVisible whether the confirmation is in front of the user.
 * @property confirmationAccepted whether they have ticked the box that says they understand. It
 *   lives here rather than in the dialog so that a configuration change can neither silently untick
 *   it nor leave it ticked over a dialog that was recreated.
 * @property inProgress whether the erase is running. The confirm button is disabled while it is, so
 *   a double tap erases once.
 */
data class ResetUiState(
    val dialogVisible: Boolean = false,
    val confirmationAccepted: Boolean = false,
    val inProgress: Boolean = false,
) {
    /** Nothing may be erased until the user has said, in as many words, that they understand. */
    val canConfirm: Boolean get() = confirmationAccepted && !inProgress
}

/** A one-shot instruction to the Settings screen; never part of the state, so never replayed. */
sealed interface ResetEffect {
    /** Everything is gone. The screen says so with a snackbar and stays where it is. */
    data object Erased : ResetEffect
}
