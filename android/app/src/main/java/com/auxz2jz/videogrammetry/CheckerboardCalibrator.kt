package com.auxz2jz.videogrammetry

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point3
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.util.UUID

/**
 * Optional saved calibration candidate. Does NOT silently change existing
 * sparse reconstructions: camera identity, crop, focus and lens must be proven.
 */
class CheckerboardCalibrator(private val context: Context) {
    private val root = File(context.filesDir,"camera_calibration").also { it.mkdirs() }
    fun interruptedPreviousRun(): Boolean =
        File(root,"calibration_in_progress.json").isFile
    fun saved(): String? = File(root,"last_checkerboard.json")
        .takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }

    fun calibrate(images: List<Uri>, progress: (Int,Int)->Unit): JSONObject {
        require(images.size in 8..40) { "Select 8–40 photos taken using one camera/lens/zoom" }
        val runId=UUID.randomUUID().toString()
        val report=JSONObject()
            .put("calibrationId",runId).put("appVersion","android-" + BuildConfig.VERSION_NAME)
            .put("targetType","CHECKERBOARD")
            .put("innerColumns",CheckerboardTarget.INNER_COLUMNS)
            .put("innerRows",CheckerboardTarget.INNER_ROWS)
            .put("squaresAcross",CheckerboardTarget.SQUARE_COLUMNS)
            .put("squaresDown",CheckerboardTarget.SQUARE_ROWS)
            .put("squareSizeMm",CheckerboardTarget.SQUARE_MM)
            .put("patternWidthMm",CheckerboardTarget.widthMm)
            .put("patternHeightMm",CheckerboardTarget.heightMm)
            .put("imageCountSelected",images.size)
            .put("status","IN_PROGRESS")
            .put("intrinsicsUse","NOT_APPLIED_TO_CAPTURE_OR_SPARSE_UNTIL_CAMERA_COMPATIBILITY_VERIFIED")
        val objectPoints=ArrayList<Mat>()
        val imagePoints=ArrayList<Mat>()
        var validWidth=0
        var validHeight=0
        val issues=JSONArray()
        val target=Size(CheckerboardTarget.INNER_COLUMNS.toDouble(),
            CheckerboardTarget.INNER_ROWS.toDouble())
        val prototype = MatOfPoint3f()
        prototype.fromList((0 until CheckerboardTarget.INNER_ROWS).flatMap { row ->
            (0 until CheckerboardTarget.INNER_COLUMNS).map { col ->
                Point3(col*CheckerboardTarget.SQUARE_MM, row*CheckerboardTarget.SQUARE_MM, 0.0)
            }
        })
        fun event(operation:String, extra: JSONObject=JSONObject()) {
            val data=JSONObject().put("operation",operation)
                .put("calibrationId",runId).put("utcEpochMs",System.currentTimeMillis())
                .put("details",extra)
            File(root,"events.jsonl").appendText(data.toString()+"\n")
        }
        File(root,"calibration_in_progress.json").writeText(
            JSONObject().put("calibrationId",runId)
                .put("appVersion","android-"+BuildConfig.VERSION_NAME)
                .put("selectedPhotoCount",images.size)
                .put("startedUtcMs",System.currentTimeMillis())
                .put("status","IN_PROGRESS_NOT_PROOF_OF_CRASH").toString(2))
        event("CALIBRATION_REQUEST",JSONObject().put("selected",images.size)
            .put("priorVideoRequired",false).put("maxDecodeDimensionPx",1200))
        try {
            check(OpenCVLoader.initLocal()) { "OpenCV calibration module unavailable" }
            for ((i,uri) in images.withIndex()) {
                val info=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it,null,info)
                }
                if (info.outWidth<=0 || info.outHeight<=0) {
                    issues.put(JSONObject().put("imageIndex",i).put("reason","UNREADABLE_IMAGE"))
                    progress(i+1,images.size);continue
                }
                var sample=1
                while(info.outWidth/sample>1200 || info.outHeight/sample>1200)sample*=2
                val bitmap=context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it,null,
                        BitmapFactory.Options().apply { inSampleSize=sample })
                }
                if(bitmap==null) {
                    issues.put(JSONObject().put("imageIndex",i).put("reason","DECODE_FAILED"))
                    progress(i+1,images.size);continue
                }
                // Bound native OpenCV memory per photo; no 4K+ decode.
                val rgba=Mat();val gray=Mat();val corners=MatOfPoint2f()
                try {
                    if (validWidth!=0 &&
                        (validWidth!=bitmap.width || validHeight!=bitmap.height)) {
                        issues.put(JSONObject().put("imageIndex",i)
                            .put("reason","MISMATCHED_RESOLUTION"))
                        continue
                    }
                    Utils.bitmapToMat(bitmap,rgba)
                    Imgproc.cvtColor(rgba,gray,Imgproc.COLOR_RGBA2GRAY)
                    val found=Calib3d.findChessboardCornersSB(gray,target,corners)
                    if(found && corners.total()==54L) {
                        if(validWidth==0) {validWidth=bitmap.width;validHeight=bitmap.height}
                        val obj=MatOfPoint3f()
                        prototype.copyTo(obj)
                        objectPoints.add(obj)
                        val measured=MatOfPoint2f()
                        corners.copyTo(measured)
                        imagePoints.add(measured)
                        event("CHESSBOARD_ACCEPTED",JSONObject().put("imageIndex",i)
                            .put("innerCorners",54))
                    } else {
                        issues.put(JSONObject().put("imageIndex",i)
                            .put("reason","NINE_BY_SIX_INNER_CORNERS_NOT_FOUND"))
                    }
                } finally {
                    rgba.release();gray.release();corners.release()
                    bitmap.recycle()
                    progress(i+1,images.size)
                }
            }
            report.put("acceptedImages",imagePoints.size)
                .put("rejectedImages",issues.length())
                .put("rejectionSummary",issues)
                .put("workingWidthPx",validWidth).put("workingHeightPx",validHeight)
            if(imagePoints.size<CheckerboardTarget.MIN_CALIBRATION_IMAGES) {
                report.put("status","INSUFFICIENT_VALID_CHESSBOARD_IMAGES")
                    .put("error","Need at least 8 valid views with varied angles")
                event("CALIBRATION_INCONCLUSIVE",JSONObject()
                    .put("acceptedImages",imagePoints.size))
                return report
            }
            val intrinsic=Mat.eye(3,3,CvType.CV_64F)
            val distortion=Mat()
            val rotations=ArrayList<Mat>()
            val translations=ArrayList<Mat>()
            try {
                val rms=Calib3d.calibrateCamera(objectPoints,imagePoints,
                    Size(validWidth.toDouble(),validHeight.toDouble()),
                    intrinsic,distortion,rotations,translations)
                val fx=intrinsic.get(0,0)[0]
                val fy=intrinsic.get(1,1)[0]
                val cx=intrinsic.get(0,2)[0]
                val cy=intrinsic.get(1,2)[0]
                val max=maxOf(validWidth,validHeight).toDouble()
                val numerical=rms.isFinite() && rms>0.0 && fx.isFinite() && fy.isFinite() &&
                    cx.isFinite() && cy.isFinite() &&
                    fx in (0.15*max)..(10.0*max) &&
                    fy in (0.15*max)..(10.0*max) &&
                    cx in 0.0..validWidth.toDouble() &&
                    cy in 0.0..validHeight.toDouble()
                val dist=JSONArray()
                for(j in 0 until distortion.total().toInt()) {
                    val value=if(distortion.rows()==1)distortion.get(0,j)[0]
                        else distortion.get(j,0)[0]
                    dist.put(if(value.isFinite()) value else JSONObject.NULL)
                }
                report.put("rmsReprojectionPx",if(rms.isFinite())rms else JSONObject.NULL)
                    .put("fxPx",fx).put("fyPx",fy)
                    .put("cxPx",cx).put("cyPx",cy)
                    .put("distortionCoefficients",dist)
                    .put("status",if(numerical && rms<=3.0)
                        "CALIBRATION_CANDIDATE" else "CALIBRATION_REVIEW_REQUIRED")
                    .put("warning","Intrinsics apply ONLY to the same camera/lens/zoom/crop and image dimensions. Verify against test imagery before any sparse analysis.")
                if(numerical && rms<=3.0) {
                    val tmp=File(root,"last_checkerboard.json.tmp")
                    tmp.writeText(report.toString(2))
                    check(tmp.renameTo(File(root,"last_checkerboard.json")))
                }
                event("CALIBRATION_RESULT",JSONObject().put("status",report.getString("status"))
                    .put("rms",rms).put("acceptedImages",imagePoints.size))
            } finally {
                intrinsic.release();distortion.release()
                rotations.forEach{it.release()};translations.forEach{it.release()}
            }
            return report
        } catch(exc:Exception) {
            report.put("status","FAILED").put("errorType",exc.javaClass.simpleName)
            event("CALIBRATION_ERROR",JSONObject().put("errorType",exc.javaClass.simpleName))
            throw exc
        } finally {
            prototype.release()
            objectPoints.forEach{it.release()}
            imagePoints.forEach{it.release()}
            File(root,"last_attempt.json").writeText(report.toString(2))
            File(root,"calibration_in_progress.json").delete()
        }
    }
}
