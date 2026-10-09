package com.auxz2jz.videogrammetry

/**
 * v0.1.0 fixed-interval sampling. Do not confuse this with intelligent
 * photogrammetry keyframe selection, which requires real geometric evidence.
 */
object FramePolicy {
    const val LIVE_INTERVAL_MS = 1200L
    const val LIVE_FRAME_LIMIT = 30
    const val IMPORT_INTERVAL_MS = 1000L
    const val IMPORT_FRAME_LIMIT = 40

    fun shouldAccept(nowMs: Long, lastMs: Long, intervalMs: Long): Boolean =
        intervalMs > 0 && (lastMs == Long.MIN_VALUE || nowMs >= lastMs && nowMs - lastMs >= intervalMs)
}
