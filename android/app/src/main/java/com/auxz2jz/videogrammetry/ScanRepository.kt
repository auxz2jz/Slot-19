package com.auxz2jz.videogrammetry

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class SavedPlyEntry(val runId: String, val sourceKind: String,
    val file: File, val points: Int)

/** Local project data, stored separately from original recordings. */
class ScanRepository(private val context: Context) {
    private val root = File(context.filesDir, "capture_runs").also { it.mkdirs() }
    private val globalLog = File(context.filesDir, "diagnostics/actions.jsonl")
    private val mutex = Any()

    fun create(kind: String): ScanRun {
        val id = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date()) + "_" + UUID.randomUUID().toString().take(10)
        return ScanRun(this, File(root, id).also { it.mkdirs() }, id, kind, false)
    }

    fun savedPlyEntries(): List<SavedPlyEntry> =
        (root.listFiles() ?: emptyArray()).filter { it.isDirectory }
            .flatMap { folder ->
                val source=runCatching {
                    JSONObject(File(folder,"result.json").readText()).optString("sourceKind")
                }.getOrDefault("unknown")
                val variants=listOf(
                    Triple(CloudArtifacts.SCENE_PLY,"Full scene · 2-view","sparse_report.json"),
                    Triple(CloudArtifacts.ROI_RECONSTRUCTED_PLY,
                        "Object reconstruction · ROI-first",CloudArtifacts.ROI_RECONSTRUCTED_REPORT),
                    Triple(CloudArtifacts.FILTERED_SCENE_PLY,
                        "Filtered scene · subset",CloudArtifacts.FILTERED_SCENE_REPORT),
                    Triple(CloudArtifacts.MULTIVIEW_PLY,
                        "Object reconstruction · multi-view",CloudArtifacts.MULTIVIEW_REPORT))
                variants.mapNotNull { (name,label,reportName) ->
                    val file=File(folder,name)
                    if(!file.isFile) return@mapNotNull null
                    val meta=runCatching { JSONObject(File(folder,reportName).readText()) }.getOrNull()
                    val count=if(name==CloudArtifacts.SCENE_PLY)
                        meta?.optInt("pointCount") ?: 0
                        else if(name==CloudArtifacts.MULTIVIEW_PLY)
                            meta?.optInt("totalPoints") ?: 0
                        else meta?.optInt("objectCandidatePoints") ?: 0
                    SavedPlyEntry(folder.name,source+" / "+label,file,count)
                }
            }.sortedWith(compareByDescending<SavedPlyEntry> { it.runId }.thenBy { it.sourceKind })

    fun completedRunCount(): Int = root.listFiles()?.count {
        it.isDirectory && File(it, "result.json").isFile()
    } ?: 0

    /** Compare all completed runs, retaining original runs and excluding camera media. */
    fun exportAllRunDiagnostics(uri: Uri): Int {
        val runs = root.listFiles()?.filter { it.isDirectory && File(it, "result.json").isFile() }
            ?.sortedBy { it.name } ?: emptyList()
        val calibrationLog=File(context.filesDir,"camera_calibration/events.jsonl")
        require(runs.isNotEmpty() || calibrationLog.isFile) {
            "Capture a run OR attempt checkerboard calibration before exporting history"
        }
        val files = listOf("manifest.json", "result.json", "events.jsonl",
            "test_results.json", "test_report.txt",
            "geometry_report.json", "geometry_pairs.jsonl", "geometry_last_failure.json",
            "sparse_report.json", "sparse_last_failure.json",
            "third_view_report.json", "third_view_last_failure.json",
            "sparse_point_projections.json", "object_focus_report.json",
            "object_focus_selection.json", "object_focus_last_failure.json",
            "early_object_focus_selection.json",
            "early_object_reconstruction_report.json",
            "early_object_reconstruction_last_failure.json",
            "object_multiview_report.json",
            "object_multiview_last_failure.json")
        val rows = JSONArray()
        val destination = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Cannot write history ZIP")
        destination.use { sink ->
            ZipOutputStream(sink).use { zip ->
                fun add(path: String, bytes: ByteArray) {
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
                add("export_metadata.json", JSONObject()
                    .put("exportType", "ALL_RUNS_ZIP")
                    .put("exportingAppVersion", BuildConfig.VERSION_NAME)
                    .put("exportCreatedUtcMs", System.currentTimeMillis())
                    .put("note", "Stored manifests preserve the version of the app that captured each run.").toString(2).toByteArray())
                add("README.txt", ("Exported with Android v" + BuildConfig.VERSION_NAME + ". " +
                    "Capture comparison history: all completed runs, " +
                    "without saved images or source recordings.\n" +
                    "Frame speed and edge scores alone cannot establish which 3D model is best.\n").toByteArray())
                for (folder in runs) {
                    val result = runCatching {
                        JSONObject(File(folder, "result.json").readText())
                    }.getOrElse {
                        JSONObject().put("runId", folder.name).put("status", "UNREADABLE")
                    }
                    val geometry = File(folder, "geometry_report.json").let { reportFile ->
                        if (reportFile.isFile) runCatching {
                            JSONObject(reportFile.readText())
                        }.getOrNull() else null
                    }
                    val captureVersion = runCatching {
                        JSONObject(File(folder, "manifest.json").readText())
                            .optString("appVersion", "UNKNOWN")
                    }.getOrDefault("UNKNOWN")
                    rows.put(JSONObject()
                        .put("originalCaptureAppVersion", captureVersion)
                        .put("geometryStatus", geometry?.optString("status") ?: "NOT_ANALYZED")
                        .put("geometryConsistentPairs",
                            geometry?.optInt("epipolarConsistentPairs") ?: 0)
                        .put("geometryPairCount", geometry?.optInt("pairCount") ?: 0)
                        .put("runId", folder.name)
                        .put("sourceKind", result.optString("sourceKind"))
                        .put("status", result.optString("status"))
                        .put("frameCount", result.optInt("frameCount"))
                        .put("requestedFps", result.opt("requestedFps"))
                        .put("measuredFps", result.opt("measuredFps"))
                        .put("qualitySummary", result.optJSONObject("qualitySummary")))
                    for (name in files) {
                        val source = File(folder, name)
                        if (source.isFile) add("runs/" + folder.name + "/" + name, source.readBytes())
                    }
                }
                val calDir = File(context.filesDir, "camera_calibration")
                for (name in listOf("last_checkerboard.json",
                    "last_attempt.json", "events.jsonl",
                    "calibration_in_progress.json")) {
                    val source = File(calDir,name)
                    if (source.isFile) add("calibration/"+name,source.readBytes())
                }
                add("all_runs_summary.json", JSONObject()
                    .put("appVersion", "android-" + BuildConfig.VERSION_NAME)
                    .put("exportedByAppVersion", BuildConfig.VERSION_NAME)
                    .put("exportCreatedUtcMs", System.currentTimeMillis())
                    .put("includedRunCount", runs.size)
                    .put("runs", rows).toString(2).toByteArray())
            }
        }
        return runs.size
    }

    /** PLY is intentional user export, NOT in redacted diagnostic ZIPs. */
    fun exportSparsePly(run: ScanRun, uri: Uri): Long {
        val cloud = File(run.directory, "sparse_two_view.ply")
        check(run.isClosed && cloud.isFile && cloud.length() > 150) {
            "No valid sparse PLY exists for this run"
        }
        run.event("USER_ACTION", "EXPORT_SPARSE_PLY",
            JSONObject().put("bytes", cloud.length()))
        try {
            val destination = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Cannot open selected PLY destination")
            val length = destination.use { output ->
                cloud.inputStream().use { input -> input.copyTo(output) }
            }
            run.event("EXPORT_RESULT", "SPARSE_PLY",
                JSONObject().put("success",true).put("bytes",length))
            return length
        } catch (ex: Exception) {
            run.event("ERROR", "SPARSE_PLY_EXPORT",
                JSONObject().put("errorType",ex.javaClass.simpleName))
            throw ex
        }
    }

    /** Object-only candidate is opt-in. Never overwrite the full-scene PLY. */
    fun exportObjectFocusPly(run: ScanRun, uri: Uri): Long {
        val cloud=File(run.directory,"sparse_object_focus.ply")
        check(run.isClosed && cloud.isFile && cloud.length()>150) {
            "No object-focus candidate; select the object in both source images first"
        }
        run.event("USER_ACTION","EXPORT_OBJECT_FOCUS_PLY",
            JSONObject().put("bytes",cloud.length()))
        try {
            val destination=context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Cannot open object PLY destination")
            val count=destination.use { output -> cloud.inputStream().use { it.copyTo(output) } }
            run.event("EXPORT_RESULT","OBJECT_FOCUS_PLY",
                JSONObject().put("success",true).put("bytes",count))
            return count
        } catch(ex:Exception) {
            run.event("ERROR","OBJECT_FOCUS_PLY_EXPORT",
                JSONObject().put("errorType",ex.javaClass.simpleName))
            throw ex
        }
    }

    /** Independent reconstructed object / multi-view PLY exports. */
    fun exportSeparatePly(run: ScanRun, uri: Uri, name: String): Long {
        require(name in setOf(CloudArtifacts.ROI_RECONSTRUCTED_PLY,
            CloudArtifacts.MULTIVIEW_PLY)) { "Only new reconstructed PLY variants permitted" }
        val cloud=File(run.directory,name)
        require(run.isClosed && cloud.isFile && cloud.length()>150) {
            "Requested reconstructed point cloud is unavailable"
        }
        val category=if(name==CloudArtifacts.ROI_RECONSTRUCTED_PLY)
            "ROI_RECONSTRUCTED_PLY" else "OBJECT_MULTIVIEW_PLY"
        run.event("USER_ACTION","EXPORT_"+category,
            JSONObject().put("fileKind",name).put("bytes",cloud.length()))
        try {
            val output=context.contentResolver.openOutputStream(uri)
                ?: error("Cannot open selected destination")
            val count=output.use { sink -> cloud.inputStream().use { it.copyTo(sink) } }
            run.event("EXPORT_RESULT",category,
                JSONObject().put("success",true).put("bytes",count))
            return count
        } catch(ex:Exception) {
            run.event("ERROR",category+"_EXPORT",
                JSONObject().put("errorType",ex.javaClass.simpleName))
            throw ex
        }
    }

    fun latest(): ScanRun? {
        val completed = root.listFiles()?.filter { it.isDirectory && File(it, "result.json").isFile() }
            ?.maxByOrNull { it.name } ?: return null
        val kind = runCatching {
            JSONObject(File(completed, "result.json").readText()).optString("sourceKind", "unknown")
        }.getOrDefault("unknown")
        return ScanRun(this, completed, completed.name, kind, true)
    }

    internal fun appendGlobal(line: String) {
        synchronized(mutex) {
            globalLog.parentFile?.mkdirs()
            if (globalLog.exists() && globalLog.length() > 2_000_000) {
                val old = File(globalLog.parentFile, "actions.previous.jsonl")
                if (old.exists()) old.delete()
                globalLog.renameTo(old)
            }
            globalLog.appendText(line)
        }
    }

    fun export(run: ScanRun, uri: Uri): Long {
        run.event("USER_ACTION", "EXPORT_TEST_AND_DIAGNOSTICS")
        run.event("OPERATION_START", "DIAGNOSTIC_EXPORT")
        val names = listOf("manifest.json", "result.json", "events.jsonl", "test_results.json", "test_report.txt",
            "geometry_report.json", "geometry_pairs.jsonl", "geometry_last_failure.json",
            "sparse_report.json", "sparse_last_failure.json",
            "third_view_report.json", "third_view_last_failure.json",
            "sparse_point_projections.json", "object_focus_report.json",
            "object_focus_selection.json", "object_focus_last_failure.json",
            "early_object_focus_selection.json",
            "early_object_reconstruction_report.json",
            "early_object_reconstruction_last_failure.json",
            "object_multiview_report.json",
            "object_multiview_last_failure.json")
        var total = 0L
        try {
            val output = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not open selected destination")
            output.use { stream ->
                ZipOutputStream(stream).use { zip ->
                    fun entry(name: String, bytes: ByteArray) {
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(bytes)
                        zip.closeEntry()
                        total += bytes.size
                    }
                    entry("README.txt", ("Android Video 3D Capture Lab v" + BuildConfig.VERSION_NAME + " diagnostics.\n" +
                        "No source video, camera image, filename or private URI included.\n").toByteArray())
                    val capturedVersion = runCatching {
                        JSONObject(File(run.directory, "manifest.json").readText())
                            .optString("appVersion", "UNKNOWN")
                    }.getOrDefault("UNKNOWN")
                    entry("export_metadata.json", JSONObject()
                        .put("exportType", "LATEST_RUN_ZIP")
                        .put("exportingAppVersion", BuildConfig.VERSION_NAME)
                        .put("exportCreatedUtcMs", System.currentTimeMillis())
                        .put("runId", run.id)
                        .put("originalCaptureAppVersion", capturedVersion)
                        .put("note", "Old capture and analysis metadata is preserved unchanged.")
                        .toString(2).toByteArray())
                    names.forEach { name ->
                        val file = File(run.directory, name)
                        if (file.isFile) entry(name, file.readBytes())
                    }
                }
            }
            if (total <= 0) throw IllegalStateException("Diagnostic export was empty")
            run.event("EXPORT_RESULT", "DIAGNOSTIC_EXPORT", JSONObject().put("success", true).put("bytes", total))
            return total
        } catch (exc: Exception) {
            run.event("ERROR", "DIAGNOSTIC_EXPORT", JSONObject().put("errorType", exc.javaClass.simpleName))
            throw exc
        }
    }
}

class ScanRun internal constructor(
    private val repository: ScanRepository,
    val directory: File,
    val id: String,
    val sourceKind: String,
    resume: Boolean
) {
    private val startedMs = SystemClock.elapsedRealtime()
    private val sessionId = UUID.randomUUID().toString()
    private var sequence = 0
    private val frameEntries = JSONArray()
    private var closed = false
    private var options: CaptureOptions? = null
    private var previousSignature: SmartSignature? = null
    private var nearDuplicateCount = 0
    private var imageAnalysisCount = 0
    private var processingStartedAtMs = SystemClock.elapsedRealtime()
    private var previousState = "CREATED"

    init {
        if (resume) {
            closed = true
            val manifest = File(directory, "manifest.json")
            if (manifest.isFile) {
                runCatching {
                    val saved = JSONObject(manifest.readText()).optJSONArray("frames")
                    if (saved != null) for (i in 0 until saved.length()) frameEntries.put(saved.getJSONObject(i))
                }
            }
        } else {
            File(directory, "frames").mkdirs()
            event("OPERATION_START", "RUN_CREATED", JSONObject().put("sourceKind", sourceKind))
        }
    }

    @Synchronized
    fun configure(newOptions: CaptureOptions) {
        check(!closed && frameEntries.length() == 0) { "Settings must be set before capture" }
        options = newOptions
        processingStartedAtMs = SystemClock.elapsedRealtime()
        event("CAPTURE_SETTINGS", "CONFIGURE_RUN", JSONObject()
            .put("requestedFps", newOptions.targetFps)
            .put("requestedIntervalMs", newOptions.intervalMs)
            .put("maxFrames", newOptions.maxFrames).put("mode", sourceKind))
    }

    val frameCount: Int get() = synchronized(this) { frameEntries.length() }
    val isClosed: Boolean get() = synchronized(this) { closed }

    @Synchronized
    fun event(category: String, operation: String, details: JSONObject = JSONObject()) {
        val item = JSONObject()
            .put("category", category)
            .put("operation", operation)
            .put("eventId", UUID.randomUUID().toString())
            .put("sequenceNumber", ++sequence)
            .put("timestampUtcMs", System.currentTimeMillis())
            .put("monotonicTimeMs", SystemClock.elapsedRealtime() - startedMs)
            .put("appVersion", "android-" + BuildConfig.VERSION_NAME)
            .put("sessionId", sessionId)
            .put("correlationId", id)
            .put("state", previousState)
            .put("details", details)
        val line = item.toString() + "\n"
        File(directory, "events.jsonl").appendText(line)
        repository.appendGlobal(line)
    }

    @Synchronized
    fun saveFrame(bitmap: Bitmap, sourceTimeMs: Long): Int {
        check(!closed) { "Run already finalized" }
        val index = frameEntries.length() + 1
        val name = "frame_%04d.jpg".format(Locale.US, index)
        val dir = File(directory, "frames")
        val temp = File(dir, "$name.tmp")
        val output = File(dir, name)
        try {
            FileOutputStream(temp).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)) { "JPEG encoder failed" }
                stream.fd.sync()
            }
            check(temp.length() > 100) { "JPEG output empty" }
            check(temp.renameTo(output)) { "Frame rename failed" }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(output.absolutePath, options)
            check(options.outWidth > 0 && options.outHeight > 0) { "Frame is not decodable" }
            val small = Bitmap.createScaledBitmap(bitmap, SmartFrameSelector.WIDTH,
                SmartFrameSelector.HEIGHT, true)
            val pixels = IntArray(SmartFrameSelector.WIDTH * SmartFrameSelector.HEIGHT)
            small.getPixels(pixels, 0, SmartFrameSelector.WIDTH, 0, 0,
                SmartFrameSelector.WIDTH, SmartFrameSelector.HEIGHT)
            if (small !== bitmap) small.recycle()
            val signature = SmartFrameSelector.signature(IntArray(pixels.size) { i ->
                val c = pixels[i]
                (((c shr 16) and 255) * 30 + ((c shr 8) and 255) * 59 +
                    (c and 255) * 11) / 100
            })
            val change = previousSignature?.let {
                SmartFrameSelector.imageChange(it, signature)
            }
            if (change != null && change < 0.025) nearDuplicateCount++
            previousSignature = signature
            imageAnalysisCount++
            frameEntries.put(JSONObject()
                .put("name", name).put("width", options.outWidth).put("height", options.outHeight)
                .put("bytes", output.length()).put("sha256", sha256(output))
                .put("sourceTimeMs", sourceTimeMs)
                .put("brightnessMean", signature.meanBrightness)
                .put("sharpnessProxy", signature.meanEdgeStrength)
                .put("previousViewChangeProxy", change))
            if (index == 1 || index % 5 == 0)
                event("STATE_TRANSITION", "FRAME_SAVED", JSONObject()
                    .put("frameCount", index).put("sourceTimeMs", sourceTimeMs))
            return index
        } catch (exc: Exception) {
            temp.delete()
            event("ERROR", "SAVE_FRAME", JSONObject()
                .put("errorType", exc.javaClass.simpleName).put("frameIndex", index))
            throw exc
        }
    }

    /**
     * CameraX ImageCapture saves a full-resolution JPEG with orientation EXIF.
     * Move it intact into this run; no Bitmap recompression or image overlay.
     * Only validated physical files become accepted scan photos.
     */
    @Synchronized
    fun saveCapturedJpeg(pending: File, sourceTimeMs: Long,
                         metrics: SmartDecision): Int {
        check(!closed) { "Cannot append to finalized smart capture" }
        val index = frameEntries.length() + 1
        val name = "frame_%04d.jpg".format(Locale.US, index)
        val destination = File(File(directory, "frames"), name)
        try {
            check(pending.isFile && pending.length() > 100) { "ImageCapture JPEG missing or empty" }
            check(pending.renameTo(destination)) { "Unable to move full-size JPEG to run" }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, options)
            check(options.outWidth > 0 && options.outHeight > 0) { "Full-size JPEG decode failed" }
            val entry = JSONObject()
                .put("name", name).put("width", options.outWidth).put("height", options.outHeight)
                .put("bytes", destination.length()).put("sha256", sha256(destination))
                .put("sourceTimeMs", sourceTimeMs)
                .put("captureType", "CAMERAX_IMAGE_CAPTURE_FULL_RES")
                .put("imageChangeProxy", metrics.novelty)
                .put("sharpnessProxy", metrics.sharpness)
                .put("brightnessMean", metrics.brightness)
                .put("stabilityProxy", metrics.instability)
            frameEntries.put(entry)
            event("OUTPUT_VALIDATION", "SMART_PHOTO_SAVED", JSONObject()
                .put("frameCount", index).put("width", options.outWidth)
                .put("height", options.outHeight).put("bytes", destination.length())
                .put("imageChangeProxy", metrics.novelty))
            return index
        } catch (exc: Exception) {
            pending.delete()
            destination.delete()
            event("ERROR", "SMART_PHOTO_SAVED", JSONObject()
                .put("errorType", exc.javaClass.simpleName).put("frameIndex", index))
            throw exc
        }
    }

    fun recentPreview(): File? = File(directory, "frames").listFiles()
        ?.filter { it.isFile && it.extension == "jpg" }?.maxByOrNull { it.name }

    @Synchronized
    fun finish(inputSucceeded: Boolean, reason: String = ""): Boolean {
        if (closed) return resultIsValid()
        closed = true
        previousState = "VALIDATING_OUTPUT"
        event("STATE_TRANSITION", "VALIDATE_FRAMES")
        val verified = validateSavedFrames()
        val success = inputSucceeded && verified && frameEntries.length() > 0
        val entries = (0 until frameEntries.length()).map { frameEntries.getJSONObject(it) }
        val requestedTimes = entries.map { it.optLong("sourceTimeMs", 0L) }
        val measuredFps = FrameRateAdvice.effectiveFps(requestedTimes)
        val sharpness = entries.filter { it.has("sharpnessProxy") }
            .map { it.optDouble("sharpnessProxy") }
        val brightness = entries.filter { it.has("brightnessMean") }
            .map { it.optDouble("brightnessMean") }
        val quality = JSONObject()
            .put("adjacentNearDuplicateProxyCount", nearDuplicateCount)
            .put("adjacentPairCount", (imageAnalysisCount - 1).coerceAtLeast(0))
            .put("totalSavedBytes", entries.sumOf { it.optLong("bytes", 0L) })
            .put("interpretation", "Pixel proxies only; not geometric overlap or 3D model quality")
        if (sharpness.isNotEmpty()) quality.put("meanSharpnessProxy", sharpness.average())
        if (brightness.isNotEmpty()) quality.put("meanBrightness", brightness.average())
        val processingElapsedMs = SystemClock.elapsedRealtime() - processingStartedAtMs
        File(directory, "manifest.json").writeText(JSONObject()
            .put("schemaVersion", 1).put("appVersion", "android-" + BuildConfig.VERSION_NAME)
            .put("runId", id).put("sourceKind", sourceKind)
            .put("requestedFps", options?.targetFps)
            .put("requestedMaxFrames", options?.maxFrames)
            .put("measuredFps", measuredFps)
            .put("qualitySummary", quality)
            .put("frameIntervalMs", if (sourceKind == "smart_auto") 0L else options?.intervalMs
                ?: if (sourceKind == "live_camera") FramePolicy.LIVE_INTERVAL_MS else FramePolicy.IMPORT_INTERVAL_MS)
            .put("samplingMethod", if (sourceKind == "smart_auto")
                "heuristic_quality_and_view_change_no_pose" else "fixed_interval_not_quality_filtered")
            .put("sourceTimestampNote", "Requested sampling time; not verified geometric camera pose")
            .put("frames", frameEntries).toString(2))
        val status = if (success) "PASS" else "FAIL"
        File(directory, "result.json").writeText(JSONObject()
            .put("appVersion", "android-" + BuildConfig.VERSION_NAME)
            .put("runId", id).put("sourceKind", sourceKind)
            .put("frameCount", frameEntries.length()).put("status", status)
            .put("requestedFps", options?.targetFps).put("requestedMaxFrames", options?.maxFrames)
            .put("measuredFps", measuredFps)
            .put("processingElapsedMs", processingElapsedMs)
            .put("qualitySummary", quality)
            .put("outputValidated", verified).put("reason", reason.take(100)).toString(2))
        event("OUTPUT_VALIDATION", "VALIDATE_FRAMES", JSONObject()
            .put("frameCount", frameEntries.length()).put("valid", verified)
            .put("requestedFps", options?.targetFps).put("measuredFps", measuredFps)
            .put("processingElapsedMs", processingElapsedMs)
            .put("qualitySummary", quality))
        previousState = status
        event("OPERATION_RESULT", "CAPTURE_FINISHED", JSONObject()
            .put("success", success).put("status", status).put("frameCount", frameEntries.length())
            .put("requestedFps", options?.targetFps).put("measuredFps", measuredFps))
        return success
    }

    @Synchronized
    private fun validateSavedFrames(): Boolean {
        if (frameEntries.length() == 0) return false
        val frames = File(directory, "frames")
        val actual = frames.listFiles()?.filter { it.extension == "jpg" } ?: return false
        if (actual.size != frameEntries.length()) return false
        for (i in 0 until frameEntries.length()) {
            val entry = frameEntries.getJSONObject(i)
            val name = entry.optString("name")
            if (!Regex("frame_[0-9]{4}\\.jpg").matches(name)) return false
            val file = File(frames, name)
            if (!file.isFile || file.length() != entry.optLong("bytes")) return false
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            if (opts.outWidth != entry.optInt("width") || opts.outHeight != entry.optInt("height")) return false
            if (sha256(file) != entry.optString("sha256")) return false
        }
        return true
    }

    @Synchronized
    fun resultIsValid(): Boolean = runCatching {
        val result = JSONObject(File(directory, "result.json").readText())
        result.optString("status") == "PASS" &&
            result.optInt("frameCount") == frameEntries.length() && validateSavedFrames()
    }.getOrDefault(false)

    /** Test PASS requires output validation and user visual confirmation. */
    @Synchronized
    fun recordTest(looksCorrect: Boolean): Boolean {
        event("USER_ACTION", if (looksCorrect) "FRAMES_LOOK_CORRECT" else "EXPECTED_BEHAVIOR_FAILED")
        val objective = resultIsValid()
        val passed = objective && looksCorrect
        val testStatus = if (passed) "PASS" else "FAIL"
        File(directory, "test_results.json").writeText(JSONObject()
            .put("testId", "android_v" + BuildConfig.VERSION_NAME + "_capture")
            .put("testSessionId", UUID.randomUUID().toString())
            .put("runId", id).put("objectiveOutputValid", objective)
            .put("visualResultSource", if (looksCorrect) "MANUAL_PASS" else "MANUAL_FAIL")
            .put("status", testStatus)
            .put("note", "A PASS does not establish user-verified baseline without user confirmation.")
            .toString(2))
        File(directory, "test_report.txt").writeText(
            "Android v" + BuildConfig.VERSION_NAME + " Test This Version\nRun: $id\nFrames: $frameCount\n" +
            "Output files/hashes validated: $objective\nVisual confirmation: $looksCorrect\n" +
            "Test: $testStatus\n")
        event("TEST_VERIFICATION", "VALIDATE_CAPTURE", JSONObject()
            .put("objective", objective).put("manualVisualConfirmation", looksCorrect))
        event("TEST_RESULT", "TEST_THIS_VERSION", JSONObject().put("status", testStatus))
        return passed
    }

    private fun sha256(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { stream ->
            val chunk = ByteArray(8192)
            while (true) {
                val size = stream.read(chunk)
                if (size < 0) break
                hash.update(chunk, 0, size)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }
}
