package com.auxz2jz.videogrammetry

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class PlyParserTest {
    private fun parse(txt: String) =
        PlyParser.parse(ByteArrayInputStream(txt.toByteArray(Charsets.UTF_8)))
    @Test fun parsesGeneratedSparsePly() {
        val ply=SparsePolicy.asciiPly(listOf(SparseVertex(1.0,2.0,3.0,12,34,56),
            SparseVertex(-1.0,0.2,7.0,3,4,5)))
        val v=parse(ply)
        assertEquals(2,v.count)
        assertEquals(12,v.vertices.first().r)
        assertEquals(3.0f,v.vertices.first().z,0.001f)
    }
    @Test fun parsesDifferentColumnOrder() {
        val source="ply\nformat ascii 1.0\nelement vertex 1\nproperty uchar green\n"+
            "property float z\nproperty float y\nproperty float x\nend_header\n42 7.0 8.0 9.0\n"
        val v=parse(source).vertices.first()
        assertEquals(9.0f,v.x,0.0001f)
        assertEquals(42,v.g)
    }
    @Test fun rejectsInvalidPLY() {
        for (bad in listOf("hello","ply\nformat binary_little_endian 1.0\n" +
            "element vertex 1\nproperty float x\nend_header\n0",
            "ply\nformat ascii 1.0\nelement vertex 1\nproperty float x\n" +
            "property float y\nproperty float z\nend_header\nNaN 1 2\n")) {
            try { parse(bad);fail("Must reject invalid PLY") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun correctTargetSize() {
        assertEquals(9,CheckerboardTarget.INNER_COLUMNS)
        assertEquals(6,CheckerboardTarget.INNER_ROWS)
        assertEquals(250.0,CheckerboardTarget.widthMm,0.001)
        assertEquals(175.0,CheckerboardTarget.heightMm,0.001)
    }
}
