package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

class SoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val trackSemaphore = Semaphore(2)
    var sfxVolume: Float = 0.75f
    var musicVolume: Float = 0.45f
    var isMuted: Boolean = false
    private var musicJob: Job? = null

    fun startAmbientMusic() {
        if (musicJob?.isActive == true) return
        musicJob = scope.launch {
            val notes = floatArrayOf(110f, 130.81f, 146.83f, 164.81f, 196.00f, 146.83f)
            var idx = 0
            while (isActive) {
                if (!isMuted && musicVolume > 0.02f) {
                    val freq = notes[idx % notes.size]
                    playSynthTone(
                        freqStart = freq,
                        freqEnd = freq * 1.005f,
                        durationMs = 480,
                        volume = musicVolume * 0.16f,
                        harmonic = 0.35f
                    )
                    idx++
                }
                delay(1600L)
            }
        }
    }

    fun stopAmbientMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun playBuildSound() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            playSynthTone(220f, 440f, 95, sfxVolume * 0.45f, harmonic = 0.5f)
        }
    }

    fun playRemoveSound() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            playSynthTone(340f, 150f, 110, sfxVolume * 0.4f, harmonic = 0.4f)
        }
    }

    fun playSellChime() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            playSynthTone(587.33f, 880f, 85, sfxVolume * 0.35f, harmonic = 0.25f)
        }
    }

    fun playResearchOrObjectiveComplete() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            playSynthTone(440f, 554.37f, 110, sfxVolume * 0.5f, harmonic = 0.3f)
            delay(95)
            playSynthTone(659.25f, 880f, 180, sfxVolume * 0.55f, harmonic = 0.4f)
        }
    }

    fun playAlertSound() {
        if (isMuted || sfxVolume <= 0.01f) return
        scope.launch {
            playSynthTone(190f, 140f, 140, sfxVolume * 0.5f, harmonic = 0.8f)
        }
    }

    private suspend fun playSynthTone(
        freqStart: Float,
        freqEnd: Float,
        durationMs: Int,
        volume: Float,
        harmonic: Float = 0.3f
    ) {
        if (!trackSemaphore.tryAcquire()) return
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationMs) / 1000
            if (numSamples <= 0) return
            val pcm = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                val freq = freqStart + (freqEnd - freqStart) * progress
                val t = i.toDouble() / sampleRate
                val envelope = exp(-3.2 * progress) * (1.0 - progress * 0.4)
                val wave = sin(2.0 * PI * freq * t) + harmonic * sin(4.0 * PI * freq * t)
                val normalized = (wave / (1.0 + harmonic)) * envelope * volume.coerceIn(0f, 1f)
                pcm[i] = (normalized * Short.MAX_VALUE * 0.65).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(pcm.size * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(pcm.size * 2, minBufBytes))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.write(pcm, 0, pcm.size)
                track.play()
                delay(durationMs.toLong() + 25L)
                runCatching { track.stop() }
            }
            runCatching { track.release() }
        } catch (_: Throwable) {
            // Safe fallback in headless JVM / Robolectric or restricted audio HAL
        } finally {
            trackSemaphore.release()
        }
    }

    fun release() {
        stopAmbientMusic()
        scope.cancel()
    }
}
