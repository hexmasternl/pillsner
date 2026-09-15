package nl.hexmaster.pillsner.ui.settings.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.model.AppTheme
import nl.hexmaster.pillsner.domain.repository.ThemeRepository

/**
 * What the Theme section shows.
 *
 * @property selected the user's stored choice.
 * @property options what the dropdown offers: follow the phone, then each fixed scheme.
 */
data class ThemeSectionState(
    val selected: AppTheme = AppTheme.SYSTEM,
    val options: List<AppTheme> = AppTheme.entries,
)

/**
 * The theme choice (design D5).
 *
 * Deliberately smaller than [nl.hexmaster.pillsner.ui.settings.language.LanguageSectionViewModel]:
 * there is no "in effect" to compare against and no restart state to expose, because a colour
 * scheme is an argument to a composable and a new one recomposes the screen the user is looking at
 * (design D3).
 */
class ThemeSectionViewModel(
    private val repository: ThemeRepository,
) : ViewModel() {

    val state: StateFlow<ThemeSectionState> = repository.observeTheme()
        .map { ThemeSectionState(selected = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = ThemeSectionState(),
        )

    fun onThemeSelected(theme: AppTheme) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
