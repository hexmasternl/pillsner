package nl.hexmaster.pillsner.data.labelscan

import androidx.annotation.WorkerThread
import nl.hexmaster.pillsner.domain.labelscan.RecognisedLine

/**
 * What the scanning screen and the picked-photo path need from a text recogniser
 * (medicine-label-photo-prefill design D3). [LabelTextRecogniser] is the Tesseract implementation;
 * tests use a fake, since the native engine cannot run on the JVM.
 */
interface FrameRecogniser {

    /** Whether [open] has succeeded and [close] has not run since. */
    val isOpen: Boolean

    /** Prepares the engine for a session. Blocking; call off the main thread. */
    @WorkerThread
    fun open()

    /** The text lines on [frame] in reading order; empty when not open or stopped mid-frame. */
    @WorkerThread
    fun recognise(frame: GreyFrame): List<RecognisedLine>

    /** Interrupts a recognition in progress, from any thread. The recogniser stays open. */
    fun stop()

    /** Releases the engine. */
    fun close()
}
