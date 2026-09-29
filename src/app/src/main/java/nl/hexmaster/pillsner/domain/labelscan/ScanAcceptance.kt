package nl.hexmaster.pillsner.domain.labelscan

/**
 * Decides when a live scan has read well enough to stop (medicine-label-photo-prefill design D3).
 *
 * A frame is *good* when its interpretation has a name and at least one of a default dose or a
 * schedule. The scan accepts itself when two consecutive good frames agree on the name, compared
 * without regard to case or whitespace; the later frame's interpretation is the one used. Two
 * frames rather than one keeps a single misread from being accepted; more than two only makes the
 * user hold still for longer.
 *
 * Pure Kotlin and deterministic. One instance per scan session; it holds only the previous good
 * frame's name key, never the frame itself.
 */
class ScanAcceptance {

    private var previousGoodName: String? = null

    /** How many frames have been offered since the session started or was last reset. */
    var framesSeen: Int = 0
        private set

    /**
     * Offers the next frame's interpretation.
     *
     * @return [frame] when it is the second consecutive good frame with the same name, else null.
     */
    fun offer(frame: LabelInterpretation): LabelInterpretation? {
        framesSeen++
        val key = if (frame.isGood) nameKey(frame.name.orEmpty()) else null
        val accepted = key != null && key == previousGoodName
        previousGoodName = key
        return if (accepted) frame else null
    }

    /** Forgets the previous frame, as when the camera is re-bound after the app was backgrounded. */
    fun reset() {
        previousGoodName = null
        framesSeen = 0
    }

    companion object {
        /** Whether a frame read enough to count towards acceptance. */
        val LabelInterpretation.isGood: Boolean
            get() = name != null && (defaultDose != null || schedules.isNotEmpty())

        private fun nameKey(name: String): String = name.filterNot(Char::isWhitespace).lowercase()
    }
}
