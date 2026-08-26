package ir.tivan.controller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.tivan.controller.TivanApp
import ir.tivan.controller.data.Device
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.data.MessageLog
import ir.tivan.controller.sms.SmsSender
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OutputState(val on: Boolean = false, val name: String = "")

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val tivanApp get() = getApplication<TivanApp>()
    private val repo get() = tivanApp.repository

    val devices: StateFlow<List<Device>> =
        repo.devices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedDeviceId: StateFlow<Long?> = repo.selectedDeviceId

    val selectedDevice: StateFlow<Device?> =
        combine(devices, selectedDeviceId) { list, id ->
            list.firstOrNull { it.id == id } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _outputs = MutableStateFlow(List(4) { OutputState(name = "OUT${it + 1}") })
    val outputs: StateFlow<List<OutputState>> = _outputs.asStateFlow()

    private val _securityArmed = MutableStateFlow(false)
    val securityArmed: StateFlow<Boolean> = _securityArmed.asStateFlow()

    private val _lastReportText = MutableStateFlow("هنوز گزارشی دریافت نشده")
    val lastReportText: StateFlow<String> = _lastReportText.asStateFlow()

    val logs: StateFlow<List<MessageLog>> =
        selectedDeviceId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.logsFor(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            devices.collect { list ->
                if (repo.selectedDeviceId.value == null && list.isNotEmpty()) {
                    repo.selectDevice(list.first().id)
                }
            }
        }
        viewModelScope.launch {
            selectedDevice.collect { device ->
                device?.let { d ->
                    _outputs.value = List(4) { i ->
                        OutputState(name = d.outputNames.getOrElse(i) { "OUT${i + 1}" })
                    }
                }
            }
        }
        viewModelScope.launch {
            tivanApp.reportBus.collect { (deviceId, body) ->
                if (deviceId == selectedDeviceId.value) {
                    _lastReportText.value = body
                    applyParsedStatus(body)
                }
            }
        }
    }

    private fun applyParsedStatus(body: String) {
        val upper = body.uppercase()
        Regex("OUT(\\d)\\s*(ON|OFF)").findAll(upper).forEach { m ->
            val idx = (m.groupValues[1].toIntOrNull() ?: return@forEach) - 1
            if (idx in 0..3) {
                val current = _outputs.value.toMutableList()
                current[idx] = current[idx].copy(on = m.groupValues[2] == "ON")
                _outputs.value = current
            }
        }
    }

    fun selectDevice(id: Long) = repo.selectDevice(id)

    fun addDevice(name: String, phone: String, icon: String = "🏠") {
        viewModelScope.launch { repo.addDevice(name, phone, icon) }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch { repo.deleteDevice(device) }
    }

    fun renameOutput(index: Int, newName: String) {
        val device = selectedDevice.value ?: return
        val names = device.outputNames.toMutableList()
        while (names.size < 4) names.add("OUT${names.size + 1}")
        names[index] = newName
        viewModelScope.launch { repo.updateDevice(device.copy(outputNames = names)) }
        val current = _outputs.value.toMutableList()
        current[index] = current[index].copy(name = newName)
        _outputs.value = current
    }

    /** Sends [command] to the currently selected device and logs it. */
    fun sendCommand(command: String) {
        val device = selectedDevice.value ?: return
        SmsSender.send(tivanApp, device.phoneNumber, command)
        viewModelScope.launch { repo.addLog(device.id, LogDirection.OUT, command) }
    }

    fun toggleOutput(index: Int, turnOn: Boolean, command: String) {
        val current = _outputs.value.toMutableList()
        current[index] = current[index].copy(on = turnOn)
        _outputs.value = current
        sendCommand(command)
    }

    fun setSecurityArmed(armed: Boolean, command: String) {
        _securityArmed.value = armed
        sendCommand(command)
    }
}
