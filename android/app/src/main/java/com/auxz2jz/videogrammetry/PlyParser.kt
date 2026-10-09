package com.auxz2jz.videogrammetry

import java.io.InputStream

data class CloudVertex(val x: Float, val y: Float, val z: Float,
    val r: Int, val g: Int, val b: Int)
data class PointCloud(val vertices: List<CloudVertex>, val sourceLabel: String) {
    val count: Int get() = vertices.size
}

/** Offline ASCII PLY parser with a vertex bound and schema checks. */
object PlyParser {
    const val MAX_VERTICES = 50000
    fun parse(stream: InputStream, sourceLabel: String = "Imported PLY"): PointCloud {
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            require(reader.readLine()?.trim() == "ply") { "Not a PLY file" }
            var ascii = false
            var vertexSection = false
            var count = -1
            var end = false
            val columns = ArrayList<String>()
            for (n in 0 until 160) {
                val tokens = (reader.readLine() ?: break).trim().split(Regex("\\s+"))
                when (tokens.firstOrNull()) {
                    "format" -> ascii = tokens.size >= 3 &&
                        tokens[1] == "ascii" && tokens[2] == "1.0"
                    "element" -> {
                        vertexSection = tokens.size >= 3 && tokens[1] == "vertex"
                        if (vertexSection) {
                            count = tokens[2].toIntOrNull() ?: -1
                            require(count in 1..MAX_VERTICES) {
                                "Unsupported PLY vertex count: " + count
                            }
                        }
                    }
                    "property" -> if (vertexSection) {
                        require(tokens.size >= 3 && tokens[1] != "list") {
                            "Unsupported vertex list property"
                        }
                        columns.add(tokens[2])
                    }
                    "end_header" -> { end = true; break }
                }
            }
            require(ascii && end && count > 0) {
                "Expected ASCII 1.0 PLY with vertices"
            }
            require(listOf("x","y","z").all { it in columns }) {
                "Missing XYZ vertex properties"
            }
            val ix = columns.indexOf("x")
            val iy = columns.indexOf("y")
            val iz = columns.indexOf("z")
            fun colorColumn(long: String, short: String): Int =
                columns.indexOf(long).takeIf { it >= 0 } ?: columns.indexOf(short)
            val ir = colorColumn("red","r")
            val ig = colorColumn("green","g")
            val ib = colorColumn("blue","b")
            fun color(t: List<String>, c: Int): Int =
                if (c < 0) 225 else t[c].toDouble().toInt().coerceIn(0,255)
            val points = ArrayList<CloudVertex>(count)
            for (i in 0 until count) {
                val line = reader.readLine()
                    ?: throw IllegalArgumentException("PLY truncated at vertex " + (i+1))
                val t = line.trim().split(Regex("\\s+"))
                require(t.size >= columns.size) { "Incomplete PLY vertex " + (i+1) }
                val x=t[ix].toFloat()
                val y=t[iy].toFloat()
                val z=t[iz].toFloat()
                require(x.isFinite() && y.isFinite() && z.isFinite()) {
                    "Nonfinite PLY coordinate at vertex " + (i+1)
                }
                points.add(CloudVertex(x,y,z,color(t,ir),color(t,ig),color(t,ib)))
            }
            PointCloud(points,sourceLabel)
        }
    }
}

/** Verified physical specification, NOT itself a calibrated camera model. */
object CheckerboardTarget {
    const val INNER_COLUMNS=9
    const val INNER_ROWS=6
    const val SQUARE_MM=25.0
    const val SQUARE_COLUMNS=10
    const val SQUARE_ROWS=7
    val widthMm get()=SQUARE_COLUMNS*SQUARE_MM
    val heightMm get()=SQUARE_ROWS*SQUARE_MM
    const val MIN_CALIBRATION_IMAGES=8
}
