package nl.hexmaster.pillsner.shared.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The wire format is the one thing the phone and the watch have to agree on for ever, so it is
 * tested where both of them compile it (design D1).
 */
class SyncedDosesTest {

    private val payload = SyncedDoses(
        languageTag = "nl-NL",
        publishedAtEpochMillis = 1_789_000_000_000,
        doses = listOf(
            SyncedDose(
                doseId = 1,
                medicationName = "Ibuprofen",
                amountText = "400 mg",
                scheduledAtEpochMillis = 1_789_000_600_000,
                details = SyncedMedicineDetails(
                    defaultDoseText = "400 mg",
                    scheduleLines = listOf("400 mg tweemaal per dag"),
                    stockText = "24 tabletten",
                ),
            ),
            SyncedDose(2, "Paracetamol", "2 tabletten", 1_789_007_200_000),
        ),
    )

    @Test
    fun `what the phone writes is what the watch reads`() {
        assertEquals(payload, SyncedDoses.decode(SyncedDoses.encode(payload)))
    }

    @Test
    fun `a field this build has never heard of is ignored`() {
        val fromANewerPhone = """
            {"version":1,"languageTag":"en","publishedAtEpochMillis":1,
             "doses":[{"doseId":7,"medicationName":"Ibuprofen","amountText":"400 mg",
                       "scheduledAtEpochMillis":2,"stockRemaining":12}],
             "publishedByBuild":"future"}
        """.trimIndent()

        val decoded = SyncedDoses.decode(fromANewerPhone)

        assertEquals(1, decoded?.doses?.size)
        assertEquals("Ibuprofen", decoded?.doses?.first()?.medicationName)
    }

    @Test
    fun `a payload from a later version is not guessed at`() {
        val raw = SyncedDoses.encode(payload.copy(version = SyncedDoses.CURRENT_VERSION + 1))

        assertNull("Better no list than a misread one", SyncedDoses.decode(raw))
    }

    @Test
    fun `nonsense decodes to nothing rather than throwing`() {
        assertNull(SyncedDoses.decode("not json at all"))
        assertNull(SyncedDoses.decode("""{"version":1}"""))
    }

    @Test
    fun `an empty list is a payload like any other`() {
        val empty = payload.copy(doses = emptyList())

        assertEquals(empty, SyncedDoses.decode(SyncedDoses.encode(empty)))
    }

    @Test
    fun `a dose a phone sent without medicine details is still a dose`() {
        val fromAnOlderPhone = """
            {"version":1,"languageTag":"en","publishedAtEpochMillis":1,
             "doses":[{"doseId":7,"medicationName":"Ibuprofen","amountText":"400 mg",
                       "scheduledAtEpochMillis":2}]}
        """.trimIndent()

        val dose = SyncedDoses.decode(fromAnOlderPhone)?.doses?.single()

        assertEquals("Ibuprofen", dose?.medicationName)
        assertNull("Nothing to show on the details screen, rather than a broken payload", dose?.details)
    }

    @Test
    fun `a medicine with no stock recorded has no stock line`() {
        val withoutStock = payload.copy(
            doses = listOf(payload.doses.first().copy(details = SyncedMedicineDetails("400 mg"))),
        )

        val decoded = SyncedDoses.decode(SyncedDoses.encode(withoutStock))

        assertEquals(withoutStock, decoded)
        assertNull(decoded?.doses?.single()?.details?.stockText)
    }
}
