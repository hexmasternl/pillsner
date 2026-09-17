package nl.hexmaster.pillsner.ui.medicines

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec: medicine-label-scan, "Camera permission is requested only on demand, with a working
 * fallback" (design D4). The decision itself needs no Android launcher, so it is tested in
 * isolation from the camera/permission plumbing around it.
 */
class LabelScanActionTest {

    @Test
    fun cameraAvailableAndPermissionGranted_goesStraightToCapture() {
        assertEquals(
            LabelScanAction.LAUNCH_CAMERA,
            decideLabelScanAction(hasCamera = true, hasCameraPermission = true),
        )
    }

    @Test
    fun cameraAvailableButPermissionNotYetGranted_requestsPermission() {
        assertEquals(
            LabelScanAction.REQUEST_PERMISSION,
            decideLabelScanAction(hasCamera = true, hasCameraPermission = false),
        )
    }

    @Test
    fun noCamera_fallsBackToThePickerRegardlessOfPermission() {
        assertEquals(
            LabelScanAction.LAUNCH_PICKER,
            decideLabelScanAction(hasCamera = false, hasCameraPermission = false),
        )
        assertEquals(
            LabelScanAction.LAUNCH_PICKER,
            decideLabelScanAction(hasCamera = false, hasCameraPermission = true),
        )
    }
}
