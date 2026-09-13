package nl.hexmaster.pillsner.ui.home

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpcomingDoseTimeFormatterTest {

    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val locale: Locale = Locale.UK

    // Friday 11 September 2026, 10:00 local time.
    private val now: Instant = LocalDateTime.of(2026, 9, 11, 10, 0).atZone(zone).toInstant()
    private val formatter = UpcomingDoseTimeFormatter(zone, locale, Clock.fixed(now, zone))

    @Test
    fun `dose later today shows the time only`() {
        val result = formatter.format(localTime(2026, 9, 11, 20, 0))

        assertEquals("20:00", result.time)
        assertNull(result.day)
    }

    @Test
    fun `dose tomorrow shows the time and the tomorrow label`() {
        val result = formatter.format(localTime(2026, 9, 12, 8, 0))

        assertEquals("08:00", result.time)
        assertEquals(DayLabel.Tomorrow, result.day)
    }

    @Test
    fun `dose one minute before midnight is still today`() {
        val result = formatter.format(localTime(2026, 9, 11, 23, 59))

        assertEquals("23:59", result.time)
        assertNull(result.day)
    }

    @Test
    fun `dose at midnight is tomorrow`() {
        val result = formatter.format(localTime(2026, 9, 12, 0, 0))

        assertEquals("00:00", result.time)
        assertEquals(DayLabel.Tomorrow, result.day)
    }

    @Test
    fun `dose within the week shows the weekday name`() {
        val result = formatter.format(localTime(2026, 9, 14, 9, 30))

        assertEquals("09:30", result.time)
        assertEquals(DayLabel.Text(DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, locale)), result.day)
    }

    @Test
    fun `dose a week or more away shows a date`() {
        val result = formatter.format(localTime(2026, 9, 18, 9, 30))

        assertEquals(DayLabel.Text("18 Sept 2026"), result.day)
    }

    @Test
    fun `day boundary follows the device time zone, not UTC`() {
        // 23:30 in Amsterdam on the 11th is 21:30 UTC; still today locally.
        val stillToday = formatter.format(localTime(2026, 9, 11, 23, 30))
        assertNull(stillToday.day)

        // 00:30 in Amsterdam on the 12th is 22:30 UTC on the 11th; tomorrow locally.
        val tomorrow = formatter.format(localTime(2026, 9, 12, 0, 30))
        assertEquals(DayLabel.Tomorrow, tomorrow.day)
    }

    @Test
    fun `twelve hour locales get a twelve hour time`() {
        val usFormatter = UpcomingDoseTimeFormatter(zone, Locale.US, Clock.fixed(now, zone))

        val result = usFormatter.format(localTime(2026, 9, 11, 20, 0))

        assertEquals("8:00", result.time.filter { it.isDigit() || it == ':' })
        assertEquals("PM", result.time.filter { it.isLetter() })
    }

    private fun localTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant()
}
