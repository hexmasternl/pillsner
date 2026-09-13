package nl.hexmaster.pillsner.applock.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.applock.domain.PinCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real Preferences DataStore, so this runs on a device or emulator (task 3.6). */
@RunWith(AndroidJUnit4::class)
class DataStoreAppLockRepositoryTest {

    private val repository = DataStoreAppLockRepository(ApplicationProvider.getApplicationContext())

    @Before
    fun clearPersistedState() = runBlocking {
        repository.clearCredential()
    }

    @Test
    fun storingACredentialPersistsItAndEnablesTheLock() = runBlocking {
        val credential = PinCredential(salt = byteArrayOf(1, 2, 3), verifier = byteArrayOf(4, 5, 6))

        repository.storeCredential(credential)

        val settings = repository.settings.first()
        assertTrue(settings.enabled)
        assertEquals(credential, settings.credential)
        assertEquals(0, settings.consecutiveFailures)
        assertNull(settings.cooldownEndsAt)
    }

    @Test
    fun clearingTheCredentialRemovesEverything() = runBlocking {
        repository.storeCredential(PinCredential(byteArrayOf(1), byteArrayOf(2)))
        repository.setBiometricEnabled(true)

        repository.clearCredential()

        val settings = repository.settings.first()
        assertFalse(settings.enabled)
        assertFalse(settings.biometricEnabled)
        assertNull(settings.credential)
    }

    @Test
    fun recordedFailuresAndCooldownSurvivePersistence() = runBlocking {
        repository.storeCredential(PinCredential(byteArrayOf(1), byteArrayOf(2)))
        val cooldownEndsAt = java.time.Instant.now().plusSeconds(30)

        repository.recordFailedAttempt(5, cooldownEndsAt)

        val settings = repository.settings.first()
        assertEquals(5, settings.consecutiveFailures)
        assertEquals(cooldownEndsAt.toEpochMilli(), settings.cooldownEndsAt?.toEpochMilli())
    }
}
