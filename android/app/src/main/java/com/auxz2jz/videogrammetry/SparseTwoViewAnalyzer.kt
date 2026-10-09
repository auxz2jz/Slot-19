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
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.File
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
                                 val score: Double)
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
                    points.add(SparseVertex(x,y,z,Color.red(pix),Color.green(pix),Color.blue(pix)))
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
                return Candidate(report,if(valid)points else emptyList(),score)
            } finally {
                in1.release();in2.release();P1.release();P2.release();X.release()
            }
        } finally {
            P1points.release();P2points.release();K.release();EMask.release()
            R.release();t.release();poseMask.release();E?.release()
        }
    }

    fun analyze(run: ScanRun, progress:(done:Int,total:Int)->Unit): JSONObject {
        require(run.isClosed && run.resultIsValid()) { "Only completed valid runs may be analyzed" }
        val frames=JSONObject(File(run.directory,"manifest.json").readText()).getJSONArray("frames")
        val pairs=SparsePolicy.candidatePairs(frames.length())
        require(pairs.isNotEmpty()) { "At least two images required" }
        val id=UUID.randomUUID().toString()
        val report=JSONObject().put("analysisId",id).put("runId",run.id)
            .put("appVersion","android-0.5.0")
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
