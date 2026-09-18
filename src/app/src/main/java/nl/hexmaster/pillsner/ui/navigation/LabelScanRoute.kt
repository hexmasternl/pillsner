package nl.hexmaster.pillsner.ui.navigation

import nl.hexmaster.pillsner.domain.model.LabelScanResult

/**
 * Turns a label scan's best-effort guess into the route that opens a fresh Add medicine form with
 * it (medicine-add-label-scan design D3). A plain, Android-free mapping so the "what did we
 * recognize, and did we recognize anything at all" decision is unit-testable on its own, away from
 * the camera/permission plumbing that produces [LabelScanResult] in the first place.
 *
 * [MedicationFormGraph.scanFailed] is true exactly when nothing usable was recognized (spec,
 * "Nothing usable is recognized"): a blank or missing name and a blank or missing amount. A unit
 * alone, with no amount, cannot happen from [nl.hexmaster.pillsner.domain.labelscan.ParseLabelText]
 * and would not count as "something recognized" either.
 */
fun LabelScanResult.toMedicationFormRoute(): MedicationFormGraph {
    val name = name?.takeIf(String::isNotBlank)
    val amountText = amountText?.takeIf(String::isNotBlank)
    return MedicationFormGraph(
        scannedName = name,
        scannedDoseAmount = amountText,
        scannedDoseUnit = unit,
        scanFailed = name == null && amountText == null,
    )
}
