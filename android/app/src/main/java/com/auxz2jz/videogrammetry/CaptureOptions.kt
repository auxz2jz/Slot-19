package com.auxz2jz.videogrammetry

import kotlin.math.roundToInt

/** Requested frame cadence is a target, never a guarantee of camera/decoder throughput. */
data class CaptureOptions(val targetFps: Double, val maxFrames: Int) {
    init {
        require(targetFps >= 0.5 && targetFps <= 5.0) { "Target FPS outside safe preview range" }
        require(maxFrames in 10..300) { "Frame count outside safe range" }
    }
    val intervalMs: Long get() = (1000.0 / targetFps).roundToInt().toLong().coerceAtLeast(200L)

    companion object {
        val LIVE_DEFAULT = CaptureOptions(1.0, 30)
        val VIDEO_DEFAULT = CaptureOptions(1.0, 40)
        val SMART_DEFAULT = CaptureOptions(0.5, 30)
        const val MAX_FRAMES = 300
        const val MAX_FPS = 5.0
    }
}

/** Guardrails and human-readable honest comparisons. */
object FrameRateAdvice {
    const val VIDEO = "Recommended: 1 frame/sec for a slow handheld recording. Compare 0.5, 1, 2 or 3 frames/sec using the same object. More frames do NOT guarantee more useful viewpoints."
    const val LIVE = "Recommended: 1 frame/sec for careful handheld movement. 2–3/sec can help when moving around detailed objects, but creates more files."
    const val SMART = "Recommended: at most 0.5 full-resolution photos/sec. Smart mode still waits for a new, clear viewpoint; this setting is a maximum shutter rate, NOT a forced frame rate."
    const val STORAGE = "High rates and large frame counts use more processing, battery and storage. Each run is saved separately."

    fun effectiveFps(sourceTimesMs: List<Long>): Double? {
        if (sourceTimesMs.size < 2) return null
        val span = sourceTimesMs.last() - sourceTimesMs.first()
        if (span <= 0) return null
        return (sourceTimesMs.size - 1) * 1000.0 / span
    }
}
