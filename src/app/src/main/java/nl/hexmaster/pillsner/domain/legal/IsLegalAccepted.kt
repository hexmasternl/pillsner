package nl.hexmaster.pillsner.domain.legal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.domain.repository.LegalRepository

/**
 * The one question the rest of the app asks about the legal documents (design D4): are the
 * documents this build ships with accepted?
 *
 * True only when a record exists and both stored versions are at least the current ones. At least,
 * rather than exactly, so a user who downgrades to an older build is not asked again for text they
 * have already seen a newer version of.
 */
class IsLegalAccepted(
    private val repository: LegalRepository,
    private val documents: LegalDocuments = CurrentLegalDocuments,
) {

    operator fun invoke(): Flow<Boolean> =
        repository.observeAcceptance().map { covers(it) }

    /** Whether [acceptance] covers both current documents. A missing record never does. */
    fun covers(acceptance: LegalAcceptance?): Boolean =
        acceptance != null &&
            acceptance.disclaimerVersion >= documents.disclaimer.version &&
            acceptance.termsVersion >= documents.terms.version
}
