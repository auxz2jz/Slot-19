package com.auxz2jz.videogrammetry

import org.junit.Assert.*
import org.junit.Test

class CloudArtifactsTest {
    @Test fun eachAlgorithmOwnsDifferentPlyAndReport() {
        assertTrue(CloudArtifacts.distinct())
        assertEquals(6,setOf(CloudArtifacts.SCENE_PLY,
            CloudArtifacts.ROI_RECONSTRUCTED_PLY,
            CloudArtifacts.FILTERED_SCENE_PLY,
            CloudArtifacts.MULTIVIEW_PLY,
            CloudArtifacts.SILHOUETTE_HULL_PLY,
            CloudArtifacts.MASK_FUSION_PLY).size)
        assertNotEquals(CloudArtifacts.ROI_RECONSTRUCTED_REPORT,
            CloudArtifacts.FILTERED_SCENE_REPORT)
    }
    @Test fun exportNamesNeverCollideEvenSameRunAndTimestamp() {
        val run="20261009T224303Z_d8cd8225-5"
        val names=setOf(
            ExportNames.sparsePly("0.12.0",run,0),
            ExportNames.filteredScenePly("0.12.0",run,0),
            ExportNames.reconstructedPly("0.12.0",run,0),
            ExportNames.multiviewPly("0.12.0",run,0),
            ExportNames.silhouetteHullPly("0.12.0",run,0),
            ExportNames.maskFusionPly("0.12.0",run,0))
        assertEquals(6,names.size)
        assertTrue(names.all { it.contains("v0.12.0") && it.endsWith(".ply") })
    }
}
