package com.auxz2jz.videogrammetry

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Separates user-marked object tracks from all-scene tracks; original PLY untouched. */
class ObjectFocusProcessor {
    private fun mapping(run: ScanRun): JSONObject {
        val file=File(run.directory,"sparse_point_projections.json")
        require(file.isFile) { "Re-run Analyze Sparse 3D to create the source-point map" }
        val map=JSONObject(file.readText())
        require(map.getString("runId")==run.id) { "Source run mismatch" }
        require(map.getInt("schemaVersion")==1) { "Unsupported projection schema" }
        return map
    }
    fun sourcePhotos(run: ScanRun): Pair<File,File> {
        val map=mapping(run)
        val manifest=JSONObject(File(run.directory,"manifest.json").readText())
            .getJSONArray("frames")
        val pair=map.getJSONArray("sourcePair")
        fun select(index: Int):File {
            val position=pair.getInt(index)
            require(position in 0 until manifest.length()) { "Missing selected photo" }
            val name=manifest.getJSONObject(position).getString("name")
            require(Regex("frame_[0-9]{4}\\.jpg").matches(name)) { "Unexpected source filename" }
            val file=File(File(run.directory,"frames"),name)
            require(file.isFile) { "Source photo unavailable" }
            return file
        }
        return select(0) to select(1)
    }

    /** Before any sparse model exists, pick deterministic actual saved JPEG frames. */
    fun sourcePhotosBeforeSparse(run: ScanRun): Pair<File,File> {
        require(run.isClosed && run.resultIsValid()) { "Complete video/frame capture first" }
        val frames=JSONObject(File(run.directory,"manifest.json").readText())
            .getJSONArray("frames")
        val pair=EarlyObjectFocusPolicy.sourcePair(frames.length())
        fun checked(index:Int):File {
            val name=frames.getJSONObject(index).getString("name")
            require(Regex("frame_[0-9]{4}\\.jpg").matches(name)) {
                "Invalid saved frame name"
            }
            return File(File(run.directory,"frames"),name)
                .also { require(it.isFile) { "Saved source photo missing" } }
        }
        return checked(pair.first) to checked(pair.second)
    }

    /** Stable selection only. No full scene PLY required or changed. */
    fun saveBeforeSparse(run: ScanRun,first: FocusRect,second: FocusRect):JSONObject {
        require(EarlyObjectFocusPolicy.selectionValid(first,second)) {
            "Choose a valid object rectangle in BOTH photographs"
        }
        val frames=JSONObject(File(run.directory,"manifest.json").readText())
            .getJSONArray("frames")
        val pair=EarlyObjectFocusPolicy.sourcePair(frames.length())
        // Verify the source files before committing a potentially stale selection.
        sourcePhotosBeforeSparse(run)
        val metadata=JSONObject()
            .put("schemaVersion",1).put("runId",run.id)
            .put("appVersion","android-"+BuildConfig.VERSION_NAME)
            .put("selectedAtUtcMs",System.currentTimeMillis())
            .put("sourcePair",JSONArray().put(pair.first).put(pair.second))
            .put("sourceFrameCount",frames.length())
            .put("firstRectangle",regionJson(first))
            .put("secondRectangle",regionJson(second))
            .put("status","SELECTED_BEFORE_RECONSTRUCTION")
        val target=File(run.directory,"early_object_focus_selection.json")
        val tmp=File(run.directory,"early_object_focus_selection.json.tmp")
        tmp.writeText(metadata.toString(2))
        check(tmp.renameTo(target)) { "Could not save early object selection" }
        // New boxes invalidate only previously derived ROI-first/multi-view
        // results. Full scene and legacy scene-filter outputs are independent.
        for(name in listOf(CloudArtifacts.ROI_RECONSTRUCTED_PLY,
            CloudArtifacts.ROI_RECONSTRUCTED_REPORT,
            CloudArtifacts.MULTIVIEW_PLY,CloudArtifacts.MULTIVIEW_REPORT))
            File(run.directory,name).delete()
        run.event("ANALYSIS_RESULT","EARLY_OBJECT_SELECTION",
            JSONObject().put("sourceFrameA",pair.first).put("sourceFrameB",pair.second)
                .put("status","SELECTED_BEFORE_RECONSTRUCTION"))
        return metadata
    }

    private fun regionJson(box: FocusRect):JSONObject=JSONObject()
        .put("left",box.left).put("top",box.top)
        .put("right",box.right).put("bottom",box.bottom)

    private fun fingerprint(file: File):String {
        val hash=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val block=ByteArray(8192)
            while(true) {
                val n=input.read(block)
                if(n<0)break
                hash.update(block,0,n)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    fun apply(run: ScanRun, first: FocusRect, second: FocusRect): JSONObject {
        require(run.isClosed && run.resultIsValid()) { "Completed valid scan required" }
        require(first.valid() && second.valid()) { "Select the object in BOTH source images" }
        val operationId=UUID.randomUUID().toString()
        val report=JSONObject().put("operationId",operationId).put("runId",run.id)
            .put("appVersion","android-"+BuildConfig.VERSION_NAME)
            .put("method","TWO_SOURCE_IMAGE_RECTANGLES_FILTER_MATCHED_PLY_POINTS")
            .put("status","IN_PROGRESS")
            .put("scale","UNKNOWN")
            .put("warning","Coarse rectangle annotation, not semantic segmentation, physical measurements, or a finished 3D model.")
        run.event("USER_ACTION","APPLY_OBJECT_FOCUS",JSONObject()
            .put("operationId",operationId).put("boxA",regionJson(first))
            .put("boxB",regionJson(second)))
        try {
            val map=mapping(run)
            val source=File(run.directory,"sparse_two_view.ply")
            require(source.isFile) { "Original sparse PLY missing" }
            require(fingerprint(source)==map.getString("plySha256")) {
                "PLY was regenerated: re-run sparse analysis before filtering"
            }
            val vertices=source.inputStream().use { PlyParser.parse(it, "Original scene") }.vertices
            val raw=map.getJSONArray("projections")
            require(vertices.size==map.getInt("pointCount") && raw.length()==vertices.size) {
                "Point projection/PLY count mismatch"
            }
            val imagePoints=ArrayList<FocusProjection>(vertices.size)
            for(i in vertices.indices) {
                val value=raw.getJSONObject(i)
                require(value.getInt("pointIndex")==i) { "Projection order changed" }
                imagePoints.add(FocusProjection(value.getDouble("firstX"),
                    value.getDouble("firstY"),value.getDouble("secondX"),
                    value.getDouble("secondY")))
            }
            val kept=ObjectFocusPolicy.retainedIndices(imagePoints,first,second)
            val filtered=kept.map { i ->
                val v=vertices[i]
                SparseVertex(v.x.toDouble(),v.y.toDouble(),v.z.toDouble(),v.r,v.g,v.b)
            }
            report.put("originalScenePoints",vertices.size)
                .put("objectCandidatePoints",filtered.size)
                .put("excludedScenePoints",vertices.size-filtered.size)
                .put("sourcePair",map.getJSONArray("sourcePair"))
                .put("sourcePlySha256",map.getString("plySha256"))
                .put("firstRectangle",regionJson(first))
                .put("secondRectangle",regionJson(second))
                .put("status",if(filtered.isEmpty())"NO_POINTS_IN_BOTH_REGIONS"
                    else "OBJECT_FOCUS_CANDIDATE")
            val focused=File(run.directory,"sparse_object_focus.ply")
            if(filtered.isEmpty()) focused.delete()
            else {
                val temp=File(run.directory,"sparse_object_focus.ply.tmp")
                temp.writeText(SparsePolicy.asciiPly(filtered))
                require(temp.length()>150) { "Filtered PLY empty" }
                check(temp.renameTo(focused)) { "Cannot finalize object-focus PLY" }
            }
            val selection=JSONObject().put("appVersion","android-"+BuildConfig.VERSION_NAME)
                .put("sourcePair",map.getJSONArray("sourcePair"))
                .put("firstRectangle",regionJson(first))
                .put("secondRectangle",regionJson(second))
            File(run.directory,"object_focus_selection.json").writeText(selection.toString(2))
            val temp=File(run.directory,"object_focus_report.json.tmp")
            temp.writeText(report.toString(2))
            check(temp.renameTo(File(run.directory,"object_focus_report.json")))
            run.event("ANALYSIS_RESULT","OBJECT_FOCUS",
                JSONObject().put("operationId",operationId)
                    .put("status",report.optString("status"))
                    .put("included",filtered.size).put("excluded",vertices.size-filtered.size))
            return report
        } catch(exc:Exception) {
            report.put("status","FAILED").put("errorType",exc.javaClass.simpleName)
            File(run.directory,"object_focus_last_failure.json").writeText(report.toString(2))
            run.event("ERROR","OBJECT_FOCUS",JSONObject()
                .put("operationId",operationId).put("errorType",exc.javaClass.simpleName))
            throw exc
        }
    }
}
