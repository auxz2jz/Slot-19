package com.auxz2jz.videogrammetry

import org.junit.Assert.*
import org.junit.Test

class ExportNamesTest {
    private val run="20261009T101817Z_280fdc45-4"
    @Test fun allExportsUseInstalledVersionNotHardcodedOldOne() {
        val ply=ExportNames.sparsePly("0.6.1",run,0L)
        val latest=ExportNames.latestRunZip("0.6.1",run,0L)
        val all=ExportNames.allRunsZip("0.6.1",0L)
        assertEquals("Android-v0.6.1-sparse-two-view-$run-19700101T000000000Z.ply",ply)
        assertEquals("Android-v0.6.1-last-run-diagnostics-$run-19700101T000000000Z.zip",latest)
        assertEquals("Android-v0.6.1-ALL-run-comparison-19700101T000000000Z.zip",all)
        assertFalse(ply.contains("v0.5.0"))
    }
    @Test fun nextReleaseAutomaticallyGetsItsOwnVersion() {
        val name=ExportNames.sparsePly("0.7.0",run,5L)
        assertTrue(name.startsWith("Android-v0.7.0-"))
        assertTrue(name.endsWith("-19700101T000000005Z.ply"))
    }
    @Test fun objectFocusExportIsDistinctAndVersioned() {
        val name=ExportNames.objectFocusPly("0.8.0",run,0L)
        assertEquals("Android-v0.8.0-object-focus-$run-19700101T000000000Z.ply",name)
        assertNotEquals(name,ExportNames.sparsePly("0.8.0",run,0L))
    }
    @Test fun repeatedExportsHaveDistinctNames() {
        assertNotEquals(ExportNames.allRunsZip("0.6.1",10L),
            ExportNames.allRunsZip("0.6.1",11L))
    }
    @Test fun rejectUnsafeIdentifierAndVersion() {
        try { ExportNames.sparsePly("0.6.1","../../bad",0L);fail() }
        catch (_:IllegalArgumentException) { }
        try { ExportNames.allRunsZip("bad/path",0L);fail() }
        catch (_:IllegalArgumentException) { }
    }
}
