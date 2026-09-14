package nl.hexmaster.pillsner.domain

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * A clock a test can move. Every use case takes a [Clock], so a test can put the device at any
 * moment, in any zone, and step it forward or backward without waiting.
 */
class MutableTestClock(
    private var now: Instant,
    private var zone: ZoneId = ZoneId.of("Europe/Amsterdam"),
) : Clock() {

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableTestClock(now, zone)

    override fun instant(): Instant = now

    /** Moves the device clock forward. */
    fun advance(by: Duration) {
        now = now.plus(by)
    }

    /** Puts the device clock at an exact moment, forwards or backwards. */
    fun setTo(instant: Instant) {
        now = instant
    }

    /** Moves the device to another time zone, as a traveller would. */
    fun moveTo(zone: ZoneId) {
        this.zone = zone
    }
}
