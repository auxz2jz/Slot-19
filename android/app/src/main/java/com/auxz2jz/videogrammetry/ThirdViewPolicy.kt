package com.auxz2jz.videogrammetry

import kotlin.math.abs

/**
 * Third-view validation is independent of two-view sparse candidate creation.
 * Estimated intrinsics never become a measured camera model.
 */
object ThirdViewPolicy {
    const val MIN_CORRESPONDENCES = 12
    const val MIN_RANSAC_INLIERS = 10
    const val MAX_MEDIAN_REPROJECTION_PX = 3.0
    const val MIN_INLIER_RATIO = 0.45
    const val MAX_THIRD_CHECKS = 4

    fun thirdIndices(frameCount: Int, secondIndex: Int): List<Int> {
        if (secondIndex >= frameCount - 1) return emptyList()
        return listOf(secondIndex+1,secondIndex+3,secondIndex+7,secondIndex+15,
            secondIndex+30).filter { it < frameCount }.distinct().take(MAX_THIRD_CHECKS)
    }

    fun verdict(correspondences: Int, inliers: Int, medianError: Double): String {
        if (correspondences < MIN_CORRESPONDENCES) return "INSUFFICIENT_THREE_VIEW_TRACKS"
        if (inliers < MIN_RANSAC_INLIERS || inliers.toDouble()/correspondences < MIN_INLIER_RATIO)
            return "PNP_RANSAC_REJECTED"
        if (!medianError.isFinite() || medianError > MAX_MEDIAN_REPROJECTION_PX)
            return "THIRD_VIEW_REPROJECTION_TOO_HIGH"
        return "THIRD_VIEW_CONSISTENT"
    }

    fun compatibility(calWidth: Int, calHeight: Int,
        frameWidth: Int, frameHeight: Int): String {
        if (calWidth <= 0 || calHeight <= 0 || frameWidth <= 0 || frameHeight <= 0)
            return "UNKNOWN_IMAGE_DIMENSIONS"
        val a = maxOf(calWidth,calHeight).toDouble()/minOf(calWidth,calHeight)
        val b = maxOf(frameWidth,frameHeight).toDouble()/minOf(frameWidth,frameHeight)
        if (abs(a-b)/b > 0.01) return "INCOMPATIBLE_ASPECT_RATIO"
        return "ASPECT_ONLY_MATCH_LENS_CROP_UNVERIFIED"
    }
}
