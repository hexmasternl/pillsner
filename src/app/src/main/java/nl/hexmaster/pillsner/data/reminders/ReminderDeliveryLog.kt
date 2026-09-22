package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import java.io.File
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What the reminders did, step by step, kept on the device so a reminder that does not arrive
 * leaves evidence behind.
 *
 * Every wake, posting, lapse, failure and alarm set is one line here, with the moment it happened
 * and at most a dose id or a wake reason beside it. Never a medicine's name or an amount: the log
 * is shown on a Settings screen and can be copied out of the app, so it is held to the same rule
 * as logcat. It replaces guessing when the question is "why did I not get my eight o'clock
 * reminder", which no amount of reading the code can answer for a phone the developer does not
 * hold.
 *
 * A plain text file in the app's private storage, bounded at [MAX_ENTRIES] lines and rewritten
 * when it grows past that. Writes are serialised on one thread so the order in the file is the
 * order things happened. Storage is unreadable before the first unlock after a reboot, so a record
 * made in that window is simply dropped rather than failing the wake that made it.
 */
open class ReminderDeliveryLog(
    private val file: File,
    private val clock: Clock = Clock.systemUTC(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    constructor(context: Context, clock: Clock = Clock.systemUTC()) :
        this(File(context.applicationContext.filesDir, FILE_NAME), clock)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val writer = dispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + writer)

    // Bumped after every write, so a screen showing the log re-reads it.
    private val version = MutableStateFlow(0)

    /**
     * How many lines [file] holds, or null until first needed. Read from the file exactly once,
     * on the [writer] dispatcher, the first time [append] runs; every append then increments it in
     * memory instead of re-reading the whole file just to decide whether it has grown past the trim
     * threshold (reminder-wake-cycle-db-efficiency design D4). Only ever touched on [writer], which
     * is single-threaded, so it needs no further synchronisation.
     */
    private var lineCount: Int? = null

    /** One line of the log. */
    data class Entry(val at: Instant, val event: DeliveryEvent, val detail: String?)

    /** Records [event] now. Fire-and-forget, so a receiver or a `finally` block can call it. */
    open fun record(event: DeliveryEvent, detail: String? = null) {
        val at = clock.instant()
        scope.launch { append(Entry(at, event, detail)) }
    }

    /** The log, oldest first, re-emitted after every write. */
    fun observe(): Flow<List<Entry>> = version.map { read() }

    /** Everything in the log as text, for copying out of the app. */
    suspend fun asText(): String = read().joinToString("\n") { it.toLine() }

    private suspend fun append(entry: Entry) = withContext(writer) {
        runCatching {
            // The file may not exist yet - the very first entry ever written creates it, just as
            // appendText below always has - so an absent file reads as zero lines rather than
            // throwing before that first entry gets the chance to create it.
            if (lineCount == null) lineCount = if (file.exists()) file.readLines().size else 0
            file.appendText(entry.toLine() + "\n")
            lineCount = lineCount!! + 1
            trimIfNeeded()
        }
        version.value++
    }

    private suspend fun read(): List<Entry> = withContext(writer) {
        runCatching { file.readLines().mapNotNull(::parse) }.getOrDefault(emptyList())
    }

    private fun trimIfNeeded() {
        if ((lineCount ?: 0) <= MAX_ENTRIES + TRIM_SLACK) return
        val lines = file.readLines()
        file.writeText(lines.takeLast(MAX_ENTRIES).joinToString("\n", postfix = "\n"))
        lineCount = MAX_ENTRIES
    }

    private fun Entry.toLine(): String =
        listOfNotNull(at.toString(), event.name, detail).joinToString("\t")

    private fun parse(line: String): Entry? {
        val parts = line.split('\t', limit = 3)
        if (parts.size < 2) return null
        val at = runCatching { Instant.parse(parts[0]) }.getOrNull() ?: return null
        val event = DeliveryEvent.entries.firstOrNull { it.name == parts[1] } ?: return null
        return Entry(at, event, parts.getOrNull(2))
    }

    companion object {
        private const val FILE_NAME = "reminder-delivery.log"

        /** A few days of a busy schedule; enough to see a pattern, small enough to read on a phone. */
        const val MAX_ENTRIES = 400

        /** Rewriting the file on every append would be wasteful; let it run a little over first. */
        private const val TRIM_SLACK = 50
    }
}

/**
 * The vocabulary of the delivery log. Fixed names rather than free text, so the log stays readable
 * across app versions and never grows a line that names a medicine by accident.
 */
enum class DeliveryEvent {
    /** The app woke; the detail is the reason. */
    WAKE,

    /** The app woke before the first unlock and could only re-arm; the detail is the reason. */
    WAKE_DEFERRED,

    /** The platform refused to start the wake service and the receiver ran the wake itself. */
    SERVICE_REFUSED,

    /** A reminder was posted; the detail is the dose id. */
    POSTED,

    /** A reminder was not posted because notifications are not allowed; the detail is the dose id. */
    POST_REFUSED,

    /** A dose lapsed and was recorded missed; the detail is the dose id. */
    LAPSED,

    /** A dose lapsed with no reminder ever posted for it; the detail is the dose id. */
    LAPSED_UNANNOUNCED,

    /** The wake ran out of its budget; the detail is the reason. */
    TIMED_OUT,

    /** The wake threw; the detail is the reason and the exception type. */
    FAILED,

    /** A retry alarm was armed after a wake that did not complete. */
    RETRY_ARMED,

    /** Retrying stopped helping and the ordinary alarm set was armed instead. */
    GAVE_UP,

    /** The alarm set was reconciled; the detail is how many alarms and when the first one is. */
    ALARMS_ARMED,

    /** Reconciling failed and the alarms already armed were left as they were. */
    ALARMS_LEFT_AS_IS,

    /** The dose-history purge removed at least one row; the detail is how many. */
    HISTORY_PURGED,

    /** The dose-history purge did not complete; the detail is the exception type. */
    HISTORY_PURGE_FAILED,
}
