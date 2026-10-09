package com.auxz2jz.videogrammetry

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private lateinit var capture: CaptureCoordinator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capture = CaptureCoordinator(this)
        setContent { CaptureScreen(capture) }
    }
    override fun onDestroy() {
        capture.shutdown()
        super.onDestroy()
    }
}

class CaptureCoordinator(private val activity: MainActivity) {
    private val repository = ScanRepository(activity)
    private val worker = Executors.newSingleThreadExecutor()
    private val sampling = AtomicBoolean(false)
    private var active: ScanRun? = null
    private var lastSampleMs = Long.MIN_VALUE
    private var provider: ProcessCameraProvider? = null
    private var disposed = false
    private var currentRun = repository.latest()

    var status by mutableStateOf("Ready — no 3D reconstruction engine in v0.1.0")
        private set
    var cameraBound by mutableStateOf(false)
        private set
    var importing by mutableStateOf(false)
        private set
    var liveSampling by mutableStateOf(false)
        private set
    var visibleCount by mutableStateOf(currentRun?.frameCount ?: 0)
        private set
    var previewFile by mutableStateOf(currentRun?.recentPreview()?.absolutePath ?: "")
        private set
    val latestRun: ScanRun? get() = currentRun

    private fun ui(action: () -> Unit) {
        if (!disposed) activity.runOnUiThread { if (!disposed) action() }
    }

    fun bindCamera(view: PreviewView) {
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            try {
                val cameraProvider = future.get()
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
                        sampling.set(false)
                        if (run != null && !run.isClosed) {
                            run.event("ERROR", "LIVE_FRAME", JSONObject().put("errorType", exc.javaClass.simpleName))
                            val success = run.finish(false, "Live frame processing failed")
                            finishUi(run, success)
                        }
                    } finally {
                        image.close()
                    }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                cameraBound = true
                status = "Camera preview ready — move around a stationary object."
            } catch (exc: Exception) {
                cameraBound = false
                status = "Camera unavailable: " + exc.javaClass.simpleName
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun unbindCamera() {
        cameraBound = false
        provider?.unbindAll()
        if (sampling.get()) stopLive()
    }

    fun startLive() {
        if (!cameraBound || importing || liveSampling) {
            status = "Camera is not ready or capture already active"
            return
        }
        val run = repository.create("live_camera")
        currentRun = run
        active = run
        lastSampleMs = Long.MIN_VALUE
        visibleCount = 0
        previewFile = ""
        run.event("USER_ACTION", "START_LIVE_SAMPLING")
        run.event("OPERATION_START", "LIVE_FRAME_SAMPLING", JSONObject()
            .put("intervalMs", FramePolicy.LIVE_INTERVAL_MS).put("maxFrames", FramePolicy.LIVE_FRAME_LIMIT))
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
        if (!sampling.get()) return
        val run = active ?: return
        val now = SystemClock.elapsedRealtime()
        if (!FramePolicy.shouldAccept(now, lastSampleMs, FramePolicy.LIVE_INTERVAL_MS)) return
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
            if (count >= FramePolicy.LIVE_FRAME_LIMIT) {
                sampling.set(false)
                finishUi(run, run.finish(true))
            }
        } finally {
            bitmap.recycle()
        }
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
        if (importing || liveSampling) {
            status = "Finish current capture first"
            return
        }
        importing = true
        visibleCount = 0
        previewFile = ""
        status = "Opening selected video recording..."
        worker.execute {
            val run = repository.create("recorded_video")
            currentRun = run
            active = run
            run.event("USER_ACTION", "CHOOSE_VIDEO")
            run.event("OPERATION_START", "VIDEO_FRAME_EXTRACTION", JSONObject()
                .put("intervalMs", FramePolicy.IMPORT_INTERVAL_MS)
                .put("maxFrames", FramePolicy.IMPORT_FRAME_LIMIT))
            val retriever = MediaMetadataRetriever()
            var valid = false
            var reason = ""
            try {
                retriever.setDataSource(activity, uri)
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: throw IllegalArgumentException("No video duration")
                require(durationMs > 0) { "Video duration was zero" }
                val attempts = min(FramePolicy.IMPORT_FRAME_LIMIT,
                    (durationMs / FramePolicy.IMPORT_INTERVAL_MS + 1).toInt())
                var missed = 0
                for (index in 0 until attempts) {
                    val ms = index * FramePolicy.IMPORT_INTERVAL_MS
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
            active = null
            visibleCount = run.frameCount
            previewFile = run.recentPreview()?.absolutePath ?: ""
            sampling.set(false)
            liveSampling = false
            importing = false
            status = if (success) "Saved and validated " + run.frameCount +
                " frames. No 3D model generated."
            else "Capture FAILED — use Export Test + Diagnostics."
        }
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

    fun shutdown() {
        sampling.set(false)
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
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) coordinator.exportDiagnostics(uri) }
    val preview = remember { PreviewView(context) }
    var showGuide by remember { mutableStateOf(false) }
    val latest = coordinator.latestRun
    val latestBitmap = remember(coordinator.previewFile) {
        if (coordinator.previewFile.isBlank()) null
        else BitmapFactory.decodeFile(coordinator.previewFile)
    }

    DisposableEffect(permissionGranted, preview) {
        if (permissionGranted) coordinator.bindCamera(preview)
        onDispose { coordinator.unbindCamera() }
    }

    MaterialTheme {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Video 3D Capture Lab", style = MaterialTheme.typography.headlineSmall)
            Text("Android v0.1.0 CANDIDATE — frame capture only")
            Text("Keep the object stationary; move the phone slowly around it.")
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
                        enabled = coordinator.cameraBound && !coordinator.importing) {
                        Text("Start Live Sampling")
                    }
                }
            }
            Button(onClick = { videoPicker.launch(arrayOf("video/*")) },
                enabled = !coordinator.liveSampling && !coordinator.importing) {
                Text("Choose Video")
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(coordinator.status)
                    Text("Saved frames: " + coordinator.visibleCount)
                    Text("Live: max 30/1.2 s. Recording: max 40/1 s.")
                }
            }
            if (latestBitmap != null) {
                Text("Last saved frame — check image orientation, color and detail:")
                Image(latestBitmap.asImageBitmap(), "Last saved frame",
                    Modifier.fillMaxWidth().height(185.dp), contentScale = ContentScale.Fit)
            }
            Button(onClick = { showGuide = true },
                enabled = !coordinator.liveSampling && !coordinator.importing) {
                Text("Test This Version")
            }
            Button(onClick = {
                exportPicker.launch("Android-v0.1.0-test-diagnostics.zip")
            }, enabled = latest != null && latest.isClosed) {
                Text("Export Test + Diagnostics")
            }
            Text("Reconstruction engines: NOT IMPLEMENTED.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showGuide) AlertDialog(
        onDismissRequest = { showGuide = false },
        title = { Text("Test This Version — v0.1.0") },
        text = { Text("1. Choose Video (a handheld recording circling a stationary object), " +
            "or Start Live Sampling, move around the object, and Stop.\n\n" +
            "2. Wait until the saved/validated frame count appears. Look at the saved-frame preview " +
            "for correct orientation and sharpness.\n\n" +
            "3. Tap Frames Look Correct only if the result is actually correct. " +
            "Otherwise tap Expected Behavior Failed.\n\n" +
            "4. Export Test + Diagnostics ZIP. This does not test any 3D reconstruction.") },
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
