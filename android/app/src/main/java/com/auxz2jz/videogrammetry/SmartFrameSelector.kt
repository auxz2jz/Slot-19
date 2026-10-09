package com.auxz2jz.videogrammetry

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Conservative, fast image-appearance guidance, NOT metric pose, compass, or
 * scientifically validated geometric overlap. v0.2.0 experimental.
 *
 * Input must be a 64 x 48 thumbnail in grayscale (values 0..255). Caller
 * only marks an image as accepted after CameraX ImageCapture really saves it.
 */
data class SmartSignature(
    val gray: IntArray,
    val meanBrightness: Double,
    val meanEdgeStrength: Double
)

data class SmartDecision(
    val accept: Boolean,
    val reason: String,
    val guidance: String,
    val viewProgress: Float,
    val novelty: Double,
    val instability: Double,
    val brightness: Double,
    val sharpness: Double
)

class SmartFrameSelector {
    companion object {
        const val WIDTH = 64
        const val HEIGHT = 48
        const val MIN_GAP_MS = 1800L
        const val MAX_SHOTS = 30

        fun signature(values: IntArray): SmartSignature {
            require(values.size == WIDTH * HEIGHT)
            var sum = 0.0
            var edges = 0.0
            var terms = 0
            for (y in 0 until HEIGHT) {
                for (x in 0 until WIDTH) {
                    val i = y * WIDTH + x
                    val value = values[i].coerceIn(0, 255)
                    sum += value
                    if (x > 0) {
                        edges += abs(value - values[i - 1].coerceIn(0, 255))
                        terms++
                    }
                    if (y > 0) {
                        edges += abs(value - values[i - WIDTH].coerceIn(0, 255))
                        terms++
                    }
                }
            }
            return SmartSignature(values.copyOf(), sum / values.size,
                if (terms == 0) 0.0 else edges / terms)
        }

        /** Image difference after compensating for uniform brightness offset. */
        fun imageChange(a: SmartSignature, b: SmartSignature): Double {
            require(a.gray.size == b.gray.size)
            val correction = b.meanBrightness - a.meanBrightness
            var difference = 0.0
            // Compare sampled spatial points. No geometric overlap is inferred.
            for (index in a.gray.indices step 2) {
                difference += abs((b.gray[index] - a.gray[index]) - correction)
            }
            return (difference / ((a.gray.size + 1) / 2) / 255.0).coerceIn(0.0, 1.0)
        }
    }

    private var previous: SmartSignature? = null
    private var accepted: SmartSignature? = null
    private var lastCaptureMs = Long.MIN_VALUE

    fun reset() {
        previous = null
        accepted = null
        lastCaptureMs = Long.MIN_VALUE
    }

    fun decide(candidate: SmartSignature, timeMs: Long): SmartDecision {
        val instability = previous?.let { imageChange(it, candidate) } ?: 0.0
        val novelty = accepted?.let { imageChange(it, candidate) } ?: 0.0
        previous = candidate

        val progress = if (accepted == null) 1f
            else (novelty / 0.12).toFloat().coerceIn(0f, 1f)
        fun output(allow: Boolean, code: String, label: String) =
            SmartDecision(allow, code, label, progress, novelty, instability,
                candidate.meanBrightness, candidate.meanEdgeStrength)

        if (candidate.meanBrightness < 32) return output(false, "TOO_DARK", "More light needed")
        if (candidate.meanBrightness > 223) return output(false, "TOO_BRIGHT", "Reduce glare / brightness")
        if (candidate.meanEdgeStrength < 7.0) return output(false, "SOFT_IMAGE", "Focus / show more detail")
        if (instability > 0.10) return output(false, "CAMERA_MOVING", "Move slower, then hold steady")
        if (accepted != null && lastCaptureMs != Long.MIN_VALUE &&
            timeMs - lastCaptureMs < MIN_GAP_MS)
            return output(false, "COOLDOWN", "Photo saved — move toward the next angle")
        if (accepted != null && novelty < 0.07)
            return output(false, "SAME_VIEW", "Move to a new angle around the object")
        if (accepted != null && novelty > 0.39)
            return output(false, "POSSIBLE_LOST_OVERLAP",
                "View changed a lot — move back toward the last view")
        return output(true, "READY", "Good candidate — taking full-resolution photo")
    }

    fun confirmedSaved(signature: SmartSignature, timeMs: Long) {
        accepted = signature
        lastCaptureMs = timeMs
    }
}
