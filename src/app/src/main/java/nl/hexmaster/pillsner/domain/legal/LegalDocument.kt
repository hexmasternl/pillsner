package nl.hexmaster.pillsner.domain.legal

import java.time.LocalDate

/** Which of the two documents is meant. Also the route argument of the document screen. */
enum class LegalDocumentId { DISCLAIMER, TERMS }

/**
 * A reference to a piece of user-facing text, by string resource identifier.
 *
 * The domain layer never holds prose (design D2): it holds identifiers, and the UI resolves them
 * with `stringResource`. That keeps this layer free of Android types and keeps the whole legal
 * text inside the translation check that lint runs over the resources.
 */
@JvmInline
value class TextRef(val resourceId: Int)

/** One headed part of a document: a heading and the paragraphs beneath it, in reading order. */
data class LegalSection(val heading: TextRef, val paragraphs: List<TextRef>)

/**
 * One legal document (design D1).
 *
 * @property version an integer starting at 1, raised whenever the text changes. Acceptance is
 * compared against it, so nothing finer than "newer than what was accepted" is needed.
 * @property effectiveDate the day this version took effect; shown beneath the title.
 */
data class LegalDocument(
    val id: LegalDocumentId,
    val title: TextRef,
    val version: Int,
    val effectiveDate: LocalDate,
    val sections: List<LegalSection>,
)

/** The documents this build ships with. An interface so tests can supply their own versions. */
interface LegalDocuments {

    val disclaimer: LegalDocument

    val terms: LegalDocument

    /** Both documents, Disclaimer first. */
    val all: List<LegalDocument>
        get() = listOf(disclaimer, terms)

    /** The document with this identifier. */
    operator fun get(id: LegalDocumentId): LegalDocument = when (id) {
        LegalDocumentId.DISCLAIMER -> disclaimer
        LegalDocumentId.TERMS -> terms
    }
}
