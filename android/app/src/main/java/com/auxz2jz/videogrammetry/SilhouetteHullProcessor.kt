package com.auxz2jz.videogrammetry

import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Registered camera poses + candidate masks -> coarse voxel visual hull.
 * Coordinates share the scene/ROI-local pose ONLY when report confirms so.
 * Does not hallucinate 360-degree coverage or unseen geometry.
 */
class SilhouetteHullProcessor {
    private fun box(json:JSONObject):FocusRect=FocusRect(
        json.getDouble("left"),json.getDouble("top"),
        json.getDouble("right"),json.getDouble("bottom"))
    private fun numbers(array:JSONArray):List<Double> =
        (0 until array.length()).map{array.getDouble(it)}
    private fun camera(row:JSONObject):VoxelCamera=VoxelCamera(
        row.getInt("frame"),row.getInt("registeredWidth"),
        row.getInt("registeredHeight"),row.getDouble("workingFocal"),
        row.getDouble("workingCx"),row.getDouble("workingCy"),
        numbers(row.getJSONArray("cameraRotationRowMajor")),
        numbers(row.getJSONArray("cameraTranslation")))
    private fun readMask(file:File,camera:VoxelCamera):VoxelMask {
        val image=Imgcodecs.imread(file.absolutePath,Imgcodecs.IMREAD_GRAYSCALE)
        try {
            require(!image.empty()) { "Mask image unreadable" }
            val values=ByteArray(image.rows()*image.cols())
            image.get(0,0,values)
            return VoxelMask(camera,values,image.cols(),image.rows())
        } finally {image.release()}
    }
    private fun roiFromPose(points:List<SparseVertex>,cam:VoxelCamera,
        source:FocusRect,sourceCam:VoxelCamera):FocusRect? {
        fun extent(projected:List<Pair<Double,Double>>):FocusRect? {
            if(projected.size<20)return null
            val sortedX=projected.map{it.first}.sorted()
            val sortedY=projected.map{it.second}.sorted()
            fun quant(v:List<Double>,fraction:Double)=
                v[(v.lastIndex*fraction).toInt().coerceIn(0,v.lastIndex)]
            return FocusRect(quant(sortedX,.05),quant(sortedY,.05),
                quant(sortedX,.95),quant(sortedY,.95))
        }
        val first=extent(points.mapNotNull{sourceCam.project(it.x,it.y,it.z)})
            ?: return null
        val second=extent(points.mapNotNull{cam.project(it.x,it.y,it.z)})
            ?: return null
        // The sparse points often only mark lettering, not the outer silhouette.
        // Expand their projected envelope by how far the user's SOURCE rectangle
        // extends beyond source sparse features, but clamp uncertainty.
        val sx=(source.right-source.left)/(first.right-first.left).coerceAtLeast(.015)
        val sy=(source.bottom-source.top)/(first.bottom-first.top).coerceAtLeast(.015)
        val width=(second.right-second.left)*sx.coerceIn(1.0,4.0)*1.10
        val height=(second.bottom-second.top)*sy.coerceIn(1.0,4.0)*1.10
        val cx=(second.left+second.right)/2
        val cy=(second.top+second.bottom)/2
        val bounds=FocusRect((cx-width/2).coerceIn(.002,.998),
            (cy-height/2).coerceIn(.002,.998),
            (cx+width/2).coerceIn(.002,.998),
            (cy+height/2).coerceIn(.002,.998))
        return bounds.takeIf{it.valid() &&
            (it.right-it.left)*(it.bottom-it.top) in .004..0.80}
    }
    fun build(run:ScanRun):JSONObject {
        require(run.isClosed && run.resultIsValid())
        check(OpenCVLoader.initLocal()) { "OpenCV unavailable" }
        val masker=ObjectMaskProcessor()
        val selections=JSONObject(File(run.directory,
            "early_object_focus_selection.json").readText())
        require(selections.getString("runId")==run.id)
        val pair=selections.getJSONArray("sourcePair")
        val first=pair.getInt(0);val second=pair.getInt(1)
        val input=JSONObject(File(run.directory,CloudArtifacts.MULTIVIEW_REPORT).readText())
        require(input.getString("runId")==run.id &&
            input.optBoolean("sameCoordinateSystemAsFullScene") &&
            input.optString("status")=="MULTIVIEW_SPARSE_CANDIDATE") {
            "Hull needs scene-aligned, PnP-registered multi-view reconstruction"
        }
        val scene=JSONObject(File(run.directory,"sparse_report.json").readText())
            .getJSONObject("selectedPair")
        require(scene.optInt("indexA",-1)==first &&
            scene.optInt("indexB",-1)==second) { "Source camera pair mismatch" }
        val baseCloud=File(run.directory,CloudArtifacts.ROI_RECONSTRUCTED_PLY)
        require(baseCloud.isFile)
        val sparse=baseCloud.inputStream().use {
            PlyParser.parse(it,"ROI-first source point cloud")
        }.vertices.map {SparseVertex(it.x.toDouble(),it.y.toDouble(),it.z.toDouble(),
            it.r,it.g,it.b)}
        require(sparse.size>=24) { "Not enough ROI baseline geometry" }
        val report=JSONObject().put("runId",run.id)
            .put("appVersion","android-"+BuildConfig.VERSION_NAME)
            .put("method","CAMERA_GUIDED_SEGMENTATION_AND_VOXEL_VISUAL_HULL")
            .put("scale","UNKNOWN_ARBITRARY_UNITS")
            .put("status","IN_PROGRESS")
            .put("warning","Experimental coarse voxel hull based on automatic masks and estimated camera positions. Masks and sparse bounds may include checkerboard. Not a watertight/accurate mesh; source image silhouette tracking is approximate.")
        val hull=File(run.directory,CloudArtifacts.SILHOUETTE_HULL_PLY)
        val fused=File(run.directory,CloudArtifacts.MASK_FUSION_PLY)
        run.event("OPERATION_START","SILHOUETTE_VOXEL_HULL")
        try {
            val photo0=PhotoPointOverlayLoader().sourceFile(run,first)
            val dims=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
            android.graphics.BitmapFactory.decodeFile(photo0.absolutePath,dims)
            val scale=min(1.0,800.0/max(dims.outWidth,dims.outHeight).toDouble())
            val width=max(2,(dims.outWidth*scale).toInt())
            val height=max(2,(dims.outHeight*scale).toInt())
            val origin=VoxelCamera.anchor(first,width,height)
            val r=numbers(scene.getJSONArray("cameraPoseRotationRowMajor"))
            val t=numbers(scene.getJSONArray("cameraPoseTranslation"))
            val other=VoxelCamera(second,width,height,origin.focal,origin.cx,
                origin.cy,r,t)
            val roi0=box(selections.getJSONObject("firstRectangle"))
            val mask0=masker.activeMask(run,first)
            val mask1=masker.activeMask(run,second)
            require(mask0!=null && mask1!=null) {
                "Compare foreground mask engines before building silhouette"
            }
            val masks=ArrayList<VoxelMask>()
            masks.add(readMask(mask0,origin))
            masks.add(readMask(mask1,other))
            val active=JSONObject(File(run.directory,"foreground_mask_report.json")
                .readText()).getString("activeEngine")
            val tracked=JSONArray()
            val all=input.getJSONArray("extraViewResults")
            val registrations=(0 until all.length()).map{all.getJSONObject(it)}
                .filter {it.has("cameraRotationRowMajor")}
                .sortedBy{it.getInt("frame")}
            val candidates=if(registrations.size<=10)registrations else
                (0 until 10).map { i ->
                    registrations[(i.toLong()*(registrations.lastIndex)/9).toInt()]
                }.distinctBy{it.getInt("frame")}
            for(row in candidates) {
                val index=row.getInt("frame")
                val entry=JSONObject().put("frame",index)
                try {
                    val cam=camera(row)
                    val projected=roiFromPose(sparse,cam,roi0,origin)
                    if(projected==null) {
                        entry.put("state","UNRELIABLE_PROJECTED_OBJECT_REGION")
                    } else {
                        val segmented=masker.compareTrackedView(run,index,projected)
                        val arr=segmented.getJSONArray("engineResults")
                        val quality=(0 until arr.length()).map{arr.getJSONObject(it)}
                            .firstOrNull{it.getString("engine")==active}
                        if(quality==null || !quality.optBoolean("areaQualityValid")) {
                            entry.put("state","MASK_ENGINE_REJECTED")
                        } else {
                            val mask=masker.maskFile(run,index,active)
                            masks.add(readMask(mask,cam))
                            entry.put("state","AUTO_TRACKED_MASK_CANDIDATE")
                                .put("activeEngine",active)
                                .put("maskPixels",quality.getInt("foregroundPixels"))
                        }
                    }
                } catch(ex:Exception) {
                    entry.put("state","TRACKING_ERROR")
                        .put("errorType",ex.javaClass.simpleName)
                }
                tracked.put(entry)
            }
            val valid=SilhouettePolicy.validMasks(masks.size,
                input.optBoolean("sameCoordinateSystemAsFullScene"))
            val voxel=if(valid)SilhouettePolicy.carveVoxels(sparse,masks)
                else emptyList()
            val sparseOut=if(valid)SilhouettePolicy.carveSparse(sparse,masks)
                else emptyList()
            val status=if(!valid)"INSUFFICIENT_TRACKED_MASK_VIEWS"
                else if(voxel.size<20)"SILHOUETTE_GEOMETRY_INCONCLUSIVE"
                else "SILHOUETTE_VOXEL_CANDIDATE"
            if(status=="SILHOUETTE_VOXEL_CANDIDATE") {
                val temp=File(run.directory,hull.name+".tmp")
                temp.writeText(SparsePolicy.asciiPly(voxel))
                check(temp.renameTo(hull))
                if(sparseOut.isNotEmpty()) {
                    val tmp=File(run.directory,fused.name+".tmp")
                    tmp.writeText(SparsePolicy.asciiPly(sparseOut))
                    check(tmp.renameTo(fused))
                } else fused.delete()
            } else {hull.delete();fused.delete()}
            report.put("status",status).put("activeMaskEngine",active)
                .put("cameraViewsWithMasks",masks.size)
                .put("trackedPhotoResults",tracked)
                .put("baselineSparsePoints",sparse.size)
                .put("retainedSparseObjectPoints",sparseOut.size)
                .put("occupiedHullVoxelPoints",voxel.size)
                .put("voxelResolutionPerAxis",SilhouettePolicy.GRID)
                .put("sourcePair",pair)
            val tmp=File(run.directory,CloudArtifacts.SILHOUETTE_REPORT+".tmp")
            tmp.writeText(report.toString(2))
            check(tmp.renameTo(File(run.directory,CloudArtifacts.SILHOUETTE_REPORT)))
            run.event("ANALYSIS_RESULT","SILHOUETTE_VOXEL_HULL",
                JSONObject().put("status",status).put("views",masks.size)
                    .put("voxels",voxel.size).put("sparseSurvivors",sparseOut.size))
            return report
        } catch(ex:Exception) {
            report.put("status","FAILED").put("errorType",ex.javaClass.simpleName)
            File(run.directory,"silhouette_hull_last_failure.json")
                .writeText(report.toString(2))
            run.event("ERROR","SILHOUETTE_VOXEL_HULL",
                JSONObject().put("errorType",ex.javaClass.simpleName))
            throw ex
        }
    }
}
