package nl.hexmaster.pillsner.domain.labelscan

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The interpretation is pure Kotlin (medicine-label-photo-prefill design D4): nothing in the
 * `domain.labelscan` package may import Android, AndroidX, CameraX or the OCR library. That is
 * what keeps every rule unit-testable on the JVM and keeps the recogniser swappable.
 */
class LabelScanPurityTest {

    @Test
    fun theLabelScanPackage_importsNothingFromAndroidOrTheRecogniser() {
        val sources = sources()
        assertTrue("Expected sources under ${PACKAGE_DIR.absolutePath}", sources.isNotEmpty())

        val offenders = sources.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> FORBIDDEN_IMPORT.containsMatchIn(line) }
                .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }

        assertEquals("domain.labelscan must stay free of Android and recogniser types", emptyList<String>(), offenders)
    }

    private fun sources(): List<File> = PACKAGE_DIR.listFiles { file -> file.extension == "kt" }?.toList().orEmpty()

    private companion object {
        /** Unit tests run with the module directory as the working directory. */
        val PACKAGE_DIR = File("src/main/java/nl/hexmaster/pillsner/domain/labelscan")

        val FORBIDDEN_IMPORT = Regex("""^\s*import\s+(android\.|androidx\.|com\.googlecode\.tesseract|com\.google\.)""")
    }
}
