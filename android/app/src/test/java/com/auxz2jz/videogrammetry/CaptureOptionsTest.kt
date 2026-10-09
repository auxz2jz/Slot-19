package com.auxz2jz.videogrammetry

import org.junit.Assert.*
import org.junit.Test

class CaptureOptionsTest {
    @Test fun supportedRatesAndIntervals() {
        assertEquals(2000L, CaptureOptions(0.5, 50).intervalMs)
        assertEquals(1000L, CaptureOptions(1.0, 50).intervalMs)
        assertEquals(500L, CaptureOptions(2.0, 50).intervalMs)
        assertEquals(200L, CaptureOptions(5.0, 300).intervalMs)
    }

    @Test fun rejectUnsafeConfigurations() {
        for (fps in listOf(0.0, 0.4, 5.5, 100.0)) {
            try { CaptureOptions(fps, 40); fail("Must reject " + fps) } catch (_: IllegalArgumentException) {}
        }
        for (max in listOf(0, 9, 301, 2000)) {
            try { CaptureOptions(1.0, max); fail("Must reject " + max) } catch (_: IllegalArgumentException) {}
        }
    }

    @Test fun reportedRateUsesSourceTimeNotComputeSpeed() {
        assertEquals(2.0, FrameRateAdvice.effectiveFps(listOf(0L, 500L, 1000L))!!, 0.0001)
        assertEquals(0.5, FrameRateAdvice.effectiveFps(listOf(0L, 2000L, 4000L))!!, 0.0001)
        assertNull(FrameRateAdvice.effectiveFps(listOf(4000L)))
        assertNull(FrameRateAdvice.effectiveFps(listOf(4000L, 4000L)))
    }
}
