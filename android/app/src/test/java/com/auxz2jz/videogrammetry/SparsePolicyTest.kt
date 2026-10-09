package com.auxz2jz.videogrammetry

import org.junit.Assert.*
import org.junit.Test

class SparsePolicyTest {
    @Test fun pairsBounded() {
        assertTrue(SparsePolicy.candidatePairs(0).isEmpty())
        assertEquals(listOf(0 to 1), SparsePolicy.candidatePairs(2))
        val large = SparsePolicy.candidatePairs(300)
        assertTrue(large.size <= 7)
        assertTrue(large.all { it.first == 0 && it.second in 1..299 })
    }
    @Test fun thresholdsGuardFalsePose() {
        assertEquals("INSUFFICIENT_MATCHES", SparsePolicy.verdict(4, 40, 30, 4.0, 1.0))
        assertEquals("INSUFFICIENT_POSE_INLIERS", SparsePolicy.verdict(70, 6, 30, 4.0, 1.0))
        assertEquals("LOW_PARALLAX", SparsePolicy.verdict(70, 40, 36, 0.1, 1.0))
        assertEquals("INSUFFICIENT_VALID_3D_POINTS", SparsePolicy.verdict(70, 40, 10, 2.0, 1.0))
        assertEquals("HIGH_REPROJECTION_ERROR", SparsePolicy.verdict(70, 40, 36, 2.0, 8.0))
        assertEquals("TWO_VIEW_SPARSE_CANDIDATE", SparsePolicy.verdict(70, 40, 36, 2.0, 1.0))
    }
    @Test fun plyIsValidHeaderAndScaleCaveat() {
        val ply = SparsePolicy.asciiPly(listOf(SparseVertex(1.0, 2.0, 3.0, 1, 2, 3)))
        assertTrue(ply.startsWith("ply\nformat ascii 1.0\n"))
        assertTrue(ply.contains("unknown scale"))
        assertTrue(ply.contains("element vertex 1"))
        assertTrue(ply.endsWith("1.000000 2.000000 3.000000 1 2 3\n"))
    }
}
