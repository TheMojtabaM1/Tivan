package ir.tivan.controller.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_ADD
import java.util.Locale

/**
 * Thin wrapper over Android's built-in TextToSpeech engine, spoken in
 * Persian when a Persian voice is installed. This is the OS's own
 * (robotic-sounding) reader, not a cloud voice — no network call, no extra
 * latency, works offline. Lazily initialized on first use and kept alive
 * for the process lifetime; safe to call [speak] before init finishes since
 * utterances just queue.
 *
 * Most phones don't ship a Persian voice pack out of the box —
 * `setLanguage(fa-IR)` returning LANG_MISSING_DATA/LANG_NOT_SUPPORTED is the
 * common case, not the exception. Previously that left [ready] permanently
 * false and every announcement silently queued forever. Now a missing
 * Persian voice falls back to the engine's default language — still
 * announces the moment, just possibly with a non-Persian accent — instead
 * of staying mute.
 */
object TivanSpeaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<String>()

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val result = engine.setLanguage(Locale("fa", "IR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // No Persian voice installed — fall back to the device's
                    // default language rather than staying silent.
                    engine.setLanguage(Locale.getDefault())
                }
                ready = true
                pending.forEach { engine.speak(it, QUEUE_ADD, null, it.hashCode().toString()) }
                pending.clear()
            }
        }
    }

    fun speak(text: String) {
        val engine = tts
        if (engine != null && ready) {
            engine.speak(text, QUEUE_ADD, null, text.hashCode().toString())
        } else {
            pending.add(text)
            // Init failed or hasn't finished after a while — don't grow forever.
            if (pending.size > 20) pending.removeAt(0)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
