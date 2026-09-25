package ir.tivan.controller.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * A real Persian neural voice embedded directly in the app — not Android's
 * system TextToSpeech, which most phones simply have no Persian voice
 * installed for (that was the previous, unfixable approach: it depended on
 * whatever engine/language the phone happened to have). This wraps
 * sherpa-onnx (see `libs/sherpa-onnx-1.13.8.aar` and the vendored JNI
 * bindings in `com.k2fsa.sherpa.onnx`) running an offline Piper VITS model
 * for Persian (`assets/tts-fa/`, model card: fa-haaniye_low), so speech
 * synthesis happens on-device with no network call and no dependency on
 * what the phone ships.
 *
 * Model + espeak-ng-data load takes a moment, so init happens once on a
 * background thread; [speak] calls made before that finishes are queued.
 */
object TivanSpeaker {
    private const val TAG = "TivanSpeaker"
    private const val ASSET_DIR = "tts-fa"
    private const val MODEL_FILE = "fa-haaniye_low.onnx"
    private const val TOKENS_FILE = "tokens.txt"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tts: OfflineTts? = null
    /** Set once loading finishes — null means "still loading", not "failed". */
    private var initFailed: Boolean? = null
    private val pending = mutableListOf<String>()
    private var track: AudioTrack? = null

    fun init(context: Context) {
        if (tts != null || initFailed != null) return
        val app = context.applicationContext
        scope.launch {
            try {
                val dataDir = copyEspeakData(app)
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = "$ASSET_DIR/$MODEL_FILE",
                            tokens = "$ASSET_DIR/$TOKENS_FILE",
                            dataDir = dataDir
                        ),
                        numThreads = 2,
                        debug = false,
                        provider = "cpu"
                    )
                )
                val engine = OfflineTts(assetManager = app.assets, config = config)
                initAudioTrack(engine.sampleRate())
                synchronized(pending) {
                    tts = engine
                    initFailed = false
                }
                flushPending()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load embedded TTS engine", e)
                initFailed = true
            }
        }
    }

    /** null = still loading, true = ready to speak, false = failed to load (corrupt/missing assets). */
    fun isAvailable(): Boolean? = initFailed?.let { !it }

    fun speak(text: String) {
        if (text.isBlank()) return
        val engine = tts
        if (engine != null) {
            speakNow(engine, text)
        } else if (initFailed != true) {
            synchronized(pending) {
                pending.add(text)
                if (pending.size > 20) pending.removeAt(0)
            }
        }
    }

    private fun flushPending() {
        val engine = tts ?: return
        val queued = synchronized(pending) { pending.toList().also { pending.clear() } }
        queued.forEach { speakNow(engine, it) }
    }

    private fun speakNow(engine: OfflineTts, text: String) {
        scope.launch {
            try {
                val audio = engine.generateWithConfig(text, GenerationConfig())
                if (audio.samples.isNotEmpty()) {
                    track?.apply {
                        stop()
                        flush()
                        write(audio.samples, 0, audio.samples.size, AudioTrack.WRITE_BLOCKING)
                        play()
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "TTS synthesis failed for \"$text\"", e)
            }
        }
    }

    private fun initAudioTrack(sampleRate: Int) {
        val bufLength = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        track = AudioTrack(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(sampleRate)
                .build(),
            bufLength.coerceAtLeast(sampleRate), // several seconds of headroom for one short phrase
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    /**
     * espeak-ng (a plain C library) needs real files on disk, not Android's
     * compressed asset archive — copied once to internal storage and reused
     * on every later launch.
     */
    private fun copyEspeakData(context: Context): String {
        val destRoot = File(context.filesDir, ASSET_DIR)
        val marker = File(destRoot, ".copied")
        if (marker.exists()) return File(destRoot, "espeak-ng-data").absolutePath

        destRoot.mkdirs()
        copyAssetDir(context, "$ASSET_DIR/espeak-ng-data", File(destRoot, "espeak-ng-data"))
        marker.createNewFile()
        return File(destRoot, "espeak-ng-data").absolutePath
    }

    private fun copyAssetDir(context: Context, assetPath: String, destDir: File) {
        val entries = context.assets.list(assetPath) ?: emptyArray()
        if (entries.isEmpty()) {
            destDir.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destDir).use { output -> input.copyTo(output) }
            }
            return
        }
        destDir.mkdirs()
        entries.forEach { name -> copyAssetDir(context, "$assetPath/$name", File(destDir, name)) }
    }
}
