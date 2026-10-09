package com.auxz2jz.videogrammetry

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.KeyPoint
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point3
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

/**
 * VERY LIMITED two-view relative pose and triangulation experiment.
 * Estimated pinhole intrinsics/no lens correction, unit baseline, world scale unknown.
 */
class SparseTwoViewAnalyzer {
    private data class Features(val keys: Array<KeyPoint>, val desc: Mat,
                                val colors: Bitmap) {
        fun release() { desc.release(); colors.recycle() }
    }
    private data class Candidate(val report: JSONObject, val vertices: List<SparseVertex>,
                                 val score: Double, val anchorTracks: Map<Int,SparseVertex> = emptyMap(),
                                 val projections: List<FocusProjection> = emptyList(),
                                 val widthA: Int = 0, val heightA: Int = 0,
                                 val widthB: Int = 0, val heightB: Int = 0)
    private fun checkedFile(run: ScanRun, manifest: JSONArray, index: Int): File {
        val name = manifest.getJSONObject(index).getString("name")
        require(Regex("frame_[0-9]{4}\\.jpg").matches(name)) { "Unsafe image name" }
        return File(File(run.directory, "frames"), name)
    }
    private fun features(file: File, orb: ORB): Features {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid JPEG" }
        var sample = 1
        while (bounds.outWidth / sample > 1280 || bounds.outHeight / sample > 1280) sample *= 2
        val full = BitmapFactory.decodeFile(file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("JPEG decode failed")
        val target = 800.0
        val scale = min(1.0, target / maxOf(full.width, full.height).toDouble())
        val image = if (scale < 1.0) Bitmap.createScaledBitmap(full,
            (full.width * scale).toInt().coerceAtLeast(1),
            (full.height * scale).toInt().coerceAtLeast(1), true) else full
        if (image !== full) full.recycle()
        val rgba = Mat()
        val gray = Mat()
        val keys = MatOfKeyPoint()
        val desc = Mat()
        try {
            Utils.bitmapToMat(image, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            orb.detectAndCompute(gray, Mat(), keys, desc)
            return Features(keys.toArray(), desc, image)
        } catch (ex: Exception) {
            image.recycle(); desc.release()
            throw ex
        } finally {
            rgba.release(); gray.release(); keys.release()
        }
    }
    private fun maskOk(mask: Mat, i: Int): Boolean {
        val value = if (mask.rows() == 1) mask.get(0, i) else mask.get(i, 0)
        return value != null && value.isNotEmpty() && value[0] != 0.0
    }
    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return Double.NaN
        val sorted = values.sorted()
        return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
        else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
    }
    private fun angleDegrees(p1: Point, p2: Point, R: Mat, f: Double,
                             cx: Double, cy: Double): Double {
        fun unit(x: Double, y: Double, z: Double): DoubleArray {
            val length = sqrt(x*x + y*y + z*z)
            return doubleArrayOf(x/length, y/length, z/length)
        }
        val a = unit((p1.x - cx)/f, (p1.y - cy)/f, 1.0)
        val b = unit((p2.x - cx)/f, (p2.y - cy)/f, 1.0)
        val worldB = DoubleArray(3) { j ->
            (0..2).sumOf { row -> R.get(row, j)[0] * b[row] }
        }
        val dot = (0..2).sumOf { k -> a[k] * worldB[k] }.coerceIn(-1.0, 1.0)
        return acos(dot) * 180.0 / Math.PI
    }
    private fun projection1(f: Double, cx: Double, cy: Double): Mat {
        val P = Mat.zeros(3,4,CvType.CV_64F)
        P.put(0,0,f); P.put(0,2,cx)
        P.put(1,1,f); P.put(1,2,cy)
        P.put(2,2,1.0)
        return P
    }
    private fun projection2(f: Double, cx: Double, cy: Double, R: Mat, t: Mat): Mat {
        val ext = Mat.zeros(3,4,CvType.CV_64F)
        for (i in 0..2) {
            for (j in 0..2) ext.put(i,j,R.get(i,j)[0])
            ext.put(i,3,t.get(i,0)[0])
        }
        val K = Mat.eye(3,3,CvType.CV_64F)
        K.put(0,0,f); K.put(1,1,f)
        K.put(0,2,cx); K.put(1,2,cy)
        val result = Mat()
        try { Core.gemm(K,ext,1.0,Mat(),0.0,result) }
        finally { ext.release(); K.release() }
        return result
    }
    private fun candidate(a: Features, b: Features, indexA: Int, indexB: Int): Candidate {
        val report = JSONObject().put("indexA",indexA).put("indexB",indexB)
            .put("featuresA",a.keys.size).put("featuresB",b.keys.size)
        if (a.colors.width != b.colors.width || a.colors.height != b.colors.height) {
            return Candidate(report.put("verdict","INCONSISTENT_FRAME_DIMENSIONS"), emptyList(),0.0)
        }
        if (a.desc.empty() || b.desc.empty()) {
            return Candidate(report.put("verdict","INSUFFICIENT_FEATURES"), emptyList(),0.0)
        }
        val matcher = BFMatcher.create(Core.NORM_HAMMING,false)
        val raw = ArrayList<MatOfDMatch>()
        val good = ArrayList<org.opencv.core.DMatch>()
        try {
            matcher.knnMatch(a.desc,b.desc,raw,2)
            for (group in raw) {
                val neighbors = group.toArray()
                if (neighbors.size >= 2 && neighbors[0].distance < 0.75f*neighbors[1].distance)
                    good.add(neighbors[0])
            }
        } finally { raw.forEach { it.release() } }
        report.put("ratioMatches",good.size)
        if (good.size < SparsePolicy.MIN_RATIO_MATCHES) {
            return Candidate(report.put("verdict","INSUFFICIENT_MATCHES"),emptyList(),0.0)
        }
        val coords1=good.map { a.keys[it.queryIdx].pt }
        val coords2=good.map { b.keys[it.trainIdx].pt }
        val P1points=MatOfPoint2f()
        val P2points=MatOfPoint2f()
        val K=Mat.eye(3,3,CvType.CV_64F)
        val EMask=Mat()
        val R=Mat();val t=Mat();val poseMask=Mat()
        var E:Mat?=null
        val f=0.95*maxOf(a.colors.width,a.colors.height)
        val cx=(a.colors.width-1)/2.0
        val cy=(a.colors.height-1)/2.0
        K.put(0,0,f);K.put(1,1,f);K.put(0,2,cx);K.put(1,2,cy)
        report.put("intrinsicsSource","ESTIMATED_NOT_CALIBRATED")
            .put("assumedFocalPx",f).put("principalPoint",JSONArray().put(cx).put(cy))
        try {
            P1points.fromList(coords1)
            P2points.fromList(coords2)
            E=Calib3d.findEssentialMat(P1points,P2points,K,
                Calib3d.RANSAC,0.999,1.5,1000,EMask)
            if (E.empty() || E.rows()!=3 || E.cols()!=3) {
                return Candidate(report.put("verdict","ESSENTIAL_MATRIX_NOT_UNIQUE"),emptyList(),0.0)
            }
            EMask.copyTo(poseMask)
            val poseCount=Calib3d.recoverPose(E,P1points,P2points,K,R,t,poseMask)
            report.put("poseCheiralityInliers",poseCount)
            if (poseCount < SparsePolicy.MIN_POSE_INLIERS)
                return Candidate(report.put("verdict","INSUFFICIENT_POSE_INLIERS"),emptyList(),0.0)
            val acceptedIndices=good.indices.filter { maskOk(poseMask,it) }
            val in1=MatOfPoint2f()
            val in2=MatOfPoint2f()
            val P1=projection1(f,cx,cy)
            val P2=projection2(f,cx,cy,R,t)
            val X=Mat()
            try {
                in1.fromList(acceptedIndices.map { coords1[it] })
                in2.fromList(acceptedIndices.map { coords2[it] })
                Calib3d.triangulatePoints(P1,P2,in1,in2,X)
                val points=ArrayList<SparseVertex>()
                val tracks=HashMap<Int,SparseVertex>()
                val projections=ArrayList<FocusProjection>()
                val allAngles=ArrayList<Double>()
                val allErrors=ArrayList<Double>()
                for (i in acceptedIndices.indices) {
                    val index=acceptedIndices[i]
                    val xh=X.get(0,i)?.getOrNull(0) ?: continue
                    val yh=X.get(1,i)?.getOrNull(0) ?: continue
                    val zh=X.get(2,i)?.getOrNull(0) ?: continue
                    val w=X.get(3,i)?.getOrNull(0) ?: continue
                    if (!w.isFinite() || kotlin.math.abs(w)<1e-9) continue
                    val x=xh/w; val y=yh/w; val z=zh/w
                    if (!x.isFinite() || !y.isFinite() || !z.isFinite() || z<=0.01 || z>1e5) continue
                    val x2=R.get(0,0)[0]*x+R.get(0,1)[0]*y+R.get(0,2)[0]*z+t.get(0,0)[0]
                    val y2=R.get(1,0)[0]*x+R.get(1,1)[0]*y+R.get(1,2)[0]*z+t.get(1,0)[0]
                    val z2=R.get(2,0)[0]*x+R.get(2,1)[0]*y+R.get(2,2)[0]*z+t.get(2,0)[0]
                    if (z2<=0.01) continue
                    val p1=coords1[index];val p2=coords2[index]
                    val err=(hypot(f*x/z+cx-p1.x,f*y/z+cy-p1.y)+
                        hypot(f*x2/z2+cx-p2.x,f*y2/z2+cy-p2.y))/2
                    val angle=angleDegrees(p1,p2,R,f,cx,cy)
                    if (!err.isFinite() || err>6.0 || !angle.isFinite()) continue
                    val pix=a.colors.getPixel(p1.x.toInt().coerceIn(0,a.colors.width-1),
                        p1.y.toInt().coerceIn(0,a.colors.height-1))
                    val vertex=SparseVertex(x,y,z,Color.red(pix),Color.green(pix),Color.blue(pix))
                    points.add(vertex)
                    tracks[good[index].queryIdx]=vertex
                    projections.add(FocusProjection(p1.x/a.colors.width.toDouble(),
                        p1.y/a.colors.height.toDouble(),p2.x/b.colors.width.toDouble(),
                        p2.y/b.colors.height.toDouble()))
                    allAngles.add(angle); allErrors.add(err)
                }
                val medianAngle=median(allAngles)
                val medianError=median(allErrors)
                val verdict=SparsePolicy.verdict(good.size,poseCount,points.size,medianAngle,medianError)
                report.put("triangulatedPositiveDepth",points.size)
                    .put("medianParallaxDeg",if(medianAngle.isFinite())medianAngle else JSONObject.NULL)
                    .put("medianReprojectionPx",if(medianError.isFinite())medianError else JSONObject.NULL)
                    .put("verdict",verdict)
                val valid=verdict=="TWO_VIEW_SPARSE_CANDIDATE"
                val score=if(valid) points.size * min(medianAngle,8.0)/8.0 else 0.0
                return Candidate(report,if(valid)points else emptyList(),score,
                    if(valid)tracks else emptyMap(),
                    if(valid)projections else emptyList(),
                    a.colors.width,a.colors.height,b.colors.width,b.colors.height)
            } finally {
                in1.release();in2.release();P1.release();P2.release();X.release()
            }
        } finally {
            P1points.release();P2points.release();K.release();EMask.release()
            R.release();t.release();poseMask.release();E?.release()
        }
    }


    /**
     * Rebuild the original source pair without touching its saved PLY, then
     * independently verify the SAME 3D anchor points in later saved frames
     * with descriptor tracks and robust PnP. Not multi-view bundle adjustment.
     */
    fun validateThirdView(run: ScanRun, progress: (Int,Int)->Unit): JSONObject {
        require(run.isClosed && run.resultIsValid()) { "Completed valid capture required" }
        val original=JSONObject(File(run.directory,"sparse_report.json").readText())
        require(original.optString("status") == "SPARSE_CANDIDATE") {
            "First run Analyze Sparse 3D — Two Views"
        }
        val chosen=original.getJSONObject("selectedPair")
        require(chosen.getInt("indexA")==0) { "Unsupported pair anchor" }
        val middle=chosen.getInt("indexB")
        val manifest=JSONObject(File(run.directory,"manifest.json").readText())
        val frames=manifest.getJSONArray("frames")
        val options=ThirdViewPolicy.thirdIndices(frames.length(),middle)
        val id=UUID.randomUUID().toString()
        val out=JSONObject().put("analysisId",id).put("runId",run.id)
            .put("appVersion","android-"+BuildConfig.VERSION_NAME)
            .put("selectedSourcePair",JSONArray().put(0).put(middle))
            .put("sourcePairPointCount",original.optInt("pointCount"))
            .put("sourceIntrinsics","ESTIMATED_NOT_CALIBRATED")
            .put("method","ORB_3D2D_ANCHOR_TRACKS_PNP_RANSAC")
            .put("pointScale","UNKNOWN")
            .put("status","IN_PROGRESS")
            .put("warning","Reprojection consistency across a third photograph is not full SfM, object-only reconstruction, calibration proof or metric 3D accuracy.")
        val calibration=File(run.directory.parentFile!!.parentFile,"camera_calibration/last_checkerboard.json")
        // capture_runs/<runId> -> filesDir/camera_calibration
        val board=runCatching { JSONObject(calibration.readText()) }.getOrNull()
        val first=frames.getJSONObject(0)
        out.put("scanFrameDimensions",JSONArray().put(first.optInt("width")).put(first.optInt("height")))
        out.put("calibrationCompatibility",
            if(board==null)"NO_SAVED_CALIBRATION" else ThirdViewPolicy.compatibility(
                board.optInt("workingWidthPx"),board.optInt("workingHeightPx"),
                first.optInt("width"),first.optInt("height")))
        out.put("calibrationWasApplied",false)
        val rows=JSONArray()
        run.event("USER_ACTION","VERIFY_THIRD_VIEW",
            JSONObject().put("analysisId",id).put("candidateThirdViews",options.size))
        try {
            if (options.isEmpty()) {
                out.put("status","INCONCLUSIVE").put("reason","NO_THIRD_FRAME_AFTER_SECOND")
            } else {
                check(OpenCVLoader.initLocal()) { "OpenCV native library unavailable" }
                val orb=ORB.create(1000)
                try {
                    val firstFeatures=features(checkedFile(run,frames,0),orb)
                    try {
                        val secondFeatures=features(checkedFile(run,frames,middle),orb)
                        val base=try {
                            candidate(firstFeatures,secondFeatures,0,middle)
                        } finally { secondFeatures.release() }
                        out.put("recomputedSourcePoints",base.anchorTracks.size)
                        if(base.anchorTracks.size<ThirdViewPolicy.MIN_CORRESPONDENCES) {
                            out.put("status","INCONCLUSIVE")
                                .put("reason","BASELINE_RECONSTRUCTION_NOT_REPRODUCIBLE")
                        } else {
                            for((ordinal,index) in options.withIndex()) {
                                val third=features(checkedFile(run,frames,index),orb)
                                try {
                                    val item=try {
                                        validatePnP(firstFeatures,third,base.anchorTracks,index)
                                    } catch(ex: Exception) {
                                        JSONObject().put("thirdIndex",index)
                                            .put("verdict","PNP_RUNTIME_ERROR")
                                            .put("errorType",ex.javaClass.simpleName)
                                    }
                                    rows.put(item)
                                } finally { third.release() }
                                progress(ordinal+1,options.size)
                                run.event("ANALYSIS_PROGRESS","THIRD_VIEW_PNP",
                                    JSONObject().put("completed",ordinal+1)
                                        .put("total",options.size))
                            }
                            val success=(0 until rows.length()).count {
                                rows.getJSONObject(it).optString("verdict")=="THIRD_VIEW_CONSISTENT"
                            }
                            out.put("consistentThirdViews",success)
                                .put("attemptedThirdViews",rows.length())
                                .put("status",if(success>0)"THIRD_VIEW_SUPPORTED" else "INCONCLUSIVE")
                        }
                    } finally { firstFeatures.release() }
                } finally { orb.clear() }
            }
            out.put("thirdViewResults",rows)
            val target=File(run.directory,"third_view_report.json")
            val temp=File(run.directory,"third_view_report.json.tmp")
            temp.writeText(out.toString(2))
            check(temp.renameTo(target)) { "Cannot finalize third-view report" }
            run.event("ANALYSIS_RESULT","THIRD_VIEW_PNP",
                JSONObject().put("status",out.optString("status"))
                    .put("consistentThirdViews",out.optInt("consistentThirdViews")))
            return out
        } catch(ex:Exception) {
            out.put("status","FAILED").put("errorType",ex.javaClass.simpleName)
            File(run.directory,"third_view_last_failure.json").writeText(out.toString(2))
            run.event("ERROR","THIRD_VIEW_PNP",
                JSONObject().put("errorType",ex.javaClass.simpleName)
                    .put("analysisId",id))
            throw ex
        }
    }

    private fun validatePnP(anchor: Features, third: Features,
        sourcePoints: Map<Int,SparseVertex>, index: Int): JSONObject {
        val out=JSONObject().put("thirdIndex",index)
            .put("anchorSource3dPoints",sourcePoints.size)
        if(anchor.colors.width!=third.colors.width ||
            anchor.colors.height!=third.colors.height)
            return out.put("verdict","DIFFERENT_FRAME_DIMENSIONS")
        if(anchor.desc.empty() || third.desc.empty())
            return out.put("verdict","INSUFFICIENT_DESCRIPTORS")
        val raw=ArrayList<MatOfDMatch>()
        val pairs=ArrayList<Pair<SparseVertex,Point>>()
        try {
            BFMatcher.create(Core.NORM_HAMMING,false).knnMatch(
                anchor.desc,third.desc,raw,2)
            val usedThird=HashSet<Int>()
            for(group in raw) {
                val n=group.toArray()
                if(n.size<2 || n[0].distance>=n[1].distance*0.75f) continue
                val v=sourcePoints[n[0].queryIdx] ?: continue
                if(!usedThird.add(n[0].trainIdx))continue
                pairs.add(v to third.keys[n[0].trainIdx].pt)
            }
        } finally { raw.forEach { it.release() } }
        out.put("shared3d2dTracks",pairs.size)
        if(pairs.size<ThirdViewPolicy.MIN_CORRESPONDENCES)
            return out.put("verdict","INSUFFICIENT_THREE_VIEW_TRACKS")
        val objectPoints=MatOfPoint3f()
        val imagePoints=MatOfPoint2f()
        val K=Mat.eye(3,3,CvType.CV_64F)
        val dist=MatOfDouble()
        val Rv=Mat()
        val Tv=Mat()
        val inliers=Mat()
        val projection=MatOfPoint2f()
        try {
            val focal=0.95*maxOf(anchor.colors.width,anchor.colors.height)
            K.put(0,0,focal);K.put(1,1,focal)
            K.put(0,2,(anchor.colors.width-1)/2.0)
            K.put(1,2,(anchor.colors.height-1)/2.0)
            objectPoints.fromList(pairs.map {
                Point3(it.first.x,it.first.y,it.first.z)
            })
            imagePoints.fromList(pairs.map { it.second })
            val ok=Calib3d.solvePnPRansac(objectPoints,imagePoints,K,dist,
                Rv,Tv,false,150,3f,0.99,inliers,Calib3d.SOLVEPNP_EPNP)
            if(!ok) return out.put("verdict","PNP_RANSAC_REJECTED")
            val ids=(0 until inliers.rows()).mapNotNull {
                inliers.get(it,0)?.firstOrNull()?.toInt()
            }.filter { it in pairs.indices }
            if(ids.isEmpty())return out.put("verdict","PNP_RANSAC_REJECTED")
            Calib3d.projectPoints(objectPoints,Rv,Tv,K,dist,projection)
            val errors=ids.map {
                val predicted=projection.toArray()[it]
                val actual=pairs[it].second
                hypot(predicted.x-actual.x,predicted.y-actual.y)
            }
            val error=median(errors)
            out.put("pnpRansacInliers",ids.size)
                .put("medianPnPReprojectionPx",
                    if(error.isFinite())error else JSONObject.NULL)
                .put("verdict",ThirdViewPolicy.verdict(pairs.size,ids.size,error))
            return out
        } finally {
            objectPoints.release();imagePoints.release();K.release()
            dist.release();Rv.release();Tv.release();inliers.release();projection.release()
        }
    }

    fun analyze(run: ScanRun, progress:(done:Int,total:Int)->Unit): JSONObject {
        require(run.isClosed && run.resultIsValid()) { "Only completed valid runs may be analyzed" }
        val frames=JSONObject(File(run.directory,"manifest.json").readText()).getJSONArray("frames")
        val pairs=SparsePolicy.candidatePairs(frames.length())
        require(pairs.isNotEmpty()) { "At least two images required" }
        val id=UUID.randomUUID().toString()
        val report=JSONObject().put("analysisId",id).put("runId",run.id)
            .put("appVersion","android-" + BuildConfig.VERSION_NAME)
            .put("algorithm","ORB+Hamming+E_RANSAC+recoverPose+triangulatePoints")
            .put("scale","UNKNOWN_ARBITRARY_UNIT_BASELINE")
            .put("cameraIntrinsics","ESTIMATED_NOT_CALIBRATED")
            .put("sourceFrameCount",frames.length())
            .put("status","IN_PROGRESS")
        val existingCloud=File(run.directory,"sparse_two_view.ply")
        val details=JSONArray()
        var accepted:Candidate?=null
        run.event("USER_ACTION","ANALYZE_SPARSE_TWO_VIEW",
            JSONObject().put("analysisId",id).put("candidatePairs",pairs.size))
        try {
            check(OpenCVLoader.initLocal()) { "OpenCV runtime did not initialize" }
            val orb=ORB.create(1000)
            try {
                val anchor=features(checkedFile(run,frames,0),orb)
                try {
                    for ((i,pair) in pairs.withIndex()) {
                        val second=features(checkedFile(run,frames,pair.second),orb)
                        try {
                            val outcome=try { candidate(anchor,second,0,pair.second) }
                                catch(ex:Exception) {
                                    Candidate(JSONObject().put("indexA",0).put("indexB",pair.second)
                                        .put("verdict","PAIR_ANALYSIS_ERROR")
                                        .put("errorType",ex.javaClass.simpleName),
                                        emptyList(),0.0)
                                }
                            details.put(outcome.report)
                            if(outcome.score>(accepted?.score?:0.0)) accepted=outcome
                        } finally { second.release() }
                        run.event("ANALYSIS_PROGRESS","SPARSE_TWO_VIEW",
                            JSONObject().put("done",i+1).put("total",pairs.size))
                        progress(i+1,pairs.size)
                    }
                } finally { anchor.release() }
            } finally { orb.clear() }
            report.put("evaluatedPairs",details)
            val winner=accepted
            if(winner != null && winner.vertices.isNotEmpty()) {
                val ply=SparsePolicy.asciiPly(winner.vertices)
                val temp=File(run.directory,"sparse_two_view.ply.tmp")
                temp.writeText(ply)
                check(temp.length()>150) { "Candidate PLY empty" }
                check(temp.renameTo(existingCloud)) { "Cannot finalize candidate PLY" }
                report.put("selectedPair",winner.report)
                    .put("pointCount",winner.vertices.size)
                    .put("status","SPARSE_CANDIDATE")
                    .put("warning","Approximate intrinsics, unknown scale, only two views; background and false geometry possible. NOT a finished 3D scan.")
            } else {
                // Avoid re-exporting stale PLY from a previous attempt.
                existingCloud.delete()
                report.put("status","INCONCLUSIVE")
                    .put("pointCount",0)
                    .put("warning","No pair met parallax/inlier/reprojection gates. This is not a capture failure.")
            }
            val tmp=File(run.directory,"sparse_report.json.tmp")
            tmp.writeText(report.toString(2))
            check(tmp.renameTo(File(run.directory,"sparse_report.json")))
            run.event("ANALYSIS_RESULT","SPARSE_TWO_VIEW",
                JSONObject().put("status",report.getString("status"))
                    .put("pointCount",report.getInt("pointCount"))
                    .put("pairsChecked",pairs.size))
            return report
        } catch(ex:Exception) {
            report.put("status","FAILED").put("errorType",ex.javaClass.simpleName)
            File(run.directory,"sparse_last_failure.json").writeText(report.toString(2))
            run.event("ERROR","SPARSE_TWO_VIEW",
                JSONObject().put("errorType",ex.javaClass.simpleName).put("analysisId",id))
            throw ex
        }
    }
}
