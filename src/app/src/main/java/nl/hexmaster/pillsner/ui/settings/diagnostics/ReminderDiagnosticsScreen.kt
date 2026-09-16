package nl.hexmaster.pillsner.ui.settings.diagnostics

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.data.reminders.DeliveryEvent
import nl.hexmaster.pillsner.data.reminders.ReminderDeliveryLog
import nl.hexmaster.pillsner.ui.locale.AppLocale
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the delivery log screen, for semantics tests. */
object ReminderDiagnosticsScreenTestTags {
    const val TITLE = "reminder_diagnostics_title"
    const val BACK = "reminder_diagnostics_back"
    const val COPY = "reminder_diagnostics_copy"
    const val ENTRY = "reminder_diagnostics_entry"
    const val EMPTY = "reminder_diagnostics_empty"
}

/**
 * The delivery log: what the reminders did, newest first, with a button that copies the whole
 * thing so it can be pasted into a message.
 *
 * Nothing on this screen names a medicine or an amount. The log itself never records them, so the
 * screen has nothing to hide and nothing to blur; the dose ids it shows mean nothing outside the
 * device.
 *
 * @param entries the log, newest first.
 * @param copyText produces the text the copy button puts on the clipboard.
 * @param onBack leaves the screen; both this and the system back return to Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDiagnosticsScreen(
    entries: List<ReminderDeliveryLog.Entry>,
    copyText: suspend () -> String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.reminder_diagnostics_copied)
    val clipLabel = stringResource(R.string.reminder_diagnostics_title)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reminder_diagnostics_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(ReminderDiagnosticsScreenTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(ReminderDiagnosticsScreenTestTags.BACK)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, copyText())))
                                snackbarHostState.showSnackbar(copiedMessage)
                            }
                        },
                        enabled = entries.isNotEmpty(),
                        modifier = Modifier.testTag(ReminderDiagnosticsScreenTestTags.COPY),
                    ) {
                        Text(stringResource(R.string.reminder_diagnostics_copy))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.reminder_diagnostics_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .widthIn(max = Spacing.contentMaxWidth)
                        .padding(horizontal = Spacing.screenEdge, vertical = Spacing.xl)
                        .testTag(ReminderDiagnosticsScreenTestTags.EMPTY),
                )
            } else {
                val formatter = remember {
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        .withLocale(AppLocale.current)
                        .withZone(ZoneId.systemDefault())
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = Spacing.contentMaxWidth),
                    contentPadding = PaddingValues(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(
                        entries,
                        // Entries prepend as new ones arrive, so an index key would shift every
                        // existing row's identity on each append. A timestamp alone is not
                        // guaranteed unique - two events can log within the same millisecond -
                        // so the event and detail join it to keep the key stable per entry.
                        key = { _, entry -> "${entry.at.toEpochMilli()}:${entry.event}:${entry.detail}" },
                    ) { _, entry ->
                        LogEntry(entry = entry, time = formatter.format(entry.at))
                    }
                }
            }
        }
    }
}

/**
 * One line of the log, as three lines a person can read: when, what, and the dose or reason it
 * concerns. Merged into one node so a screen reader announces the entry whole.
 */
@Composable
private fun LogEntry(entry: ReminderDeliveryLog.Entry, time: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag(ReminderDiagnosticsScreenTestTags.ENTRY),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(entry.event.label),
            style = MaterialTheme.typography.titleSmall,
        )
        entry.detail?.let {
            // A dose id or a wake reason: a token, not prose, so it is set in the monospace family
            // where its digits line up and it is plainly not something the app is saying.
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** What each event is called on screen. */
private val DeliveryEvent.label: Int
    get() = when (this) {
        DeliveryEvent.WAKE -> R.string.reminder_diagnostics_event_wake
        DeliveryEvent.WAKE_DEFERRED -> R.string.reminder_diagnostics_event_wake_deferred
        DeliveryEvent.SERVICE_REFUSED -> R.string.reminder_diagnostics_event_service_refused
        DeliveryEvent.POSTED -> R.string.reminder_diagnostics_event_posted
        DeliveryEvent.POST_REFUSED -> R.string.reminder_diagnostics_event_post_refused
        DeliveryEvent.LAPSED -> R.string.reminder_diagnostics_event_lapsed
        DeliveryEvent.LAPSED_UNANNOUNCED -> R.string.reminder_diagnostics_event_lapsed_unannounced
        DeliveryEvent.TIMED_OUT -> R.string.reminder_diagnostics_event_timed_out
        DeliveryEvent.FAILED -> R.string.reminder_diagnostics_event_failed
        DeliveryEvent.RETRY_ARMED -> R.string.reminder_diagnostics_event_retry_armed
        DeliveryEvent.GAVE_UP -> R.string.reminder_diagnostics_event_gave_up
        DeliveryEvent.ALARMS_ARMED -> R.string.reminder_diagnostics_event_alarms_armed
        DeliveryEvent.ALARMS_LEFT_AS_IS -> R.string.reminder_diagnostics_event_alarms_left_as_is
    }

private val previewEntries = listOf(
    ReminderDeliveryLog.Entry(Instant.parse("2026-09-15T06:00:04Z"), DeliveryEvent.ALARMS_ARMED, "3 next=2026-09-15T18:00:00Z"),
    ReminderDeliveryLog.Entry(Instant.parse("2026-09-15T06:00:03Z"), DeliveryEvent.POSTED, "41"),
    ReminderDeliveryLog.Entry(Instant.parse("2026-09-15T06:00:01Z"), DeliveryEvent.WAKE, "ALARM"),
    ReminderDeliveryLog.Entry(Instant.parse("2026-09-14T18:00:02Z"), DeliveryEvent.LAPSED_UNANNOUNCED, "40"),
)

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ReminderDiagnosticsScreenPreview() {
    PillsnerTheme {
        ReminderDiagnosticsScreen(entries = previewEntries, copyText = { "" }, onBack = {})
    }
}

@PreviewLightDark
@Composable
private fun ReminderDiagnosticsScreenEmptyPreview() {
    PillsnerTheme {
        ReminderDiagnosticsScreen(entries = emptyList(), copyText = { "" }, onBack = {})
    }
}
