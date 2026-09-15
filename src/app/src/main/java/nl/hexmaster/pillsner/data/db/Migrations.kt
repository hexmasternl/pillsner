package nl.hexmaster.pillsner.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema history. Every version bump ships one migration here and one test in
 * `PillsnerDatabaseMigrationTest`, run against the exported schema, so a user's medicines and
 * intake history can never be lost to a schema change.
 */
object Migrations {

    /** Adds the `doses` table that reminders and intake history live in (app-medicine-alarm D9). */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `doses` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `medication_id` INTEGER,
                    `medication_name` TEXT NOT NULL,
                    `amount_value` TEXT NOT NULL,
                    `amount_unit` TEXT NOT NULL,
                    `scheduled_at` INTEGER NOT NULL,
                    `outcome` TEXT,
                    `recorded_at` INTEGER,
                    `snoozed_until` INTEGER,
                    `first_reminded_at` INTEGER,
                    FOREIGN KEY(`medication_id`) REFERENCES `medications`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_doses_medication_id_scheduled_at` ON `doses` (`medication_id`, `scheduled_at`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_doses_scheduled_at` ON `doses` (`scheduled_at`)",
            )
        }
    }

    /**
     * Adds the repeat counter a reminder needs to know how often it has already asked
     * (reminder-delivery-reliability D5). Additive, with a default, so every existing dose starts
     * at nought and no history is touched.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `doses` ADD COLUMN `reminder_count` INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** Every migration the database knows about, in order. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
