package nl.hexmaster.pillsner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.domain.model.AppTheme
import nl.hexmaster.pillsner.domain.repository.ThemeRepository

/**
 * Keeps the theme choice in the general settings file, beside the language (design D2).
 *
 * The same file on purpose: it is read once at startup anyway, so the second read is served from
 * DataStore's own cache. Not the app lock's file, which is excluded from backup because it holds
 * security material; a theme is harmless enough to travel to a new phone.
 *
 * An absent or unrecognised value reads as "follow the phone", which is also what a user who has
 * never opened Settings gets.
 */
class DataStoreThemeRepository(context: Context) : ThemeRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    override fun observeTheme(): Flow<AppTheme> =
        dataStore.data.map { AppTheme.ofKey(it[THEME]) }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            val key = theme.key
            if (key == null) preferences.remove(THEME) else preferences[THEME] = key
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
    }
}
