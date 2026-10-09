package com.auxz2jz.videogrammetry

/** Conservative bounded incremental multi-view settings; NOT bundle adjustment. */
object MultiViewPolicy {
    const val MIN_TRACKS=12
    const val MIN_PNP_INLIERS=10
    const val MAX_MEDIAN_PNP_ERROR_PX=3.0
    const val MAX_NEW_REPROJECTION_ERROR_PX=2.5
    const val MIN_ANGLE_DEG=0.8
    const val MAX_ANGLE_DEG=30.0
    const val MIN_NEW_POINTS=4
    const val MIN_SUPPORTING_EXTRA_VIEWS=2
    const val MAX_TOTAL_POINTS=5000

    fun extraFrames(count:Int, baseline:Int):List<Int> {
        if(count<baseline+2)return emptyList()
        return listOf(baseline+1,baseline+3,baseline+7,baseline+13,
            baseline+21,baseline+33,baseline+49,baseline+65)
            .filter { it<count }.distinct().take(8)
    }
    fun pnpAccepted(tracks:Int,inliers:Int,medianPx:Double):Boolean =
        tracks>=MIN_TRACKS && inliers>=MIN_PNP_INLIERS &&
            inliers.toDouble()/tracks>=0.45 &&
            medianPx.isFinite() && medianPx<=MAX_MEDIAN_PNP_ERROR_PX
    fun newPointAccepted(errorPx:Double,angleDeg:Double):Boolean =
        errorPx.isFinite() && errorPx<=MAX_NEW_REPROJECTION_ERROR_PX &&
            angleDeg.isFinite() && angleDeg in MIN_ANGLE_DEG..MAX_ANGLE_DEG

    fun verdict(base:Int,newPoints:Int,extraViews:Int):String =
        if(base>0 && newPoints>=MIN_NEW_POINTS &&
            extraViews>=MIN_SUPPORTING_EXTRA_VIEWS)
            "MULTIVIEW_SPARSE_CANDIDATE" else "INCONCLUSIVE"
}
