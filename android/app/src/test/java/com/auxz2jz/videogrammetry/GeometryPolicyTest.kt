package com.auxz2jz.videogrammetry

import org.junit.Assert.*
import org.junit.Test

class GeometryPolicyTest {
    @Test fun downsampleWithoutOmittingLastFrame() {
        assertEquals(emptyList<Int>(), GeometryPolicy.selectedIndices(0))
        assertEquals(listOf(0), GeometryPolicy.selectedIndices(1))
        assertEquals(listOf(0, 1), GeometryPolicy.selectedIndices(2))
        val indices = GeometryPolicy.selectedIndices(300)
        assertEquals(0, indices.first())
        assertEquals(299, indices.last())
        assertTrue(indices.size - 1 <= 80)
        assertTrue(indices.zipWithNext().all { it.first < it.second })
    }

    @Test fun verdictsAreConservative() {
        assertEquals("TOO_FEW_FEATURES", GeometryPolicy.verdict(5, 100, 80, 70))
        assertEquals("INSUFFICIENT_MATCHES", GeometryPolicy.verdict(250, 200, 7, 7))
        assertEquals("RANSAC_REJECTED", GeometryPolicy.verdict(400, 400, 40, 8))
        assertEquals("RANSAC_REJECTED", GeometryPolicy.verdict(400, 400, 40, 11))
        assertEquals("EPIPOLAR_CONSISTENT", GeometryPolicy.verdict(400, 400, 40, 19))
    }

    @Test fun ratioDenominatorIsMatchesNotAllFeatures() {
        assertNull(GeometryPolicy.inlierFraction(0, 0))
        assertEquals(0.4, GeometryPolicy.inlierFraction(50, 20)!!, 0.0001)
        assertEquals(1.0, GeometryPolicy.inlierFraction(50, 100)!!, 0.0001)
    }
}
