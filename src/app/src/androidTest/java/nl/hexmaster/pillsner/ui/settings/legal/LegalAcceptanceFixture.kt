package nl.hexmaster.pillsner.ui.settings.legal

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.settings.settingsDataStore
import nl.hexmaster.pillsner.di.AppContainer
import nl.hexmaster.pillsner.domain.legal.CurrentLegalDocuments

/**
 * Acceptance is stored on the device, so an instrumented test inherits whatever the last one left
 * behind. Every test that reaches the Medicines add button says which of the two states it wants.
 */
object LegalAcceptanceFixture {

    /** The state of a user who has already accepted the documents this build ships. */
    fun accept(container: AppContainer) = runBlocking {
        container.legalRepository.accept(
            disclaimerVersion = CurrentLegalDocuments.disclaimer.version,
            termsVersion = CurrentLegalDocuments.terms.version,
        )
    }

    /** The state of a fresh install: nothing accepted, so the gate appears. */
    fun clear(context: Context) = runBlocking {
        context.applicationContext.settingsDataStore.edit { preferences ->
            preferences.remove(intPreferencesKey("legal_disclaimer_version"))
            preferences.remove(intPreferencesKey("legal_terms_version"))
            preferences.remove(longPreferencesKey("legal_accepted_at"))
        }
    }
}
