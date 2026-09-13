package nl.hexmaster.pillsner.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The migration harness (spec: Migration test harness and schema history).
 *
 * Every version adds one `Migration` and one test here, run against the schema JSON that is
 * checked in, so a schema change can never ship without a tested migration and a user's medicines
 * and intake history can never be lost to one.
 */
@RunWith(AndroidJUnit4::class)
class PillsnerDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PillsnerDatabase::class.java,
    )

    @Test
    fun version1_matchesTheExportedSchema() {
        helper.createDatabase(TEST_DATABASE, 1).close()

        helper.runMigrationsAndValidate(TEST_DATABASE, 1, true).close()
    }

    @Test
    fun migrate1To2_addsTheDoseTableAndKeepsTheMedicines() {
        helper.createDatabase(TEST_DATABASE, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO medications
                    (id, name, default_dose_value, default_dose_unit, used_since, use_until,
                     prescribed_by, is_active)
                VALUES (1, 'Ibuprofen', '400', 'MILLIGRAM', '2026-09-14', NULL,
                        'GENERAL_PRACTITIONER', 1)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO schedules
                    (id, medication_id, position, kind, amount_value, amount_unit, interval_days,
                     interval_hours, days, times, first_dose_at)
                VALUES (1, 1, 0, 'EVERY_N_DAYS', '400', 'MILLIGRAM', 1, NULL, NULL, '08:00,20:00', NULL)
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, Migrations.MIGRATION_1_2)

        migrated.query("SELECT name, default_dose_value FROM medications").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Ibuprofen", cursor.getString(0))
            assertEquals("400", cursor.getString(1))
        }
        migrated.query("SELECT times FROM schedules").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("08:00,20:00", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM doses").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
