package nl.hexmaster.pillsner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.domain.legal.LegalAcceptance
import nl.hexmaster.pillsner.domain.repository.LegalRepository

/**
 * Keeps the user's acceptance of the legal documents in the general settings file (design D3).
 *
 * Three scalars, so not Room: a table would drag a schema migration into what is a setting. It goes
 * in the general file rather than the app lock's, because acceptance is not security material and a
 * user who restores a backup onto a new phone has genuinely already accepted these documents.
 *
 * @param clock the moment an acceptance is stamped with; injectable so a test can pin it.
 */
class DataStoreLegalRepository(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
) : LegalRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    override fun observeAcceptance(): Flow<LegalAcceptance?> = dataStore.data.map { preferences ->
        val disclaimer = preferences[DISCLAIMER_VERSION]
        val terms = preferences[TERMS_VERSION]
        val acceptedAt = preferences[ACCEPTED_AT]

        // A partial record is no record: rather than report an acceptance with a version but no
        // moment, or a moment but no version, ask again. Asking twice is the harmless mistake.
        if (disclaimer == null || terms == null || acceptedAt == null) {
            null
        } else {
            LegalAcceptance(
                disclaimerVersion = disclaimer,
                termsVersion = terms,
                acceptedAt = Instant.ofEpochMilli(acceptedAt),
            )
        }
    }

    override suspend fun accept(disclaimerVersion: Int, termsVersion: Int) {
        // One edit, all three keys: a version can never end up stored without the time it was
        // accepted, whatever happens between one write and the next.
        dataStore.edit { preferences ->
            preferences[DISCLAIMER_VERSION] = disclaimerVersion
            preferences[TERMS_VERSION] = termsVersion
            preferences[ACCEPTED_AT] = clock.instant().toEpochMilli()
        }
    }

    private companion object {
        val DISCLAIMER_VERSION = intPreferencesKey("legal_disclaimer_version")
        val TERMS_VERSION = intPreferencesKey("legal_terms_version")
        val ACCEPTED_AT = longPreferencesKey("legal_accepted_at")
    }
}
