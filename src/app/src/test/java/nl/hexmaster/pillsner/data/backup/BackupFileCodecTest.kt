package nl.hexmaster.pillsner.data.backup

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.time.Instant
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import nl.hexmaster.pillsner.domain.backup.BackupFixtures
import nl.hexmaster.pillsner.domain.backup.BackupSnapshot
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: data-backup-restore, the file format, its encryption and every way a read can fail. */
class BackupFileCodecTest {

    private val passphrase = "correct horse battery staple"

    /** The lowest count an import accepts, so tests stay fast; export's own count is tested apart. */
    private var derivations = 0
    private val countingDerivation = KeyDerivation { pass, salt, iterations ->
        derivations++
        Pbkdf2KeyDerivation.derive(pass, salt, iterations)
    }
    private val codec = BackupFileCodec(keyDerivation = countingDerivation, iterations = 100_000)

    private fun encode(snapshot: BackupSnapshot = BackupFixtures.populated()) = codec.encode(snapshot, passphrase.toCharArray())

    @Test
    fun `a file decodes to exactly the snapshot it was made from`() {
        val snapshot = BackupFixtures.populated()

        assertEquals(snapshot, codec.decode(encode(snapshot), passphrase.toCharArray()))
    }

    @Test
    fun `an empty snapshot round-trips too`() {
        assertEquals(BackupSnapshot.EMPTY, codec.decode(encode(BackupSnapshot.EMPTY), passphrase.toCharArray()))
    }

    @Test
    fun `the payload is compressed before it is encrypted`() {
        // A year of three doses a day, which is what a backup is mostly made of.
        val base = BackupFixtures.populated()
        val start = Instant.parse("2025-01-01T08:00:00Z")
        val year = base.copy(doses = (0 until 1_095).map { BackupFixtures.dose("a", start.plusSeconds(it * 28_800L), null) })
        val file = encode(year)

        val plaintext = decryptToCompressed(file)
        val json = GZIPInputStream(plaintext.inputStream()).readBytes()

        assertTrue("GZIP-framed", plaintext[0] == 0x1f.toByte() && plaintext[1] == 0x8b.toByte())
        assertTrue("At least five times smaller (${file.size} vs ${json.size})", file.size * 5 < json.size)
    }

    @Test
    fun `the header can be read without the passphrase, but the payload cannot`() {
        val file = encode()
        derivations = 0

        val header = codec.inspect(file)

        assertEquals(BackupHeader.CURRENT_FORMAT_VERSION, header.formatVersion)
        assertEquals(100_000, header.iterations)
        assertEquals(BackupHeader.SALT_BYTES, header.salt.size)
        assertEquals(BackupHeader.NONCE_BYTES, header.nonce.size)
        assertEquals("Inspecting derives no key", 0, derivations)
        assertFalse("No medication name in the clear", String(file, Charsets.ISO_8859_1).contains("Ibuprofen"))
    }

    @Test
    fun `an export is written with 600,000 iterations`() {
        val file = BackupFileCodec().encode(BackupSnapshot.EMPTY, passphrase.toCharArray())

        assertEquals(600_000, BackupFileCodec().inspect(file).iterations)
    }

    @Test
    fun `the same passphrase twice gives different salts and nonces`() {
        val first = codec.inspect(encode())
        val second = codec.inspect(encode())

        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.nonce.contentEquals(second.nonce))
    }

    @Test
    fun `the wrong passphrase cannot decrypt`() {
        val file = encode()

        assertThrows(BackupReadException.CannotDecrypt::class.java) {
            codec.decode(file, "Correct horse battery staple".toCharArray())
        }
    }

    @Test
    fun `a changed ciphertext byte cannot decrypt`() {
        val file = encode().also { it[it.size - 40] = (it[it.size - 40] + 1).toByte() }

        assertThrows(BackupReadException.CannotDecrypt::class.java) { codec.decode(file, passphrase.toCharArray()) }
    }

    @Test
    fun `a changed header byte cannot decrypt either, because the header is authenticated`() {
        // The first salt byte: still a well-formed header, so only the authentication can catch it.
        val file = encode().also { it[SALT_OFFSET] = (it[SALT_OFFSET] + 1).toByte() }

        assertThrows(BackupReadException.CannotDecrypt::class.java) { codec.decode(file, passphrase.toCharArray()) }
    }

    @Test
    fun `a truncated file is refused`() {
        val file = encode()

        assertThrows(BackupReadException.NotABackup::class.java) { codec.decode(file.copyOf(10), passphrase.toCharArray()) }
        assertThrows(BackupReadException.CannotDecrypt::class.java) {
            codec.decode(file.copyOf(file.size - 1), passphrase.toCharArray())
        }
    }

    @Test
    fun `something that is not a backup is refused before any key is derived`() {
        assertThrows(BackupReadException.NotABackup::class.java) {
            codec.decode("just a text file".toByteArray(), passphrase.toCharArray())
        }
        assertEquals(0, derivations)
    }

    @Test
    fun `a newer format version is refused by name, before any key is derived`() {
        val file = encode().also { ByteBuffer.wrap(it).putInt(VERSION_OFFSET, 2) }
        derivations = 0

        val error = assertThrows(BackupReadException.NewerVersion::class.java) { codec.decode(file, passphrase.toCharArray()) }

        assertEquals(2, error.formatVersion)
        assertEquals(0, derivations)
        assertThrows(BackupReadException.NewerVersion::class.java) { codec.inspect(file) }
    }

    @Test
    fun `an iteration count outside the allowed range is refused without deriving a key`() {
        listOf(99_999, 2_000_001, 0, Int.MAX_VALUE).forEach { count ->
            val file = encode().also { ByteBuffer.wrap(it).putInt(ITERATIONS_OFFSET, count) }

            assertThrows("$count", BackupReadException.NotABackup::class.java) { codec.decode(file, passphrase.toCharArray()) }
        }
        assertEquals("Only the four encodes derived a key", 4, derivations)
    }

    @Test
    fun `a payload that authenticates but is not a valid backup is refused whole`() {
        // Written with the right passphrase by something other than this exporter: a dose amount of
        // zero, which no stored dose can have.
        val file = encryptRaw("""{"medications":[],"stockBatches":[],"doses":[{"medicationId":null,"medicationName":"X","amountValue":"0","amountUnit":"TABLET","scheduledAt":"2026-01-01T00:00:00Z","plannedAt":"2026-01-01T00:00:00Z","outcome":null,"recordedAt":null,"snoozedUntil":null,"firstRemindedAt":null,"lastRemindedAt":null,"reminderCount":0}]}""")

        assertThrows(BackupReadException.NotABackup::class.java) { codec.decode(file, passphrase.toCharArray()) }
    }

    @Test
    fun `the header layout is the documented one`() {
        val header = BackupHeader(1, 600_000, ByteArray(16) { 1 }, ByteArray(12) { 2 })
        val expected = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use {
                it.write("PILL".toByteArray())
                it.writeInt(1)
                it.writeInt(600_000)
                it.writeByte(16)
                it.write(ByteArray(16) { 1 })
                it.writeByte(12)
                it.write(ByteArray(12) { 2 })
            }
        }.toByteArray()

        assertArrayEquals(expected, header.toBytes())
    }

    private fun decryptToCompressed(file: ByteArray): ByteArray {
        val (header, length) = BackupHeader.read(file)
        val key = Pbkdf2KeyDerivation.derive(passphrase.toCharArray(), header.salt, header.iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, header.nonce)) }
        cipher.updateAAD(file, 0, length)
        return cipher.doFinal(file, length, file.size - length)
    }

    private fun encryptRaw(json: String): ByteArray {
        val header = BackupHeader(1, 100_000, ByteArray(16) { 7 }, ByteArray(12) { 9 })
        val headerBytes = header.toBytes()
        val compressed = ByteArrayOutputStream().also { out ->
            java.util.zip.GZIPOutputStream(out).use { it.write(json.toByteArray()) }
        }.toByteArray()
        val key = Pbkdf2KeyDerivation.derive(passphrase.toCharArray(), header.salt, header.iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, header.nonce)) }
        cipher.updateAAD(headerBytes)
        return headerBytes + cipher.doFinal(compressed)
    }

    private companion object {
        const val VERSION_OFFSET = 4
        const val ITERATIONS_OFFSET = 8
        const val SALT_OFFSET = 13
    }
}
