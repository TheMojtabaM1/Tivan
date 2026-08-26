package ir.tivan.controller

import android.app.Application
import ir.tivan.controller.data.AppDatabase
import ir.tivan.controller.data.Device
import ir.tivan.controller.data.DeviceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class TivanApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: DeviceRepository by lazy { DeviceRepository(database) }

    /** Emits (deviceId, rawSmsBody) whenever an incoming SMS from a known device arrives. */
    val reportBus = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 16)

    suspend fun allDevicesSnapshot(): List<Device> = database.deviceDao().observeAll().first()
}
