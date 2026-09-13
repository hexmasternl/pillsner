package nl.hexmaster.pillsner.ui.settings.language

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.repository.LanguageRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Spec: app-language section and the restart notice. */
@OptIn(ExperimentalCoroutinesApi::class)
class LanguageSectionViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeLanguageRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a user who has chosen nothing follows the phone`() = runTest(dispatcher) {
        val viewModel = collecting(inEffect = AppLanguage.ENGLISH)

        val state = viewModel.state.value
        assertEquals(AppLanguage.SYSTEM, state.selected)
        assertFalse(state.restartRequired)
    }

    @Test
    fun `the dropdown offers the phone first, then every language the app ships`() = runTest(dispatcher) {
        val viewModel = collecting(inEffect = AppLanguage.ENGLISH)

        assertEquals(
            listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH, AppLanguage.DUTCH),
            viewModel.state.value.options,
        )
    }

    @Test
    fun `choosing a language stores it straight away`() = runTest(dispatcher) {
        val viewModel = collecting(inEffect = AppLanguage.ENGLISH)

        viewModel.onLanguageSelected(AppLanguage.DUTCH)

        assertEquals(AppLanguage.DUTCH, repository.stored.value)
        assertEquals(AppLanguage.DUTCH, viewModel.state.value.selected)
    }

    @Test
    fun `choosing a language the app is not running in asks for a restart`() = runTest(dispatcher) {
        val viewModel = collecting(inEffect = AppLanguage.ENGLISH)

        viewModel.onLanguageSelected(AppLanguage.DUTCH)

        assertTrue(viewModel.state.value.restartRequired)
    }

    @Test
    fun `choosing back the language on screen takes the notice away again`() = runTest(dispatcher) {
        val viewModel = collecting(inEffect = AppLanguage.ENGLISH)
        viewModel.onLanguageSelected(AppLanguage.DUTCH)
        assertTrue(viewModel.state.value.restartRequired)

        viewModel.onLanguageSelected(AppLanguage.ENGLISH)

        assertFalse(viewModel.state.value.restartRequired)
    }

    @Test
    fun `after the restart the choice and the language on screen agree`() = runTest(dispatcher) {
        repository.stored.value = AppLanguage.DUTCH

        val viewModel = collecting(inEffect = AppLanguage.DUTCH)

        assertEquals(AppLanguage.DUTCH, viewModel.state.value.selected)
        assertFalse(viewModel.state.value.restartRequired)
    }

    @Test
    fun `following the phone never asks for a restart`() = runTest(dispatcher) {
        val viewModel = collecting(inEffect = AppLanguage.DUTCH)

        viewModel.onLanguageSelected(AppLanguage.SYSTEM)

        assertFalse(
            "The phone's language is already what the app resolved against",
            viewModel.state.value.restartRequired,
        )
    }

    private fun TestScope.collecting(inEffect: AppLanguage): LanguageSectionViewModel {
        val viewModel = LanguageSectionViewModel(repository, inEffect)
        backgroundScope.launch { viewModel.state.collect {} }
        return viewModel
    }

    private class FakeLanguageRepository : LanguageRepository {
        val stored = MutableStateFlow(AppLanguage.SYSTEM)
        override fun observeLanguage(): Flow<AppLanguage> = stored
        override suspend fun setLanguage(language: AppLanguage) {
            stored.value = language
        }
    }
}
