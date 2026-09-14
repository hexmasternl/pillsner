package nl.hexmaster.pillsner.domain.repository

import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.legal.LegalAcceptance

/** The one place the user's acceptance of the legal documents is kept (design D3). */
interface LegalRepository {

    /** The stored acceptance, or null while the user has never accepted. */
    fun observeAcceptance(): Flow<LegalAcceptance?>

    /** Records that the user accepted both documents at these versions, now. */
    suspend fun accept(disclaimerVersion: Int, termsVersion: Int)
}
