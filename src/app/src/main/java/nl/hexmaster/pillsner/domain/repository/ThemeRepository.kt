package nl.hexmaster.pillsner.domain.repository

import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.AppTheme

/** The one place the user's theme choice is kept. */
interface ThemeRepository {

    /** The stored choice, or [AppTheme.SYSTEM] while the user has not made one. */
    fun observeTheme(): Flow<AppTheme>

    /** Records the user's choice. It takes effect at once, without a restart. */
    suspend fun setTheme(theme: AppTheme)
}
