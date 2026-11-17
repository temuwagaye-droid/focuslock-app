package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val isProOnly: Boolean
) {
    NONE("none", "Silent", "🔇", false),
    WHITE_NOISE("white", "White Noise", "🌊", false),
    RAIN("rain", "Gentle Rain", "🌧️", true),
    BROWN_NOISE("brown", "Warm Focus", "🪵", true),
    FOREST_HUM("forest", "Zen Drone", "🍃", true);

    companion object {
        fun fromId(id: String?): AmbientSoundType {
            return entries.firstOrNull { it.id == id } ?: NONE
        }
    }
}

/**
 * Procedural real-time DSP ambient sound synthesizer using Android AudioTrack.
 * Works 100% offline with zero external audio files.
 */
object AmbientSoundPlayer {
    private const val SAMPLE_RATE = 22050

    private val scope = CoroutineScope(Dispatchers.Default)
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _currentSound = MutableStateFlow(AmbientSoundType.NONE)
    val currentSound: StateFlow<AmbientSoundType> = _currentSound.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _volume = MutableStateFlow(0.45f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun play(sound: AmbientSoundType, targetVolume: Float = _volume.value) {
        if (sound == AmbientSoundType.NONE) {
            stop()
            return
        }
        if (_isPlaying.value && _currentSound.value == sound) {
            setVolume(targetVolume)
            return
        }

        stopInternal()
        _currentSound.value = sound
        _volume.value = targetVolume.coerceIn(0f, 1f)
        _isPlaying.value = true

        playJob = scope.launch {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val track = try {
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
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Throwable) {
                null
            }

            if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                track?.release()
                _isPlaying.value = false
                return@launch
            }

            audioTrack = track
            try {
                track.play()
                val buffer = ShortArray(minBuf / 2)
                var brownLast = 0f
                var rainLP = 0f
                var phase1 = 0.0
                var phase2 = 0.0
                var dropletEnv = 0f

                while (isActive && _isPlaying.value) {
                    val vol = _volume.value
                    for (i in buffer.indices) {
                        val raw = when (sound) {
                            AmbientSoundType.WHITE_NOISE -> {
                                (Random.nextFloat() * 2f - 1f) * 0.28f
                            }
                            AmbientSoundType.BROWN_NOISE -> {
                                val white = Random.nextFloat() * 2f - 1f
                                brownLast = (brownLast + (0.04f * white)) / 1.04f
                                (brownLast * 2.2f).coerceIn(-0.6f, 0.6f)
                            }
                            AmbientSoundType.RAIN -> {
                                val white = Random.nextFloat() * 2f - 1f
                                rainLP = rainLP * 0.86f + white * 0.14f
                                if (Random.nextInt(900) == 0) dropletEnv = 0.35f
                                dropletEnv *= 0.992f
                                val drop = dropletEnv * (Random.nextFloat() * 2f - 1f) * 0.4f
                                (rainLP * 1.4f + drop).coerceIn(-0.6f, 0.6f)
                            }
                            AmbientSoundType.FOREST_HUM -> {
                                phase1 += 2.0 * PI * 110.0 / SAMPLE_RATE
                                phase2 += 2.0 * PI * 164.81 / SAMPLE_RATE
                                val wind = (Random.nextFloat() * 2f - 1f)
                                rainLP = rainLP * 0.95f + wind * 0.05f
                                val chord = (sin(phase1) * 0.14 + sin(phase2) * 0.09).toFloat()
                                (chord + rainLP * 0.7f).coerceIn(-0.6f, 0.6f)
                            }
                            AmbientSoundType.NONE -> 0f
                        }
                        buffer[i] = (raw * vol * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Throwable) {
                // Gracefully stop if audio focus/hardware disconnects
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {
                }
                if (audioTrack === track) audioTrack = null
            }
        }
    }

    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
    }

    fun stop() {
        stopInternal()
        _currentSound.value = AmbientSoundType.NONE
    }

    private fun stopInternal() {
        _isPlaying.value = false
        playJob?.cancel()
        playJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Throwable) {
        }
        audioTrack = null
    }
}
