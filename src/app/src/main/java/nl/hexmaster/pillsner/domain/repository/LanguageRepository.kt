package nl.hexmaster.pillsner.domain.repository

import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.AppLanguage

/** The one place the user's language choice is kept. */
interface LanguageRepository {

    /** The stored choice, or [AppLanguage.SYSTEM] while the user has not made one. */
    fun observeLanguage(): Flow<AppLanguage>

    /** Records the user's choice. It takes effect when the app is next started. */
    suspend fun setLanguage(language: AppLanguage)
}
