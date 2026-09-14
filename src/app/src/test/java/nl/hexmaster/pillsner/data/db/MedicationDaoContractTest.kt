package nl.hexmaster.pillsner.data.db

import androidx.room.Delete
import androidx.room.Query
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A medicine can never be removed from Pillsner.
 *
 * A user who stops taking something deactivates it; the record, its schedules and every dose it ever
 * produced stay on the device. That is a product rule, not an implementation detail, so it is
 * guarded here rather than left to a review note: a future change that adds a delete fails the
 * build long before it reaches anybody's phone.
 */
class MedicationDaoContractTest {

    @Test
    fun theMedicationDao_hasNoDeleteMethod() {
        val offenders = MedicationDao::class.java.declaredMethods
            .filter { it.isAnnotationPresent(Delete::class.java) }
            .map { it.name }

        assertEquals(RULE, emptyList<String>(), offenders)
    }

    @Test
    fun theMedicationDao_neverDeletesAMedicationRow() {
        val offenders = MedicationDao::class.java.declaredMethods
            .mapNotNull { method ->
                method.getAnnotation(Query::class.java)?.value
                    ?.takeIf { it.deletesMedications() }
                    ?.let { "${method.name}: $it" }
            }

        assertEquals(RULE, emptyList<String>(), offenders)
    }

    private fun String.deletesMedications(): Boolean {
        val sql = replace(Regex("\\s+"), " ").trim().uppercase()
        return sql.startsWith("DELETE") && sql.contains("FROM MEDICATIONS")
    }

    private companion object {
        const val RULE = "A medicine can never be removed. Deactivate it instead."
    }
}
