package nl.hexmaster.pillsner.data.labelscan

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Reads a photo the user picked into a greyscale frame for the recogniser (medicine-label-photo-
 * prefill design D3, D6). The photo is opened through the content resolver as many times as it
 * takes to read its bounds, its orientation and its pixels, and never copied anywhere.
 */
class PickedPhotoDecoder(private val contentResolver: ContentResolver) {

    /**
     * The photo as an upright greyscale frame whose long side is at most [MAX_LONG_SIDE] pixels,
     * or null when it cannot be decoded. Blocking; call off the main thread.
     */
    @WorkerThread
    fun decode(uri: Uri): GreyFrame? = try {
        // One read of the source, into memory: bounds, orientation and pixels all come from these
        // bytes, so the photo is opened exactly once and never written anywhere (design D6).
        val encoded = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encoded, 0, encoded.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(maxOf(bounds.outWidth, bounds.outHeight))
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(encoded, 0, encoded.size, options) ?: return null
        val orientation = ByteArrayInputStream(encoded).use {
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

    /** The power of two that brings [longSide] to at most [MAX_LONG_SIDE]. */
    private fun sampleSizeFor(longSide: Int): Int {
        var sample = 1
        while (longSide / sample > MAX_LONG_SIDE) sample *= 2
        return sample
    }

    /** Luminance from the usual weights, in integer arithmetic. */
    private fun toGrey(bitmap: Bitmap): GreyFrame {
        val width = bitmap.width
        val height = bitmap.height
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        val grey = ByteArray(argb.size)
        for (index in argb.indices) {
            val pixel = argb[index]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            grey[index] = ((RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue) shr 8).toByte()
        }
        return GreyFrame(grey, width, height)
    }

    private companion object {
        const val TAG = "LabelScan"

        /** Plenty for a label; bounds memory on a 48-megapixel photo (design D3). */
        const val MAX_LONG_SIDE = 2_000

        // 0.299, 0.587 and 0.114 scaled by 256.
        const val RED_WEIGHT = 77
        const val GREEN_WEIGHT = 150
        const val BLUE_WEIGHT = 29
    }
}
