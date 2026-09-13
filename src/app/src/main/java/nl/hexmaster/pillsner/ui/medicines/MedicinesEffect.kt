package nl.hexmaster.pillsner.ui.medicines

/** Something that happened once on the Medicines screen and must not be replayed. */
sealed interface MedicinesEffect {

    /** Starting or stopping a medicine did not go through; the screen says so and nothing changed. */
    data object UpdateFailed : MedicinesEffect

    /**
     * A medicine could not be opened, so the form closed again. Medicines are never removed, so
     * this can only be a defect; the user is told plainly and stays on the overview.
     */
    data object OpenFailed : MedicinesEffect
}
