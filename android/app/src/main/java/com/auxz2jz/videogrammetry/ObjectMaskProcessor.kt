package com.auxz2jz.videogrammetry
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.security.MessageDigest

/** Multiple real local segmentation engines; never claim a correct object mask automatically. */
class ObjectMaskProcessor {
    private fun fingerprint(raw:ByteArray):String =
        MessageDigest.getInstance("SHA-256").digest(raw)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun selection(run:ScanRun):Pair<JSONObject,String> {
        val raw=File(run.directory,"early_object_focus_selection.json").readBytes()
        val json=JSONObject(String(raw))
        require(json.getString("runId")==run.id)
        return json to fingerprint(raw)
    }
    fun maskFile(run:ScanRun,frame:Int,engine:String):File =
        File(run.directory,"foreground_"+frame+"_"+engine+".png")
    fun activeMask(run:ScanRun,index:Int):File? {
        val report=runCatching {
            JSONObject(File(run.directory,"foreground_mask_report.json").readText())
        }.getOrNull() ?: return null
        if(report.optString("runId")!=run.id ||
            report.optString("selectionSha256")!=runCatching{selection(run).second}.getOrNull())
            return null
        if(!report.optBoolean("bothPhotosContainAcceptableMask"))return null
        val engine=report.optString("activeEngine")
        if(engine !in MaskEnginePolicy.engineNames)return null
        return maskFile(run,index,engine).takeIf {it.isFile}
    }
    private fun rect(box:FocusRect,w:Int,h:Int):Rect {
        require(box.valid())
        val x=(box.left*w).toInt().coerceIn(1,w-3)
        val y=(box.top*h).toInt().coerceIn(1,h-3)
        val r=(box.right*w).toInt().coerceIn(x+1,w-1)
        val b=(box.bottom*h).toInt().coerceIn(y+1,h-1)
        return Rect(x,y,r-x,b-y)
    }
    private fun analyzeFrame(run:ScanRun,index:Int,box:FocusRect):JSONObject {
        val photo=PhotoPointOverlayLoader().sourceFile(run,index)
        val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        BitmapFactory.decodeFile(photo.absolutePath,opts)
        require(opts.outWidth>0 && opts.outHeight>0)
        var sample=1
        while(opts.outWidth/sample>800 || opts.outHeight/sample>800)sample*=2
        val bitmap=BitmapFactory.decodeFile(photo.absolutePath,
            BitmapFactory.Options().apply{inSampleSize=sample})
            ?: error("Could not decode source image")
        val rgba=Mat();val rgb=Mat();val gray=Mat();val lap=Mat();val magnitude=Mat()
        val smooth=Mat();val gm=Mat();val bg=Mat();val fg=Mat()
        try {
            Utils.bitmapToMat(bitmap,rgba)
            Imgproc.cvtColor(rgba,rgb,Imgproc.COLOR_RGBA2BGR)
            Imgproc.cvtColor(rgba,gray,Imgproc.COLOR_RGBA2GRAY)
            Imgproc.Laplacian(gray,lap,CvType.CV_16S,3)
            Core.convertScaleAbs(lap,magnitude)
            Imgproc.blur(magnitude,smooth,Size(9.0,9.0))
            val r=rect(box,rgb.cols(),rgb.rows())
            val roiArea=r.width*r.height
            var grabValid=true
            try {
                Imgproc.grabCut(rgb,gm,r,bg,fg,3,Imgproc.GC_INIT_WITH_RECT)
            } catch(ex:Exception) {
                grabValid=false
                gm.create(rgb.rows(),rgb.cols(),CvType.CV_8UC1)
                gm.setTo(Scalar(0.0))
            }
            val engines=MaskEnginePolicy.engineNames
            val raw=engines.associateWith {ByteArray(rgb.cols()*rgb.rows())}
            var gcCount=0;var smoothCount=0;var shared=0
            val cols=rgb.cols()
            for(y in r.y until r.y+r.height) for(x in r.x until r.x+r.width) {
                val v=gm.get(y,x)?.firstOrNull()?.toInt() ?: 0
                val gc=grabValid && (v==Imgproc.GC_FGD || v==Imgproc.GC_PR_FGD)
                val low=(smooth.get(y,x)?.firstOrNull() ?: 255.0)<34.0
                if(gc)gcCount++
                if(low)smoothCount++
                if(gc && low)shared++
                for(engine in engines) if(MaskEnginePolicy.selectMask(true,gc,low,engine))
                    raw.getValue(engine)[y*cols+x]=255.toByte()
            }
            val reports=JSONArray()
            for(engine in engines) {
                val buf=raw.getValue(engine)
                val n=buf.count {it.toInt()!=0}
                val m=Mat(rgb.rows(),cols,CvType.CV_8UC1)
                try {
                    m.put(0,0,buf)
                    check(Imgcodecs.imwrite(maskFile(run,index,engine).absolutePath,m))
                } finally {m.release()}
                reports.put(JSONObject().put("engine",engine).put("foregroundPixels",n)
                    .put("roiAreaPixels",roiArea).put("coverage",n.toDouble()/roiArea)
                    .put("areaQualityValid",MaskEnginePolicy.acceptable(n,roiArea)))
            }
            return JSONObject().put("frame",index)
                .put("maskWidth",cols).put("maskHeight",rgb.rows())
                .put("grabcutWorked",grabValid).put("engineResults",reports)
                .put("grabcutVsLowTextureIoU",MaskEnginePolicy.iou(
                    shared,gcCount,smoothCount))
        } finally {
            bitmap.recycle()
            listOf(rgba,rgb,gray,lap,magnitude,smooth,gm,bg,fg).forEach{it.release()}
        }
    }
    fun compare(run:ScanRun,engine:String):JSONObject {
        require(run.isClosed && run.resultIsValid())
        require(engine in MaskEnginePolicy.engineNames)
        check(OpenCVLoader.initLocal()) { "OpenCV unavailable" }
        val (selection,sha)=selection(run)
        val frames=selection.getJSONArray("sourcePair")
        fun box(key:String):FocusRect {
            val j=selection.getJSONObject(key)
            return FocusRect(j.getDouble("left"),j.getDouble("top"),
                j.getDouble("right"),j.getDouble("bottom"))
        }
        val report=JSONObject().put("runId",run.id)
            .put("appVersion","android-"+BuildConfig.VERSION_NAME)
            .put("selectionSha256",sha).put("sourcePair",frames)
            .put("activeEngine",engine)
            .put("warning","Masks are candidate object regions. GrabCut, low-texture and consensus can still include checkerboard. Verify visually.")
            .put("status","IN_PROGRESS")
        run.event("OPERATION_START","FOREGROUND_MASK_COMPARISON",
            JSONObject().put("engine",engine))
        try {
            val a=analyzeFrame(run,frames.getInt(0),box("firstRectangle"))
            val b=analyzeFrame(run,frames.getInt(1),box("secondRectangle"))
            val results=JSONArray().put(a).put(b)
            fun acceptable(x:JSONObject):Boolean {
                val arr=x.getJSONArray("engineResults")
                return (0 until arr.length()).any {
                    val row=arr.getJSONObject(it)
                    row.getString("engine")==engine && row.optBoolean("areaQualityValid")
                }
            }
            val valid=acceptable(a) && acceptable(b)
            report.put("photos",results)
                .put("bothPhotosContainAcceptableMask",valid)
                .put("status",if(valid)"MASK_CANDIDATES_AVAILABLE" else "MASK_INCONCLUSIVE")
            val tmp=File(run.directory,"foreground_mask_report.json.tmp")
            tmp.writeText(report.toString(2))
            check(tmp.renameTo(File(run.directory,"foreground_mask_report.json")))
            run.event("ANALYSIS_RESULT","FOREGROUND_MASK_COMPARISON",
                JSONObject().put("status",report.getString("status"))
                    .put("selectedEngine",engine))
            return report
        } catch(ex:Exception) {
            report.put("status","FAILED").put("errorType",ex.javaClass.simpleName)
            File(run.directory,"foreground_mask_last_failure.json")
                .writeText(report.toString(2))
            run.event("ERROR","FOREGROUND_MASK_COMPARISON",
                JSONObject().put("errorType",ex.javaClass.simpleName))
            throw ex
        }
    }
}
