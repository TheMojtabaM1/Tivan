package ir.tivan.controller.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogDirection { OUT, IN }

@Entity(tableName = "message_logs")
data class MessageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: Long,
    val direction: LogDirection,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)
