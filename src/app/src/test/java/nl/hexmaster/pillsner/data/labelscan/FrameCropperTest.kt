package nl.hexmaster.pillsner.data.labelscan

import java.nio.ByteBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Design D3: the frame path as plain byte-array loops, on frames small enough to read by eye. */
class FrameCropperTest {

    /**
     * A 4 x 3 frame whose pixel values are their own coordinates, `x + 10 * y`:
     * ```
     *  0  1  2  3
     * 10 11 12 13
     * 20 21 22 23
     * ```
     */
    private val frame = GreyFrame(byteArrayOf(0, 1, 2, 3, 10, 11, 12, 13, 20, 21, 22, 23), width = 4, height = 3)

    @Test
    fun `a plane with row padding is packed tightly`() {
        // Row stride 6 for a width of 4: two padding bytes (99) end every row.
        val padded = byteArrayOf(0, 1, 2, 3, 99, 99, 10, 11, 12, 13, 99, 99, 20, 21, 22, 23, 99, 99)

        val result = FrameCropper.fromPlane(ByteBuffer.wrap(padded), width = 4, height = 3, rowStride = 6, pixelStride = 1)

        assertArrayEquals(frame.pixels, result.pixels)
        assertEquals(4, result.width)
        assertEquals(3, result.height)
    }

    @Test
    fun `a plane with a pixel stride of two takes every other byte`() {
        val interleaved = byteArrayOf(0, 99, 1, 99, 2, 99, 3, 99, 10, 99, 11, 99, 12, 99, 13, 99)

        val result = FrameCropper.fromPlane(ByteBuffer.wrap(interleaved), width = 4, height = 2, rowStride = 8, pixelStride = 2)

        assertArrayEquals(byteArrayOf(0, 1, 2, 3, 10, 11, 12, 13), result.pixels)
    }

    @Test
    fun `reading a plane leaves the buffer position alone`() {
        val buffer = ByteBuffer.wrap(frame.pixels)

        FrameCropper.fromPlane(buffer, 4, 3, 4, 1)

        assertEquals(0, buffer.position())
    }

    @Test
    fun `rotating by zero returns the frame itself`() {
        assertEquals(frame, FrameCropper.rotate(frame, 0))
    }

    @Test
    fun `rotating clockwise by 90 turns rows into columns, last row first`() {
        val result = FrameCropper.rotate(frame, 90)

        assertEquals(3, result.width)
        assertEquals(4, result.height)
        // 20 10 0 / 21 11 1 / 22 12 2 / 23 13 3
        assertArrayEquals(byteArrayOf(20, 10, 0, 21, 11, 1, 22, 12, 2, 23, 13, 3), result.pixels)
    }

    @Test
    fun `rotating by 180 reverses the pixels`() {
        val result = FrameCropper.rotate(frame, 180)

        assertEquals(4, result.width)
        assertArrayEquals(frame.pixels.reversedArray(), result.pixels)
    }

    @Test
    fun `rotating by 270 is the inverse of rotating by 90`() {
        val result = FrameCropper.rotate(FrameCropper.rotate(frame, 90), 270)

        assertArrayEquals(frame.pixels, result.pixels)
        assertEquals(4, result.width)
        // 3 13 23 / 2 12 22 / 1 11 21 / 0 10 20
        assertArrayEquals(byteArrayOf(3, 13, 23, 2, 12, 22, 1, 11, 21, 0, 10, 20), FrameCropper.rotate(frame, 270).pixels)
    }

    @Test
    fun `an unsupported rotation is refused`() {
        assertThrows(IllegalArgumentException::class.java) { FrameCropper.rotate(frame, 45) }
    }

    @Test
    fun `cropping keeps the part inside the guide`() {
        // Columns 1..2 of rows 1..2.
        val result = FrameCropper.crop(frame, FrameCropper.Guide(left = 0.25f, top = 0.34f, right = 0.75f, bottom = 1f))

        assertEquals(2, result.width)
        assertEquals(2, result.height)
        assertArrayEquals(byteArrayOf(11, 12, 21, 22), result.pixels)
    }

    @Test
    fun `a guide that rounds to nothing still yields one pixel`() {
        val result = FrameCropper.crop(frame, FrameCropper.Guide(left = 0.9f, top = 0.9f, right = 0.95f, bottom = 0.95f))

        assertEquals(1, result.width)
        assertEquals(1, result.height)
        assertEquals(23, result.pixel(0, 0))
    }

    @Test
    fun `upright rotates first and crops in the rotated frame`() {
        // After a 90 degree turn the frame is 3 wide and 4 tall; the guide takes its bottom half.
        val result = FrameCropper.upright(frame, 90, FrameCropper.Guide(left = 0f, top = 0.5f, right = 1f, bottom = 1f))

        assertEquals(3, result.width)
        assertEquals(2, result.height)
        assertArrayEquals(byteArrayOf(22, 12, 2, 23, 13, 3), result.pixels)
    }

    @Test
    fun `the default guide is a strip across the middle`() {
        val guide = FrameCropper.Guide.DEFAULT
        val result = FrameCropper.crop(GreyFrame(ByteArray(100 * 100), 100, 100), guide)

        assertEquals(84, result.width)
        assertEquals(40, result.height)
    }

    @Test
    fun `a guide with inverted or out-of-range edges is refused`() {
        assertThrows(IllegalArgumentException::class.java) { FrameCropper.Guide(0.5f, 0f, 0.4f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { FrameCropper.Guide(0f, 0f, 1.2f, 1f) }
    }

    @Test
    fun `a frame must hold exactly width times height bytes`() {
        assertThrows(IllegalArgumentException::class.java) { GreyFrame(ByteArray(5), 2, 2) }
    }
}
