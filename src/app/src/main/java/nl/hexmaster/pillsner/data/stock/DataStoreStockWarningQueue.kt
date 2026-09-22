package nl.hexmaster.pillsner.data.stock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.repository.StockWarningQueue

/** The stock warning queue's own Preferences file; nothing in it is secret. */
private val Context.stockWarningDataStore: DataStore<Preferences> by preferencesDataStore(name = "stock_warnings")

/**
 * The wired [StockWarningQueue]: which medicines have a stock warning waiting, kept on the device so
 * it survives the process dying between a backgrounded take and the app next being opened.
 */
class DataStoreStockWarningQueue(context: Context) : StockWarningQueue {

    private val dataStore = context.applicationContext.stockWarningDataStore

    override fun observePending(): Flow<Set<MedicationId>> =
        dataStore.data.map { prefs -> prefs[PENDING].orEmpty().map { MedicationId(it.toLong()) }.toSet() }

    override suspend fun enqueue(medicationId: MedicationId) {
        dataStore.edit { prefs -> prefs[PENDING] = prefs[PENDING].orEmpty() + medicationId.value.toString() }
    }

    override suspend fun clear(medicationIds: Set<MedicationId>) {
        val remove = medicationIds.map { it.value.toString() }.toSet()
        dataStore.edit { prefs -> prefs[PENDING] = prefs[PENDING].orEmpty() - remove }
    }

    private companion object {
        val PENDING = stringSetPreferencesKey("pending_medication_ids")
    }
}
