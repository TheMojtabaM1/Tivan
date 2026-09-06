package ir.tivan.controller.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_ADD
import java.util.Locale

/**
 * Thin wrapper over Android's built-in TextToSpeech engine, spoken in
 * Persian. This is the OS's own (robotic-sounding) reader, not a cloud
 * voice — no network call, no extra latency, works offline. Lazily
 * initialized on first use and kept alive for the process lifetime; safe to
 * call [speak] before init finishes since utterances just queue.
 */
object TivanSpeaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<String>()

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("fa", "IR"))
                ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                if (ready) {
                    pending.forEach { tts?.speak(it, QUEUE_ADD, null, it.hashCode().toString()) }
                    pending.clear()
                }
            }
        }
    }

    fun speak(text: String) {
        val engine = tts
        if (engine != null && ready) {
            engine.speak(text, QUEUE_ADD, null, text.hashCode().toString())
        } else {
            pending.add(text)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
