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
     * Adds the two things a repeating reminder has to remember between one ask and the next
     * (reminder-delivery-reliability D5): when it last posted, and how often it has asked since.
     *
     * Both are additive and default to "never asked", so every existing dose starts its repeat
     * sequence at the next posting and no history is touched.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `doses` ADD COLUMN `last_reminded_at` INTEGER")
            db.execSQL("ALTER TABLE `doses` ADD COLUMN `reminder_count` INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Adds the moment a dose was first stored (design D4).
     *
     * The column is what tells a reminder the platform did not deliver from a dose that never had
     * a chance to be announced, so the Home banner is raised by evidence rather than by a setting.
     *
     * Existing rows take their own `scheduled_at`, which reads as "planned at the moment it was
     * due" and so fails the "existed before it was due" test. Silent on history is the right
     * default: the app reports what it observes from now on rather than re-litigating doses from
     * before it could tell the two apart.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `doses` ADD COLUMN `planned_at` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `doses` SET `planned_at` = `scheduled_at`")
        }
    }

    /** Every migration the database knows about, in order. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
