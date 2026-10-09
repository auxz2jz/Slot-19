package com.auxz2jz.videogrammetry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePolicyTest {
    @Test fun firstFrameAccepted() {
        assertTrue(FramePolicy.shouldAccept(1000, Long.MIN_VALUE, 1200))
    }

    @Test fun intervalAndReversedClockRejected() {
        assertFalse(FramePolicy.shouldAccept(1700, 1000, 1200))
        assertTrue(FramePolicy.shouldAccept(2200, 1000, 1200))
        assertFalse(FramePolicy.shouldAccept(900, 1000, 1200))
        assertFalse(FramePolicy.shouldAccept(2200, 1000, 0))
    }
}
