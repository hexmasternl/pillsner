package nl.hexmaster.pillsner.data.labelscan

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Reads a photo the user picked into a greyscale frame for the recogniser (medicine-label-photo-
 * prefill design D3, D6). The source is opened once, read once into one bounded buffer, and never
 * copied anywhere; bounds, orientation and pixels all come from that buffer.
 *
 * Memory is bounded at every step: the encoded photo may hold at most [MAX_ENCODED_BYTES] and is
 * kept in a single array, the bitmap is sampled down to a long side of [MAX_LONG_SIDE] pixels, and
 * the greyscale conversion walks the bitmap one row at a time.
 */
class PickedPhotoDecoder(private val contentResolver: ContentResolver) {

    /**
     * The photo as an upright greyscale frame whose long side is at most [MAX_LONG_SIDE] pixels,
     * or null when it cannot be decoded or is larger than a phone photo has any reason to be.
     * Blocking; call off the main thread.
     */
    @WorkerThread
    fun decode(uri: Uri): GreyFrame? = try {
        val encoded = readOnce(uri) ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encoded.bytes, 0, encoded.length, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight))
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(encoded.bytes, 0, encoded.length, options) ?: return null
        val orientation = ByteArrayInputStream(encoded.bytes, 0, encoded.length).use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        val grey = try {
            toGrey(bitmap)
        } finally {
            bitmap.recycle()
        }
        // All eight EXIF values, the mirrored ones included, so a flipped photo is not read backwards.
        FrameCropper.orient(grey, orientation)
    } catch (failure: IOException) {
        Log.d(TAG, "Picked photo could not be read: ${failure.javaClass.simpleName}")
        null
    } catch (failure: IllegalArgumentException) {
        Log.d(TAG, "Picked photo could not be read: ${failure.javaClass.simpleName}")
        null
    } catch (failure: SecurityException) {
        Log.d(TAG, "Picked photo could not be read: ${failure.javaClass.simpleName}")
        null
    }

    /** The encoded bytes and how many of them are meaningful; the array may be longer. */
    private class Encoded(val bytes: ByteArray, val length: Int)

    /**
     * Opens the source exactly once. When the provider states the length, exactly that many bytes
     * are allocated; otherwise the buffer grows while reading. Either way nothing is read past
     * [MAX_ENCODED_BYTES], and the buffer is used in place rather than copied.
     */
    private fun readOnce(uri: Uri): Encoded? = contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
        val declared = descriptor.length
        if (declared > MAX_ENCODED_BYTES) {
            Log.d(TAG, "Picked photo larger than the ${MAX_ENCODED_BYTES / MEBIBYTE} MB limit")
            return null
        }
        descriptor.createInputStream().use { input ->
            if (declared != AssetFileDescriptor.UNKNOWN_LENGTH) readExactly(input, declared.toInt()) else readBounded(input)
        }
    }

    private fun readExactly(input: InputStream, length: Int): Encoded {
        val bytes = ByteArray(length)
        var filled = 0
        while (filled < length) {
            val read = input.read(bytes, filled, length - filled)
            if (read < 0) break
            filled += read
        }
        return Encoded(bytes, filled)
    }

    private fun readBounded(input: InputStream): Encoded? {
        val out = InPlaceBuffer()
        val chunk = ByteArray(READ_CHUNK_BYTES)
        while (true) {
            val read = input.read(chunk)
            if (read < 0) return Encoded(out.bytes(), out.size())
            if (out.size() + read > MAX_ENCODED_BYTES) {
                Log.d(TAG, "Picked photo larger than the ${MAX_ENCODED_BYTES / MEBIBYTE} MB limit")
                return null
            }
            out.write(chunk, 0, read)
        }
    }

    /** A growable buffer that hands out its backing array instead of copying it. */
    private class InPlaceBuffer : ByteArrayOutputStream(INITIAL_BUFFER_BYTES) {
        fun bytes(): ByteArray = buf
    }

    /** The power of two that brings [longSide] to at most [MAX_LONG_SIDE]. */
    private fun sampleSizeFor(longSide: Int): Int {
        var sample = 1
        while (longSide / sample > MAX_LONG_SIDE) sample *= 2
        return sample
    }

    /** Luminance from the usual weights, in integer arithmetic, one row of pixels at a time. */
    private fun toGrey(bitmap: Bitmap): GreyFrame {
        val width = bitmap.width
        val height = bitmap.height
        val row = IntArray(width)
        val grey = ByteArray(width * height)
        for (y in 0 until height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            val rowStart = y * width
            for (x in 0 until width) {
                val pixel = row[x]
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                grey[rowStart + x] = ((RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue) shr 8).toByte()
            }
        }
        return GreyFrame(grey, width, height)
    }

    private companion object {
        const val TAG = "LabelScan"

        /** Plenty for a label; bounds the decoded bitmap at about 12 MB (design D3). */
        const val MAX_LONG_SIDE = 2_000

        /**
         * Above what a phone camera writes for one photo (a 48-megapixel JPEG is 8 to 15 MB), and
         * with the sampled bitmap and one row buffer still inside a low-memory device's heap.
         */
        const val MEBIBYTE = 1024 * 1024
        const val MAX_ENCODED_BYTES = 24 * MEBIBYTE
        const val READ_CHUNK_BYTES = 64 * 1024
        const val INITIAL_BUFFER_BYTES = 4 * MEBIBYTE

        // 0.299, 0.587 and 0.114 scaled by 256.
        const val RED_WEIGHT = 77
        const val GREEN_WEIGHT = 150
        const val BLUE_WEIGHT = 29
    }
}
