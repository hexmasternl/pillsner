package nl.hexmaster.pillsner.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.model.AppLanguage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: app-language storage. */
@RunWith(AndroidJUnit4::class)
class DataStoreLanguageRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository = DataStoreLanguageRepository(context)

    @Before
    fun setUp() = runBlocking { repository.setLanguage(AppLanguage.SYSTEM) }

    @After
    fun tearDown() = runBlocking { repository.setLanguage(AppLanguage.SYSTEM) }

    @Test
    fun aUserWhoHasChosenNothingFollowsThePhone() = runBlocking {
        assertEquals(AppLanguage.SYSTEM, repository.observeLanguage().first())
    }

    @Test
    fun aChoiceIsReadBack() = runBlocking {
        repository.setLanguage(AppLanguage.DUTCH)

        assertEquals(AppLanguage.DUTCH, repository.observeLanguage().first())
    }

    @Test
    fun aChoiceOutlivesTheObjectThatStoredIt() = runBlocking {
        repository.setLanguage(AppLanguage.DUTCH)

        // A new instance on the same file is what the next app start sees.
        assertEquals(AppLanguage.DUTCH, DataStoreLanguageRepository(context).observeLanguage().first())
    }

    @Test
    fun choosingToFollowThePhoneAgainClearsTheChoice() = runBlocking {
        repository.setLanguage(AppLanguage.DUTCH)

        repository.setLanguage(AppLanguage.SYSTEM)

        assertEquals(AppLanguage.SYSTEM, repository.observeLanguage().first())
    }
}
