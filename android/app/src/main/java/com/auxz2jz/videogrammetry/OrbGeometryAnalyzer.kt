package com.auxz2jz.videogrammetry

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.KeyPoint
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.File
import java.util.UUID
import kotlin.math.min

/** Analyzes existing JPGs only. Never changes capture acceptance or original JPEGs. */
class OrbGeometryAnalyzer {
    private data class Features(val keypoints: Array<KeyPoint>, val descriptors: Mat) {
        fun release() = descriptors.release()
    }
    private val maxDimension = 640.0
    private val maxFeatures = 800

    private fun extract(file: File, orb: ORB): Features {
        val info = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, info)
        require(info.outWidth > 0 && info.outHeight > 0) { "Unreadable JPG dimensions" }
        var sample = 1
        while (info.outWidth / sample > 1280 || info.outHeight / sample > 1280) sample *= 2
        val original = BitmapFactory.decodeFile(file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalArgumentException("Unable to decode frame")
        val scaled = if (original.width > maxDimension || original.height > maxDimension) {
            val factor = min(maxDimension / original.width, maxDimension / original.height)
            Bitmap.createScaledBitmap(original,
                (original.width * factor).toInt().coerceAtLeast(1),
                (original.height * factor).toInt().coerceAtLeast(1), true)
        } else original
        val rgba = Mat()
        val gray = Mat()
        val keypoints = MatOfKeyPoint()
        val descriptors = Mat()
        try {
            Utils.bitmapToMat(scaled, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            orb.detectAndCompute(gray, Mat(), keypoints, descriptors)
            return Features(keypoints.toArray(), descriptors)
        } finally {
            if (scaled !== original) scaled.recycle()
            original.recycle()
            rgba.release()
            gray.release()
            keypoints.release()
        }
    }

    private fun pair(a: Features, b: Features): JSONObject {
        val matches = ArrayList<org.opencv.core.DMatch>()
        var inliers = 0
        if (!a.descriptors.empty() && !b.descriptors.empty()) {
            val matcher = BFMatcher.create(Core.NORM_HAMMING, false)
            val candidateMatches = ArrayList<MatOfDMatch>()
            try {
                matcher.knnMatch(a.descriptors, b.descriptors, candidateMatches, 2)
                for (group in candidateMatches) {
                    val list = group.toArray()
                    if (list.size >= 2 && list[0].distance < list[1].distance * 0.75f)
                        matches.add(list[0])
                }
            } finally {
                candidateMatches.forEach { it.release() }
            }
        }
        if (matches.size >= GeometryPolicy.MIN_CORRESPONDENCES) {
            val pts1 = MatOfPoint2f()
            val pts2 = MatOfPoint2f()
            val mask = Mat()
            var fundamental: Mat? = null
            try {
                pts1.fromList(matches.map { a.keypoints[it.queryIdx].pt })
                pts2.fromList(matches.map { b.keypoints[it.trainIdx].pt })
                fundamental = Calib3d.findFundamentalMat(pts1, pts2, Calib3d.FM_RANSAC,
                    1.5, 0.99, mask)
                if (!fundamental.empty() && !mask.empty()) inliers = Core.countNonZero(mask)
            } finally {
                pts1.release()
                pts2.release()
                fundamental?.release()
                mask.release()
            }
        }
        val verdict = GeometryPolicy.verdict(a.keypoints.size, b.keypoints.size,
            matches.size, inliers)
        return JSONObject()
            .put("featuresA", a.keypoints.size)
            .put("featuresB", b.keypoints.size)
            .put("ratioTestMatches", matches.size)
            .put("fundamentalRansacInliers", inliers)
            .put("inlierRatioOfMatches", GeometryPolicy.inlierFraction(matches.size, inliers))
            .put("verdict", verdict)
            .put("interpretation",
                "Epipolar inliers across full image only; background and degenerate geometry may bias result")
    }

    /**
     * Each pair uses ORB descriptor matching + 8-point fundamental RANSAC; no
     * persistent native matrices between runs and no live-camera performance cost.
     */
    fun analyze(run: ScanRun, progress: (done: Int, total: Int) -> Unit): JSONObject {
        require(run.isClosed && run.resultIsValid()) { "Analyze a completed valid capture run" }
        val manifest = JSONObject(File(run.directory, "manifest.json").readText())
        val frames = manifest.getJSONArray("frames")
        val indices = GeometryPolicy.selectedIndices(frames.length())
        require(indices.size >= 2) { "Need at least two saved photos" }
        val analysisId = UUID.randomUUID().toString()
        val summary = JSONObject()
            .put("analysisId", analysisId)
            .put("runId", run.id)
            .put("appVersion", "android-" + BuildConfig.VERSION_NAME)
            .put("engine", "opencv-4.12.0_ORB_HAMMING_0.75_F_RANSAC")
            .put("sourceFrames", frames.length())
            .put("analyzedFrames", indices.size)
            .put("skippedFrames", frames.length() - indices.size)
            .put("maxPairs", GeometryPolicy.MAX_ANALYZED_PAIRS)
            .put("resizedMaxDimension", maxDimension)
            .put("status", "IN_PROGRESS")
        val orb = try {
            check(OpenCVLoader.initLocal()) { "OpenCV native library failed to initialize" }
            ORB.create(maxFeatures)
        } catch (failure: Throwable) {
            val diagnostic = JSONObject().put("analysisId", analysisId)
                .put("runId", run.id).put("status", "FAILED")
                .put("errorType", failure.javaClass.simpleName)
                .put("engine", "opencv-4.12.0")
            File(run.directory, "geometry_last_failure.json").writeText(diagnostic.toString(2))
            run.event("ERROR", "ORB_GEOMETRY_INIT",
                JSONObject().put("errorType", failure.javaClass.simpleName))
            throw IllegalStateException("OpenCV library or ORB initialization failed", failure)
        }
        val pairs = JSONArray()
        var supported = 0
        var insufficient = 0
        var totalInliers = 0
        var totalMatches = 0
        var previous: Features? = null
        run.event("OPERATION_START", "ORB_GEOMETRY_ANALYSIS",
            JSONObject().put("analysisId", analysisId).put("pairCount", indices.size - 1))
        try {
            for ((position, index) in indices.withIndex()) {
                val entry = frames.getJSONObject(index)
                val filename = entry.getString("name")
                require(Regex("frame_[0-9]{4}\\.jpg").matches(filename)) { "Unsafe manifest frame path" }
                val current = extract(File(File(run.directory, "frames"), filename), orb)
                if (previous != null) {
                    val priorIndex = indices[position - 1]
                    val otherName = frames.getJSONObject(priorIndex).getString("name")
                    val result = pair(previous, current)
                        .put("indexA", priorIndex).put("indexB", index)
                        .put("frameA", otherName).put("frameB", filename)
                    pairs.put(result)
                    if (result.getString("verdict") == "EPIPOLAR_CONSISTENT") supported++
                    else insufficient++
                    totalInliers += result.getInt("fundamentalRansacInliers")
                    totalMatches += result.getInt("ratioTestMatches")
                    if (position == 1 || position % 8 == 0 || position == indices.lastIndex)
                        run.event("ANALYSIS_PROGRESS", "ORB_GEOMETRY_ANALYSIS",
                            JSONObject().put("donePairs", position)
                                .put("totalPairs", indices.size - 1)
                                .put("supportedPairs", supported))
                    progress(position, indices.size - 1)
                }
                previous?.release()
                previous = current
            }
            summary.put("pairCount", pairs.length())
                .put("epipolarConsistentPairs", supported)
                .put("weakOrUnavailablePairs", insufficient)
                .put("totalRatioMatches", totalMatches)
                .put("totalRansacInliers", totalInliers)
                .put("aggregateInlierFraction", GeometryPolicy.inlierFraction(totalMatches, totalInliers))
                .put("status", "COMPLETED")
                .put("warning",
                    "Evidence of 2D feature correspondences / epipolar consistency, not registered cameras, 3D reconstruction, percent scan completion, or confirmed object overlap.")
            // Analyze on a separate task after capture; overwriting this derived
            // report is OK. Original saved frames/manifests are never modified.
            val pairFile = File(run.directory, "geometry_pairs.jsonl")
            val pairTemp = File(run.directory, "geometry_pairs.jsonl.tmp")
            pairTemp.writeText((0 until pairs.length()).joinToString("\n") {
                pairs.getJSONObject(it).toString()
            } + "\n")
            check(pairTemp.renameTo(pairFile)) { "Could not finalize geometry pairs log" }
            val reportTemp = File(run.directory, "geometry_report.json.tmp")
            reportTemp.writeText(summary.toString(2))
            check(reportTemp.renameTo(File(run.directory, "geometry_report.json"))) {
                "Could not finalize geometry report"
            }
            run.event("ANALYSIS_RESULT", "ORB_GEOMETRY_ANALYSIS",
                JSONObject().put("status", "COMPLETED")
                    .put("pairCount", pairs.length())
                    .put("epipolarConsistentPairs", supported)
                    .put("weakOrUnavailablePairs", insufficient))
            return summary
        } catch (exc: Exception) {
            summary.put("status", "FAILED").put("errorType", exc.javaClass.simpleName)
            // Preserve any prior completed report; save failure separately.
            File(run.directory, "geometry_last_failure.json").writeText(summary.toString(2))
            run.event("ERROR", "ORB_GEOMETRY_ANALYSIS",
                JSONObject().put("errorType", exc.javaClass.simpleName)
                    .put("analysisId", analysisId))
            throw exc
        } finally {
            previous?.release()
            orb.clear()
        }
    }
}
