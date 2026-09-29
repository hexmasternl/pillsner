package nl.hexmaster.pillsner.data.labelscan

import android.net.Uri
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import nl.hexmaster.pillsner.domain.labelscan.InterpretLabelText
import nl.hexmaster.pillsner.domain.labelscan.LabelInterpretation

/**
 * Reads one picked photo into a label interpretation (medicine-label-photo-prefill design D3).
 * The form's view model depends on this rather than on the recogniser, so its tests use a fake.
 */
interface PhotoScanner {

    /**
     * Decodes and recognises the photo at [uri] once.
     *
     * @param uri the picked photo's content URI as a string, so callers need no Android type.
     * @return the interpretation, or null when the photo could not be decoded or read.
     */
    suspend fun scan(uri: String): LabelInterpretation?

    /** Interrupts a scan in progress; the coroutine running [scan] then returns promptly. */
    fun stop()
}

/** The real path: decoder, one recogniser session, then the pure interpretation. */
class PickedPhotoScanner(
    private val decoder: PickedPhotoDecoder,
    private val recogniser: FrameRecogniser,
    private val interpret: InterpretLabelText,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PhotoScanner {

    override suspend fun scan(uri: String): LabelInterpretation? = withContext(ioDispatcher) {
        // The decoder polls for cancellation between chunks of its read; opening the engine cannot
        // be interrupted, so a cancel that lands there is honoured at the next boundary rather than
        // after a whole recognition.
        val context = currentCoroutineContext()
        val frame = decoder.decode(Uri.parse(uri), isCancelled = { !context.isActive }) ?: return@withContext null
        context.ensureActive()
        try {
            recogniser.open()
            currentCoroutineContext().ensureActive()
            val lines = recogniser.recognise(frame)
            interpret(lines, LocalDate.now(clock))
        } catch (cancelled: CancellationException) {
            // A subtype of IllegalStateException, so it has to be let through before the next clause.
            throw cancelled
        } catch (failure: IllegalStateException) {
            null
        } finally {
            recogniser.close()
        }
    }

    override fun stop() = recogniser.stop()
}
