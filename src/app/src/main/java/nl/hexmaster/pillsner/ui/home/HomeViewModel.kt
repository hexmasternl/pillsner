package nl.hexmaster.pillsner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.data.reminders.ReminderPreferences
import nl.hexmaster.pillsner.domain.repository.UpcomingDosesRepository

/**
 * Exposes the upcoming doses for the welcome screen as a [StateFlow], together with whether
 * reminders can actually be delivered.
 *
 * The repository promises ordering and the limit; the view model enforces both again so a
 * misbehaving implementation can never overflow or reorder the screen (design D4, D7).
 *
 * @param reminderReadiness whether the platform currently allows exact alarms.
 */
class HomeViewModel(
    repository: UpcomingDosesRepository,
    reminderReadiness: StateFlow<Boolean> = MutableStateFlow(true),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val preferences: ReminderPreferences? = null,
) : ViewModel() {

    /** Set by the screen once it knows whether the user has allowed notifications. */
    private val notificationsAllowed = MutableStateFlow(true)

    /**
     * Whether the permission dialog should still be put in front of the user. Once it has been,
     * the banner takes over rather than asking again on every launch.
     */
    val shouldRequestNotificationPermission: StateFlow<Boolean> =
        (preferences?.hasRequestedNotificationPermission?.map { !it } ?: flowOf(false))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeUpcoming(limit = MAX_UPCOMING_DOSES),
        reminderReadiness,
        notificationsAllowed,
    ) { doses, alarmsAreExact, notifications ->
        HomeUiState(
            upcomingDoses = doses.sortedBy { it.scheduledAt }.take(MAX_UPCOMING_DOSES),
            isLoading = false,
            notificationsAllowed = notifications,
            alarmsAreExact = alarmsAreExact,
            now = clock.instant(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    /** Re-checked whenever the screen resumes, so the banner goes as soon as the user fixes it. */
    fun onNotificationPermissionChecked(granted: Boolean) {
        notificationsAllowed.value = granted
    }

    /** Records that the user has now been shown the permission dialog once. */
    fun onNotificationPermissionRequested() {
        viewModelScope.launch { preferences?.markNotificationPermissionRequested() }
    }

    companion object {
        /** The welcome screen never shows more than this many doses. */
        const val MAX_UPCOMING_DOSES = 5
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
