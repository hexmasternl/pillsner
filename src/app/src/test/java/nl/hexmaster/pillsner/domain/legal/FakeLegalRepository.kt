package nl.hexmaster.pillsner.domain.legal

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import nl.hexmaster.pillsner.domain.repository.LegalRepository

/** A [LegalRepository] held in memory, for tests that care only about what it reports. */
class FakeLegalRepository(initial: LegalAcceptance? = null) : LegalRepository {

    private val acceptance = MutableStateFlow(initial)

    var accepted: LegalAcceptance? get() = acceptance.value
        set(value) { acceptance.value = value }

    override fun observeAcceptance(): Flow<LegalAcceptance?> = acceptance

    override suspend fun accept(disclaimerVersion: Int, termsVersion: Int) {
        acceptance.value = LegalAcceptance(disclaimerVersion, termsVersion, Instant.parse("2026-09-14T10:15:30Z"))
    }
}

/** Two documents with the versions a test wants, and no interest in what they say. */
fun documentsAtVersions(disclaimerVersion: Int, termsVersion: Int): LegalDocuments =
    object : LegalDocuments {
        override val disclaimer = document(LegalDocumentId.DISCLAIMER, disclaimerVersion)
        override val terms = document(LegalDocumentId.TERMS, termsVersion)
    }

private fun document(id: LegalDocumentId, version: Int) = LegalDocument(
    id = id,
    title = TextRef(1),
    version = version,
    effectiveDate = java.time.LocalDate.of(2026, 9, 14),
    sections = listOf(LegalSection(heading = TextRef(2), paragraphs = listOf(TextRef(3)))),
)
