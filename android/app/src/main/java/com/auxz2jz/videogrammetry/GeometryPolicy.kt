package com.auxz2jz.videogrammetry

import kotlin.math.ceil

/** Pure, independently unit-testable geometric verdicts; no pose/3D claim. */
object GeometryPolicy {
    const val MAX_ANALYZED_PAIRS = 80
    const val MIN_CORRESPONDENCES = 12
    const val MIN_INLIERS = 12
    const val MIN_INLIER_FRACTION = 0.30

    /** All samples represent real saved frames. Downsample *pairs*, not image content. */
    fun selectedIndices(frameCount: Int, maxPairs: Int = MAX_ANALYZED_PAIRS): List<Int> {
        if (frameCount <= 0) return emptyList()
        if (frameCount == 1) return listOf(0)
        require(maxPairs > 0)
        val step = ceil((frameCount - 1).toDouble() / maxPairs).toInt().coerceAtLeast(1)
        val indices = (0 until frameCount step step).toMutableList()
        if (indices.last() != frameCount - 1) indices.add(frameCount - 1)
        return indices
    }

    fun verdict(featuresA: Int, featuresB: Int, matches: Int, inliers: Int): String {
        if (featuresA < MIN_CORRESPONDENCES || featuresB < MIN_CORRESPONDENCES)
            return "TOO_FEW_FEATURES"
        if (matches < MIN_CORRESPONDENCES) return "INSUFFICIENT_MATCHES"
        if (inliers < MIN_INLIERS || inliers.toDouble() / matches < MIN_INLIER_FRACTION)
            return "RANSAC_REJECTED"
        return "EPIPOLAR_CONSISTENT"
    }

    fun inlierFraction(matches: Int, inliers: Int): Double? =
        if (matches <= 0) null else inliers.coerceIn(0, matches).toDouble() / matches
}
