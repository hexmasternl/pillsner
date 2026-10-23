package nl.hexmaster.pillsner.data.backup

import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import nl.hexmaster.pillsner.domain.backup.BackupSnapshot

/** Turns a passphrase into an AES key. A port only so a test can see whether it was ever asked. */
fun interface KeyDerivation {
    fun derive(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKey
}

/** PBKDF2-HMAC-SHA256 to a 256-bit AES key, from the platform's own provider. */
object Pbkdf2KeyDerivation : KeyDerivation {
    override fun derive(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        try {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
            return SecretKeySpec(key.encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private const val KEY_BITS = 256
}

/**
 * Writes and reads `.pill` files (`data-backup-restore` design).
 *
 * Export is serialise, compress, encrypt, in that order: JSON, then GZIP, then AES-256-GCM under a
 * key derived from the passphrase with a fresh random salt, and a fresh random nonce. Compression
 * has to come first, because ciphertext does not compress.
 *
 * Import runs the same steps backwards, and the only outcome other than a whole snapshot is a
 * [BackupReadException]. GCM authenticates the entire file as one unit, so there is no partial
 * result to fall back on, and none is attempted.
 *
 * No Android type anywhere, so all of this runs in a plain unit test.
 */
class BackupFileCodec(
    private val random: SecureRandom = SecureRandom(),
    private val keyDerivation: KeyDerivation = Pbkdf2KeyDerivation,
    private val iterations: Int = BackupHeader.ITERATIONS,
) {

    private val json = Json

    /** The finished file for [snapshot], encrypted under [passphrase]. */
    fun encode(snapshot: BackupSnapshot, passphrase: CharArray): ByteArray {
        val header = BackupHeader(
            formatVersion = BackupHeader.CURRENT_FORMAT_VERSION,
            iterations = iterations,
            salt = ByteArray(BackupHeader.SALT_BYTES).also(random::nextBytes),
            nonce = ByteArray(BackupHeader.NONCE_BYTES).also(random::nextBytes),
        )
        val headerBytes = header.toBytes()
        val payload = gzip(json.encodeToString(PayloadV1.serializer(), snapshot.toPayloadV1()).toByteArray(Charsets.UTF_8))

        val cipher = cipher(Cipher.ENCRYPT_MODE, keyDerivation.derive(passphrase, header.salt, header.iterations), header.nonce)
        cipher.updateAAD(headerBytes)
        return headerBytes + cipher.doFinal(payload)
    }

    /**
     * Only the plaintext header: whether [file] is a backup at all, and in which format. Needs no
     * passphrase, which is what lets import refuse an unusable file before asking for one.
     */
    fun inspect(file: ByteArray): BackupHeader = BackupHeader.read(file).first

    /**
     * The snapshot inside [file].
     *
     * @throws BackupReadException.NotABackup when [file] is not a backup, is cut short, or decrypts
     *   to something that is not a valid backup.
     * @throws BackupReadException.NewerVersion when a newer app wrote it.
     * @throws BackupReadException.CannotDecrypt on the wrong passphrase or a file altered after export.
     */
    fun decode(file: ByteArray, passphrase: CharArray): BackupSnapshot {
        // Header checks come first and are complete, so an out-of-range iteration count is refused
        // here and the key derivation below is never reached.
        val (header, headerLength) = BackupHeader.read(file)

        val compressed = try {
            val cipher = cipher(Cipher.DECRYPT_MODE, keyDerivation.derive(passphrase, header.salt, header.iterations), header.nonce)
            cipher.updateAAD(file, 0, headerLength)
            cipher.doFinal(file, headerLength, file.size - headerLength)
        } catch (_: GeneralSecurityException) {
            throw BackupReadException.CannotDecrypt()
        }

        return try {
            json.decodeFromString(PayloadV1.serializer(), gunzip(compressed).toString(Charsets.UTF_8)).toSnapshot()
        } catch (error: BackupReadException) {
            throw error
        } catch (_: Exception) {
            // Only reachable for a file that authenticated, so written with the right passphrase by
            // something that was not this app's own exporter. Still never a partial import.
            throw BackupReadException.NotABackup("The payload is not a valid backup")
        }
    }

    private fun cipher(mode: Int, key: SecretKey, nonce: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply { init(mode, key, GCMParameterSpec(TAG_BITS, nonce)) }

    private fun gzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bytes) }
        return out.toByteArray()
    }

    /** Bounded, so even a payload that authenticated cannot unpack into more memory than a phone has. */
    private fun gunzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPInputStream(bytes.inputStream()).use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                if (out.size() > MAX_PAYLOAD_BYTES) throw BackupReadException.NotABackup("Payload too large")
            }
        }
        return out.toByteArray()
    }

    companion object {
        private const val TAG_BITS = 128
        private const val BUFFER_BYTES = 8 * 1024

        /** Far beyond any real history: a year of hourly doses for twenty medicines is a few MB. */
        const val MAX_PAYLOAD_BYTES = 64 * 1024 * 1024
    }
}
