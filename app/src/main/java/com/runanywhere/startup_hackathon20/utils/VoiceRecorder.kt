package com.runanywhere.startup_hackathon20.utils

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs

/**
 * Voice Recorder for SafeSphere
 * Handles audio capture, amplitude monitoring, and text-to-speech
 */
class VoiceRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val audioBuffer = ShortArray(BUFFER_SIZE)

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        initializeTextToSpeech()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                isTtsInitialized = true
            }
        }
    }

    /**
     * Start recording audio and monitoring amplitude
     */
    suspend fun startRecording(onAmplitudeUpdate: (Float) -> Unit): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize
                )

                audioRecord?.startRecording()
                isRecording = true

                val audioData = mutableListOf<Short>()

                while (isRecording) {
                    val bytesRead = audioRecord?.read(audioBuffer, 0, BUFFER_SIZE) ?: 0

                    if (bytesRead > 0) {
                        // Calculate amplitude for visualization
                        val amplitude = calculateAmplitude(audioBuffer, bytesRead)
                        _amplitude.value = amplitude
                        onAmplitudeUpdate(amplitude)

                        // Store audio data
                        audioData.addAll(audioBuffer.take(bytesRead))
                    }
                }

                // Convert to byte array
                val result = ByteArray(audioData.size * 2)
                for (i in audioData.indices) {
                    result[i * 2] = (audioData[i].toInt() and 0xff).toByte()
                    result[i * 2 + 1] = ((audioData[i].toInt() shr 8) and 0xff).toByte()
                }
                result
            } catch (e: Exception) {
                e.printStackTrace()
                ByteArray(0)
            }
        }
    }

    /**
     * Stop recording
     */
    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _amplitude.value = 0f
    }

    /**
     * Calculate amplitude from audio buffer for visualization
     */
    private fun calculateAmplitude(buffer: ShortArray, size: Int): Float {
        var sum = 0L
        for (i in 0 until size) {
            sum += abs(buffer[i].toInt())
        }
        val avg = sum / size.toFloat()
        return (avg / Short.MAX_VALUE).coerceIn(0f, 1f)
    }

    /**
     * Speak text using Android TTS
     */
    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isTtsInitialized) {
            onComplete()
            return
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                onComplete()
            }

            override fun onError(utteranceId: String?) {
                onComplete()
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SafeSphere_TTS")
    }

    /**
     * Stop speaking
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    /**
     * Check if TTS is speaking
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    /**
     * Clean up resources
     */
    fun release() {
        stopRecording()
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 1024
    }
}
