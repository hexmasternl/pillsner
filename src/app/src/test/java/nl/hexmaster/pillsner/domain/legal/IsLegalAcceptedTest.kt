package nl.hexmaster.pillsner.domain.legal

import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one predicate the gate reads (design D4). Every combination of stored and current versions,
 * because getting this wrong either locks a user out of adding a medicine or lets a revised text
 * pass unread.
 */
class IsLegalAcceptedTest {

    @Test
    fun `no record is not accepted`() = runTest {
        assertFalse(isAccepted(stored = null, currentDisclaimer = 1, currentTerms = 1))
    }

    @Test
    fun `both versions current is accepted`() = runTest {
        assertTrue(isAccepted(stored = acceptance(1, 1), currentDisclaimer = 1, currentTerms = 1))
    }

    @Test
    fun `a revised disclaimer is not accepted`() = runTest {
        assertFalse(isAccepted(stored = acceptance(1, 1), currentDisclaimer = 2, currentTerms = 1))
    }

    @Test
    fun `revised terms are not accepted`() = runTest {
        assertFalse(isAccepted(stored = acceptance(1, 1), currentDisclaimer = 1, currentTerms = 2))
    }

    @Test
    fun `both documents revised is not accepted`() = runTest {
        assertFalse(isAccepted(stored = acceptance(1, 1), currentDisclaimer = 2, currentTerms = 2))
    }

    @Test
    fun `a record newer than this build is accepted and does not ask again`() = runTest {
        assertTrue(isAccepted(stored = acceptance(2, 2), currentDisclaimer = 1, currentTerms = 1))
    }

    @Test
    fun `accepting turns the answer to true`() = runTest {
        val repository = FakeLegalRepository()
        val isLegalAccepted = IsLegalAccepted(repository, documentsAtVersions(1, 1))
        assertFalse(isLegalAccepted().first())

        repository.accept(disclaimerVersion = 1, termsVersion = 1)

        assertTrue(isLegalAccepted().first())
    }

    private suspend fun isAccepted(stored: LegalAcceptance?, currentDisclaimer: Int, currentTerms: Int): Boolean =
        IsLegalAccepted(
            repository = FakeLegalRepository(stored),
            documents = documentsAtVersions(currentDisclaimer, currentTerms),
        )().first()

    private fun acceptance(disclaimerVersion: Int, termsVersion: Int) = LegalAcceptance(
        disclaimerVersion = disclaimerVersion,
        termsVersion = termsVersion,
        acceptedAt = Instant.parse("2026-01-01T09:00:00Z"),
    )
}
