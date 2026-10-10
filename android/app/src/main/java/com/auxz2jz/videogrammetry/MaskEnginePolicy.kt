package com.auxz2jz.videogrammetry

/** Engines are explicit alternatives; combining masks requires verified support. */
object MaskEnginePolicy {
    const val BOX="BOX_BASELINE"
    const val GRABCUT="GRABCUT"
    const val LOW_TEXTURE="LOW_TEXTURE"
    const val CONSENSUS="CONSENSUS_INTERSECTION"
    val engineNames=listOf(BOX,GRABCUT,LOW_TEXTURE,CONSENSUS)
    fun acceptable(maskPixels:Int,roiPixels:Int):Boolean =
        roiPixels>=100 && maskPixels >= 20 &&
            maskPixels.toDouble()/roiPixels in 0.02..0.95
    fun selectMask(roi:Boolean,grabcut:Boolean,lowTexture:Boolean,engine:String):Boolean =
        roi && when(engine) {
            BOX -> true
            GRABCUT -> grabcut
            LOW_TEXTURE -> lowTexture
            CONSENSUS -> grabcut && lowTexture
            else -> false
        }
    fun iou(shared:Int,totalA:Int,totalB:Int):Double {
        val total=totalA+totalB-shared
        return if(total<=0)0.0 else shared.toDouble()/total
    }
    /** Never silently merge independent 3D coordinates, even if filenames differ. */
    fun canFuse3d(sameRun:Boolean,sceneAligned:Boolean,
        poseVerified:Boolean,maskValid:Boolean):Boolean =
        sameRun && sceneAligned && poseVerified && maskValid
}
