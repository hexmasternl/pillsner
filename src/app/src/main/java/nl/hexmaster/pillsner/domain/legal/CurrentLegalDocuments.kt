package nl.hexmaster.pillsner.domain.legal

import java.time.LocalDate
import nl.hexmaster.pillsner.R

/**
 * The two documents this build ships with: their sections in reading order, their versions and the
 * dates those versions took effect (design D1).
 *
 * One place, deliberately: raising a version is a one-line edit beside the text it describes, which
 * is the only way it reliably happens when the wording changes. Change the text of a section and
 * raise that document's [LegalDocument.version] and [LegalDocument.effectiveDate] in the same edit.
 */
object CurrentLegalDocuments : LegalDocuments {

    override val disclaimer = LegalDocument(
        id = LegalDocumentId.DISCLAIMER,
        title = TextRef(R.string.legal_disclaimer_title),
        version = 1,
        effectiveDate = LocalDate.of(2026, 9, 14),
        sections = listOf(
            LegalSection(
                heading = TextRef(R.string.legal_disclaimer_s1_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_disclaimer_s1_p1),
                    TextRef(R.string.legal_disclaimer_s1_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_disclaimer_s2_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_disclaimer_s2_p1),
                    TextRef(R.string.legal_disclaimer_s2_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_disclaimer_s3_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_disclaimer_s3_p1),
                    TextRef(R.string.legal_disclaimer_s3_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_disclaimer_s4_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_disclaimer_s4_p1),
                    TextRef(R.string.legal_disclaimer_s4_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_disclaimer_s5_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_disclaimer_s5_p1),
                    TextRef(R.string.legal_disclaimer_s5_p2),
                ),
            ),
        ),
    )

    override val terms = LegalDocument(
        id = LegalDocumentId.TERMS,
        title = TextRef(R.string.legal_terms_title),
        version = 1,
        effectiveDate = LocalDate.of(2026, 9, 14),
        sections = listOf(
            LegalSection(
                heading = TextRef(R.string.legal_terms_s1_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_terms_s1_p1),
                    TextRef(R.string.legal_terms_s1_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_terms_s2_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_terms_s2_p1),
                    TextRef(R.string.legal_terms_s2_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_terms_s3_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_terms_s3_p1),
                    TextRef(R.string.legal_terms_s3_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_terms_s4_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_terms_s4_p1),
                    TextRef(R.string.legal_terms_s4_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_terms_s5_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_terms_s5_p1),
                    TextRef(R.string.legal_terms_s5_p2),
                ),
            ),
            LegalSection(
                heading = TextRef(R.string.legal_terms_s6_heading),
                paragraphs = listOf(
                    TextRef(R.string.legal_terms_s6_p1),
                    TextRef(R.string.legal_terms_s6_p2),
                ),
            ),
        ),
    )
}
