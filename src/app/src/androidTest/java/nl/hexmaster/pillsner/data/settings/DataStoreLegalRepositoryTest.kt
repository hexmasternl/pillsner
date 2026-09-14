package nl.hexmaster.pillsner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-legal "Acceptance is recorded on the device". */
@RunWith(AndroidJUnit4::class)
class DataStoreLegalRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val acceptedAt = Instant.parse("2026-09-14T08:30:00Z")
    private val repository = DataStoreLegalRepository(context, Clock.fixed(acceptedAt, ZoneOffset.UTC))

    @Before
    fun setUp() = runBlocking { clearAcceptance() }

    @After
    fun tearDown() = runBlocking { clearAcceptance() }

    @Test
    fun aFreshInstallHasNoAcceptance() = runBlocking {
        assertNull(repository.observeAcceptance().first())
    }

    @Test
    fun anAcceptanceIsReadBackWithItsVersionsAndItsMoment() = runBlocking {
        repository.accept(disclaimerVersion = 1, termsVersion = 1)

        val stored = repository.observeAcceptance().first()

        assertEquals(1, stored?.disclaimerVersion)
        assertEquals(1, stored?.termsVersion)
        assertEquals(acceptedAt, stored?.acceptedAt)
    }

    @Test
    fun anAcceptanceOutlivesTheObjectThatStoredIt() = runBlocking {
        repository.accept(disclaimerVersion = 2, termsVersion = 3)

        // A new instance on the same file is what the next app start sees.
        val stored = DataStoreLegalRepository(context).observeAcceptance().first()

        assertEquals(2, stored?.disclaimerVersion)
        assertEquals(3, stored?.termsVersion)
        assertEquals(acceptedAt, stored?.acceptedAt)
    }

    @Test
    fun aRecordWithNoMomentIsNoRecord() = runBlocking {
        context.settingsDataStore.edit { preferences ->
            preferences[DISCLAIMER_VERSION] = 1
            preferences[TERMS_VERSION] = 1
            preferences.remove(ACCEPTED_AT)
        }

        assertNull(repository.observeAcceptance().first())
    }

    @Test
    fun aRecordMissingAVersionIsNoRecord() = runBlocking {
        context.settingsDataStore.edit { preferences ->
            preferences[DISCLAIMER_VERSION] = 1
            preferences.remove(TERMS_VERSION)
            preferences[ACCEPTED_AT] = acceptedAt.toEpochMilli()
        }

        assertNull(repository.observeAcceptance().first())
    }

    private suspend fun clearAcceptance() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(DISCLAIMER_VERSION)
            preferences.remove(TERMS_VERSION)
            preferences.remove(ACCEPTED_AT)
        }
    }

    private companion object {
        val DISCLAIMER_VERSION = intPreferencesKey("legal_disclaimer_version")
        val TERMS_VERSION = intPreferencesKey("legal_terms_version")
        val ACCEPTED_AT = longPreferencesKey("legal_accepted_at")
    }
}
