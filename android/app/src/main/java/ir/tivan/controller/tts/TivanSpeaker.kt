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
    private var usePcm16: Boolean = false
    /** The exception's own message, so a failure can actually be diagnosed instead of just reported as "doesn't work". */
    private var lastError: String? = null

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
                runCatching { initAudioTrack(engine.sampleRate()) }
                    .onFailure { Log.e(TAG, "Failed to init AudioTrack, TTS will be silent", it) }
                synchronized(pending) {
                    tts = engine
                    initFailed = false
                }
                flushPending()
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load embedded TTS engine", e)
                lastError = "${e.javaClass.simpleName}: ${e.message}"
                initFailed = true
            }
        }
    }

    /** null = still loading, true = ready to speak, false = failed to load (corrupt/missing assets). */
    fun isAvailable(): Boolean? = initFailed?.let { !it }

    /** The failure's own error text, for when [isAvailable] is false — null while still loading or once it succeeds. */
    fun failureReason(): String? = lastError

    /**
     * Wipes the copied espeak-ng-data (the one piece of the engine that
     * isn't read straight from the APK's assets, so it's the one piece
     * that can end up half-written — e.g. from a launch that was killed
     * mid-copy, back before the app stopped crashing on startup) and
     * retries loading from scratch.
     */
    fun resetAndRetry(context: Context) {
        tts = null
        initFailed = null
        lastError = null
        runCatching { File(context.applicationContext.filesDir, ASSET_DIR).deleteRecursively() }
        init(context)
    }

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
                        // MODE_STREAM only drains its internal buffer while playing, so a
                        // blocking write() issued before play() can only ever fill that one
                        // buffer (a couple of seconds) and then has nothing left to hand the
                        // rest of a longer phrase to — play() must start first so the track
                        // is actively consuming while the rest of write() blocks and feeds it.
                        play()
                        if (usePcm16) {
                            val pcm = ShortArray(audio.samples.size) { i ->
                                (audio.samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                            }
                            write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING)
                        } else {
                            write(audio.samples, 0, audio.samples.size, AudioTrack.WRITE_BLOCKING)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "TTS synthesis failed for \"$text\"", e)
            }
        }
    }

    private fun initAudioTrack(sampleRate: Int) {
        // getMinBufferSize returns ERROR (-1) or ERROR_BAD_VALUE (-2) when the
        // device's audio HAL rejects this encoding/rate/channel combo — on
        // some devices that happens for ENCODING_PCM_FLOAT, so fall back to
        // 16-bit PCM (and manually convert samples before writing) rather
        // than handing AudioTrack's constructor a negative buffer size.
        var floatMin = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        if (floatMin > 0) {
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
                floatMin.coerceAtLeast(sampleRate * 4), // several seconds of float-sample headroom for one short phrase
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            usePcm16 = false
            return
        }

        val pcm16Min = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val safeMin = if (pcm16Min > 0) pcm16Min else sampleRate * 2
        track = AudioTrack(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setSampleRate(sampleRate)
                .build(),
            safeMin.coerceAtLeast(sampleRate * 2), // several seconds of headroom for one short phrase
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        usePcm16 = true
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
