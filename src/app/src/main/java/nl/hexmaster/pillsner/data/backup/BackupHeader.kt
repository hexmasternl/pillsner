package nl.hexmaster.pillsner.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

/**
 * Why a `.pill` file could not be turned back into a backup (`data-backup-restore`).
 *
 * Three outcomes the user can act on differently, and deliberately no more. A wrong passphrase and
 * a file altered after export are one outcome, [CannotDecrypt], because AES-GCM cannot tell them
 * apart and pretending otherwise would be a guess.
 */
sealed class BackupReadException(message: String) : Exception(message) {

    /** Not a Pillsner backup at all, or damaged before its encrypted part even starts. */
    class NotABackup(reason: String) : BackupReadException(reason)

    /** Written by a newer Pillsner, in a format this version does not know how to read. */
    class NewerVersion(val formatVersion: Int) : BackupReadException("Backup format $formatVersion is newer than this app reads")

    /** The authentication check failed: the wrong passphrase, or the file was changed after export. */
    class CannotDecrypt : BackupReadException("The backup could not be decrypted")
}

/**
 * The plaintext start of a `.pill` file (`data-backup-restore` design).
 *
 * ```
 * "PILL"          4 bytes   magic marker
 * format version  int32     big-endian
 * iterations      int32     PBKDF2-HMAC-SHA256 iteration count
 * salt length     uint8     then the salt
 * nonce length    uint8     then the AES-GCM nonce
 * ```
 *
 * Followed directly by the ciphertext and its GCM tag. Nothing here is secret: it is what is needed
 * to derive the key and nothing more. The marker and version come first and keep their place in
 * every future format, so any version of the app can at least say which version a file is.
 *
 * The exact header bytes are also the GCM associated data, so editing any of them, the iteration
 * count included, fails decryption just as editing the ciphertext would.
 */
class BackupHeader(
    val formatVersion: Int,
    val iterations: Int,
    val salt: ByteArray,
    val nonce: ByteArray,
) {

    fun toBytes(): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.write(MAGIC)
            out.writeInt(formatVersion)
            out.writeInt(iterations)
            out.writeByte(salt.size)
            out.write(salt)
            out.writeByte(nonce.size)
            out.write(nonce)
        }
        return bytes.toByteArray()
    }

    companion object {
        private val MAGIC = "PILL".toByteArray(Charsets.US_ASCII)

        /** The newest format this app writes and reads. Older ones keep their own readers. */
        const val CURRENT_FORMAT_VERSION = 1

        /** The current OWASP minimum for PBKDF2-HMAC-SHA256; what every export is written with. */
        const val ITERATIONS = 600_000

        /**
         * The counts an import will spend a key derivation on. A header outside it is refused
         * before PBKDF2 runs, so a crafted file cannot make the app burn minutes of CPU.
         */
        val ALLOWED_ITERATIONS = 100_000..2_000_000

        const val SALT_BYTES = 16
        private val ALLOWED_SALT_BYTES = 16..64
        const val NONCE_BYTES = 12

        /**
         * Reads the header at the start of [file], and returns it with the number of bytes it took.
         *
         * Checks everything it can without the passphrase: the marker, the version, the iteration
         * range and the field lengths.
         *
         * @throws BackupReadException.NotABackup when [file] is not a `.pill` file or its header is
         *   damaged or out of range.
         * @throws BackupReadException.NewerVersion when it was written in a newer format.
         */
        fun read(file: ByteArray): Pair<BackupHeader, Int> {
            val input = DataInputStream(ByteArrayInputStream(file))
            try {
                val magic = ByteArray(MAGIC.size).also(input::readFully)
                if (!magic.contentEquals(MAGIC)) throw BackupReadException.NotABackup("No backup marker")

                val version = input.readInt()
                if (version > CURRENT_FORMAT_VERSION) throw BackupReadException.NewerVersion(version)
                if (version < 1) throw BackupReadException.NotABackup("Unknown format version")

                val iterations = input.readInt()
                if (iterations !in ALLOWED_ITERATIONS) throw BackupReadException.NotABackup("Iteration count out of range")

                val salt = ByteArray(input.readUnsignedByte()).also(input::readFully)
                if (salt.size !in ALLOWED_SALT_BYTES) throw BackupReadException.NotABackup("Salt length out of range")

                val nonce = ByteArray(input.readUnsignedByte()).also(input::readFully)
                if (nonce.size != NONCE_BYTES) throw BackupReadException.NotABackup("Nonce length out of range")

                val header = BackupHeader(version, iterations, salt, nonce)
                return header to file.size - input.available()
            } catch (_: EOFException) {
                throw BackupReadException.NotABackup("The file ends inside its header")
            }
        }
    }
}
