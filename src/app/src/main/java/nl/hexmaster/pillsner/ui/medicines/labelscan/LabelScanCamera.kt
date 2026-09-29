package nl.hexmaster.pillsner.ui.medicines.labelscan

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation
import nl.hexmaster.pillsner.data.labelscan.toUprightGreyFrame

/**
 * Binds the camera for one visit to the scanning screen (medicine-label-photo-prefill design D3):
 * a `Preview` that feeds the view model's surface requests and an `ImageAnalysis` that hands each
 * frame to the view model on its analysis thread. Deliberately no `ImageCapture`: nothing is ever
 * encoded or written.
 *
 * Bound to the screen's lifecycle owner, so the camera is released when the app goes to the
 * background and when this composable leaves the composition (cancel, acceptance, back).
 */
@Composable
fun LabelScanCamera(viewModel: LabelScanViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider { request -> viewModel.onSurfaceRequest(request) }
        }
        val analysis = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            // A frame that arrives while the previous one is still being read is dropped; the
            // screen never queues frames (design D3).
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .build()
        analysis.setAnalyzer(viewModel.analysisExecutor) { image ->
            try {
                viewModel.analyse(image.toUprightGreyFrame(viewModel.guide))
            } finally {
                image.close()
            }
        }

        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            viewModel.onCameraBound(camera.cameraInfo.hasFlashUnit()) { on -> camera.cameraControl.enableTorch(on) }
            awaitCancellation()
        } catch (failure: IllegalArgumentException) {
            // No back camera that can serve these use cases: the screen says so and offers Cancel.
            Log.d(TAG, "Camera could not be bound")
            viewModel.onCameraFailed()
            awaitCancellation()
        } finally {
            provider.unbindAll()
        }
    }
}

private const val TAG = "LabelScan"

/** Design D3: enough pixels for label text, few enough to read several frames a second. */
private const val ANALYSIS_WIDTH = 1280
private const val ANALYSIS_HEIGHT = 960
