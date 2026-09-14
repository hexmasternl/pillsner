package nl.hexmaster.pillsner.applock.domain

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PinTest {

    @Test
    fun `accepts 4 to 6 digits`() {
        assertNotNull(Pin.of("1234"))
        assertNotNull(Pin.of("12345"))
        assertNotNull(Pin.of("123456"))
    }

    @Test
    fun `rejects fewer than 4 digits`() {
        assertNull(Pin.of("123"))
        assertNull(Pin.of(""))
    }

    @Test
    fun `rejects more than 6 digits`() {
        assertNull(Pin.of("1234567"))
    }

    @Test
    fun `rejects non-digit characters`() {
        assertNull(Pin.of("12a4"))
        assertNull(Pin.of("12 4"))
        assertNull(Pin.of("-1234"))
    }
}
