package com.example.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sin

object AudioSynth {
    private const val SAMPLE_RATE = 22050 // Lower sample rate is lighter and perfectly sufficient

    suspend fun playTone(frequency: Double, durationMs: Long) = withContext(Dispatchers.Default) {
        val numSamples = (durationMs * SAMPLE_RATE / 1000).toInt()
        val sample = DoubleArray(numSamples)
        val generatedSnd = ByteArray(2 * numSamples)

        // Generate a smooth sine wave with subtle fade-in / fade-out envelope to avoid pops
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / frequency)
            var amplitude = sin(angle)

            // Dynamic attack / decay envelope (fade in first 10%, fade out last 30%)
            val attackSamples = numSamples * 0.1
            val decaySamples = numSamples * 0.3
            if (i < attackSamples) {
                amplitude *= (i / attackSamples)
            } else if (i > numSamples - decaySamples) {
                amplitude *= ((numSamples - i) / decaySamples)
            }

            sample[i] = amplitude
        }

        var idx = 0
        for (dVal in sample) {
            val valShort = (dVal * 20000).toInt().toShort() // Keep volume soft and eye-safe
            generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
            generatedSnd[idx++] = (valShort.toInt() and 0xff00 ushr 8).toByte()
        }

        try {
            val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    generatedSnd.size,
                    AudioTrack.MODE_STATIC
                )
            }

            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            delay(durationMs + 50)
            audioTrack.release()
        } catch (e: Exception) {
            Log.e("AudioSynth", "Error playing synth tone", e)
        }
    }
}
