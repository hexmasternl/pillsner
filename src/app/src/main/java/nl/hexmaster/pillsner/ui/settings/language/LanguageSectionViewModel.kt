package nl.hexmaster.pillsner.ui.settings.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.model.AppLanguage
import nl.hexmaster.pillsner.domain.model.SupportedLanguages
import nl.hexmaster.pillsner.domain.repository.LanguageRepository

/**
 * What the Language section shows.
 *
 * @property selected the user's stored choice.
 * @property options what the dropdown offers: follow the phone, then every language the app ships.
 * @property restartRequired the choice differs from the language the app is actually running in,
 *   so the user has to restart before they see it.
 */
data class LanguageSectionState(
    val selected: AppLanguage = AppLanguage.SYSTEM,
    val options: List<AppLanguage> = listOf(AppLanguage.SYSTEM) + SupportedLanguages.all,
    val restartRequired: Boolean = false,
)

/**
 * The language choice (design D6).
 *
 * Choosing a language writes it and nothing else: it takes effect the next time the app starts,
 * which is what the notice under the dropdown says. Once the app is running in the chosen
 * language the two agree and the notice goes.
 *
 * @param languageInEffect the language this process actually resolved at start.
 */
class LanguageSectionViewModel(
    private val repository: LanguageRepository,
    languageInEffect: AppLanguage,
) : ViewModel() {

    val state: StateFlow<LanguageSectionState> = repository.observeLanguage()
        .map { stored ->
            LanguageSectionState(
                selected = stored,
                restartRequired = stored != AppLanguage.SYSTEM && stored != languageInEffect,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = LanguageSectionState(),
        )

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
