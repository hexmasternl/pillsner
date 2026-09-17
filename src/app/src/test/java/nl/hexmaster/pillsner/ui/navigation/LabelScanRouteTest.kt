package nl.hexmaster.pillsner.ui.navigation

import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.LabelScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec: medicine-label-scan, "A recognized photo opens a new Add medicine form prefilled from it"
 * and "Nothing usable is recognized". Covers the pure mapping only; the camera/permission plumbing
 * that produces a [LabelScanResult] is exercised separately.
 */
class LabelScanRouteTest {

    @Test
    fun nameAndDoseRecognized_carriesAllThreeAndDoesNotReportFailure() {
        val route = LabelScanResult("Amoxicillin", "500", DoseUnit.MILLIGRAM).toMedicationFormRoute()

        assertEquals("Amoxicillin", route.scannedName)
        assertEquals("500", route.scannedDoseAmount)
        assertEquals(DoseUnit.MILLIGRAM, route.scannedDoseUnit)
        assertFalse(route.scanFailed)
        assertNull(route.medicationId)
    }

    @Test
    fun nameOnlyRecognized_leavesDoseFieldsNullAndDoesNotReportFailure() {
        val route = LabelScanResult(name = "Ibuprofen").toMedicationFormRoute()

        assertEquals("Ibuprofen", route.scannedName)
        assertNull(route.scannedDoseAmount)
        assertNull(route.scannedDoseUnit)
        assertFalse(route.scanFailed)
    }

    @Test
    fun nothingRecognized_reportsFailureWithEveryFieldNull() {
        val route = LabelScanResult().toMedicationFormRoute()

        assertNull(route.scannedName)
        assertNull(route.scannedDoseAmount)
        assertNull(route.scannedDoseUnit)
        assertTrue(route.scanFailed)
    }

    @Test
    fun blankNameAndAmount_countAsNothingRecognized() {
        val route = LabelScanResult(name = "  ", amountText = " ").toMedicationFormRoute()

        assertNull(route.scannedName)
        assertNull(route.scannedDoseAmount)
        assertTrue(route.scanFailed)
    }
}
