package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural fruit fly wingbeat buzz sound synthesized using native AudioTrack.
 * Fundamental Drosophila wingbeat is ~220 Hz with rich harmonic content and amplitude flutter.
 */
class AudioBuzzer {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private val lock = Any()

    init {
        try {
            val sampleRate = 22050
            val numSamples = sampleRate // 1 second buffer
            val samples = ShortArray(numSamples)

            val f0 = 220.0
            val twoPi = 2.0 * PI

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // 18 Hz subtle flight wobble amplitude modulation
                val flutter = 0.82 + 0.18 * sin(twoPi * 18.0 * t)

                // Harmonics of Drosophila flight tone
                val wave = 0.45 * sin(twoPi * f0 * t) +
                        0.30 * sin(twoPi * 2.0 * f0 * t) +
                        0.18 * sin(twoPi * 3.0 * f0 * t) +
                        0.12 * sin(twoPi * 4.0 * f0 * t) +
                        0.08 * sin(twoPi * 5.0 * f0 * t) +
                        0.05 * sin(twoPi * 6.0 * f0 * t)

                val pcm = (wave * flutter * 12000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                samples[i] = pcm
            }

            val bufferSizeInBytes = samples.size * 2
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
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.setLoopPoints(0, samples.size, -1)
            audioTrack = track
        } catch (e: Exception) {
            Log.e("AudioBuzzer", "Error initializing AudioTrack", e)
        }
    }

    fun setBuzzing(buzzing: Boolean) {
        synchronized(lock) {
            val track = audioTrack ?: return
            try {
                if (buzzing && !isPlaying) {
                    if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        track.play()
                    }
                    isPlaying = true
                } else if (!buzzing && isPlaying) {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.pause()
                        track.reloadStaticData()
                    }
                    isPlaying = false
                }
            } catch (e: Exception) {
                Log.e("AudioBuzzer", "Error controlling buzz audio", e)
            }
        }
    }

    fun release() {
        synchronized(lock) {
            try {
                audioTrack?.pause()
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
                isPlaying = false
            } catch (e: Exception) {
                Log.e("AudioBuzzer", "Error releasing AudioTrack", e)
            }
        }
    }
}
