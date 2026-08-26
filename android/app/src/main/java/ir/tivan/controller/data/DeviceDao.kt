package ir.tivan.controller.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY id ASC")
    fun observeAll(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getById(id: Long): Device?

    @Query("SELECT * FROM devices WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): Device?

    @Insert
    suspend fun insert(device: Device): Long

    @Update
    suspend fun update(device: Device)

    @Delete
    suspend fun delete(device: Device)
}

@Dao
interface DeviceStatusDao {
    @Query("SELECT * FROM device_status WHERE deviceId = :deviceId")
    fun observe(deviceId: Long): Flow<DeviceStatus?>

    @Query("SELECT * FROM device_status WHERE deviceId = :deviceId")
    suspend fun get(deviceId: Long): DeviceStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: DeviceStatus)

    @Query("DELETE FROM device_status WHERE deviceId = :deviceId")
    suspend fun deleteFor(deviceId: Long)
}

@Dao
interface MessageLogDao {
    @Query("SELECT * FROM message_logs WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 100")
    fun observeForDevice(deviceId: Long): Flow<List<MessageLog>>

    @Insert
    suspend fun insert(log: MessageLog)

    @Query("DELETE FROM message_logs WHERE deviceId = :deviceId")
    suspend fun clearFor(deviceId: Long)
}
