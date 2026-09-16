package nl.hexmaster.pillsner

import java.io.File
import nl.hexmaster.pillsner.domain.model.SupportedLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `resourceConfigurations` in `build.gradle.kts` decides which languages' resources are actually
 * packaged into the app; a language can resolve correctly and have a correct `strings.xml` while
 * still reading English if it is missing from that list — the app-language-german-string-resolution-fix
 * bug. `build.gradle.kts` cannot import [SupportedLanguages] to derive the list itself (it runs on
 * Gradle's build-script classpath, not the app's), so this test cross-checks the two by parsing the
 * build file's text instead, and fails the moment they drift apart again.
 */
class PackagedResourceConfigurationsTest {

    @Test
    fun everySupportedLanguageIsPackaged() {
        val expected = SupportedLanguages.all.mapNotNull { it.tag }.toSet()

        assertEquals(expected, packagedResourceConfigurations())
    }

    private fun packagedResourceConfigurations(): Set<String> {
        val file = File("build.gradle.kts")
        assertTrue("Expected a build script at ${file.absolutePath}", file.isFile)
        val xml = file.readText()

        val listed = RESOURCE_CONFIGURATIONS.find(xml)
            ?: error("No `resourceConfigurations` assignment found in ${file.absolutePath}")

        return TAG.findAll(listed.groupValues[1]).map { it.groupValues[1] }.toSet()
    }

    private companion object {
        val RESOURCE_CONFIGURATIONS = Regex("""resourceConfigurations\s*\+=\s*listOf\(([^)]*)\)""")
        val TAG = Regex(""""([^"]+)"""")
    }
}
