package com.auxz2jz.videogrammetry

import org.junit.Assert.*
import org.junit.Test

class CloudArtifactsTest {
    @Test fun eachAlgorithmOwnsDifferentPlyAndReport() {
        assertTrue(CloudArtifacts.distinct())
        assertEquals(4,setOf(CloudArtifacts.SCENE_PLY,
            CloudArtifacts.ROI_RECONSTRUCTED_PLY,
            CloudArtifacts.FILTERED_SCENE_PLY,
            CloudArtifacts.MULTIVIEW_PLY).size)
        assertNotEquals(CloudArtifacts.ROI_RECONSTRUCTED_REPORT,
            CloudArtifacts.FILTERED_SCENE_REPORT)
    }
    @Test fun exportNamesNeverCollideEvenSameRunAndTimestamp() {
        val run="20261009T224303Z_d8cd8225-5"
        val names=setOf(
            ExportNames.sparsePly("0.10.0",run,0),
            ExportNames.filteredScenePly("0.10.0",run,0),
            ExportNames.reconstructedPly("0.10.0",run,0),
            ExportNames.multiviewPly("0.10.0",run,0))
        assertEquals(4,names.size)
        assertTrue(names.all { it.contains("v0.10.0") && it.endsWith(".ply") })
    }
}
