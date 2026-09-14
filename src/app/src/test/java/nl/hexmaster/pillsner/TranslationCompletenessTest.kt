package nl.hexmaster.pillsner

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every language the app ships says the same things.
 *
 * Lint already fails the build on a missing or stray translation, but lint is a separate task that
 * someone has to remember to run; this fails in the ordinary test run, and it also checks the one
 * thing lint does not: that a translated string takes the same format arguments as the original,
 * because `%1$s` and `%2$s` swapped silently is a wrong sentence rather than a crash.
 */
class TranslationCompletenessTest {

    @Test
    fun everyTranslatableStringIsTranslatedIntoDutch() {
        val missing = english().keys - dutch().keys

        assertEquals("Strings with no Dutch translation", emptySet<String>(), missing)
    }

    @Test
    fun theDutchTranslationHasNoStringsTheOriginalDoesNot() {
        val extra = dutch().keys - english().keys

        assertEquals("Dutch strings with no English original", emptySet<String>(), extra)
    }

    @Test
    fun everyTranslationTakesTheSameFormatArguments() {
        val english = english()
        val offenders = dutch().mapNotNull { (name, translation) ->
            val original = english[name] ?: return@mapNotNull null
            val expected = translation.formatArguments()
            val actual = original.formatArguments()
            if (expected == actual) null else "$name: expected $actual, translation uses $expected"
        }

        assertEquals("Translations whose format arguments differ", emptyList<String>(), offenders)
    }

    /** Every translatable `<string>` and `<plurals>` in a resource file, by name. */
    private fun strings(file: File): Map<String, String> {
        assertTrue("Expected resources at ${file.absolutePath}", file.isFile)
        val xml = file.readText()

        val simple = STRING.findAll(xml)
            .filterNot { it.value.contains("translatable=\"false\"") }
            .associate { it.groupValues[1] to it.groupValues[2] }

        val plurals = PLURALS.findAll(xml)
            .associate { it.groupValues[1] to it.groupValues[2] }

        return simple + plurals
    }

    private fun english() = strings(File("src/main/res/values/strings.xml"))

    private fun dutch() = strings(File("src/main/res/values-nl/strings.xml"))

    /** The positional and plain format arguments a string uses, as a set so order does not matter. */
    private fun String.formatArguments(): Set<String> =
        FORMAT_ARGUMENT.findAll(this).map { it.value }.toSet()

    private companion object {
        val STRING = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        val PLURALS = Regex("""<plurals name="([^"]+)"[^>]*>(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
        val FORMAT_ARGUMENT = Regex("""%(\d+\$)?[sd]""")
    }
}
