package nl.hexmaster.pillsner.data.labelscan

import androidx.camera.core.ImageProxy

/**
 * The luminance plane of a `YUV_420_888` frame as an upright greyscale image cropped to [guide]
 * (medicine-label-photo-prefill design D3). The Y plane is already 8-bit grey, so there is no
 * colour conversion at all; the frame's own `rotationDegrees` says how far to turn it.
 *
 * The caller still closes the [ImageProxy]; this only reads it.
 */
fun ImageProxy.toUprightGreyFrame(guide: FrameCropper.Guide): GreyFrame {
    val plane = planes[0]
    val full = FrameCropper.fromPlane(plane.buffer, width, height, plane.rowStride, plane.pixelStride)
    return FrameCropper.upright(full, imageInfo.rotationDegrees, guide)
}
