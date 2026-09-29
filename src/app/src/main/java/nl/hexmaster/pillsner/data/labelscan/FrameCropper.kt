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
     * Copies a luminance plane with any row and pixel stride into a tightly packed frame. The
     * buffer's position is not changed.
     */
    fun fromPlane(plane: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int): GreyFrame {
        require(rowStride >= width && pixelStride >= 1) { "Strides cannot be smaller than the row" }
        val pixels = ByteArray(width * height)
        val source = plane.duplicate()
        for (row in 0 until height) {
            val rowStart = row * rowStride
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
