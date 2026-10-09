package com.auxz2jz.videogrammetry

/** Stable two-source preview before 3D exists. The pair can be revised later. */
object EarlyObjectFocusPolicy {
    fun sourcePair(frameCount: Int): Pair<Int,Int> {
        require(frameCount >= 2) { "At least two saved photographs needed" }
        return 0 to minOf(6,frameCount-1)
    }
    fun selectionValid(first: FocusRect, second: FocusRect): Boolean =
        first.valid() && second.valid()
    fun candidateLabel(pointCount: Int): String =
        if(pointCount > 0) "EARLY_OBJECT_FOCUS_CANDIDATE" else "INCONCLUSIVE"
}
