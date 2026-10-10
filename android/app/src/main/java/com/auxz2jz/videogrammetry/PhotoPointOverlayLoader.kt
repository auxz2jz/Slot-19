package com.auxz2jz.videogrammetry

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class PhotoPoint(val x:Float,val y:Float)
data class OverlayModel(val name:String,val sourceIndices:Pair<Int,Int>,
    val originalPointCount:Int,val pointsA:List<PhotoPoint>,val pointsB:List<PhotoPoint>)

/**
 * Ground truth source-image FEATURE positions captured at triangulation time,
 * NOT a reprojection based on unknown camera poses for unregistered frames.
 */
class PhotoPointOverlayLoader {
    private fun sha(file:File):String {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use {input ->
            val bytes=ByteArray(8192)
            while(true) {
                val n=input.read(bytes)
                if(n<0)break
                digest.update(bytes,0,n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    fun sourceFile(run:ScanRun,index:Int):File {
        val photos=JSONObject(File(run.directory,"manifest.json").readText())
            .getJSONArray("frames")
        require(index in 0 until photos.length()) { "Unknown source photo" }
        val name=photos.getJSONObject(index).getString("name")
        require(Regex("frame_[0-9]{4}\\.jpg").matches(name)) { "Bad saved photo name" }
        return File(File(run.directory,"frames"),name)
            .also { require(it.isFile) { "Missing original photograph" } }
    }

    fun load(run:ScanRun, model:String): OverlayModel {
        require(model in setOf(CloudArtifacts.SCENE_PLY,
            CloudArtifacts.FILTERED_SCENE_PLY,
            CloudArtifacts.ROI_RECONSTRUCTED_PLY)) {
            "Photo correspondences unavailable for this model"
        }
        val roi=model==CloudArtifacts.ROI_RECONSTRUCTED_PLY
        val mapFile=File(run.directory,if(roi)
            "early_object_point_projections.json" else "sparse_point_projections.json")
        require(mapFile.isFile) { "No saved photo-to-point mapping: rerun reconstruction" }
        val map=JSONObject(mapFile.readText())
        require(map.getInt("schemaVersion")==1 && map.getString("runId")==run.id) {
            "Photo mapping belongs to another run"
        }
        val ply=File(run.directory,
            if(roi)CloudArtifacts.ROI_RECONSTRUCTED_PLY else CloudArtifacts.SCENE_PLY)
        require(ply.isFile && sha(ply)==map.getString("plySha256")) {
            "Point cloud and image feature positions do not match"
        }
        val entries=map.getJSONArray("projections")
        val sourceCount=map.getInt("pointCount")
        require(sourceCount==entries.length()) { "Point order/size changed" }
        val indices=if(model==CloudArtifacts.FILTERED_SCENE_PLY) {
            val report=JSONObject(File(run.directory,
                CloudArtifacts.FILTERED_SCENE_REPORT).readText())
            require(report.getString("sourcePlySha256")==map.getString("plySha256")) {
                "Filtered scene was produced from a different reconstruction"
            }
            val selected=if(report.has("sourcePointIndices")) {
                val arr=report.getJSONArray("sourcePointIndices")
                (0 until arr.length()).map { arr.getInt(it) }
            } else {
                // Older v0.10 saved runs lacked indices; recompute ONLY from
                // their original user boxes and matched features, do not guess.
                val a=report.getJSONObject("firstRectangle")
                val b=report.getJSONObject("secondRectangle")
                fun rect(o:JSONObject)=FocusRect(o.getDouble("left"),o.getDouble("top"),
                    o.getDouble("right"),o.getDouble("bottom"))
                val positions=(0 until entries.length()).map { index ->
                    val pt=entries.getJSONObject(index)
                    FocusProjection(pt.getDouble("firstX"),pt.getDouble("firstY"),
                        pt.getDouble("secondX"),pt.getDouble("secondY"))
                }
                ObjectFocusPolicy.retainedIndices(positions,rect(a),rect(b))
            }
            require(selected.size==report.getInt("objectCandidatePoints")) {
                "Filtered point count does not match selected features"
            }
            selected
        } else (0 until entries.length()).toList()
        val aa=ArrayList<PhotoPoint>(indices.size)
        val bb=ArrayList<PhotoPoint>(indices.size)
        for(idx in indices) {
            require(idx in 0 until entries.length()) { "Invalid vertex index" }
            val item=entries.getJSONObject(idx)
            require(item.getInt("pointIndex")==idx) { "Wrong PLY order" }
            fun value(x:String):Float=item.getDouble(x).also {
                require(it.isFinite() && it>=0.0 && it<=1.0) {
                    "Feature coordinate outside decoded source image"
                }
            }.toFloat()
            aa.add(PhotoPoint(value("firstX"),value("firstY")))
            bb.add(PhotoPoint(value("secondX"),value("secondY")))
        }
        val pair=map.getJSONArray("sourcePair")
        return OverlayModel(
            when(model) {
                CloudArtifacts.SCENE_PLY -> "Full scene"
                CloudArtifacts.FILTERED_SCENE_PLY -> "Filtered scene"
                else -> "ROI-first reconstruction"
            },pair.getInt(0) to pair.getInt(1),sourceCount,aa,bb)
    }
}
