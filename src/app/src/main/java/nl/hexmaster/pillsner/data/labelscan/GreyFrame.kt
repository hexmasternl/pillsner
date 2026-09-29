package nl.hexmaster.pillsner.data.labelscan

/**
 * An 8-bit greyscale image, one byte per pixel, rows tightly packed: exactly what the recogniser
 * reads (medicine-label-photo-prefill design D3). Lives only in memory and is never written
 * anywhere.
 */
class GreyFrame(val pixels: ByteArray, val width: Int, val height: Int) {

    init {
        require(width > 0 && height > 0) { "A frame needs a positive size" }
        require(pixels.size == width * height) { "A frame holds exactly width * height bytes" }
    }

    /** The pixel at ([x], [y]) as 0..255. */
    fun pixel(x: Int, y: Int): Int = pixels[y * width + x].toInt() and 0xFF

    /** Debug only: the size, never the pixels. */
    override fun toString(): String = "GreyFrame(${width}x$height)"
}
