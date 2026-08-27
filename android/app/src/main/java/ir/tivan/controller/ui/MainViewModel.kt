package ir.tivan.controller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.tivan.controller.TivanApp
import ir.tivan.controller.data.Device
import ir.tivan.controller.data.DeviceStatus
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.data.MessageLog
import ir.tivan.controller.sms.SmsSender
import ir.tivan.controller.sms.StatusParser
import ir.tivan.controller.util.SmsPermissions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** What the UI shows for one relay output. */
data class OutputUi(
    val index: Int,
    val name: String,
    val icon: String,
    val on: Boolean?,          // null = never heard from the device
    val pending: Boolean,
    val pendingTarget: Boolean?,
    val updatedAt: Long
)

data class InputUi(
    val index: Int,
    val message: String,
    val icon: String,
    /** 0 = OFF, 1 = N.O, 2 = N.C. */
    val mode: Int,
    /** Live state from the last REPORT; null when never reported. */
    val closed: Boolean?,
    val stateAt: Long,
    /** When this input last fired; 0 = never. */
    val triggeredAt: Long,
    /** True only while that trigger is still fresh. */
    val recentlyTriggered: Boolean
)

/** One-shot messages surfaced as a snackbar. */
data class Toast(val id: Long, val text: String)

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

    /** Persisted last-known device state, so the UI is populated on cold start. */
    val status: StateFlow<DeviceStatus?> =
        selectedDevice.flatMapLatest { d ->
            if (d == null) flowOf(null) else repo.statusFor(d.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs: StateFlow<List<MessageLog>> =
        selectedDevice.flatMapLatest { d ->
            if (d == null) flowOf(emptyList()) else repo.logsFor(d.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- pending commands ---------------------------------------------------
    // An action is never treated as done just because the SMS left the phone.
    // It sits in `pendingOutputs` / `pendingSecurity` until the controller sends
    // back a matching confirmation, or until CONFIRM_TIMEOUT_MS elapses.
    private val _pendingOutputs = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    private val _pendingSecurity = MutableStateFlow<Boolean?>(null)
    val pendingSecurity: StateFlow<Boolean?> = _pendingSecurity.asStateFlow()

    private val timeoutJobs = mutableMapOf<String, Job>()

    private val _toast = MutableSharedFlow<Toast>(extraBufferCapacity = 8)
    val toast: SharedFlow<Toast> = _toast.asSharedFlow()

    /** Emitted when an input trips, so the hero card can go red. */
    private val _alarm = MutableStateFlow<Int?>(null)
    val alarm: StateFlow<Int?> = _alarm.asStateFlow()

    val outputs: StateFlow<List<OutputUi>> =
        combine(selectedDevice, status, _pendingOutputs) { device, st, pending ->
            (0..3).map { i ->
                OutputUi(
                    index = i,
                    name = device?.outputName(i) ?: "OUT${i + 1}",
                    icon = device?.outputIcon(i) ?: "🔌",
                    on = st?.output(i),
                    pending = pending.containsKey(i),
                    pendingTarget = pending[i],
                    updatedAt = st?.outputsAt ?: 0L
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ticks while the screen is open so a "just triggered" highlight expires on
    // its own instead of waiting for the next SMS to redraw the list.
    private val highlightTicker: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(20_000)
        }
    }

    val inputs: StateFlow<List<InputUi>> =
        combine(selectedDevice, status, highlightTicker) { device, st, now ->
            (0..3).map { i ->
                InputUi(
                    index = i,
                    message = device?.inputMessage(i) ?: "In${i + 1} Triggered",
                    icon = device?.inputIcon(i) ?: "📥",
                    mode = device?.inputModes?.getOrNull(i) ?: 1,
                    closed = st?.input(i),
                    stateAt = st?.inputsAt ?: 0L,
                    triggeredAt = st?.triggeredAt(i) ?: 0L,
                    recentlyTriggered = st?.recentlyTriggered(i, now) ?: false
                )
            }
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
            tivanApp.reportBus.collect { (deviceId, body) -> onSmsReceived(deviceId, body) }
        }
    }

    // ---- incoming SMS -------------------------------------------------------
    private suspend fun onSmsReceived(deviceId: Long, body: String) {
        val device = devices.value.firstOrNull { it.id == deviceId }
            ?: repo.getDevice(deviceId) ?: return
        val previous = repo.statusOnce(deviceId) ?: DeviceStatus.empty(deviceId)

        val result = StatusParser.parse(device, body, previous)
        repo.saveStatus(result.status)

        if (deviceId != selectedDevice.value?.id) return

        // Settle any pending output whose confirmation just arrived.
        val stillPending = _pendingOutputs.value.toMutableMap()
        var settled = false
        for ((index, target) in _pendingOutputs.value) {
            if (StatusParser.confirmsOutput(device, body, index, target)) {
                stillPending.remove(index)
                cancelTimeout("out$index")
                settled = true
            }
        }
        if (settled) _pendingOutputs.value = stillPending

        _pendingSecurity.value?.let { target ->
            if (StatusParser.confirmsSecurity(body, target)) {
                _pendingSecurity.value = null
                cancelTimeout("sec")
                emitToast(if (target) "دزدگیر فعال شد" else "دزدگیر غیرفعال شد")
            }
        }

        result.triggeredInputs.firstOrNull()?.let { idx ->
            _alarm.value = idx
            emitToast("تحریک ورودی ${idx + 1}")
        }

        if (result.recognized && !settled) {
            emitToast("گزارش جدید از دستگاه دریافت شد")
        }
    }

    fun clearAlarm() {
        _alarm.value = null
    }

    // ---- commands -----------------------------------------------------------
    fun sendCommand(command: String, toastText: String? = null): SmsSender.Result {
        val device = selectedDevice.value ?: return SmsSender.Result.Failed
        val result = SmsSender.send(tivanApp, device.phoneNumber, command)
        viewModelScope.launch {
            if (result != SmsSender.Result.Failed) {
                repo.addLog(device.id, LogDirection.OUT, command)
            }
            emitToast(
                when (result) {
                    SmsSender.Result.Sent -> toastText ?: "پیامک «$command» ارسال شد"
                    SmsSender.Result.Handoff -> "برنامه پیامک باز شد — دکمه ارسال را بزنید"
                    SmsSender.Result.Failed -> "ارسال ناموفق بود — دسترسی پیامک را بررسی کنید"
                }
            )
        }
        return result
    }

    /**
     * Sends the on/off command and parks the output in the pending state.
     *
     * Only a command that actually left the device starts the pending timer;
     * a failed send would otherwise leave the tile spinning for 90 seconds.
     */
    fun toggleOutput(index: Int, turnOn: Boolean) {
        if (_pendingOutputs.value.containsKey(index)) return
        val command = if (turnOn) "${index + 1}1" else "${index + 1}0"
        _pendingOutputs.value = _pendingOutputs.value + (index to turnOn)
        val result = sendCommand(command, "دستور ارسال شد — منتظر تأیید دستگاه")
        if (result == SmsSender.Result.Failed) {
            _pendingOutputs.value = _pendingOutputs.value - index
            return
        }
        startTimeout("out$index") {
            _pendingOutputs.value = _pendingOutputs.value - index
            emitToast(confirmTimeoutMessage())
        }
    }

    fun setSecurity(armed: Boolean) {
        if (_pendingSecurity.value != null) return
        _pendingSecurity.value = armed
        val result = sendCommand(
            if (armed) "SECON" else "SECOF",
            "دستور ارسال شد — منتظر تأیید دستگاه"
        )
        if (result == SmsSender.Result.Failed) {
            _pendingSecurity.value = null
            return
        }
        startTimeout("sec") {
            _pendingSecurity.value = null
            emitToast(confirmTimeoutMessage())
        }
    }

    /**
     * Without RECEIVE_SMS the confirmation can never arrive, so say that
     * plainly instead of blaming the controller for not answering.
     */
    private fun confirmTimeoutMessage(): String =
        if (SmsPermissions.canReceive(tivanApp)) {
            "تأییدی از دستگاه نرسید — وضعیت تغییر نکرد"
        } else {
            "بدون دسترسی دریافت پیامک، تأیید خودکار ممکن نیست"
        }

    // ---- device management --------------------------------------------------
    fun selectDevice(id: Long) = repo.selectDevice(id)

    fun addDevice(name: String, phone: String, icon: String) {
        viewModelScope.launch {
            repo.addDevice(name.ifBlank { "دستگاه جدید" }, phone, icon)
            emitToast("دستگاه اضافه شد")
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            repo.deleteDevice(device)
            emitToast("دستگاه حذف شد")
        }
    }

    private fun updateDevice(transform: (Device) -> Device) {
        val device = selectedDevice.value ?: return
        viewModelScope.launch { repo.updateDevice(transform(device)) }
    }

    /**
     * Renames an output locally *and* on the controller. Keeping both in sync
     * matters: the device echoes this name in every future report, and the
     * parser matches against it.
     */
    fun renameOutput(index: Int, name: String, icon: String) {
        val clean = name.take(14).trim()
        updateDevice { d ->
            d.copy(
                outputNames = d.outputNames.padTo(4, Device.DEFAULT_OUTPUT_NAMES)
                    .replaceAt(index, clean.ifBlank { "OUT${index + 1}" }),
                outputIcons = d.outputIcons.padTo(4, Device.DEFAULT_OUTPUT_ICONS)
                    .replaceAt(index, icon)
            )
        }
        if (clean.isNotBlank()) sendCommand("NAMEOUT${index + 1}:$clean", "نام خروجی ${index + 1} تغییر کرد")
    }

    fun setInputMessage(index: Int, message: String, icon: String) {
        val clean = message.take(24).trim()
        updateDevice { d ->
            d.copy(
                inputMessages = d.inputMessages.padTo(4, Device.DEFAULT_INPUT_MESSAGES)
                    .replaceAt(index, clean.ifBlank { "In${index + 1} Triggered" }),
                inputIcons = d.inputIcons.padTo(4, Device.DEFAULT_INPUT_ICONS)
                    .replaceAt(index, icon)
            )
        }
        if (clean.isNotBlank()) sendCommand("PAYAMEIN${index + 1}:$clean", "پیام ورودی ${index + 1} تغییر کرد")
    }

    fun setInputMode(index: Int, mode: Int) {
        updateDevice { d ->
            d.copy(inputModes = d.inputModes.padTo(4, listOf(1, 1, 1, 1)).replaceAt(index, mode))
        }
        sendCommand("MODE${index + 1}$mode")
    }

    fun setInputResponse(index: Int, level: Int) {
        updateDevice { d ->
            d.copy(inputResponses = d.inputResponses.padTo(4, listOf(0, 0, 0, 0)).replaceAt(index, level))
        }
        sendCommand("SET${index + 1}$level")
    }

    fun setSecurityZones(zones: Int) {
        updateDevice { it.copy(securityZones = zones) }
        sendCommand("SEC${zones}Z")
    }

    fun setAutoReport(mode: Int) {
        updateDevice { it.copy(autoReportMode = mode) }
        sendCommand("REP$mode")
    }

    fun setOutputMemory(enabled: Boolean) {
        updateDevice { it.copy(outputMemory = enabled) }
        sendCommand("MEM${if (enabled) 1 else 0}")
    }

    fun setBuzzer(enabled: Boolean) {
        updateDevice { it.copy(buzzer = enabled) }
        sendCommand("BUZZER${if (enabled) 1 else 0}")
    }

    fun setRemoteLatch(latch: Boolean) {
        updateDevice { it.copy(remoteLatch = latch) }
        sendCommand(if (latch) "REMOTEFF" else "REMOTEPL")
    }

    fun setRemoteSecurityMode(security: Boolean) {
        updateDevice { it.copy(remoteSecurityMode = security) }
        sendCommand(if (security) "RMODESE" else "RMODENO")
    }

    // ---- helpers ------------------------------------------------------------
    private fun startTimeout(key: String, onTimeout: suspend () -> Unit) {
        timeoutJobs.remove(key)?.cancel()
        timeoutJobs[key] = viewModelScope.launch {
            delay(CONFIRM_TIMEOUT_MS)
            onTimeout()
            timeoutJobs.remove(key)
        }
    }

    private fun cancelTimeout(key: String) {
        timeoutJobs.remove(key)?.cancel()
    }

    private suspend fun emitToast(text: String) {
        _toast.emit(Toast(System.nanoTime(), text))
    }

    override fun onCleared() {
        timeoutJobs.values.forEach { it.cancel() }
        timeoutJobs.clear()
        super.onCleared()
    }

    companion object {
        /** How long to wait for the controller to confirm before giving up. */
        const val CONFIRM_TIMEOUT_MS = 90_000L
    }
}

private fun <T> List<T>.padTo(size: Int, defaults: List<T>): List<T> {
    if (this.size >= size) return this
    return this + (this.size until size).map { defaults[it] }
}

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    if (index !in indices) this else toMutableList().also { it[index] = value }
