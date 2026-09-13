package nl.hexmaster.pillsner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.repository.LanguageRepository

/**
 * Keeps the language choice in the general settings file (design D2).
 *
 * One scalar, so not Room: a table would drag a schema migration into every future preference.
 * An absent or unrecognised value reads as "follow the phone", which is also what a user who has
 * never opened Settings gets.
 */
class DataStoreLanguageRepository(context: Context) : LanguageRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    override fun observeLanguage(): Flow<AppLanguage> =
        dataStore.data.map { AppLanguage.ofTag(it[LANGUAGE]) }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            val tag = language.tag
            if (tag == null) preferences.remove(LANGUAGE) else preferences[LANGUAGE] = tag
        }
    }

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
    }
}
