package nl.hexmaster.pillsner.data.db

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Everything this database stores is kept as text (design D3): decimals as their plain string so
 * "2.50" never becomes 2.4999, dates and times as ISO values, enums as their names. That keeps the
 * rows readable in a migration test, which matters more here than a few bytes.
 */
class Converters {

    @TypeConverter
    fun decimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun dateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun timeToString(value: LocalTime?): String? = value?.format(TIME_FORMAT)

    @TypeConverter
    fun stringToTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun timesToString(value: List<LocalTime>?): String? =
        value?.joinToString(SEPARATOR) { it.format(TIME_FORMAT) }

    @TypeConverter
    fun stringToTimes(value: String?): List<LocalTime>? =
        value?.split(SEPARATOR)?.filter { it.isNotBlank() }?.map(LocalTime::parse)

    @TypeConverter
    fun daysToString(value: Set<DayOfWeek>?): String? =
        value?.sorted()?.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun stringToDays(value: String?): Set<DayOfWeek>? =
        value?.split(SEPARATOR)?.filter { it.isNotBlank() }?.map(DayOfWeek::valueOf)?.toSet()

    private companion object {
        const val SEPARATOR = ","

        /** HH:mm, so a stored time never carries seconds it does not have. */
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
