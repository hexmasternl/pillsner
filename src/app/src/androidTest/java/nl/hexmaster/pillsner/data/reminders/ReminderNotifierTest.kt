package nl.hexmaster.pillsner.data.reminders

import android.Manifest
import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.ui.home.UpcomingDoseTimeFormatter
import nl.hexmaster.pillsner.ui.medicines.QuantityFormatter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Spec: medicine-reminders notification content, actions and lock-screen behaviour. */
@RunWith(AndroidJUnit4::class)
class ReminderNotifierTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notificationManager = NotificationManagerCompat.from(context)

    /** 08:00 in UTC, with the device clock on the same day so no day label appears. */
    private val scheduledAt: Instant = Instant.parse("2026-09-14T08:00:00Z")

    private val notifier = ReminderNotifier(
        context = context,
        quantityFormatter = QuantityFormatter(context, Locale.UK),
        timeFormatter = UpcomingDoseTimeFormatter(
            zoneId = ZoneOffset.UTC,
            locale = Locale.UK,
            clock = Clock.fixed(scheduledAt, ZoneOffset.UTC),
        ),
    )

    private val dose = Dose(
        id = DoseId(1),
        medicationId = MedicationId(1),
        medicationName = "Ibuprofen",
        amount = Quantity.of("40", DoseUnit.MILLIGRAM),
        scheduledAt = scheduledAt,
    )

    @Before
    fun setUp() {
        ReminderChannels.create(context)
        notifier.cancel(dose)
    }

    @After
    fun tearDown() {
        notifier.cancel(dose)
    }

    @Test
    fun theReminder_namesTheMedicineTheAmountAndTheTime() {
        notifier.show(dose, dueCount = 1)

        val posted = posted().extras
        assertEquals("Ibuprofen", posted.getString(Notification.EXTRA_TITLE))
        assertEquals(
            "Take 40 mg of your medicine 'Ibuprofen', on 08:00",
            posted.getString(Notification.EXTRA_TEXT),
        )
    }

    @Test
    fun theReminder_offersTheThreeAnswersInTheFixedOrder() {
        notifier.show(dose, dueCount = 1)

        val actions = posted().actions
        assertEquals(3, actions.size)
        assertEquals("I took it", actions[0].title)
        assertEquals("Not yet", actions[1].title)
        assertEquals("Not going to", actions[2].title)
    }

    @Test
    fun theReminder_staysUntilItIsAnsweredAndSwipingItAwayCountsAsNotYet() {
        notifier.show(dose, dueCount = 1)

        val notification = posted()
        assertEquals(0, notification.flags and Notification.FLAG_AUTO_CANCEL)
        assertNotNull(notification.deleteIntent)
    }

    @Test
    fun theLockScreenVersion_saysSomethingIsDueWithoutNamingIt() {
        notifier.show(dose, dueCount = 1)

        val notification = posted()
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        val public = checkNotNull(notification.publicVersion)
        assertEquals("Time for your medicine", public.extras.getString(Notification.EXTRA_TITLE))
        assertFalse(public.extras.getString(Notification.EXTRA_TITLE).orEmpty().contains("Ibuprofen"))
        assertEquals(3, public.actions.size)
    }

    @Test
    fun theReminder_isNotLocalOnlySoAWatchCanMirrorIt() {
        notifier.show(dose, dueCount = 1)

        assertEquals(0, posted().flags and Notification.FLAG_LOCAL_ONLY)
    }

    @Test
    fun theReminder_isAnAlarmAtHighPriority() {
        notifier.show(dose, dueCount = 1)

        val notification = posted()
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        assertEquals(ReminderChannels.REMINDERS, notification.channelId)
    }

    @Test
    fun cancellingTakesTheReminderDown() {
        notifier.show(dose, dueCount = 1)
        posted()

        notifier.cancel(dose)

        waitUntilGone()
        assertNull(ours())
    }

    /**
     * The system posts and takes down notifications on its own thread, so both are waited for
     * rather than read once.
     */
    private fun posted(): Notification =
        checkNotNull(waitFor { ours() }) { "The reminder was not posted" }.notification

    private fun waitUntilGone() {
        waitFor { if (ours() == null) ABSENT else null }
    }

    private fun ours(): StatusBarNotification? =
        notificationManager.activeNotifications.firstOrNull { it.id == dose.id.value.toInt() }

    private fun <T : Any> waitFor(condition: () -> T?): T? {
        repeat(MAX_ATTEMPTS) {
            condition()?.let { result -> return result }
            Thread.sleep(POLL_MILLIS)
        }
        return condition()
    }

    private companion object {
        const val POLL_MILLIS = 50L
        const val MAX_ATTEMPTS = 60

        /** A non-null marker, because [waitFor] signals "not yet" with null. */
        val ABSENT = Any()
    }
}
