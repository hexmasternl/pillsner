package nl.hexmaster.pillsner.data.labelscan

import java.nio.ByteBuffer

/**
 * Turns a camera frame's luminance plane into the upright, cropped greyscale image the recogniser
 * reads (medicine-label-photo-prefill design D3). Plain byte-array loops with no Android type in
 * them, so every step is unit-tested on synthetic frames.
 */
object FrameCropper {

    /**
     * The framing guide as fractions of the *upright* frame, left to right and top to bottom. The
     * scanning screen draws the same rectangle over the preview, so what the user frames is what
     * the recogniser reads.
     */
    data class Guide(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        init {
            require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f) { "Guide edges are fractions" }
            require(left < right && top < bottom) { "A guide needs a positive size" }
        }

        companion object {
            /** A landscape strip across the middle of the frame: the shape of a label's text block. */
            val DEFAULT = Guide(left = 0.08f, top = 0.30f, right = 0.92f, bottom = 0.70f)
        }
    }

    /**
     * Copies a [width] x [height] region of a luminance plane, starting at ([left], [top]) and with
     * any row and pixel stride, into a tightly packed frame. The buffer's position is not changed.
     * The region is CameraX's crop rect: the part of the sensor frame the viewport shows.
     */
    fun fromPlane(
        plane: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        left: Int = 0,
        top: Int = 0,
    ): GreyFrame {
        require(rowStride >= (left + width) * pixelStride && pixelStride >= 1) { "The region must fit in the row" }
        require(left >= 0 && top >= 0) { "The region starts inside the plane" }
        val pixels = ByteArray(width * height)
        val source = plane.duplicate()
        for (row in 0 until height) {
            val rowStart = (top + row) * rowStride + left * pixelStride
            if (pixelStride == 1) {
                source.position(rowStart)
                source.get(pixels, row * width, width)
            } else {
                for (column in 0 until width) {
                    pixels[row * width + column] = source.get(rowStart + column * pixelStride)
                }
            }
        }
        return GreyFrame(pixels, width, height)
    }

    /**
     * Rotates clockwise by [degrees], which must be 0, 90, 180 or 270: CameraX's
     * `rotationDegrees` is exactly the clockwise turn that makes the frame upright.
     */
    fun rotate(frame: GreyFrame, degrees: Int): GreyFrame {
        val w = frame.width
        val h = frame.height
        val source = frame.pixels
        return when (degrees) {
            0 -> frame
            90 -> {
                val out = ByteArray(source.size)
                for (y in 0 until h) for (x in 0 until w) out[x * h + (h - 1 - y)] = source[y * w + x]
                GreyFrame(out, h, w)
            }
            180 -> {
                val out = ByteArray(source.size)
                for (y in 0 until h) for (x in 0 until w) out[(h - 1 - y) * w + (w - 1 - x)] = source[y * w + x]
                GreyFrame(out, w, h)
            }
            270 -> {
                val out = ByteArray(source.size)
                for (y in 0 until h) for (x in 0 until w) out[(w - 1 - x) * h + y] = source[y * w + x]
                GreyFrame(out, h, w)
            }
            else -> throw IllegalArgumentException("Rotation must be a multiple of 90 degrees, not $degrees")
        }
    }

    /** Mirrors left to right, for the EXIF orientations that include a flip. */
    fun flipHorizontal(frame: GreyFrame): GreyFrame {
        val w = frame.width
        val out = ByteArray(frame.pixels.size)
        for (y in 0 until frame.height) {
            val rowStart = y * w
            for (x in 0 until w) out[rowStart + (w - 1 - x)] = frame.pixels[rowStart + x]
        }
        return GreyFrame(out, w, frame.height)
    }

    /**
     * Makes a photo upright from its EXIF orientation (TIFF tag 274), including the four values that
     * mirror the image. The rotation is applied first, then the mirror, which is the transform the
     * platform's own image loaders use.
     */
    fun orient(frame: GreyFrame, exifOrientation: Int): GreyFrame = when (exifOrientation) {
        ExifOrientation.FLIP_HORIZONTAL -> flipHorizontal(frame)
        ExifOrientation.ROTATE_180 -> rotate(frame, 180)
        ExifOrientation.FLIP_VERTICAL -> flipHorizontal(rotate(frame, 180))
        ExifOrientation.TRANSPOSE -> flipHorizontal(rotate(frame, 90))
        ExifOrientation.ROTATE_90 -> rotate(frame, 90)
        ExifOrientation.TRANSVERSE -> flipHorizontal(rotate(frame, 270))
        ExifOrientation.ROTATE_270 -> rotate(frame, 270)
        else -> frame
    }

    /** The EXIF orientation values, as `android.media.ExifInterface` defines them, kept here so [orient] stays testable on the JVM. */
    object ExifOrientation {
        const val NORMAL = 1
        const val FLIP_HORIZONTAL = 2
        const val ROTATE_180 = 3
        const val FLIP_VERTICAL = 4
        const val TRANSPOSE = 5
        const val ROTATE_90 = 6
        const val TRANSVERSE = 7
        const val ROTATE_270 = 8
    }

    /** The part of [frame] inside [guide]; never smaller than one pixel. */
    fun crop(frame: GreyFrame, guide: Guide): GreyFrame {
        val left = (guide.left * frame.width).toInt().coerceIn(0, frame.width - 1)
        val top = (guide.top * frame.height).toInt().coerceIn(0, frame.height - 1)
        val right = (guide.right * frame.width).toInt().coerceIn(left + 1, frame.width)
        val bottom = (guide.bottom * frame.height).toInt().coerceIn(top + 1, frame.height)
        val width = right - left
        val height = bottom - top
        val out = ByteArray(width * height)
        for (row in 0 until height) {
            System.arraycopy(frame.pixels, (top + row) * frame.width + left, out, row * width, width)
        }
        return GreyFrame(out, width, height)
    }

    /** Rotates by [rotationDegrees] and crops to [guide]: the whole frame path in one call. */
    fun upright(frame: GreyFrame, rotationDegrees: Int, guide: Guide): GreyFrame =
        crop(rotate(frame, rotationDegrees), guide)
}
