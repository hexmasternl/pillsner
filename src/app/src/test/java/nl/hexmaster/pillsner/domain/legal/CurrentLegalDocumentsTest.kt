package nl.hexmaster.pillsner.domain.legal

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shape of the shipped documents (design D1, D2).
 *
 * Nothing here reads the wording — that is the translator's and the lawyer's business. What it does
 * guard are the two rules the code depends on: both documents exist, complete and versioned, and
 * the domain layer holds identifiers rather than prose, so the whole legal text stays inside the
 * translation check that lint and [nl.hexmaster.pillsner.TranslationCompletenessTest] run over the
 * resources.
 */
class CurrentLegalDocumentsTest {

    @Test
    fun `both documents ship, each identified once`() {
        assertEquals(
            listOf(LegalDocumentId.DISCLAIMER, LegalDocumentId.TERMS),
            CurrentLegalDocuments.all.map { it.id },
        )
    }

    @Test
    fun `each document is looked up by its own identifier`() {
        LegalDocumentId.entries.forEach { id ->
            assertEquals(id, CurrentLegalDocuments[id].id)
        }
    }

    @Test
    fun `each document is at version 1 with an effective date and a title`() {
        CurrentLegalDocuments.all.forEach { document ->
            assertEquals("${document.id} version", 1, document.version)
            assertTrue("${document.id} has an effective date", document.effectiveDate.year >= 2026)
            assertTrue("${document.id} has a title", document.title.resourceId != 0)
        }
    }

    @Test
    fun `each document has sections, and every section has a heading and at least one paragraph`() {
        CurrentLegalDocuments.all.forEach { document ->
            assertTrue("${document.id} has sections", document.sections.isNotEmpty())
            document.sections.forEachIndexed { index, section ->
                assertTrue("${document.id} section $index heading", section.heading.resourceId != 0)
                assertTrue("${document.id} section $index paragraphs", section.paragraphs.isNotEmpty())
                section.paragraphs.forEachIndexed { paragraph, text ->
                    assertTrue("${document.id} section $index paragraph $paragraph", text.resourceId != 0)
                }
            }
        }
    }

    @Test
    fun `no two pieces of text point at the same resource`() {
        val references = CurrentLegalDocuments.all.flatMap { document ->
            document.sections.flatMap { listOf(it.heading) + it.paragraphs }
        }

        assertEquals("Every heading and paragraph is its own string", references.size, references.toSet().size)
    }

    @Test
    fun `the legal domain package holds no prose`() {
        val offenders = sources().flatMap { file ->
            file.readLines().withIndex()
                .filterNot { (_, line) -> line.isComment() }
                .filter { (_, line) -> QUOTE.containsMatchIn(line) || STRING_TYPE.containsMatchIn(line) }
                .map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }

        assertEquals(
            "The legal domain package holds string resource identifiers, never text",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun `the legal domain package has no Android dependency`() {
        val offenders = sources().flatMap { file ->
            file.readLines()
                .filter { it.startsWith("import android") }
                .map { "${file.name}: $it" }
        }

        assertEquals("The domain layer stays unit-testable", emptyList<String>(), offenders)
    }

    private fun sources(): List<File> {
        val packageDir = File("src/main/java/nl/hexmaster/pillsner/domain/legal")
        val sources = packageDir.listFiles { file -> file.extension == "kt" }?.toList().orEmpty()

        assertTrue("No sources found in $packageDir", sources.isNotEmpty())
        return sources
    }

    /** A line of KDoc, a block comment or a line comment; wording there is not shipped text. */
    private fun String.isComment(): Boolean {
        val trimmed = trim()
        return trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
    }

    private companion object {
        val QUOTE = Regex("\"")
        val STRING_TYPE = Regex(":\s*String\b")
    }
}
