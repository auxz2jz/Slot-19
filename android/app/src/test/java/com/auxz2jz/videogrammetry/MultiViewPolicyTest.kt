package com.auxz2jz.videogrammetry
import org.junit.Assert.*
import org.junit.Test

class MultiViewPolicyTest {
    @Test fun coversEntireAvailableVideoExceptAnchorAndSecondView() {
        val all=MultiViewPolicy.extraFrames(300,6)
        assertEquals(298,all.size)
        assertEquals((1 until 300).filter{it!=6},all)
        assertEquals((1 until 7).filter{it!=6},MultiViewPolicy.extraFrames(7,6))
        assertEquals(300,MultiViewPolicy.extraFrames(800,6).size)
        assertEquals(300,MultiViewPolicy.extraFrames(800,6).toSet().size)
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
