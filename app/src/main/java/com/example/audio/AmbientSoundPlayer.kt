package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(
    val id: String,
    val displayName: String,
    val emoji: String,
    val isProOnly: Boolean = false
) {
    NONE("none", "Silent", "🔇", false),
    WHITE_NOISE("white_noise", "White Noise", "🌬️", false),
    RAIN("rain", "Rain", "🌧️", true),
    BROWN_NOISE("brown_noise", "Brown Noise", "🌊", true),
    FOREST_HUM("forest_hum", "Zen Drone", "🌿", true);

    companion object {
        fun fromId(id: String): AmbientSoundType =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

object AmbientSoundPlayer {
    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _currentSound = MutableStateFlow(AmbientSoundType.NONE)
    val currentSound: StateFlow<AmbientSoundType> = _currentSound.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _volume = MutableStateFlow(0.65f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun play(sound: AmbientSoundType, initialVolume: Float = _volume.value) {
        _volume.value = initialVolume.coerceIn(0f, 1f)
        if (sound == AmbientSoundType.NONE) {
            stop()
            return
        }
        stopInternal()
        _currentSound.value = sound
        _isPlaying.value = true

        playJob = playerScope.launch {
            val sampleRate = 22050
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull()

            if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                _isPlaying.value = false
                return@launch
            }

            audioTrack = track
            runCatching {
                track.setVolume(_volume.value)
                track.play()
            }

            val samples = ShortArray(2048)
            var lastBrown = 0.0
            var lastRain = 0.0
            var phase = 0.0
            while (isActive && _isPlaying.value) {
                for (i in samples.indices) {
                    val v = when (sound) {
                        AmbientSoundType.WHITE_NOISE -> (Random.nextDouble() * 2.0 - 1.0) * 0.14
                        AmbientSoundType.BROWN_NOISE -> {
                            val w = Random.nextDouble() * 2.0 - 1.0
                            lastBrown = (lastBrown + 0.02 * w) / 1.02
                            (lastBrown * 1.9).coerceIn(-1.0, 1.0) * 0.4
                        }
                        AmbientSoundType.RAIN -> {
                            val w = Random.nextDouble() * 2.0 - 1.0
                            lastRain = lastRain * 0.86 + w * 0.14
                            val drop = if (Random.nextDouble() < 0.002) (Random.nextDouble() * 0.35) else 0.0
                            (lastRain + drop).coerceIn(-1.0, 1.0) * 0.35
                        }
                        AmbientSoundType.FOREST_HUM -> {
                            phase += (2.0 * PI * 110.0) / sampleRate
                            if (phase > 2.0 * PI) phase -= 2.0 * PI
                            val drone = sin(phase) * 0.14 + sin(phase * 1.5) * 0.07
                            val breeze = (Random.nextDouble() * 2.0 - 1.0) * 0.03
                            drone + breeze
                        }
                        AmbientSoundType.NONE -> 0.0
                    }
                    samples[i] = (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                val written = runCatching { track.write(samples, 0, samples.size) }.getOrDefault(-1)
                if (written < 0) break
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _volume.value = clamped
        runCatching { audioTrack?.setVolume(clamped) }
    }

    fun stop() {
        stopInternal()
        _currentSound.value = AmbientSoundType.NONE
        _isPlaying.value = false
    }

    private fun stopInternal() {
        _isPlaying.value = false
        playJob?.cancel()
        playJob = null
        val t = audioTrack
        audioTrack = null
        if (t != null) {
            runCatching {
                t.pause()
                t.flush()
                t.stop()
                t.release()
            }
        }
    }
}
