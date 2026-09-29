package com.itantra.app.core.performance

import android.os.SystemClock
import com.itantra.app.core.logging.ITantraLogger

/**
 * Performance monitor for tracking latencies and resource usage.
 *
 * Tracks:
 *  - Connection latency
 *  - Message transmission latency
 *  - STT/TTS latency
 *  - End-to-end latency
 *  - Memory/CPU usage
 *  - Model loading time
 *
 * Useful for SIH evaluation demonstrations.
 */
object PerformanceMonitor {

    private const val TAG = "Perf"

    data class LatencyRecord(
        val label: String,
        val startMs: Long,
        val endMs: Long
    ) {
        val durationMs: Long get() = endMs - startMs
    }

    private val records = mutableListOf<LatencyRecord>()

    fun startTimer(): Long = SystemClock.elapsedRealtime()

    fun recordLatency(label: String, startMs: Long) {
        val endMs = SystemClock.elapsedRealtime()
        val record = LatencyRecord(label, startMs, endMs)
        records.add(record)
        ITantraLogger.d(TAG, "$label: ${record.durationMs}ms")
    }

    fun trackConnectionLatency(startMs: Long, endMs: Long) {
        val record = LatencyRecord("Connection", startMs, endMs)
        records.add(record)
        ITantraLogger.d(TAG, "Connection latency: ${record.durationMs}ms")
    }

    fun trackMessageLatency(messageId: String, sendMs: Long, deliveredMs: Long) {
        val record = LatencyRecord("Message[$messageId]", sendMs, deliveredMs)
        records.add(record)
        ITantraLogger.d(TAG, "Message latency: ${record.durationMs}ms")
    }

    fun trackModelLoadTime(modelName: String, loadMs: Long) {
        ITantraLogger.d(TAG, "Model load [$modelName]: ${loadMs}ms")
    }

    fun getMemoryUsageMB(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024f * 1024f)
    }

    fun getRecords(): List<LatencyRecord> = records.toList()

    fun clearRecords() {
        records.clear()
    }
}
