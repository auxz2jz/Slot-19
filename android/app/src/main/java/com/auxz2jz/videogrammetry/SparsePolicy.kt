package com.auxz2jz.videogrammetry

/** Conservative, independent verdicts: a two-view cloud is NOT a metrically accurate scan. */
object SparsePolicy {
    const val MIN_RATIO_MATCHES = 35
    const val MIN_POSE_INLIERS = 24
    const val MIN_POINTS = 24
    const val MIN_MEDIAN_PARALLAX_DEG = 0.8
    const val MAX_MEDIAN_PARALLAX_DEG = 30.0
    const val MAX_REPROJECTION_ERROR_PX = 3.0

    fun candidatePairs(frameCount: Int): List<Pair<Int, Int>> {
        if (frameCount < 2) return emptyList()
        return listOf(1, 2, 3, 4, 6, 9, 13, 19, 27)
            .map { it.coerceAtMost(frameCount - 1) }.distinct()
            .take(7).map { 0 to it }
    }
    fun verdict(matches: Int, poseInliers: Int, triangulated: Int,
        medianParallaxDeg: Double, medianReprojectionPx: Double
    ): String {
        if (matches < MIN_RATIO_MATCHES) return "INSUFFICIENT_MATCHES"
        if (poseInliers < MIN_POSE_INLIERS) return "INSUFFICIENT_POSE_INLIERS"
        if (!medianParallaxDeg.isFinite() ||
            medianParallaxDeg < MIN_MEDIAN_PARALLAX_DEG) return "LOW_PARALLAX"
        if (medianParallaxDeg > MAX_MEDIAN_PARALLAX_DEG)
            return "EXCESSIVE_VIEW_CHANGE"
        if (triangulated < MIN_POINTS) return "INSUFFICIENT_VALID_3D_POINTS"
        if (!medianReprojectionPx.isFinite() ||
            medianReprojectionPx > MAX_REPROJECTION_ERROR_PX)
            return "HIGH_REPROJECTION_ERROR"
        return "TWO_VIEW_SPARSE_CANDIDATE"
    }

    fun asciiPly(points: List<SparseVertex>): String {
        val result = StringBuilder()
            .append("ply\nformat ascii 1.0\n")
            .append("comment EXPERIMENTAL two-view sparse point set\n")
            .append("comment relative units, unknown scale; estimated camera intrinsics\n")
            .append("element vertex ").append(points.size).append("\n")
            .append("property float x\nproperty float y\nproperty float z\n")
            .append("property uchar red\nproperty uchar green\nproperty uchar blue\nend_header\n")
        for (v in points) {
            require(v.x.isFinite() && v.y.isFinite() && v.z.isFinite())
            result.append("%.6f %.6f %.6f %d %d %d\n".format(
                java.util.Locale.US, v.x, v.y, v.z,
                v.red.coerceIn(0, 255), v.green.coerceIn(0, 255), v.blue.coerceIn(0, 255)))
        }
        return result.toString()
    }
}
data class SparseVertex(val x: Double, val y: Double, val z: Double,
    val red: Int, val green: Int, val blue: Int)
