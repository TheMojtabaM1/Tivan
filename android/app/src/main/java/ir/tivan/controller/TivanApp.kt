package ir.tivan.controller

import android.app.Application
import ir.tivan.controller.data.AppDatabase
import ir.tivan.controller.data.Device
import ir.tivan.controller.data.DeviceRepository
import ir.tivan.controller.data.DeviceStatus
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.sms.StatusParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TivanApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: DeviceRepository by lazy { DeviceRepository(database) }

    /** Emits (deviceId, rawSmsBody) whenever an incoming SMS from a known device arrives. */
    val reportBus = MutableSharedFlow<Pair<Long, String>>(extraBufferCapacity = 16)

    // One message can arrive twice: once as SMS_RECEIVED and once through the
    // SMS app's notification. Keyed on deviceId+body rather than body alone —
    // two different paired devices can legitimately send the identical text
    // (e.g. two un-renamed controllers both replying "OUT1 OFF"), and a
    // body-only key would drop the second one as a false duplicate.
    private val recentMutex = Mutex()
    private val recent = LinkedHashMap<String, Long>()

    suspend fun allDevicesSnapshot(): List<Device> = database.deviceDao().observeAll().first()

    /**
     * Single entry point for both SMS receivers: matches the sender to a paired
     * device, logs the message and hands it to the parser.
     */
    suspend fun onIncomingSms(sender: String, body: String) {
        if (body.isBlank()) return

        val suffix = sender.filter { it.isDigit() }.takeLast(10)
        if (suffix.isEmpty()) return
        val device = allDevicesSnapshot().firstOrNull {
            it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == suffix
        } ?: return

        if (!claimMessage("${device.id}|$body")) return
        repository.addLog(device.id, LogDirection.IN, body)
        reportBus.tryEmit(device.id to body)
    }

    /**
     * Entry point for a message seen in the SMS app's notification, used when
     * RECEIVE_SMS cannot be granted.
     *
     * The title is whatever the SMS app shows — often the contact name rather
     * than the number — so matching by digits is only the first attempt. When
     * that fails, the body is offered to the parser: if exactly one paired
     * device recognises it as one of its own replies, it is attributed there.
     * Anything more ambiguous is ignored rather than guessed at, so an unrelated
     * text can never flip an output's state.
     */
    suspend fun onIncomingNotification(title: String, body: String) {
        if (body.isBlank()) return

        val devices = allDevicesSnapshot()
        if (devices.isEmpty()) return

        val titleDigits = title.filter { it.isDigit() }.takeLast(10)
        val byNumber = if (titleDigits.length >= 10) {
            devices.firstOrNull { it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == titleDigits }
        } else null

        val device = byNumber ?: devices.singleOrNull { device ->
            StatusParser.parse(device, body, DeviceStatus.empty(device.id)).recognized
        } ?: return

        if (!claimMessage("${device.id}|$body")) return
        repository.addLog(device.id, LogDirection.IN, body)
        reportBus.tryEmit(device.id to body)
    }

    /** False when this exact message was already handled by the other receiver. */
    private suspend fun claimMessage(key: String): Boolean = recentMutex.withLock {
        val now = System.currentTimeMillis()
        recent.entries.removeAll { now - it.value > DEDUPE_WINDOW_MS }
        if (recent.containsKey(key)) return@withLock false
        recent[key] = now
        while (recent.size > MAX_RECENT) {
            recent.remove(recent.keys.first())
        }
        true
    }

    private companion object {
        const val DEDUPE_WINDOW_MS = 15_000L
        const val MAX_RECENT = 32
    }
}
