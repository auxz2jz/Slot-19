package com.auxz2jz.videogrammetry
import org.junit.Assert.*
import org.junit.Test

class ThirdViewPolicyTest {
    @Test fun viewsAlwaysLaterAndBounded() {
        assertTrue(ThirdViewPolicy.thirdIndices(7,6).isEmpty())
        val indices=ThirdViewPolicy.thirdIndices(300,6)
        assertTrue(indices.size <= 4)
        assertTrue(indices.all { it>6 && it<300 })
        assertEquals(indices.distinct(),indices)
    }
    @Test fun poseThresholdsAreConservative() {
        assertEquals("INSUFFICIENT_THREE_VIEW_TRACKS",ThirdViewPolicy.verdict(4,4,0.1))
        assertEquals("PNP_RANSAC_REJECTED",ThirdViewPolicy.verdict(30,8,0.1))
        assertEquals("PNP_RANSAC_REJECTED",ThirdViewPolicy.verdict(30,12,0.1))
        assertEquals("THIRD_VIEW_REPROJECTION_TOO_HIGH",ThirdViewPolicy.verdict(30,22,9.0))
        assertEquals("THIRD_VIEW_CONSISTENT",ThirdViewPolicy.verdict(30,22,1.5))
    }
    @Test fun mismatchBlocksBlindCalibration() {
        assertEquals("INCOMPATIBLE_ASPECT_RATIO",
            ThirdViewPolicy.compatibility(1000,467,720,1280))
        assertEquals("ASPECT_ONLY_MATCH_LENS_CROP_UNVERIFIED",
            ThirdViewPolicy.compatibility(4000,3000,1000,750))
        assertEquals("UNKNOWN_IMAGE_DIMENSIONS",
            ThirdViewPolicy.compatibility(0,467,720,1280))
    }
}
