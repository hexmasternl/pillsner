package nl.hexmaster.pillsner.data.stock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.repository.StockWarningQueue

/** The stock warning queue's own Preferences file; nothing in it is secret. */
private val Context.stockWarningDataStore: DataStore<Preferences> by preferencesDataStore(name = "stock_warnings")

/**
 * The wired [StockWarningQueue]: which medicines have a stock warning waiting, kept on the device so
 * it survives the process dying between a backgrounded take and the app next being opened.
 *
 * Each entry is `<medication id>` or `<medication id>@<ISO expiry date>`; see [PendingEntry].
 */
class DataStoreStockWarningQueue(context: Context) : StockWarningQueue {

    private val dataStore = context.applicationContext.stockWarningDataStore

    override fun observePending(): Flow<Map<MedicationId, LocalDate?>> =
        dataStore.data.map { prefs ->
            prefs[PENDING].orEmpty().map(PendingEntry::decode).associate { it.medicationId to it.drawnBatchExpiry }
        }

    override suspend fun enqueue(medicationId: MedicationId, drawnBatchExpiry: LocalDate?) {
        dataStore.edit { prefs ->
            val others = prefs[PENDING].orEmpty().filterNot { PendingEntry.decode(it).medicationId == medicationId }
            prefs[PENDING] = others.toSet() + PendingEntry(medicationId, drawnBatchExpiry).encode()
        }
    }

    override suspend fun clear(medicationIds: Set<MedicationId>) {
        dataStore.edit { prefs ->
            prefs[PENDING] = prefs[PENDING].orEmpty().filterNot { PendingEntry.decode(it).medicationId in medicationIds }.toSet()
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { prefs -> prefs.remove(PENDING) }
    }

    private companion object {
        val PENDING = stringSetPreferencesKey("pending_medication_ids")
    }
}

/** One stored queue entry. An entry with no date is also what an older build wrote. */
internal data class PendingEntry(val medicationId: MedicationId, val drawnBatchExpiry: LocalDate?) {

    fun encode(): String = drawnBatchExpiry?.let { "${medicationId.value}$SEPARATOR$it" } ?: "${medicationId.value}"

    companion object {
        private const val SEPARATOR = "@"

        fun decode(value: String): PendingEntry {
            val id = value.substringBefore(SEPARATOR)
            val date = value.substringAfter(SEPARATOR, missingDelimiterValue = "")
            return PendingEntry(MedicationId(id.toLong()), date.takeIf { it.isNotEmpty() }?.let(LocalDate::parse))
        }
    }
}
