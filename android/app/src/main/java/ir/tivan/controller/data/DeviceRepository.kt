package ir.tivan.controller.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceRepository(private val db: AppDatabase) {

    val devices: Flow<List<Device>> = db.deviceDao().observeAll()

    private val _selectedDeviceId = MutableStateFlow<Long?>(null)
    val selectedDeviceId: StateFlow<Long?> = _selectedDeviceId.asStateFlow()

    fun selectDevice(id: Long) {
        _selectedDeviceId.value = id
    }

    suspend fun addDevice(name: String, phone: String, icon: String = "🏠"): Long {
        val id = db.deviceDao().insert(Device(name = name, phoneNumber = phone, icon = icon))
        if (_selectedDeviceId.value == null) _selectedDeviceId.value = id
        return id
    }

    suspend fun updateDevice(device: Device) = db.deviceDao().update(device)

    suspend fun deleteDevice(device: Device) = db.deviceDao().delete(device)

    suspend fun getDevice(id: Long) = db.deviceDao().getById(id)

    fun logsFor(deviceId: Long): Flow<List<MessageLog>> = db.messageLogDao().observeForDevice(deviceId)

    suspend fun addLog(deviceId: Long, direction: LogDirection, body: String) {
        db.messageLogDao().insert(MessageLog(deviceId = deviceId, direction = direction, body = body))
    }

    suspend fun findByPhone(phone: String): Device? = db.deviceDao().getByPhone(phone)
}
