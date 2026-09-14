package nl.hexmaster.pillsner.shared.wear

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One dose on the wire (design D2).
 *
 * @property amountText the amount already written out by the phone, "40 mg" or "2 tabletten". The
 * phone is the only side that knows the units and their translations, and the amount is a snapshot
 * anyway, so the watch never needs `Quantity`, `DoseUnit` or a plural resource of its own.
 * @property scheduledAtEpochMillis an instant, not a local time: the watch formats it in its own
 * zone, so a difference between the two devices cannot produce a wrong time.
 */
@Serializable
data class SyncedDose(
    val doseId: Long,
    val medicationName: String,
    val amountText: String,
    val scheduledAtEpochMillis: Long,
)

/**
 * Everything the phone tells the watch (design D2): every pending dose it holds, not just the six
 * hours the watch shows. The watch does its own filtering with its own clock, so the list stays
 * right for as long as the payload lasts even while the phone is out of reach.
 *
 * @property version the payload's major version. A watch that meets a higher one shows its
 * "not connected" footer rather than a list it cannot read.
 * @property languageTag the phone app's effective language, BCP 47. The watch reads its own strings
 * in it, so "2 tabletten" never appears under an English heading.
 * @property publishedAtEpochMillis changes on every publish, so the Data Layer sees a changed item
 * and syncs it even when the doses are identical — which is what makes a language change arrive.
 */
@Serializable
data class SyncedDoses(
    val version: Int = CURRENT_VERSION,
    val languageTag: String,
    val publishedAtEpochMillis: Long,
    val doses: List<SyncedDose>,
) {
    companion object {
        /** Additive changes keep this; removing or reinterpreting a field raises it. */
        const val CURRENT_VERSION = 1

        /**
         * Unknown fields are ignored so a newer phone can add one without breaking an older watch.
         */
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(doses: SyncedDoses): String = json.encodeToString(doses)

        /** Returns null for anything this build cannot read: malformed, or from a later version. */
        fun decode(raw: String): SyncedDoses? {
            val decoded = try {
                json.decodeFromString<SyncedDoses>(raw)
            } catch (error: SerializationException) {
                return null
            } catch (error: IllegalArgumentException) {
                return null
            }
            return decoded.takeIf { it.version <= CURRENT_VERSION }
        }
    }
}
