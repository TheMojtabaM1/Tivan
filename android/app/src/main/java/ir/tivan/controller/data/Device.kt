package ir.tivan.controller.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val icon: String = "🏠",
    val outputNames: List<String> = listOf("OUT1", "OUT2", "OUT3", "OUT4"),
    val isSelected: Boolean = false
)

class Converters {
    @androidx.room.TypeConverter
    fun fromList(list: List<String>): String = list.joinToString("§")

    @androidx.room.TypeConverter
    fun toList(data: String): List<String> = if (data.isBlank()) emptyList() else data.split("§")
}
