package ir.tivan.controller.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Device::class, MessageLog::class, DeviceStatus::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun messageLogDao(): MessageLogDao
    abstract fun deviceStatusDao(): DeviceStatusDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tivan.db"
                )
                    // Upgrades keep the user's devices, custom names and message
                    // history. No destructive fallback: a missing migration should
                    // surface as a crash in testing, never as silent data loss on
                    // someone's phone.
                    .addMigrations(*Migrations.ALL)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
