package com.auxz2jz.videogrammetry

import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.nio.ByteBuffer
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var capture: CaptureCoordinator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capture = CaptureCoordinator(this)
        setContent { CaptureScreen(capture) }
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Normal phone rotation must NOT destroy active frame extraction,
        // calibration or geometry workers and must NOT reset their progress.
        capture.onConfigurationChanged(newConfig.orientation)
    }
    override fun onDestroy() {
        capture.shutdown()
        super.onDestroy()
    }
}

class CaptureCoordinator(private val activity: MainActivity) {
    private val repository = ScanRepository(activity)
    private val worker = Executors.newSingleThreadExecutor()
    // Separate from CameraX so a long ORB/RANSAC analysis cannot block preview.
    private val geometryWorker = Executors.newSingleThreadExecutor()
    private val calibrationWorker = Executors.newSingleThreadExecutor()
    private val checkerboardCalibrator = CheckerboardCalibrator(activity)
    private val sampling = AtomicBoolean(false)
    private val smartSelecting = AtomicBoolean(false)
    private val smartInFlight = AtomicBoolean(false)
    private val smartHadError = AtomicBoolean(false)
    private val smartSelector = SmartFrameSelector()
    private var smartLastAnalysisMs = Long.MIN_VALUE
    private var lastGuidanceReason = ""
    private var smartImageCapture: ImageCapture? = null
    private var active: ScanRun? = null
    private var activeOptions: CaptureOptions = CaptureOptions.LIVE_DEFAULT
    private var lastSampleMs = Long.MIN_VALUE
    private var provider: ProcessCameraProvider? = null
    private var disposed = false
    private var currentRun = repository.latest()

    var status by mutableStateOf("Ready — Android v" + BuildConfig.VERSION_NAME + " (experimental two-view sparse 3D)")
        private set
    var cameraBound by mutableStateOf(false)
        private set
    var importing by mutableStateOf(false)
        private set
    var liveSampling by mutableStateOf(false)
        private set
    var smartSampling by mutableStateOf(false)
        private set
    var smartAvailable by mutableStateOf(false)
        private set
    var smartGuidance by mutableStateOf("Move slowly around the object; the app will suggest the next photo")
        private set
    var smartProgress by mutableStateOf(0f)
        private set
    var visibleCount by mutableStateOf(currentRun?.frameCount ?: 0)
        private set
    var previewFile by mutableStateOf(currentRun?.recentPreview()?.absolutePath ?: "")
        private set
    private val settingsPrefs = activity.getSharedPreferences("sampling_v03", android.content.Context.MODE_PRIVATE)
    private fun loadOptions(mode: String, defaults: CaptureOptions): CaptureOptions {
        return CaptureOptions(
            settingsPrefs.getFloat(mode + "_fps", defaults.targetFps.toFloat()).toDouble()
                .coerceIn(0.5, 5.0),
            settingsPrefs.getInt(mode + "_max", defaults.maxFrames).coerceIn(10, 300)
        )
    }
    var settingsMode by mutableStateOf("VIDEO")
        private set
    private var liveOptions by mutableStateOf(loadOptions("LIVE", CaptureOptions.LIVE_DEFAULT))
    private var videoOptions by mutableStateOf(loadOptions("VIDEO", CaptureOptions.VIDEO_DEFAULT))
    private var smartOptions by mutableStateOf(loadOptions("SMART", CaptureOptions.SMART_DEFAULT))
    var savedRuns by mutableStateOf(repository.completedRunCount())
        private set
    var geometryAnalyzing by mutableStateOf(false)
        private set
    var sparseAnalyzing by mutableStateOf(false)
        private set
    var thirdViewAnalyzing by mutableStateOf(false)
        private set
    var thirdViewProgress by mutableStateOf(0f)
        private set
    var thirdViewMessage by mutableStateOf("Third-view geometry not checked yet")
        private set
    var sparseProgress by mutableStateOf(0f)
        private set
    var sparseMessage by mutableStateOf("No sparse two-view analysis yet")
        private set
    var sparseAvailable by mutableStateOf(
        currentRun?.let { File(it.directory, "sparse_two_view.ply").isFile() } ?: false)
        private set
    var earlyObjectSelected by mutableStateOf(
        currentRun?.let { File(it.directory,"early_object_focus_selection.json").isFile() } ?: false)
        private set
    var objectFocusAvailable by mutableStateOf(
        currentRun?.let { File(it.directory,"sparse_object_focus.ply").isFile() } ?: false)
        private set
    var reconstructedAvailable by mutableStateOf(
        currentRun?.let { File(it.directory,CloudArtifacts.ROI_RECONSTRUCTED_PLY).isFile() } ?: false)
        private set
    var multiviewAvailable by mutableStateOf(
        currentRun?.let { File(it.directory,CloudArtifacts.MULTIVIEW_PLY).isFile() } ?: false)
        private set
    var multiviewWorking by mutableStateOf(false)
        private set
    var multiviewProgress by mutableStateOf(0f)
        private set
    var multiviewMessage by mutableStateOf("Multi-view not checked yet")
        private set
    var objectFocusWorking by mutableStateOf(false)
        private set
    var objectFocusMessage by mutableStateOf("Object focus not selected yet")
        private set
    var savedClouds by mutableStateOf(repository.savedPlyEntries())
        private set
    var viewerCloud by mutableStateOf<PointCloud?>(null)
        private set
    var viewerStatus by mutableStateOf("Select a saved or imported PLY")
        private set
    var viewerLoading by mutableStateOf(false)
        private set
    var calibrating by mutableStateOf(false)
        private set
    var calibrationProgress by mutableStateOf(0f)
        private set
    var calibrationStatus by mutableStateOf(
        if(checkerboardCalibrator.interruptedPreviousRun())
            "Previous calibration stopped unexpectedly; export ALL Runs + Calibration Diagnostics before retrying."
        else "No camera calibration measured yet")
        private set

    var geometryProgress by mutableStateOf(0f)
        private set
    var geometrySummary by mutableStateOf("Geometric feature analysis has not run on the latest capture")
        private set
    val editingOptions: CaptureOptions
        get() = when (settingsMode) {
            "LIVE" -> liveOptions
            "SMART" -> smartOptions
            else -> videoOptions
        }
    fun chooseSettingsMode(mode: String) {
        if (!liveSampling && !smartSampling && !importing && !geometryAnalyzing && !sparseAnalyzing &&
            mode in setOf("LIVE", "VIDEO", "SMART")) settingsMode = mode
    }
    fun setTargetFps(value: Double) {
        val old = editingOptions
        val quantized = ((value * 2.0).roundToInt() / 2.0).coerceIn(0.5, 5.0)
        applyOptions(CaptureOptions(quantized, old.maxFrames))
    }
    fun setFrameLimit(value: Int) {
        val old = editingOptions
        applyOptions(CaptureOptions(old.targetFps,
            ((value / 10.0).roundToInt() * 10).coerceIn(10, 300)))
    }
    private fun applyOptions(value: CaptureOptions) {
        settingsPrefs.edit().putFloat(settingsMode + "_fps", value.targetFps.toFloat())
            .putInt(settingsMode + "_max", value.maxFrames).apply()
        when(settingsMode) {
            "LIVE" -> liveOptions = value
            "SMART" -> smartOptions = value
            else -> videoOptions = value
        }
    }
    val latestRun: ScanRun? get() = currentRun
    val hasCalibrationHistory: Boolean
        get() = File(activity.filesDir,"camera_calibration/events.jsonl").isFile

    private fun ui(action: () -> Unit) {
        if (!disposed) activity.runOnUiThread { if (!disposed) action() }
    }

    fun onConfigurationChanged(orientation: Int) {
        val phase=when {
            importing -> "VIDEO_IMPORT"
            calibrating -> "CHECKERBOARD_CALIBRATION"
            geometryAnalyzing -> "ORB_GEOMETRY"
            sparseAnalyzing -> "SPARSE_3D"
            thirdViewAnalyzing -> "THIRD_VIEW"
            objectFocusWorking -> "OBJECT_FOCUS"
            liveSampling -> "LIVE_CAMERA"
            smartSampling -> "SMART_CAMERA"
            else -> "IDLE"
        }
        currentRun?.event("STATE_TRANSITION","DEVICE_ORIENTATION_CHANGED",
            JSONObject().put("orientation",orientation).put("activePhase",phase)
                .put("operationPreserved",true))
        // CameraX PreviewView resizes/reorients with the existing view and display.
        // No unbindAll()/shutdown on orientation change.
    }

    fun bindCamera(view: PreviewView) {
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            try {
                val cameraProvider = future.get()
                if (calibrating || disposed) return@addListener
                provider = cameraProvider
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(640, 480))
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(worker) { image ->
                    try {
                        analyze(image)
                    } catch (exc: Exception) {
                        val run = active
                        if (smartSelecting.get()) {
                            smartHadError.set(true)
                            smartSelecting.set(false)
                            if (run != null && !run.isClosed) {
                                run.event("ERROR", "SMART_ANALYSIS",
                                    JSONObject().put("errorType", exc.javaClass.simpleName))
                                if (!smartInFlight.get()) finishSmart(run, false)
                            }
                        } else {
                            sampling.set(false)
                            if (run != null && !run.isClosed) {
                                run.event("ERROR", "LIVE_FRAME", JSONObject()
                                    .put("errorType", exc.javaClass.simpleName))
                                val success = run.finish(false, "Live frame processing failed")
                                finishUi(run, success)
                            }
                        }
                    } finally {
                        image.close()
                    }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                cameraBound = true
                // Leave the verified Preview + Analysis pair bound even if the
                // optional still-photo use case is unsupported on this camera.
                try {
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture)
                    smartImageCapture = imageCapture
                    smartAvailable = true
                } catch (smartError: Exception) {
                    smartImageCapture = null
                    smartAvailable = false
                }
                status = if (smartAvailable) "Camera ready — smart auto photos available"
                    else "Camera ready. Smart still capture not supported on this camera."
            } catch (exc: Exception) {
                cameraBound = false
                status = "Camera unavailable: " + exc.javaClass.simpleName
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun unbindCamera() {
        cameraBound = false
        if (smartSelecting.get()) stopSmart()
        if (sampling.get()) stopLive()
        provider?.unbindAll()
        smartAvailable = false
        smartImageCapture = null
    }

    fun startLive() {
        if (!cameraBound || importing || liveSampling || smartSampling || geometryAnalyzing || sparseAnalyzing) {
            status = "Camera is not ready or capture already active"
            return
        }
        val run = repository.create("live_camera")
        activeOptions = liveOptions
        run.configure(activeOptions)
        currentRun = run
        active = run
        lastSampleMs = Long.MIN_VALUE
        visibleCount = 0
        previewFile = ""
        run.event("USER_ACTION", "START_LIVE_SAMPLING")
        run.event("OPERATION_START", "LIVE_FRAME_SAMPLING", JSONObject()
            .put("intervalMs", activeOptions.intervalMs)
            .put("requestedFps", activeOptions.targetFps)
            .put("maxFrames", activeOptions.maxFrames))
        sampling.set(true)
        liveSampling = true
        status = "Live sampling — slowly walk around the stationary object"
    }

    fun stopLive() {
        if (!sampling.getAndSet(false) && !liveSampling) return
        liveSampling = false
        worker.execute {
            val run = active
            if (run != null && !run.isClosed) {
                run.event("USER_ACTION", "STOP_LIVE_SAMPLING")
                finishUi(run, run.finish(true))
            }
        }
    }

    private fun analyze(image: ImageProxy) {
        if (smartSelecting.get()) {
            analyzeSmart(image)
            return
        }
        if (!sampling.get()) return
        val run = active ?: return
        val now = SystemClock.elapsedRealtime()
        if (!FramePolicy.shouldAccept(now, lastSampleMs, activeOptions.intervalMs)) return
        lastSampleMs = now
        val bitmap = bitmapFromImage(image)
        try {
            val count = run.saveFrame(bitmap, now)
            ui {
                visibleCount = count
                if (count == 1 || count % 5 == 0) {
                    previewFile = run.recentPreview()?.absolutePath ?: ""
                    status = "Live: " + count + " frames actually saved"
                }
            }
            if (count >= activeOptions.maxFrames) {
                sampling.set(false)
                finishUi(run, run.finish(true))
            }
        } finally {
            bitmap.recycle()
        }
    }

    /** Third capture mode; uses actual CameraX full-resolution JPEG ImageCapture. */
    fun startSmart() {
        if (!cameraBound || !smartAvailable || smartImageCapture == null ||
            smartSampling || liveSampling || importing || geometryAnalyzing || sparseAnalyzing) {
            status = "Smart capture unavailable or another mode is already running"
            return
        }
        val run = repository.create("smart_auto")
        activeOptions = smartOptions
        run.configure(activeOptions)
        currentRun = run
        active = run
        visibleCount = 0
        previewFile = ""
        smartProgress = 0f
        lastGuidanceReason = ""
        smartSelector.reset()
        smartHadError.set(false)
        smartLastAnalysisMs = Long.MIN_VALUE
        run.event("USER_ACTION", "START_SMART_AUTO_CAPTURE")
        run.event("OPERATION_START", "SMART_AUTO_CAPTURE",
            JSONObject().put("maxPhotos", activeOptions.maxFrames)
                .put("maxShutterFps", activeOptions.targetFps)
                .put("cameraOutput", "ImageCapture original JPEG")
                .put("guidance", "approximate image change, not tracked position"))
        smartSelecting.set(true)
        smartSampling = true
        smartGuidance = "Hold steady for the first clear photo"
        status = "Smart Auto Capture active"
    }

    fun stopSmart() {
        if (!smartSelecting.getAndSet(false) && !smartSampling) return
        smartSampling = false
        val run = active
        run?.event("USER_ACTION", "STOP_SMART_AUTO_CAPTURE")
        if (smartInFlight.get()) {
            status = "Finishing the last smart photo..."
        } else if (run != null) {
            worker.execute { finishSmart(run, !smartHadError.get()) }
        }
    }

    private fun analyzeSmart(image: ImageProxy) {
        val run = active ?: return
        if (smartInFlight.get() || !smartSelecting.get()) return
        val now = SystemClock.elapsedRealtime()
        if (smartLastAnalysisMs != Long.MIN_VALUE && now - smartLastAnalysisMs < 350L) return
        smartLastAnalysisMs = now
        // Sample RGBA camera analysis memory only; no UI overlay, JPEG encoding,
        // or full-resolution Bitmap allocation on every preview frame.
        val plane = image.planes[0]
        if (plane.pixelStride != 4 || plane.rowStride < image.width * 4)
            throw IllegalStateException("Camera RGB layout cannot be analyzed")
        val buffer = plane.buffer
        val gray = IntArray(SmartFrameSelector.WIDTH * SmartFrameSelector.HEIGHT)
        for (y in 0 until SmartFrameSelector.HEIGHT) {
            val iy = (y * image.height) / SmartFrameSelector.HEIGHT
            for (x in 0 until SmartFrameSelector.WIDTH) {
                val ix = (x * image.width) / SmartFrameSelector.WIDTH
                val pos = iy * plane.rowStride + ix * plane.pixelStride
                val r = buffer.get(pos).toInt() and 255
                val g = buffer.get(pos + 1).toInt() and 255
                val b = buffer.get(pos + 2).toInt() and 255
                gray[y * SmartFrameSelector.WIDTH + x] = (r * 30 + g * 59 + b * 11) / 100
            }
        }
        val signature = SmartFrameSelector.signature(gray)
        val decision = smartSelector.decide(signature, now, activeOptions.intervalMs)
        if (lastGuidanceReason != decision.reason) {
            lastGuidanceReason = decision.reason
            run.event("FRAME_DECISION", "SMART_VIEW_ASSESSMENT",
                JSONObject().put("reason", decision.reason)
                    .put("imageChangeProxy", decision.novelty)
                    .put("instability", decision.instability)
                    .put("sharpnessProxy", decision.sharpness)
                    .put("meanBrightness", decision.brightness))
        }
        ui {
            smartProgress = decision.viewProgress
            smartGuidance = decision.guidance
        }
        if (decision.accept && smartInFlight.compareAndSet(false, true)) {
            requestSmartPhoto(run, signature, decision, now)
        }
    }

    private fun requestSmartPhoto(
        run: ScanRun, signature: SmartSignature, decision: SmartDecision, timeMs: Long
    ) {
        val capture = smartImageCapture
        if (capture == null) {
            smartSelecting.set(false)
            smartHadError.set(true)
            smartInFlight.set(false)
            run.event("ERROR", "SMART_SHUTTER", JSONObject().put("errorType", "ImageCaptureUnavailable"))
            smartHadError.set(true)
            finishSmart(run, false)
            return
        }
        val pending = File(run.directory, "pending_photo_" + System.nanoTime() + ".jpg")
        run.event("STATE_TRANSITION", "SMART_SHUTTER_REQUESTED",
            JSONObject().put("viewChange", decision.novelty))
        try {
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(pending).build(),
                worker,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            if (run.isClosed) {
                                pending.delete()
                                return
                            }
                            val count = run.saveCapturedJpeg(pending, timeMs, decision)
                            smartSelector.confirmedSaved(signature, timeMs)
                            ui {
                                visibleCount = count
                                previewFile = run.recentPreview()?.absolutePath ?: ""
                                smartGuidance = "Photo " + count + " saved — move to a new angle"
                                smartProgress = 0f
                            }
                            if (count >= activeOptions.maxFrames) smartSelecting.set(false)
                        } catch (exc: Exception) {
                            smartSelecting.set(false)
                            smartHadError.set(true)
                            run.event("ERROR", "SMART_IMAGE_CAPTURE_VALIDATE",
                                JSONObject().put("errorType", exc.javaClass.simpleName))
                        } finally {
                            smartInFlight.set(false)
                            if (!smartSelecting.get()) finishSmart(run, !smartHadError.get())
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        pending.delete()
                        smartSelecting.set(false)
                        smartHadError.set(true)
                        smartInFlight.set(false)
                        run.event("ERROR", "SMART_SHUTTER",
                            JSONObject().put("errorType", exception.javaClass.simpleName)
                                .put("cameraErrorCode", exception.imageCaptureError))
                        finishSmart(run, false)
                    }
                })
        } catch (exc: Exception) {
            pending.delete()
            smartSelecting.set(false)
            smartInFlight.set(false)
            run.event("ERROR", "SMART_SHUTTER",
                JSONObject().put("errorType", exc.javaClass.simpleName))
            finishSmart(run, false)
        }
    }

    private fun finishSmart(run: ScanRun, allowSuccess: Boolean) {
        if (run.isClosed) return
        smartSelecting.set(false)
        val success = run.finish(allowSuccess, if (allowSuccess) "" else "Smart photo failed")
        finishUi(run, success)
    }

    /** Read CameraX's raw RGBA plane without drawing preview overlays onto it. */
    private fun bitmapFromImage(image: ImageProxy): Bitmap {
        val plane = image.planes[0]
        val width = image.width
        val height = image.height
        val stride = plane.rowStride
        require(plane.pixelStride == 4 && stride >= width * 4) { "Unexpected CameraX RGBA plane" }
        val rgba = ByteArray(width * height * 4)
        val buffer = plane.buffer
        val row = ByteArray(width * 4)
        for (y in 0 until height) {
            buffer.position(y * stride)
            buffer.get(row, 0, row.size)
            System.arraycopy(row, 0, rgba, y * row.size, row.size)
        }
        val raw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        raw.copyPixelsFromBuffer(ByteBuffer.wrap(rgba))
        val rotation = image.imageInfo.rotationDegrees
        if (rotation == 0) return raw
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val corrected = Bitmap.createBitmap(raw, 0, 0, width, height, matrix, true)
        raw.recycle()
        return corrected
    }

    fun importVideo(uri: Uri) {
        if (importing || liveSampling || smartSampling || geometryAnalyzing || sparseAnalyzing) {
            status = "Finish current capture first"
            return
        }
        importing = true
        visibleCount = 0
        previewFile = ""
        status = "Opening selected video recording..."
        val chosenOptions = videoOptions
        worker.execute {
            val run = repository.create("recorded_video")
            run.configure(chosenOptions)
            currentRun = run
            active = run
            run.event("USER_ACTION", "CHOOSE_VIDEO")
            run.event("OPERATION_START", "VIDEO_FRAME_EXTRACTION", JSONObject()
                .put("intervalMs", chosenOptions.intervalMs)
                .put("requestedFps", chosenOptions.targetFps)
                .put("maxFrames", chosenOptions.maxFrames))
            val retriever = MediaMetadataRetriever()
            var valid = false
            var reason = ""
            try {
                retriever.setDataSource(activity, uri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: throw IllegalArgumentException("No video duration")
                require(durationMs > 0) { "Video duration was zero" }
                val attempts = minOf(chosenOptions.maxFrames.toLong(),
                    durationMs / chosenOptions.intervalMs + 1L).toInt()
                var missed = 0
                for (index in 0 until attempts) {
                    val ms = index.toLong() * chosenOptions.intervalMs
                    val frame = retriever.getFrameAtTime(ms * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                    if (frame == null) { missed++; continue }
                    val reduced = if (frame.width > 1280) {
                        val h = (frame.height * 1280.0 / frame.width).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(frame, 1280, h, true)
                    } else frame
                    run.saveFrame(reduced, ms)
                    if (reduced !== frame) reduced.recycle()
                    frame.recycle()
                    val count = run.frameCount
                    ui {
                        visibleCount = count
                        if (count == 1 || count % 5 == 0)
                            previewFile = run.recentPreview()?.absolutePath ?: ""
                        status = "Video: " + count + " frames saved"
                    }
                }
                run.event("STATE_TRANSITION", "VIDEO_DECODE_COMPLETE",
                    JSONObject().put("attempted", attempts).put("unavailableFrames", missed))
                valid = run.frameCount > 0
            } catch (exc: Exception) {
                reason = exc.javaClass.simpleName
                run.event("ERROR", "VIDEO_FRAME_EXTRACTION",
                    JSONObject().put("errorType", reason))
            } finally {
                runCatching { retriever.release() }
                finishUi(run, run.finish(valid, reason))
            }
        }
    }

    private fun finishUi(run: ScanRun, success: Boolean) {
        ui {
            currentRun = run
            sparseAvailable = File(run.directory, "sparse_two_view.ply").isFile()
            earlyObjectSelected = File(run.directory,"early_object_focus_selection.json").isFile()
            objectFocusAvailable = File(run.directory,CloudArtifacts.FILTERED_SCENE_PLY).isFile()
            reconstructedAvailable = File(run.directory,CloudArtifacts.ROI_RECONSTRUCTED_PLY).isFile()
            multiviewAvailable = File(run.directory,CloudArtifacts.MULTIVIEW_PLY).isFile()
            objectFocusMessage = "Select object regions after sparse analysis"
            savedClouds = repository.savedPlyEntries()
            sparseMessage = "No sparse two-view analysis for this capture"
            savedRuns = repository.completedRunCount()
            active = null
            visibleCount = run.frameCount
            previewFile = run.recentPreview()?.absolutePath ?: ""
            sampling.set(false)
            liveSampling = false
            smartSampling = false
            importing = false
            status = if (success) "Saved and validated " + run.frameCount +
                " frames. No 3D model generated."
            else "Capture FAILED — use Export Test + Diagnostics."
        }
    }

    /** Pairwise feature analysis is opt-in and never modifies captured JPEGs or run PASS. */
    fun analyzeLatestRunGeometry() {
        if (geometryAnalyzing || sparseAnalyzing || importing || liveSampling || smartSampling) {
            status = "Finish capture before analyzing geometry"
            return
        }
        val run = currentRun
        if (run == null || !run.isClosed || run.frameCount < 2) {
            status = "Capture at least two valid frames before geometry analysis"
            return
        }
        geometryAnalyzing = true
        geometryProgress = 0f
        geometrySummary = "Extracting ORB features and matching saved views..."
        run.event("USER_ACTION", "ANALYZE_LATEST_RUN_ORB_GEOMETRY")
        geometryWorker.execute {
            try {
                val report = OrbGeometryAnalyzer().analyze(run) { done, total ->
                    ui {
                        geometryProgress = if (total <= 0) 0f else
                            (done.toFloat() / total).coerceIn(0f, 1f)
                        geometrySummary = "Matched " + done + " / " + total +
                            " sampled view pairs; this is not a 3D model"
                    }
                }
                ui {
                    val supported = report.optInt("epipolarConsistentPairs")
                    val total = report.optInt("pairCount")
                    geometrySummary = "ORB/RANSAC: " + supported + " / " + total +
                        " pairs showed epipolar consistency. Background can affect results."
                    status = "Geometry diagnostics saved. Export Test + Diagnostics or ALL Runs ZIP."
                }
            } catch (exc: Exception) {
                ui {
                    geometrySummary = "Geometry analysis FAILED: " +
                        exc.javaClass.simpleName + ". Capture files remain intact."
                    status = "Export diagnostics after failed geometry test."
                }
            } finally {
                ui { geometryAnalyzing = false }
            }
        }
    }

    /** Experimental relative two-view pose, unknown scale; no validated full scan. */
    fun analyzeSparseTwoView() {
        if (sparseAnalyzing || geometryAnalyzing || multiviewWorking ||
            importing || liveSampling || smartSampling) {
            status = "Finish other captures/analyses before sparse 3D"
            return
        }
        val run = currentRun
        if (run == null || !run.isClosed || run.frameCount < 2) {
            status = "At least two saved frames are required"
            return
        }
        sparseAnalyzing = true
        sparseAvailable = false
        objectFocusAvailable = false
        reconstructedAvailable = false
        multiviewAvailable = false
        sparseProgress = 0f
        sparseMessage = "Testing bounded two-view camera poses..."
        geometryWorker.execute {
            try {
                val result = SparseTwoViewAnalyzer().analyze(run) { done, total ->
                    ui {
                        sparseProgress = if (total == 0) 0f else done.toFloat()/total
                        sparseMessage = "Evaluated " + done + " / " + total +
                            " candidate view pairs"
                    }
                }
                val early = if(result.optString("status")=="SPARSE_CANDIDATE" &&
                    File(run.directory,"early_object_focus_selection.json").isFile()) {
                    runCatching {
                        ui { sparseMessage="Detecting object-priority ORB features inside your two selected boxes..." }
                        SparseTwoViewAnalyzer().analyzeEarlySelectedObject(run)
                    }.onFailure { error ->
                        run.event("ERROR","EARLY_OBJECT_RECONSTRUCTION",
                            JSONObject().put("errorType",error.javaClass.simpleName))
                    }.getOrNull()
                } else null
                ui {
                    val success = result.optString("status") == "SPARSE_CANDIDATE"
                    sparseAvailable = success &&
                        File(run.directory,"sparse_two_view.ply").isFile()
                    savedClouds = repository.savedPlyEntries()
                    objectFocusAvailable = File(run.directory,CloudArtifacts.FILTERED_SCENE_PLY).isFile()
                    reconstructedAvailable = File(run.directory,CloudArtifacts.ROI_RECONSTRUCTED_PLY).isFile()
                    multiviewAvailable = File(run.directory,CloudArtifacts.MULTIVIEW_PLY).isFile()
                    objectFocusMessage = if(early!=null)
                        early.optString("status") + ": " +
                        early.optInt("objectCandidatePoints") +
                        " independently reconstructed ROI points; different relative coordinate frame from full scene."
                        else if (earlyObjectSelected)
                            "Object-priority reconstruction inconclusive or failed. Full-scene PLY remains available. Export diagnostics."
                        else if(success)
                            "Optional: select object in both photos after reconstruction"
                        else "No sparse points available for object focus"
                    sparseMessage = if (success)
                        "Experimental cloud: " + result.optInt("pointCount") +
                            " points from two views. Unknown scale; not a finished 3D model."
                    else "INCONCLUSIVE: no pair passed parallax and reprojection checks. See diagnostic ZIP."
                    status = "Sparse pose analysis " + result.optString("status") +
                        " — export report or point cloud when available"
                }
            } catch(ex:Exception) {
                ui {
                    sparseMessage = "Sparse 3D FAILED: " + ex.javaClass.simpleName +
                        " — saved capture untouched. Export diagnostics."
                    status = sparseMessage
                }
            } finally {
                ui { sparseAnalyzing = false }
            }
        }
    }

    /** Validates two-view XYZ against later real saved photographs; never edits PLY. */
    fun validateThirdView() {
        if (thirdViewAnalyzing || sparseAnalyzing || geometryAnalyzing ||
            liveSampling || smartSampling || importing || calibrating) {
            status = "Finish other work before third-view verification"
            return
        }
        val run=currentRun
        if (run==null || !run.isClosed ||
            !File(run.directory,"sparse_report.json").isFile()) {
            status = "Generate a valid Sparse Two Views candidate first"
            return
        }
        thirdViewAnalyzing = true
        thirdViewProgress = 0f
        thirdViewMessage = "Checking actual 3D-to-2D tracks in a third photograph..."
        geometryWorker.execute {
            try {
                val report=SparseTwoViewAnalyzer().validateThirdView(run) {done,total ->
                    ui {
                        thirdViewProgress=if(total==0)0f else done.toFloat()/total
                        thirdViewMessage="Third view " + done + "/" + total + " checked"
                    }
                }
                ui {
                    thirdViewMessage = report.optString("status") +
                        " — supported third views: " +
                        report.optInt("consistentThirdViews") + "/" +
                        report.optInt("attemptedThirdViews") +
                        "; calibration: " + report.optString("calibrationCompatibility") +
                        ". See latest/all ZIP."
                    status = "Third-view evidence saved; original sparse PLY unchanged."
                }
            } catch(ex:Exception) {
                ui {
                    thirdViewMessage="Third-view analysis failed: " +
                        ex.javaClass.simpleName + ". Capture and saved PLY preserved."
                    status = thirdViewMessage
                }
            } finally { ui { thirdViewAnalyzing=false } }
        }
    }

    /** Low-volume, privacy-safe trace for the step-by-step photo selector. */
    fun logObjectFocusUi(action: String, step: Int) {
        if(action in setOf("DRAW_VALID","DRAW_INVALID","NEXT_PHOTO",
                "BACK_PHOTO","ZOOM_IN","ZOOM_OUT","MODE_DRAW",
                "MODE_MOVE","RESET_VIEW","CREATE_REQUEST")) {
            currentRun?.event("USER_ACTION","OBJECT_FOCUS_UI_"+action,
                JSONObject().put("photoNumber",step))
        }
    }

    /** First select real saved object views BEFORE feature extraction/3D. */
    fun earlyObjectSourcePhotos(): Pair<File,File>? {
        if(sparseAnalyzing || geometryAnalyzing || thirdViewAnalyzing ||
            importing || liveSampling || smartSampling || calibrating ||
            objectFocusWorking) {
            objectFocusMessage="Finish the current task before choosing an object"
            return null
        }
        val run=currentRun ?: return null
        return try {
            val photos=ObjectFocusProcessor().sourcePhotosBeforeSparse(run)
            run.event("USER_ACTION","OPEN_EARLY_OBJECT_SELECTOR")
            photos
        } catch(ex:Exception) {
            objectFocusMessage="Complete frame extraction first: "+
                ex.javaClass.simpleName
            null
        }
    }

    fun saveEarlyObjectFocus(first:FocusRect,second:FocusRect) {
        val run=currentRun ?: return
        if(importing || sparseAnalyzing || geometryAnalyzing || calibrating ||
            liveSampling || smartSampling || thirdViewAnalyzing)return
        try {
            val result=ObjectFocusProcessor().saveBeforeSparse(run,first,second)
            earlyObjectSelected=true
            objectFocusMessage="Object selected in both photos BEFORE 3D. Tap Analyze Sparse 3D to build full-scene and independent object-priority clouds."
            status="Early object boxes saved for frames "+
                result.getJSONArray("sourcePair").getInt(0)+" and "+
                result.getJSONArray("sourcePair").getInt(1)
        } catch(ex:Exception) {
            objectFocusMessage="Could not save early object selection: "+
                ex.javaClass.simpleName
            run.event("ERROR","EARLY_OBJECT_SELECTION",
                JSONObject().put("errorType",ex.javaClass.simpleName))
        }
    }

    /** Real selected source pair photos, from saved sparse analysis. */
    fun objectFocusSourcePhotos(): Pair<File,File>? {
        if (objectFocusWorking || sparseAnalyzing || thirdViewAnalyzing ||
            geometryAnalyzing || importing || liveSampling || smartSampling) {
            objectFocusMessage="Finish the active capture or analysis first"
            return null
        }
        val run=currentRun ?: return null
        return try {
            val photos=ObjectFocusProcessor().sourcePhotos(run)
            run.event("USER_ACTION","OPEN_OBJECT_FOCUS_SELECTOR")
            photos
        } catch(ex:Exception) {
            objectFocusMessage="Re-run Analyze Sparse 3D — Two Views on this capture " +
                "to make image selection available ("+ex.javaClass.simpleName+")"
            null
        }
    }

    fun applyObjectFocus(first: FocusRect, second: FocusRect) {
        val run=currentRun ?: return
        if(objectFocusWorking || sparseAnalyzing || thirdViewAnalyzing ||
            geometryAnalyzing || multiviewWorking || liveSampling || smartSampling || importing)return
        objectFocusWorking=true
        objectFocusMessage="Matching 3D points against both object rectangles..."
        geometryWorker.execute {
            try {
                val report=ObjectFocusProcessor().apply(run,first,second)
                ui {
                    objectFocusAvailable=File(run.directory,"sparse_object_focus.ply").isFile()
                    savedClouds=repository.savedPlyEntries()
                    objectFocusMessage=report.optString("status") + ": " +
                        report.optInt("objectCandidatePoints") + " kept; " +
                        report.optInt("excludedScenePoints") + " excluded. " +
                        "Original scene PLY unchanged."
                    status="Object-focus report saved. Compare full-scene and focused clouds."
                }
            } catch(ex:Exception) {
                ui { objectFocusMessage="Object focus failed: "+ex.javaClass.simpleName +
                    ". Full-scene PLY remains unchanged; export diagnostics." }
            } finally { ui { objectFocusWorking=false } }
        }
    }

    fun analyzeObjectMultiView() {
        val run=currentRun ?: return
        if(multiviewWorking || sparseAnalyzing || thirdViewAnalyzing ||
            objectFocusWorking || geometryAnalyzing || importing ||
            liveSampling || smartSampling || calibrating) {
            multiviewMessage="Finish the active operation first"
            return
        }
        if(!run.isClosed || !earlyObjectSelected || !reconstructedAvailable) {
            multiviewMessage="Select object BEFORE Sparse 3D and analyze the ROI-first pair first"
            return
        }
        multiviewWorking=true
        multiviewProgress=0f
        multiviewMessage="Registering additional camera positions using matched 3D anchors..."
        geometryWorker.execute {
            try {
                val result=SparseTwoViewAnalyzer().analyzeObjectMultiView(run) { done,total ->
                    ui {
                        multiviewProgress=if(total==0)0f else done.toFloat()/total
                        multiviewMessage="Registered/checked "+done+" / "+total+" extra photographs"
                    }
                }
                ui {
                    multiviewAvailable=File(run.directory,CloudArtifacts.MULTIVIEW_PLY).isFile()
                    savedClouds=repository.savedPlyEntries()
                    multiviewMessage=result.optString("status")+": "+
                        result.optInt("baselinePoints")+" original target points + "+
                        result.optInt("newPointsFromAdditionalFrames")+" new XYZ tracks from "+
                        result.optInt("viewsContributingNewPoints")+" additional camera views. "+
                        "Unknown physical scale; verify shape manually."
                    status="Multi-view experiment "+result.optString("status")+
                        ". Export reports and each PLY separately."
                }
            } catch(ex:Exception) {
                ui {
                    multiviewAvailable=File(run.directory,CloudArtifacts.MULTIVIEW_PLY).isFile()
                    multiviewMessage="Multi-view FAILED: "+ex.javaClass.simpleName+
                        ". Original full-scene and object 2-view clouds untouched; export diagnostics."
                }
            } finally { ui { multiviewWorking=false } }
        }
    }

    fun exportSeparateCloud(uri: Uri, name:String) {
        val run=currentRun ?: return
        if(multiviewWorking || sparseAnalyzing || objectFocusWorking ||
            thirdViewAnalyzing || importing || liveSampling || smartSampling)return
        worker.execute {
            try {
                val n=repository.exportSeparatePly(run,uri,name)
                ui { status="Exported "+name+" ("+n+" bytes)" }
            } catch(ex:Exception) {
                ui { status="PLY export failed: "+ex.javaClass.simpleName }
            }
        }
    }

    fun exportObjectFocusPly(uri: Uri) {
        val run=currentRun ?: return
        if(objectFocusWorking || sparseAnalyzing || thirdViewAnalyzing || importing)return
        worker.execute {
            try {
                val count=repository.exportObjectFocusPly(run,uri)
                ui { status="Object-focused PLY exported ("+count+" bytes)" }
            } catch(ex:Exception) {
                ui { status="Object-focused PLY export failed: "+ex.javaClass.simpleName }
            }
        }
    }

    fun exportSparsePly(uri: Uri) {
        val run = currentRun ?: return
        if (sparseAnalyzing || thirdViewAnalyzing || geometryAnalyzing ||
            liveSampling || smartSampling || importing) return
        worker.execute {
            try {
                val size = repository.exportSparsePly(run, uri)
                ui { status = "Experimental sparse PLY exported (" + size +
                    " bytes, arbitrary units)" }
            } catch(ex:Exception) {
                ui { status = "Sparse PLY export failed: " + ex.javaClass.simpleName }
            }
        }
    }


    fun refreshClouds() { savedClouds = repository.savedPlyEntries() }

    fun openSavedCloud(entry: SavedPlyEntry) {
        if (viewerLoading || sparseAnalyzing) return
        viewerLoading = true
        viewerStatus = "Reading saved PLY point coordinates..."
        geometryWorker.execute {
            try {
                val points = entry.file.inputStream().use {
                    PlyParser.parse(it,entry.sourceKind + " · " +
                        entry.runId.takeLast(8))
                }
                currentRun?.event("USER_ACTION", "VIEW_SAVED_PLY", JSONObject()
                    .put("selectedRunId",entry.runId).put("pointCount",points.count))
                ui {
                    viewerCloud = points
                    viewerStatus = entry.sourceKind + ": " + points.count +
                        " colored points. Rotate or zoom; physical scale unknown."
                }
            } catch (ex: Exception) {
                currentRun?.event("ERROR", "VIEW_SAVED_PLY",
                    JSONObject().put("errorType",ex.javaClass.simpleName))
                ui { viewerStatus = "Unable to open PLY: " + ex.javaClass.simpleName }
            } finally { ui { viewerLoading = false } }
        }
    }
    fun openImportedCloud(uri: Uri) {
        if (viewerLoading) return
        viewerLoading = true
        viewerStatus = "Reading imported PLY..."
        geometryWorker.execute {
            try {
                val stream = activity.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Cannot read PLY")
                val cloud = stream.use { PlyParser.parse(it,"Imported PLY") }
                currentRun?.event("USER_ACTION","VIEW_IMPORTED_PLY",
                    JSONObject().put("pointCount",cloud.count))
                ui {
                    viewerCloud = cloud
                    viewerStatus = cloud.count.toString() +
                        " imported points. Source coordinates remain unscaled."
                }
            } catch(ex:Exception) {
                currentRun?.event("ERROR","VIEW_IMPORTED_PLY",
                    JSONObject().put("errorType",ex.javaClass.simpleName))
                ui { viewerStatus = "Unable to parse PLY: " + ex.javaClass.simpleName }
            } finally { ui { viewerLoading = false } }
        }
    }

    /** Opt-in calibration import only; do not apply to unknown image/crop pipeline. */
    fun calibrateCheckerboard(images: List<Uri>) {
        if (images.isEmpty()) return
        if (calibrating || importing || liveSampling || smartSampling ||
            sparseAnalyzing || geometryAnalyzing) {
            calibrationStatus = "Finish the other activity before calibration"
            return
        }
        calibrating = true
        calibrationProgress = 0f
        calibrationStatus = "Preparing calibration independently of imported video..."
        // Stop unused CameraX buffers during native OpenCV calibration.
        // No active capture is permitted by the checks above.
        unbindCamera()
        val calStart=System.currentTimeMillis()
        currentRun?.event("OPERATION_START","CHECKERBOARD_CALIBRATION",
            JSONObject().put("selectedImages",images.size))
        calibrationWorker.execute {
            try {
                val report = checkerboardCalibrator.calibrate(images) { done,total ->
                    ui {
                        calibrationProgress = done.toFloat()/total
                        calibrationStatus = "Checked " + done + "/" + total +
                            " checkerboard photos..."
                    }
                }
                ui {
                    calibrationStatus = report.optString("status") +
                        ": " + report.optInt("acceptedImages") + "/" +
                        images.size + " photos accepted, reprojection RMS " +
                        report.optString("rmsReprojectionPx","unavailable") +
                        " px. Not automatically applied to 3D."
                    status = "Checkerboard report saved; Export ALL Runs includes it."
                }
            } catch(ex:Exception) {
                currentRun?.event("ERROR","CHECKERBOARD_CALIBRATION",
                    JSONObject().put("errorType",ex.javaClass.simpleName))
                ui { calibrationStatus = "Calibration failed: " +
                    ex.javaClass.simpleName + ". See ALL Runs diagnostics." }
            } finally {
                currentRun?.event("OPERATION_END","CHECKERBOARD_CALIBRATION",
                    JSONObject().put("elapsedMs",System.currentTimeMillis()-calStart))
                ui { calibrating = false }
            }
        }
    }

    /** Records suggested filename; Android's document picker may allow renaming. */
    fun logExportName(kind: String, proposedFileName: String) {
        currentRun?.event("USER_ACTION", "EXPORT_NAME_SUGGESTED",
            JSONObject().put("kind", kind)
                .put("suggestedFileName", proposedFileName)
                .put("exportingAppVersion", BuildConfig.VERSION_NAME))
    }

    fun recordTest(looksCorrect: Boolean) {
        val run = currentRun
        if (run == null || !run.isClosed) {
            status = "No completed run — capture and validate frames first"
            return
        }
        val passed = run.recordTest(looksCorrect)
        status = if (passed) "Test PASS (still requires your device confirmation)"
            else "Test FAIL recorded — export diagnostics"
    }

    fun exportDiagnostics(uri: Uri) {
        val run = currentRun ?: return
        worker.execute {
            try {
                val bytes = repository.export(run, uri)
                ui { status = "Diagnostic ZIP exported (" + bytes + " bytes of evidence)" }
            } catch (exc: Exception) {
                ui { status = "Diagnostic export FAILED: " + exc.javaClass.simpleName }
            }
        }
    }

    fun exportAllRuns(uri: Uri) {
        if (liveSampling || smartSampling || importing) {
            status = "Finish current capture before exporting comparison history"
            return
        }
        worker.execute {
            try {
                val count = repository.exportAllRunDiagnostics(uri)
                ui { status = "Exported complete comparison history for " + count + " runs" }
            } catch (exc: Exception) {
                ui { status = "History export failed: " + exc.javaClass.simpleName }
            }
        }
    }

    fun shutdown() {
        geometryWorker.shutdownNow()
        calibrationWorker.shutdownNow()
        sampling.set(false)
        smartSelecting.set(false)
        provider?.unbindAll()
        val run = active
        if (run != null && !run.isClosed)
            worker.execute { run.finish(false, "Activity closed before operation finished") }
        worker.shutdown()
        disposed = true
    }
}

@Composable
private fun CaptureScreen(coordinator: CaptureCoordinator) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted = it }
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) coordinator.importVideo(uri) }
    val importPlyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) coordinator.openImportedCloud(uri) }
    val checkerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> coordinator.calibrateCheckerboard(uris) }
    val plyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) coordinator.exportSparsePly(uri) }
    val objectPlyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if(uri != null) coordinator.exportObjectFocusPly(uri) }
    val reconstructedPlyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if(uri!=null)
        coordinator.exportSeparateCloud(uri,CloudArtifacts.ROI_RECONSTRUCTED_PLY) }
    val multiViewPlyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if(uri!=null)
        coordinator.exportSeparateCloud(uri,CloudArtifacts.MULTIVIEW_PLY) }
    val historyPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) coordinator.exportAllRuns(uri) }
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) coordinator.exportDiagnostics(uri) }
    val preview = remember { PreviewView(context) }
    var showGuide by remember { mutableStateOf(false) }
    var showCloudViewer by remember { mutableStateOf(false) }
    var focusPhotos by remember { mutableStateOf<Pair<File,File>?>(null) }
    var selectingEarlyObject by remember { mutableStateOf(false) }
    val latest = coordinator.latestRun
    val latestBitmap = remember(coordinator.previewFile) {
        if (coordinator.previewFile.isBlank()) null else {
            // ImageCapture can save 12MP+ JPEGs. Never decode full resolution
            // merely to display a small Compose preview on the phone.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(coordinator.previewFile, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 800 || bounds.outHeight / sample > 800) {
                sample *= 2
            }
            BitmapFactory.decodeFile(coordinator.previewFile,
                BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    DisposableEffect(permissionGranted, preview, coordinator.calibrating) {
        if (permissionGranted && !coordinator.calibrating)
            coordinator.bindCamera(preview)
        onDispose { coordinator.unbindCamera() }
    }

    MaterialTheme {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Video 3D Capture Lab", style = MaterialTheme.typography.headlineSmall)
            Text("Android v" + BuildConfig.VERSION_NAME + " — Built-in point viewer + checkerboard calibration")
            Text("Keep the object stationary; move the phone slowly around it.")
            val settings = coordinator.editingOptions
            val editingAllowed = !coordinator.liveSampling && !coordinator.smartSampling &&
                !coordinator.importing && !coordinator.geometryAnalyzing && !coordinator.sparseAnalyzing
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Capture settings — choose which mode to configure",
                        style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((key, title) in listOf("LIVE" to "Live", "VIDEO" to "Video",
                            "SMART" to "Smart")) {
                            FilterChip(
                                selected = coordinator.settingsMode == key,
                                onClick = { coordinator.chooseSettingsMode(key) },
                                label = { Text(title) },
                                enabled = editingAllowed
                            )
                        }
                    }
                    Text(if (coordinator.settingsMode == "SMART")
                        "Maximum automatic photos per second: " + settings.targetFps
                        else "Requested frames per second: " + settings.targetFps)
                    Slider(
                        value = settings.targetFps.toFloat(),
                        onValueChange = { coordinator.setTargetFps(it.toDouble()) },
                        valueRange = 0.5f..5f,
                        steps = 8,
                        enabled = editingAllowed
                    )
                    Text("Maximum saved photos/frames: " + settings.maxFrames)
                    Slider(
                        value = settings.maxFrames.toFloat(),
                        onValueChange = { coordinator.setFrameLimit(it.roundToInt()) },
                        valueRange = 10f..300f,
                        steps = 28,
                        enabled = editingAllowed
                    )
                    Text(when (coordinator.settingsMode) {
                        "LIVE" -> FrameRateAdvice.LIVE
                        "SMART" -> FrameRateAdvice.SMART
                        else -> FrameRateAdvice.VIDEO
                    }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FrameRateAdvice.STORAGE,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!permissionGranted) {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Enable Camera")
                }
            } else {
                AndroidView(factory = { preview },
                    modifier = Modifier.fillMaxWidth().height(240.dp))
                if (coordinator.liveSampling) {
                    Button(onClick = { coordinator.stopLive() }) { Text("Stop Live Sampling") }
                } else {
                    Button(onClick = { coordinator.startLive() },
                        enabled = coordinator.cameraBound && !coordinator.importing && !coordinator.smartSampling && !coordinator.geometryAnalyzing && !coordinator.sparseAnalyzing) {
                        Text("Start Live Sampling")
                    }
                }
            }
            if (coordinator.smartSampling) {
                Button(onClick = { coordinator.stopSmart() }) { Text("Stop Smart Auto Capture") }
            } else {
                Button(onClick = { coordinator.startSmart() },
                    enabled = coordinator.smartAvailable && !coordinator.liveSampling && !coordinator.importing && !coordinator.geometryAnalyzing) {
                    Text("Start Smart Auto Capture")
                }
            }
            if (coordinator.smartSampling) {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Next photo guidance: " + coordinator.smartGuidance)
                        LinearProgressIndicator(
                            progress = { coordinator.smartProgress },
                            modifier = Modifier.fillMaxWidth())
                        Text("View-change indicator (approximate, not degrees or physical distance)")
                    }
                }
            }
            Button(onClick = { videoPicker.launch(arrayOf("video/*")) },
                enabled = !coordinator.liveSampling && !coordinator.smartSampling && !coordinator.importing && !coordinator.geometryAnalyzing) {
                Text("Choose Video")
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(coordinator.status)
                    Text("Saved frames: " + coordinator.visibleCount)
                    Text("Settings apply to the selected mode; the app records each run's requested and measured capture rate.")
                    Text("Completed runs saved: " + coordinator.savedRuns)
                }
            }
            if (latestBitmap != null) {
                Text("Last saved frame — check image orientation, color and detail:")
                Image(latestBitmap.asImageBitmap(), "Last saved frame",
                    Modifier.fillMaxWidth().height(185.dp), contentScale = ContentScale.Fit)
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Feature matching and geometric consistency",
                        style = MaterialTheme.typography.titleMedium)
                    Text("ORB detects real visual features. Hamming matching + fundamental-matrix RANSAC checks consistency between neighboring saved views. Background, flat objects or too little motion can mislead it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { coordinator.analyzeLatestRunGeometry() },
                        enabled = latest != null && latest.isClosed && latest.frameCount >= 2 &&
                            !coordinator.liveSampling && !coordinator.smartSampling &&
                            !coordinator.importing && !coordinator.geometryAnalyzing) {
                        Text(if (coordinator.geometryAnalyzing) "Analyzing saved photos..."
                            else "Analyze Latest Run — ORB Geometry")
                    }
                    if (coordinator.geometryAnalyzing) LinearProgressIndicator(
                        progress = { coordinator.geometryProgress },
                        modifier = Modifier.fillMaxWidth())
                    Text(coordinator.geometrySummary)
                }
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Experimental sparse 3D — two views",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Finds relative camera pose and triangulates a small point cloud. Intrinsics are estimated; geometry has arbitrary scale, and background can dominate.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Recommended: choose your object BEFORE reconstructing. " +
                        "These boxes will guide a separate feature-detection and reconstruction pass; " +
                        "the original full-scene point cloud is also preserved.")
                    Button(onClick={
                        selectingEarlyObject=true
                        focusPhotos=coordinator.earlyObjectSourcePhotos()
                    }, enabled=latest!=null && latest.isClosed &&
                        latest.frameCount>=2 && !coordinator.importing &&
                        !coordinator.geometryAnalyzing && !coordinator.sparseAnalyzing &&
                        !coordinator.thirdViewAnalyzing && !coordinator.calibrating &&
                        !coordinator.objectFocusWorking) {
                        Text(if(coordinator.earlyObjectSelected)
                            "Change Object BEFORE Sparse 3D"
                            else "Select Object BEFORE Sparse 3D")
                    }
                    if(coordinator.earlyObjectSelected) {
                        Text("Early object rectangles saved. Analyze Sparse 3D now to " +
                            "produce a separate ROI-priority point cloud.")
                    }
                    Button(onClick = { coordinator.analyzeSparseTwoView() },
                        enabled = latest != null && latest.isClosed && latest.frameCount >= 2 &&
                            !coordinator.liveSampling && !coordinator.smartSampling &&
                            !coordinator.importing && !coordinator.geometryAnalyzing &&
                            !coordinator.sparseAnalyzing) {
                        Text(if (coordinator.sparseAnalyzing) "Analyzing two-view pose..."
                            else "Analyze Sparse 3D — Two Views")
                    }
                    if (coordinator.sparseAnalyzing) LinearProgressIndicator(
                        progress = { coordinator.sparseProgress },
                        modifier = Modifier.fillMaxWidth())
                    Text(coordinator.sparseMessage)
                    Text("Object reconstruction — independent early-ROI model",
                        style=MaterialTheme.typography.titleMedium)
                    Text("Features were detected INSIDE both object rectangles BEFORE 3D. "+
                        "Its coordinates are independent from the full-scene cloud.")
                    Button(onClick={
                        coordinator.refreshClouds()
                        val chosen=coordinator.savedClouds.firstOrNull {
                            it.runId==latest?.id &&
                                it.file.name==CloudArtifacts.ROI_RECONSTRUCTED_PLY
                        }
                        if(chosen!=null) {
                            coordinator.openSavedCloud(chosen)
                            showCloudViewer=true
                        }
                    }, enabled=coordinator.reconstructedAvailable &&
                        !coordinator.multiviewWorking && !coordinator.sparseAnalyzing) {
                        Text("View ROI-First Reconstruction — 3D")
                    }
                    Button(onClick={
                        coordinator.latestRun?.let {
                            val name=ExportNames.reconstructedPly(
                                BuildConfig.VERSION_NAME,it.id)
                            coordinator.logExportName("ROI_RECONSTRUCTED_PLY",name)
                            reconstructedPlyPicker.launch(name)
                        }
                    }, enabled=coordinator.reconstructedAvailable &&
                        !coordinator.multiviewWorking && !coordinator.sparseAnalyzing) {
                        Text("Export ROI-First Reconstruction PLY")
                    }
                    Text("Multi-view experiment — adds new XYZ tracks from registered "+
                        "third/fourth/etc. photos into one LOCAL ROI coordinate frame. "+
                        "No bundle adjustment or physical dimensions.")
                    Button(onClick={coordinator.analyzeObjectMultiView()},
                        enabled=coordinator.reconstructedAvailable &&
                            !coordinator.multiviewWorking && !coordinator.sparseAnalyzing &&
                            !coordinator.geometryAnalyzing && !coordinator.thirdViewAnalyzing &&
                            !coordinator.objectFocusWorking && !coordinator.importing &&
                            !coordinator.calibrating) {
                        Text(if(coordinator.multiviewWorking)
                            "Registering extra photographs..."
                            else "Build Multi-View Object Cloud — Experimental")
                    }
                    if(coordinator.multiviewWorking) LinearProgressIndicator(
                        progress={coordinator.multiviewProgress},
                        modifier=Modifier.fillMaxWidth())
                    Text(coordinator.multiviewMessage)
                    Button(onClick={
                        coordinator.refreshClouds()
                        val chosen=coordinator.savedClouds.firstOrNull {
                            it.runId==latest?.id &&
                                it.file.name==CloudArtifacts.MULTIVIEW_PLY
                        }
                        if(chosen!=null) {
                            coordinator.openSavedCloud(chosen)
                            showCloudViewer=true
                        }
                    }, enabled=coordinator.multiviewAvailable &&
                        !coordinator.multiviewWorking) {
                        Text("View Multi-View Reconstruction — 3D")
                    }
                    Button(onClick={
                        coordinator.latestRun?.let {
                            val name=ExportNames.multiviewPly(
                                BuildConfig.VERSION_NAME,it.id)
                            coordinator.logExportName("MULTIVIEW_PLY",name)
                            multiViewPlyPicker.launch(name)
                        }
                    }, enabled=coordinator.multiviewAvailable &&
                        !coordinator.multiviewWorking) {
                        Text("Export Multi-View PLY — Experimental")
                    }

                    Text("Filtered scene — legacy AFTER-reconstruction subset",
                        style=MaterialTheme.typography.titleMedium)
                    Text("Object Focus — experimental", style=MaterialTheme.typography.titleMedium)
                    Text("Draw the object rectangle in BOTH saved source photographs. " +
                        "Points must match inside both rectangles; background features " +
                        "can still contribute to camera pose. Not true segmentation.")
                    Button(onClick={
                        selectingEarlyObject=false
                        focusPhotos=coordinator.objectFocusSourcePhotos()
                    },
                        enabled=latest != null && latest.isClosed &&
                            coordinator.sparseAvailable &&
                            !coordinator.sparseAnalyzing && !coordinator.thirdViewAnalyzing &&
                            !coordinator.geometryAnalyzing && !coordinator.objectFocusWorking &&
                            !coordinator.importing && !coordinator.liveSampling &&
                            !coordinator.smartSampling) {
                        Text("Select Object in Two Photos")
                    }
                    if(coordinator.objectFocusWorking) LinearProgressIndicator(
                        modifier=Modifier.fillMaxWidth())
                    Text(coordinator.objectFocusMessage)
                    Button(onClick={
                        coordinator.refreshClouds()
                        val focused=coordinator.savedClouds.firstOrNull {
                            it.runId==latest?.id && it.file.name=="sparse_object_focus.ply"
                        }
                        if(focused!=null) {
                            coordinator.openSavedCloud(focused)
                            showCloudViewer=true
                        }
                    }, enabled=coordinator.objectFocusAvailable &&
                        !coordinator.objectFocusWorking) {
                        Text("View Filtered Scene — 3D")
                    }
                    Button(onClick={
                        coordinator.latestRun?.let { run ->
                            val name=ExportNames.objectFocusPly(BuildConfig.VERSION_NAME,run.id)
                            coordinator.logExportName("OBJECT_FOCUS_PLY",name)
                            objectPlyPicker.launch(name)
                        }
                    }, enabled=coordinator.objectFocusAvailable &&
                        !coordinator.objectFocusWorking && !coordinator.sparseAnalyzing) {
                        Text("Export Filtered Scene PLY — Experimental")
                    }
                    Button(onClick={ coordinator.validateThirdView() },
                        enabled=latest!=null && latest.isClosed &&
                            coordinator.sparseAvailable &&
                            !coordinator.liveSampling && !coordinator.smartSampling &&
                            !coordinator.importing && !coordinator.sparseAnalyzing &&
                            !coordinator.geometryAnalyzing && !coordinator.thirdViewAnalyzing &&
                            !coordinator.calibrating) {
                        Text(if(coordinator.thirdViewAnalyzing)
                            "Checking third-view geometry..." else
                            "Verify Sparse Points in Third View")
                    }
                    if(coordinator.thirdViewAnalyzing) LinearProgressIndicator(
                        progress={coordinator.thirdViewProgress},
                        modifier=Modifier.fillMaxWidth())
                    Text(coordinator.thirdViewMessage)
                    Button(onClick = {
                        coordinator.refreshClouds()
                        showCloudViewer = true
                        coordinator.savedClouds.firstOrNull()?.let {
                            coordinator.openSavedCloud(it)
                        }
                    }, enabled = coordinator.savedClouds.isNotEmpty() &&
                        !coordinator.sparseAnalyzing && !coordinator.geometryAnalyzing) {
                        Text("View Sparse Points — 3D")
                    }
                    Button(onClick = {
                        showCloudViewer = true
                        importPlyPicker.launch(arrayOf("*/*"))
                    }, enabled = !coordinator.sparseAnalyzing &&
                        !coordinator.geometryAnalyzing) {
                        Text("Open a PLY File to View")
                    }
                    Button(onClick = {
                        coordinator.latestRun?.let { run ->
                            val name = ExportNames.sparsePly(BuildConfig.VERSION_NAME, run.id)
                            coordinator.logExportName("SPARSE_PLY", name)
                            plyPicker.launch(name)
                        }
                    }, enabled = coordinator.sparseAvailable &&
                        !coordinator.sparseAnalyzing && !coordinator.geometryAnalyzing &&
                        !coordinator.importing && !coordinator.liveSampling &&
                        !coordinator.smartSampling) {
                        Text("Export Sparse PLY — Experimental")
                    }
                }
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Checkerboard camera calibration",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Your US Letter pattern: 9 x 6 INNER corners, 10 x 7 squares, " +
                        "25.0 mm squares; whole checkerboard 250 x 175 mm. " +
                        "Long side runs along the 11-inch paper edge. Print at Actual Size and verify 25 mm with a ruler.")
                    Text("Take 12–20 sharp photos of the flat board at different tilts and " +
                        "positions, using the SAME rear camera, lens and zoom. " +
                        "Keep all 54 inner corners visible. Even light, minimal glare; " +
                        "a white outer backing is acceptable if the corners are distinct.")
                    Button(onClick = { checkerPicker.launch(arrayOf("image/*")) },
                        enabled = !coordinator.calibrating && !coordinator.liveSampling &&
                            !coordinator.smartSampling && !coordinator.importing &&
                            !coordinator.geometryAnalyzing && !coordinator.sparseAnalyzing) {
                        Text("Calibrate Using Checkerboard Photos")
                    }
                    if (coordinator.calibrating) LinearProgressIndicator(
                        progress = { coordinator.calibrationProgress },
                        modifier = Modifier.fillMaxWidth())
                    Text(coordinator.calibrationStatus)
                    Text("Calibration works independently of video import. The camera " +
                        "preview pauses during processing to reduce memory pressure. " +
                        "Afterward you can export calibration diagnostics even with no video runs.")
                    Text("Camera calibration candidate is saved separately. " +
                        "It is NOT automatically applied to other camera modes until verified.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = { showGuide = true },
                enabled = !coordinator.liveSampling && !coordinator.smartSampling && !coordinator.importing && !coordinator.geometryAnalyzing) {
                Text("Test This Version")
            }
            Button(onClick = {
                coordinator.latestRun?.let { run ->
                    val name = ExportNames.latestRunZip(BuildConfig.VERSION_NAME, run.id)
                    coordinator.logExportName("LATEST_RUN_ZIP", name)
                    exportPicker.launch(name)
                }
            }, enabled = latest != null && latest.isClosed && !coordinator.geometryAnalyzing) {
                Text("Export Test + Diagnostics")
            }
            Button(onClick = {
                val name = ExportNames.allRunsZip(BuildConfig.VERSION_NAME)
                coordinator.logExportName("ALL_RUNS_ZIP", name)
                historyPicker.launch(name)
            }, enabled = (coordinator.savedRuns > 0 || coordinator.hasCalibrationHistory) &&
                !coordinator.liveSampling && !coordinator.smartSampling &&
                !coordinator.importing && !coordinator.geometryAnalyzing &&
                !coordinator.calibrating) {
                Text("Export ALL Runs + Calibration Diagnostics")
            }
            Text("Export filenames automatically include the installed version, run ID (when applicable) and UTC time. Older run metadata keeps its original creation version.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("All completed runs remain stored separately. Latest-run ZIP exports only one run; All Runs ZIP includes every run's reports (no raw photos).",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Reconstruction: experimental two-view sparse points plus independent third-view pose check; no dense model.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    focusPhotos?.let { photos ->
        ObjectFocusDialog(
            photos=photos,
            onDismiss={focusPhotos=null},
            onUiEvent={action,photoNumber ->
                coordinator.logObjectFocusUi(action,photoNumber)
            },
            onConfirm={first,second ->
                focusPhotos=null
                if(selectingEarlyObject) coordinator.saveEarlyObjectFocus(first,second)
                else coordinator.applyObjectFocus(first,second)
            })
    }

    if (showCloudViewer) SparseViewerDialog(
        coordinator=coordinator,
        onClose={showCloudViewer=false},
        onImport={importPlyPicker.launch(arrayOf("*/*"))}
    )

    if (showGuide) AlertDialog(
        onDismissRequest = { showGuide = false },
        title = { Text("Test This Version — v" + BuildConfig.VERSION_NAME) },
        text = { Text("1. Choose Video (a handheld recording circling a stationary object), " +
            "or Start Live Sampling, or Start Smart Auto Capture to take photos " +
            "when the image is steady, sharp and sufficiently different.\n\n" +
            "2. Wait until the saved/validated frame count appears. Look at the saved-frame preview " +
            "for correct orientation and sharpness.\n\n" +
            "3. Tap Frames Look Correct only if the result is actually correct. " +
            "Otherwise tap Expected Behavior Failed.\n\n" +
            "4. Try the same stationary object at 0.5, 1, 2 and 3 FPS using the rate slider, then Export ALL Runs + FPS Comparison. " +
            "Look for useful sharp views and sufficient overlap; higher FPS alone is not a PASS. " +
            "5. After a capture, tap Analyze Latest Run — ORB Geometry and inspect matched-pair counts. " +
            "A low match count is not a capture-file failure; a high count is not a reconstructed model. " +
            "6. Tap Analyze Sparse 3D — Two Views. If a cloud passes checks, Export Sparse PLY. " +
            "Inspect it separately; scale is not known. " +
            "7. Tap View Sparse Points — 3D: drag/pinch, switch saved runs, " +
            "and verify point colors/relative shape; if viewing fails use Open a PLY File. " +
            "8. Optionally calibrate using 12–20 images of the printed 9x6 checkerboard. " +
            "Inspect accepted image count and RMS; calibration is not applied to sparse reconstruction yet. " +
            "9. After generating sparse PLY, tap Verify Sparse Points in Third View. " +
            "Inspect successful third-view count or inconclusive reason in diagnostic ZIP. " +
            "This checks geometry but is not a full 3D scan. " +
            "10. NEW RECOMMENDED ORDER: After extracting frames but BEFORE tapping Analyze Sparse 3D, " +
            "tap Select Object BEFORE Sparse 3D. Photo 1 opens large. " +
            "Drag ONE finger around the object, use + or - and Move to pan if needed, tap Next Photo, " +
            "mark the SAME physical object in Photo 2, and tap Create. " +
            "Then tap Analyze Sparse 3D — Two Views: full-scene pose and an independent ROI-priority ORB model run. " +
            "The ROI object model uses its OWN two-view pose and coordinates, not a combined mesh. " +
            "Legacy Select Object in Two Photos AFTER analysis still provides a subtractive filter. " +
            "11. Test CALIBRATION with no video imported; use Export ALL Runs + Calibration Diagnostics " +
            "even if there are no completed runs. Check camera preview returns afterward. " +
            "12. Turn phone sideways during video processing or calibration; check operation continues " +
            "rather than resetting. " +
            "13. If calibration stops the app, reopen and look for interrupted-calibration message. " +
            "In Draw box mode, drag ONE finger to select the object; use + and − to zoom, " +
            "or switch to Move to pan a zoomed image. Tap Next Photo. " +
            "Draw the same object in Photo 2 and tap Create Object PLY. " +
            "NEW: Compare Full scene, ROI-First Reconstruction and Filtered Scene as distinct clouds, " +
            "and export each to different filenames. Then tap Build Multi-View Object Cloud, " +
            "inspect registered third-camera count and NEW triangulated XYZ points, " +
            "compare ROI-first and multi-view from the same relative coordinate frame. " +
            "point counts, then export each separate PLY. Use a bad/empty region to test " +
            "the inconclusive path. New diagnostics should contain object_focus_report.json.") },
        confirmButton = {
            TextButton(onClick = { coordinator.recordTest(true); showGuide = false }) {
                Text("Frames Look Correct")
            }
        },
        dismissButton = {
            TextButton(onClick = { coordinator.recordTest(false); showGuide = false }) {
                Text("Expected Behavior Failed")
            }
        }
    )
}

@Composable
private fun SparseViewerDialog(
    coordinator: CaptureCoordinator,
    onClose: () -> Unit,
    onImport: () -> Unit
) {
    val context=LocalContext.current
    val surface=remember { SparseCloudView(context) }
    var showChoices by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(3.5f) }
    var focus by remember { mutableStateOf(true) }
    var showColors by remember { mutableStateOf(true) }
    Dialog(onDismissRequest=onClose,
        properties=DialogProperties(usePlatformDefaultWidth=false)) {
        OutlinedCard(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(12.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Sparse Point Cloud Viewer", style=MaterialTheme.typography.titleLarge)
                Text("Separate scene, ROI-first, filtered, and multi-view XYZ sets. " +
                    "Drag to rotate; pinch to zoom. Not a finished 3D model.")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Box {
                        Button(onClick={ showChoices=true }) {
                            Text("Saved scans (" + coordinator.savedClouds.size + ")")
                        }
                        DropdownMenu(expanded=showChoices,onDismissRequest={showChoices=false}) {
                            for(entry in coordinator.savedClouds) {
                                DropdownMenuItem(
                                    text={ Text(entry.sourceKind + " · " + entry.runId.takeLast(8) +
                                        " · " + entry.points + " pts") },
                                    onClick={
                                        showChoices=false
                                        coordinator.openSavedCloud(entry)
                                    })
                            }
                        }
                    }
                    Button(onClick=onImport) { Text("Import PLY") }
                }
                if (coordinator.viewerLoading) LinearProgressIndicator(
                    modifier=Modifier.fillMaxWidth())
                Text(coordinator.viewerStatus)
                AndroidView(
                    factory={ surface },
                    update={ v ->
                        coordinator.viewerCloud?.let { v.load(it) }
                        v.pointRadius=size
                        v.focusCluster=focus
                        v.showColors=showColors
                    },
                    modifier=Modifier.fillMaxWidth().height(380.dp)
                )
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={surface.reset()}) { Text("Reset View") }
                    Button(onClick={focus=!focus}) {
                        Text(if(focus) "Show All" else "Focus Cluster")
                    }
                    Button(onClick={showColors=!showColors}) {
                        Text(if(showColors) "Monochrome" else "Point Colors")
                    }
                }
                Text("Point size: " + size.toInt())
                Slider(value=size,onValueChange={size=it},valueRange=1f..10f)
                Button(onClick=onClose,modifier=Modifier.fillMaxWidth()) {
                    Text("Close Viewer")
                }
            }
        }
    }
}
