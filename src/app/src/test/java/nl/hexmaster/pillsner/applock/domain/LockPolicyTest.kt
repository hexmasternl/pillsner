package nl.hexmaster.pillsner.applock.domain

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class LockPolicyTest {

    @Test
    fun `no cooldown before the first block completes`() {
        assertEquals(0.seconds, LockPolicy.cooldownFor(1))
        assertEquals(0.seconds, LockPolicy.cooldownFor(4))
    }

    @Test
    fun `cooldown doubles every block up to the cap`() {
        assertEquals(30.seconds, LockPolicy.cooldownFor(5))
        assertEquals(60.seconds, LockPolicy.cooldownFor(10))
        assertEquals(120.seconds, LockPolicy.cooldownFor(15))
        assertEquals(240.seconds, LockPolicy.cooldownFor(20))
        assertEquals(300.seconds, LockPolicy.cooldownFor(25))
    }

    @Test
    fun `cooldown never exceeds the five minute cap`() {
        assertEquals(300.seconds, LockPolicy.cooldownFor(100))
    }
}
