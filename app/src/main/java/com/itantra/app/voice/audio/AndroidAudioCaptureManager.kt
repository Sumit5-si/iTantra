package com.itantra.app.voice.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android AudioCaptureManager implementation using AudioRecord.
 *
 * Config: 16 kHz sample rate, 16-bit PCM, single channel (MONO).
 * Frame size: 30ms (480 samples @ 16kHz) for lightweight VAD consumption.
 * Fully offline, never streams raw audio over the network.
 */
class AndroidAudioCaptureManager(
    private val context: Context
) : AudioCaptureManager {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val FRAME_SIZE_SAMPLES = 480 // 30ms at 16kHz
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _audioFrames = MutableSharedFlow<ShortArray>(extraBufferCapacity = 64)
    private var isCapturingActive = false

    override fun isAvailable(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isCapturing(): Boolean = isCapturingActive

    @SuppressLint("MissingPermission")
    override suspend fun start() {
        withContext(Dispatchers.IO) {
            if (isCapturingActive || !isAvailable()) return@withContext

            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = maxOf(minBufferSize, FRAME_SIZE_SAMPLES * 4)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.release()
                    audioRecord = null
                    return@withContext
                }

                audioRecord?.startRecording()
                isCapturingActive = true

                recordingJob = scope.launch(Dispatchers.IO) {
                    val bufferPool = Array(4) { ShortArray(FRAME_SIZE_SAMPLES) }
                    var poolIdx = 0
                    while (isActive && isCapturingActive) {
                        val currentBuffer = bufferPool[poolIdx]
                        val readCount = audioRecord?.read(currentBuffer, 0, FRAME_SIZE_SAMPLES) ?: -1
                        if (readCount > 0) {
                            _audioFrames.emit(if (readCount == FRAME_SIZE_SAMPLES) currentBuffer else currentBuffer.copyOf(readCount))
                            poolIdx = (poolIdx + 1) % bufferPool.size
                        }
                    }
                }
            } catch (_: Exception) {
                stop()
            }
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            isCapturingActive = false
            recordingJob?.cancel()
            recordingJob = null
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (_: Exception) {
                // Safe cleanup
            } finally {
                audioRecord = null
            }
        }
    }

    override fun audioFrames(): Flow<ShortArray> = _audioFrames.asSharedFlow()
}
