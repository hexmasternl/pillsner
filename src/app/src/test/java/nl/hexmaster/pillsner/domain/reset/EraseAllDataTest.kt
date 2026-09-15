package nl.hexmaster.pillsner.domain.reset

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: app-reset, what a reset does and in what order (design D3). */
class EraseAllDataTest {

    private val steps = mutableListOf<String>()

    private val eraseAllData = EraseAllData(
        eraser = { steps += "erase" },
        teardown = { steps += "teardown" },
        refresh = { steps += "refresh" },
    )

    @Test
    fun `a reset erases, then takes the notifications down, then re-arms`() = runBlocking {
        eraseAllData()

        // The order is the design. The irreversible step goes first, so a failure in either
        // correction leaves the data properly erased rather than half-gone with the app already
        // behaving as though it were.
        assertEquals(listOf("erase", "teardown", "refresh"), steps)
    }

    @Test
    fun `each step runs exactly once`() = runBlocking {
        eraseAllData()

        assertEquals(1, steps.count { it == "erase" })
        assertEquals(1, steps.count { it == "teardown" })
        assertEquals(1, steps.count { it == "refresh" })
    }

    @Test
    fun `a failure after the erase leaves the data erased`() {
        val failing = EraseAllData(
            eraser = { steps += "erase" },
            teardown = { error("the notification manager is gone") },
            refresh = { steps += "refresh" },
        )

        runCatching { runBlocking { failing() } }

        // A surviving notification answers into a dose id that no longer exists, which is already
        // a no-op; the erase itself must not be undone or retried because of it.
        assertEquals(listOf("erase"), steps)
    }
}
