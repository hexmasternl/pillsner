package nl.hexmaster.pillsner.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The app's general preferences file.
 *
 * Deliberately not the app lock's own file: that one holds security material and is excluded from
 * backup, while a language choice is harmless and may travel with the user to a new phone. Later
 * general settings belong here too.
 */
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
