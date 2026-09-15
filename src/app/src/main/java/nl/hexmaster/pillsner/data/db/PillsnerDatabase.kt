package nl.hexmaster.pillsner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The one database on the device. Nothing in it ever leaves the device.
 *
 * Schemas are exported to `app/schemas` and checked in, so every later version ships with a
 * migration and a test against the exported schema, as `CLAUDE.md` requires.
 */
@Database(
    entities = [MedicationEntity::class, ScheduleEntity::class, DoseEntity::class],
    version = PillsnerDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PillsnerDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao

    abstract fun doseDao(): DoseDao

    companion object {
        const val VERSION = 3
        const val NAME = "pillsner.db"

        /**
         * Builds the database. No destructive migration fallback is configured, in any build type:
         * losing a user's medication history to a schema change is never acceptable, so a missing
         * migration has to fail loudly in development instead.
         */
        fun build(context: Context): PillsnerDatabase =
            Room.databaseBuilder(context.applicationContext, PillsnerDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                .build()
    }
}
