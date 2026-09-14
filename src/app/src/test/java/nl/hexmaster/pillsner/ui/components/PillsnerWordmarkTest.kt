package nl.hexmaster.pillsner.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PillsnerWordmarkTest {

    @Test
    fun appName_splitsIntoPillsAndNer() {
        assertEquals("Pills" to "ner", splitWordmark("Pillsner"))
    }

    @Test
    fun titleWithoutAccentFragment_isAllLead() {
        assertEquals("Pillspal" to "", splitWordmark("Pillspal"))
    }

    @Test
    fun titleThatIsOnlyTheAccentFragment_keepsANonEmptyLead() {
        assertEquals("ner" to "", splitWordmark("ner"))
    }

    @Test
    fun emptyTitle_doesNotSplit() {
        assertEquals("" to "", splitWordmark(""))
    }

    @Test
    fun repeatedFragment_splitsAtTheLastOccurrence() {
        assertEquals("nerPills" to "ner", splitWordmark("nerPillsner"))
    }
}
