package com.simplestsoft.twostrokecalc.data.sensors

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AndroidAudioSampleSource {
    @Volatile
    var isRecording: Boolean = false
        private set

    private var audioRecord: AudioRecord? = null

    fun start(sampleRate: Int = 44100, frameSize: Int = 2048): Flow<FloatArray> = callbackFlow {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            close(IllegalStateException("AudioRecord buffer size unavailable"))
            return@callbackFlow
        }
        // Keep ~500 ms in the hardware buffer so a brief analysis stall does not overrun.
        val bufferSize = maxOf(minBuffer * 4, sampleRate)
        val recorder = createRecorder(sampleRate, bufferSize)
        if (recorder == null) {
            close(IllegalStateException("AudioRecord not initialized"))
            return@callbackFlow
        }
        audioRecord = recorder
        recorder.startRecording()
        isRecording = true
        val shortBuffer = ShortArray(frameSize.coerceAtLeast(1024))
        val readJob = launch(Dispatchers.IO) {
            while (isActive && isRecording) {
                val read = recorder.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0) {
                    val floats = FloatArray(read) { i -> shortBuffer[i] / 32768f }
                    send(floats)
                }
            }
        }
        awaitClose {
            readJob.cancel()
            stop()
        }
    }.buffer(capacity = 32, onBufferOverflow = BufferOverflow.SUSPEND)

    fun stop() {
        isRecording = false
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null
    }

    private fun createRecorder(sampleRate: Int, bufferSize: Int): AudioRecord? {
        val sources = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.MIC,
        )
        for (source in sources) {
            val recorder = runCatching {
                AudioRecord(
                    source,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                )
            }.getOrNull()
            if (recorder != null && recorder.state == AudioRecord.STATE_INITIALIZED) {
                return recorder
            }
            recorder?.release()
        }
        return null
    }
}
