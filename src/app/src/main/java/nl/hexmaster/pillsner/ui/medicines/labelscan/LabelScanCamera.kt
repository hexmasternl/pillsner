package nl.hexmaster.pillsner.ui.medicines.labelscan

import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation
import nl.hexmaster.pillsner.data.labelscan.toUprightGreyFrame

/**
 * Binds the camera for one visit to the scanning screen (medicine-label-photo-prefill design D3):
 * a `Preview` that feeds the view model's surface requests and an `ImageAnalysis` that hands each
 * frame to the view model on its analysis thread. Deliberately no `ImageCapture`: nothing is ever
 * encoded or written.
 *
 * Both use cases share one [ViewPort] with the viewfinder's aspect ratio, so the region the user
 * sees behind the framing guide is the region the analysis stream's crop rect covers; the guide's
 * fractions then mean the same thing on screen and in the recogniser's frame. Binding waits until
 * layout has reported a size, and rebinds when the aspect ratio changes.
 *
 * Bound to the screen's lifecycle owner, so the camera is released when the app goes to the
 * background and when this composable leaves the composition (cancel, acceptance, back).
 *
 * @param viewfinderSize the laid-out size of the viewfinder area; zero until layout has run.
 */
@Composable
fun LabelScanCamera(viewModel: LabelScanViewModel, viewfinderSize: IntSize) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val aspectRatio = viewfinderSize.takeIf { it.width > 0 && it.height > 0 }?.let { Rational(it.width, it.height) }

    LaunchedEffect(lifecycleOwner, aspectRatio) {
        if (aspectRatio == null) return@LaunchedEffect
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
        val viewPort = ViewPort.Builder(aspectRatio, context.displayRotation())
            .setScaleType(ViewPort.FILL_CENTER)
            .build()
        val useCases = UseCaseGroup.Builder()
            .setViewPort(viewPort)
            .addUseCase(preview)
            .addUseCase(analysis)
            .build()

        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCases)
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

/** The display's rotation, which the viewport's aspect ratio is expressed against. */
private fun Context.displayRotation(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.rotation ?: Surface.ROTATION_0
    } else {
        @Suppress("DEPRECATION")
        getSystemService(WindowManager::class.java).defaultDisplay.rotation
    }

private const val TAG = "LabelScan"

/** Design D3: enough pixels for label text, few enough to read several frames a second. */
private const val ANALYSIS_WIDTH = 1280
private const val ANALYSIS_HEIGHT = 960
