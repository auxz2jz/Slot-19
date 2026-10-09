package com.auxz2jz.videogrammetry
import org.junit.Assert.*
import org.junit.Test

class MultiViewPolicyTest {
    @Test fun boundedExtraFramesExcludeBaseline() {
        val a=MultiViewPolicy.extraFrames(300,6)
        assertTrue(a.isNotEmpty())
        assertTrue(a.size<=8)
        assertTrue(a.all { it>6 && it<300 })
        assertTrue(MultiViewPolicy.extraFrames(7,6).isEmpty())
    }
    @Test fun requiresRobustRegistrationAndValidNewPoint() {
        assertTrue(MultiViewPolicy.pnpAccepted(30,20,1.4))
        assertFalse(MultiViewPolicy.pnpAccepted(30,8,1.0))
        assertFalse(MultiViewPolicy.pnpAccepted(30,20,4.0))
        assertFalse(MultiViewPolicy.pnpAccepted(12,10,Double.NaN))
        assertTrue(MultiViewPolicy.newPointAccepted(1.0,3.0))
        assertFalse(MultiViewPolicy.newPointAccepted(7.0,3.0))
        assertFalse(MultiViewPolicy.newPointAccepted(1.0,0.01))
    }
    @Test fun requiresMultipleNewCameraViewsForPositiveClaim() {
        assertEquals("INCONCLUSIVE",MultiViewPolicy.verdict(250,75,1))
        assertEquals("INCONCLUSIVE",MultiViewPolicy.verdict(250,0,3))
        assertEquals("MULTIVIEW_SPARSE_CANDIDATE",MultiViewPolicy.verdict(250,8,2))
    }
}
