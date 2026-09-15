package nl.hexmaster.pillsner.ui.settings.theme

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
import nl.hexmaster.pillsner.domain.model.AppTheme
import nl.hexmaster.pillsner.domain.repository.ThemeRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Spec: app-theme section and "Choosing a theme applies immediately". */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSectionViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeThemeRepository()

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
        val viewModel = collecting()

        assertEquals(AppTheme.SYSTEM, viewModel.state.value.selected)
    }

    @Test
    fun `the dropdown offers the phone first, then light and dark`() = runTest(dispatcher) {
        val viewModel = collecting()

        assertEquals(
            listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK),
            viewModel.state.value.options,
        )
    }

    @Test
    fun `choosing dark stores it straight away`() = runTest(dispatcher) {
        val viewModel = collecting()

        viewModel.onThemeSelected(AppTheme.DARK)

        assertEquals(AppTheme.DARK, repository.stored.value)
        assertEquals(AppTheme.DARK, viewModel.state.value.selected)
    }

    @Test
    fun `choosing light after dark replaces the choice`() = runTest(dispatcher) {
        val viewModel = collecting()
        viewModel.onThemeSelected(AppTheme.DARK)

        viewModel.onThemeSelected(AppTheme.LIGHT)

        assertEquals(AppTheme.LIGHT, repository.stored.value)
        assertEquals(AppTheme.LIGHT, viewModel.state.value.selected)
    }

    @Test
    fun `a stored choice is what the section opens on`() = runTest(dispatcher) {
        repository.stored.value = AppTheme.LIGHT

        val viewModel = collecting()

        assertEquals(AppTheme.LIGHT, viewModel.state.value.selected)
    }

    @Test
    fun `choosing to follow the phone again is stored like any other choice`() = runTest(dispatcher) {
        val viewModel = collecting()
        viewModel.onThemeSelected(AppTheme.DARK)

        viewModel.onThemeSelected(AppTheme.SYSTEM)

        assertEquals(AppTheme.SYSTEM, repository.stored.value)
        assertEquals(AppTheme.SYSTEM, viewModel.state.value.selected)
    }

    private fun TestScope.collecting(): ThemeSectionViewModel {
        val viewModel = ThemeSectionViewModel(repository)
        backgroundScope.launch { viewModel.state.collect {} }
        return viewModel
    }

    private class FakeThemeRepository : ThemeRepository {
        val stored = MutableStateFlow(AppTheme.SYSTEM)
        override fun observeTheme(): Flow<AppTheme> = stored
        override suspend fun setTheme(theme: AppTheme) {
            stored.value = theme
        }
    }
}
