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
    private fun roiFromBounds(bounds:VoxelBounds,cam:VoxelCamera):FocusRect? {
        val projected=ArrayList<Pair<Double,Double>>()
        for(x in listOf(bounds.x0,bounds.x1))
            for(y in listOf(bounds.y0,bounds.y1))
                for(z in listOf(bounds.z0,bounds.z1))
                    cam.project(x,y,z)?.let{projected.add(it)}
        if(projected.size<6)return null
        val minX=projected.minOf{it.first}
        val maxX=projected.maxOf{it.first}
        val minY=projected.minOf{it.second}
        val maxY=projected.maxOf{it.second}
        val dx=maxX-minX;val dy=maxY-minY
        if(dx !in .035..0.85 || dy !in .035..0.85)return null
        val box=FocusRect((minX-.03*dx).coerceIn(.002,.998),
            (minY-.03*dy).coerceIn(.002,.998),
            (maxX+.03*dx).coerceIn(.002,.998),
            (maxY+.03*dy).coerceIn(.002,.998))
        return box.takeIf{it.valid() &&
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
        val sceneTrack=File(run.directory,"scene_camera_track_report.json")
        val sceneCandidate=runCatching {JSONObject(sceneTrack.readText())}.getOrNull()
        val independentScenePose=sceneCandidate?.let {
            it.optString("runId")==run.id && it.optBoolean("sceneAligned") &&
                it.optString("status")=="SCENE_CAMERA_POSES_AVAILABLE" &&
                it.optInt("registeredExtraViews")>0
        } ?: false
        val input=if(independentScenePose)sceneCandidate!!
            else JSONObject(File(run.directory,CloudArtifacts.MULTIVIEW_REPORT).readText())
        val poseAligned=if(independentScenePose)input.optBoolean("sceneAligned")
            else input.optBoolean("sameCoordinateSystemAsFullScene")
        require(input.getString("runId")==run.id && poseAligned &&
            (if(independentScenePose)
                input.optString("status")=="SCENE_CAMERA_POSES_AVAILABLE"
                else input.optString("status")=="MULTIVIEW_SPARSE_CANDIDATE")) {
            "Register at least one EXTRA scene camera pose or run aligned Multi-View"
        }
        val scene=JSONObject(File(run.directory,"sparse_report.json").readText())
            .getJSONObject("selectedPair")
        require(scene.optInt("indexA",-1)==first &&
            scene.optInt("indexB",-1)==second) { "Source camera pair mismatch" }
        val baseCloud=File(run.directory,CloudArtifacts.ROI_RECONSTRUCTED_PLY)
        val sparse=if(baseCloud.isFile) baseCloud.inputStream().use {
            PlyParser.parse(it,"ROI-first sparse points")
        }.vertices.map{SparseVertex(it.x.toDouble(),it.y.toDouble(),it.z.toDouble(),
            it.r,it.g,it.b)} else emptyList()
        val registered=File(run.directory,CloudArtifacts.MULTIVIEW_PLY)
        val fusionFile=if(!independentScenePose && registered.isFile)registered
            else File(run.directory,CloudArtifacts.SCENE_PLY)
        require(fusionFile.isFile) { "Full-scene XYZ required for optional mask fusion" }
        val allTracks=fusionFile.inputStream().use {
            PlyParser.parse(it,"Mask-gated scene and/or registered sparse points")
        }.vertices.map{SparseVertex(it.x.toDouble(),it.y.toDouble(),it.z.toDouble(),
            it.r,it.g,it.b)}
        val report=JSONObject().put("runId",run.id)
            .put("appVersion","android-"+BuildConfig.VERSION_NAME)
            .put("method","SCENE_POSE_GUIDED_SILHOUETTE_VOXEL_HULL")
            .put("cameraTrackingSource",if(independentScenePose)
                "FULL_SCENE_BACKGROUND_FEATURE_PNP"
                else "ROI_MULTIVIEW_PNP")
            .put("objectORBRequired",false)
            .put("sparseFusionInput",fusionFile.name)
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
            val roi1=box(selections.getJSONObject("secondRectangle"))
            val maskBounds=SilhouetteBounds.fromTwoSilhouettes(
                roi0,roi1,origin,other)
            report.put("voxelBoundsStrategy",if(maskBounds!=null)
                "TWO_USER_SILHOUETTE_CAMERA_RAY_INTERSECTION"
                else "SPARSE_FEATURE_ENVELOPE_FALLBACK")
                .put("maskRayInitializationInconclusive",maskBounds==null)
                .put("cameraGuidedObjectTracker",if(maskBounds!=null)
                    "PROJECTED_3D_SILHOUETTE_VOLUME"
                    else "SPARSE_FEATURE_ENVELOPE_FALLBACK")
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
                    val projected=maskBounds?.let{roiFromBounds(it,cam)}
                        ?:roiFromPose(sparse,cam,roi0,origin)
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
            val valid=SilhouettePolicy.validMasks(masks.size,poseAligned)
            val voxel=if(valid && (maskBounds!=null || sparse.size>=24))
                SilhouettePolicy.carveVoxels(sparse,masks,maskBounds)
                else emptyList()
            val sparseOut=if(valid)SilhouettePolicy.carveSparse(allTracks,masks)
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
                .put("multiViewInputPoints",allTracks.size)
                .put("fusionInputCoordinateFrame","SCENE_ALIGNED_ROI_LOCAL")
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
