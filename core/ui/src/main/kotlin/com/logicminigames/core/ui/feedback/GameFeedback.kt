package com.logicminigames.core.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

interface GameFeedback {
    fun onCellSelect()
    fun onDigitPlace()
    fun onNoteToggle()
    fun onMistake()
    fun onHint()
    fun onUnitCompleted()
    fun onVictory()
    fun release() = Unit
}

object NoOpGameFeedback : GameFeedback {
    override fun onCellSelect() = Unit
    override fun onDigitPlace() = Unit
    override fun onNoteToggle() = Unit
    override fun onMistake() = Unit
    override fun onHint() = Unit
    override fun onUnitCompleted() = Unit
    override fun onVictory() = Unit
}

/**
 * Tactile haptic and subtle synthesized offline sound feedback (both disabled by default).
 */
class AndroidGameFeedback(
    context: Context,
    private val hapticsEnabledProvider: () -> Boolean,
    private val soundEnabledProvider: () -> Boolean = { false },
) : GameFeedback {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private val soundPlayer = ProceduralSoundPlayer()

    override fun onCellSelect() {
        pulse(durationMs = 10L, amplitude = 40)
        playSound(ToneType.SELECT)
    }

    override fun onDigitPlace() {
        pulse(durationMs = 18L, amplitude = 85)
        playSound(ToneType.PLACE)
    }

    override fun onNoteToggle() {
        pulse(durationMs = 12L, amplitude = 55)
        playSound(ToneType.NOTE)
    }

    override fun onMistake() {
        if (canVibrate()) {
            runCatching {
                val pattern = longArrayOf(0L, 28L, 35L, 42L)
                val amps = intArrayOf(0, 140, 0, 180)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
            }
        }
        playSound(ToneType.MISTAKE)
    }

    override fun onHint() {
        pulse(durationMs = 22L, amplitude = 100)
        playSound(ToneType.HINT)
    }

    override fun onUnitCompleted() {
        pulse(durationMs = 30L, amplitude = 125)
        playSound(ToneType.UNIT)
    }

    override fun onVictory() {
        if (canVibrate()) {
            runCatching {
                val pattern = longArrayOf(0L, 25L, 40L, 30L, 45L, 60L)
                val amps = intArrayOf(0, 110, 0, 150, 0, 200)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
            }
        }
        playSound(ToneType.VICTORY)
    }

    override fun release() {
        soundPlayer.release()
    }

    private fun canVibrate(): Boolean =
        hapticsEnabledProvider() && (vibrator?.hasVibrator() == true)

    private fun pulse(durationMs: Long, amplitude: Int) {
        if (!canVibrate()) return
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        }
    }

    private fun playSound(type: ToneType) {
        if (!soundEnabledProvider()) return
        soundPlayer.play(type)
    }
}

private enum class ToneType {
    SELECT,
    PLACE,
    NOTE,
    MISTAKE,
    HINT,
    UNIT,
    VICTORY,
}

private class ProceduralSoundPlayer {
    private val sampleRate = 22_050
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sudoku-sfx").apply { isDaemon = true }
    }
    private val cache = HashMap<ToneType, ByteArray>(7)
    private var activeTrack: AudioTrack? = null
    @Volatile
    private var released = false

    fun play(type: ToneType) {
        if (released) return
        runCatching {
            executor.execute {
                if (released) return@execute
                val pcm = cache.getOrPut(type) { synthesize(type) }
                playPcm(pcm)
            }
        }
    }

    fun release() {
        released = true
        runCatching {
            executor.execute {
                runCatching {
                    activeTrack?.stop()
                    activeTrack?.release()
                }
                activeTrack = null
                executor.shutdown()
            }
        }
    }

    private fun playPcm(pcm: ByteArray) {
        runCatching {
            activeTrack?.let { prev ->
                runCatching {
                    prev.stop()
                    prev.release()
                }
                activeTrack = null
            }
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(pcm.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            activeTrack = track
        }
    }

    private fun synthesize(type: ToneType): ByteArray = when (type) {
        ToneType.SELECT -> renderSequence(listOf(ToneSegment(440.0, 24, 0.14)))
        ToneType.PLACE -> renderSequence(listOf(ToneSegment(587.33, 42, 0.22)))
        ToneType.NOTE -> renderSequence(listOf(ToneSegment(783.99, 28, 0.15)))
        ToneType.MISTAKE -> renderSequence(
            listOf(
                ToneSegment(220.0, 48, 0.24),
                ToneSegment(174.61, 60, 0.24),
            ),
        )
        ToneType.HINT -> renderSequence(listOf(ToneSegment(659.25, 65, 0.20)))
        ToneType.UNIT -> renderSequence(
            listOf(
                ToneSegment(523.25, 58, 0.20),
                ToneSegment(659.25, 76, 0.22),
            ),
        )
        ToneType.VICTORY -> renderSequence(
            listOf(
                ToneSegment(523.25, 60, 0.22),
                ToneSegment(659.25, 60, 0.22),
                ToneSegment(783.99, 65, 0.22),
                ToneSegment(1046.50, 110, 0.24),
            ),
        )
    }

    private data class ToneSegment(
        val freqHz: Double,
        val durationMs: Int,
        val amplitude: Double,
    )

    private fun renderSequence(segments: List<ToneSegment>): ByteArray {
        val totalSamples = segments.sumOf { (sampleRate * it.durationMs) / 1000 }
        val pcm = ByteArray(totalSamples * 2)
        var offset = 0
        for (segment in segments) {
            val count = (sampleRate * segment.durationMs) / 1000
            val attackSamples = (count * 0.12).coerceAtLeast(1.0)
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate
                val attack = (i / attackSamples).coerceAtMost(1.0)
                val decay = exp(-4.5 * i / count.toDouble())
                val sample = (sin(2.0 * PI * segment.freqHz * t) * segment.amplitude * attack * decay * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[offset++] = (sample and 0xFF).toByte()
                pcm[offset++] = ((sample ushr 8) and 0xFF).toByte()
            }
        }
        return pcm
    }
}
