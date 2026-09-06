package ir.tivan.controller.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A weekly on/off timer for one output or input, sent to the controller by
 * SMS at the given clock time. [days] is a bitmask over [java.util.Calendar]
 * day-of-week constants (SUNDAY=1 .. SATURDAY=7), so one row can cover
 * anywhere from a single day to every day; a device with different times on
 * different days is modeled as several rows on the same channel, each with
 * its own [days]/time. Fires forever until deleted or disabled — after each
 * firing [ScheduleAlarmReceiver] reschedules the same row for its next
 * matching day, one week out at most.
 */
@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: Long,
    val isOutput: Boolean,
    val channelIndex: Int,
    /** Bitmask, bit N (1-indexed, matching Calendar.DAY_OF_WEEK) set if that day is included. */
    val days: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    @ColumnInfo(defaultValue = "1")
    val enabled: Boolean = true
) {
    companion object {
        fun dayBit(calendarDayOfWeek: Int) = 1 shl calendarDayOfWeek
    }
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE deviceId = :deviceId AND isOutput = :isOutput AND channelIndex = :channelIndex ORDER BY startHour, startMinute")
    fun observeFor(deviceId: Long, isOutput: Boolean, channelIndex: Int): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    suspend fun getAllEnabled(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): Schedule?

    @Insert
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)
}
