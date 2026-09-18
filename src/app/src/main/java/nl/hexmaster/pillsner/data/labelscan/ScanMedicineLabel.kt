package nl.hexmaster.pillsner.data.labelscan

import android.graphics.Bitmap
import nl.hexmaster.pillsner.domain.labelscan.ParseLabelText
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.model.LabelScanResult

/**
 * Recognizes a photographed medicine label and turns it into a best-effort [LabelScanResult]
 * (medicine-add-label-scan design D3): the one call the Medicines screen's capture flow needs
 * between "here is a photo" and "navigate to the form with this prefill".
 *
 * Kept separate from [LabelTextRecognizer] so the public surface `AppContainer` wires into
 * `MedicinesViewModel` is a plain `suspend (Bitmap, Int) -> LabelScanResult` function, never an ML
 * Kit type: a UI test can substitute any lambda here, without touching ML Kit at all.
 *
 * @param currentLanguage the app's resolved language (`AppLocale.inEffect`), read fresh on every
 *   call rather than captured once, since it can change between scans within the same process.
 */
class ScanMedicineLabel(
    private val recognizer: LabelTextRecognizer,
    private val currentLanguage: () -> AppLanguage,
) {
    suspend operator fun invoke(bitmap: Bitmap, rotationDegrees: Int = 0): LabelScanResult {
        val text = recognizer.recognize(bitmap, rotationDegrees) ?: return LabelScanResult()
        return ParseLabelText.parse(text, currentLanguage())
    }
}
