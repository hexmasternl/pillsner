package nl.hexmaster.pillsner.data.labelscan

import androidx.camera.core.ImageProxy

/**
 * The luminance plane of a `YUV_420_888` frame as an upright greyscale image cropped to [guide]
 * (medicine-label-photo-prefill design D3). The Y plane is already 8-bit grey, so there is no
 * colour conversion at all; the frame's own `rotationDegrees` says how far to turn it.
 *
 * Only the frame's `cropRect` is read: with a shared viewport that is exactly the region the
 * preview shows, so the guide's fractions land on the same part of the world in both.
 *
 * The caller still closes the [ImageProxy]; this only reads it.
 */
fun ImageProxy.toUprightGreyFrame(guide: FrameCropper.Guide): GreyFrame {
    val plane = planes[0]
    val region = cropRect
    val shown = FrameCropper.fromPlane(
        plane = plane.buffer,
        width = region.width(),
        height = region.height(),
        rowStride = plane.rowStride,
        pixelStride = plane.pixelStride,
        left = region.left,
        top = region.top,
    )
    return FrameCropper.upright(shown, imageInfo.rotationDegrees, guide)
}
