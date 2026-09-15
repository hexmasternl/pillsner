package nl.hexmaster.pillsner.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.model.AppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-theme, "Theme setting is stored on the device only". */
@RunWith(AndroidJUnit4::class)
class DataStoreThemeRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = DataStoreThemeRepository(context)

    @Before
    fun setUp() = runBlocking { repository.setTheme(AppTheme.SYSTEM) }

    @After
    fun tearDown() = runBlocking { repository.setTheme(AppTheme.SYSTEM) }

    @Test
    fun aUserWhoHasChosenNothingFollowsThePhone() = runBlocking {
        assertEquals(AppTheme.SYSTEM, repository.observeTheme().first())
    }

    @Test
    fun lightIsReadBack() = runBlocking {
        repository.setTheme(AppTheme.LIGHT)

        assertEquals(AppTheme.LIGHT, repository.observeTheme().first())
    }

    @Test
    fun darkIsReadBack() = runBlocking {
        repository.setTheme(AppTheme.DARK)

        assertEquals(AppTheme.DARK, repository.observeTheme().first())
    }

    @Test
    fun aChoiceOutlivesTheObjectThatStoredIt() = runBlocking {
        repository.setTheme(AppTheme.DARK)

        // A new instance on the same file is what the next app start sees.
        assertEquals(AppTheme.DARK, DataStoreThemeRepository(context).observeTheme().first())
    }

    @Test
    fun choosingToFollowThePhoneAgainClearsTheChoice() = runBlocking {
        repository.setTheme(AppTheme.DARK)

        repository.setTheme(AppTheme.SYSTEM)

        assertEquals(AppTheme.SYSTEM, repository.observeTheme().first())
    }

    @Test
    fun aValueTheAppDoesNotRecogniseFollowsThePhone() = runBlocking {
        context.settingsDataStore.edit { it[stringPreferencesKey("theme")] = "amoled" }

        assertEquals(AppTheme.SYSTEM, repository.observeTheme().first())
    }

    @Test
    fun theThemeDoesNotLandInTheAppLockFile() = runBlocking {
        repository.setTheme(AppTheme.DARK)

        // The lock's own file is excluded from backup; a theme has no business in it.
        val settings = context.settingsDataStore.data.first()
        assertEquals("dark", settings[stringPreferencesKey("theme")])
    }
}
