package com.auxz2jz.videogrammetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartFrameSelectorTest {
    private fun sample(shift: Int = 0, light: Int = 0): SmartSignature {
        val values = IntArray(SmartFrameSelector.WIDTH * SmartFrameSelector.HEIGHT) { i ->
            val x = i % SmartFrameSelector.WIDTH
            val y = i / SmartFrameSelector.WIDTH
            (((x + shift) * 53 + y * 29 + (x * y % 7) * 19) % 175 + 40 + light).coerceIn(0, 255)
        }
        return SmartFrameSelector.signature(values)
    }

    @Test fun firstGoodViewCanTriggerAndIdenticalViewsDoNot() {
        val selector = SmartFrameSelector()
        val s = sample()
        assertTrue(selector.decide(s, 1000L).accept)
        selector.confirmedSaved(s, 1000L)
        assertFalse(selector.decide(s, 3000L).accept)
        assertEquals("SAME_VIEW", selector.decide(s, 3500L).reason)
    }

    @Test fun changingViewRequiresMotionToSettle() {
        val selector = SmartFrameSelector()
        val initial = sample()
        selector.decide(initial, 1000L)
        selector.confirmedSaved(initial, 1000L)
        val moved = sample(shift = 3)
        assertEquals("CAMERA_MOVING", selector.decide(moved, 4000L).reason)
        val stable = selector.decide(moved, 4500L)
        assertTrue("stable changed view should be accepted", stable.accept)
        assertEquals("READY", stable.reason)
    }

    @Test fun rejectsDarkBrightOrBlurredImages() {
        val selector = SmartFrameSelector()
        assertEquals("TOO_DARK", selector.decide(
            SmartFrameSelector.signature(IntArray(64 * 48) { 5 }), 1000L).reason)
        assertEquals("TOO_BRIGHT", selector.decide(
            SmartFrameSelector.signature(IntArray(64 * 48) { 245 }), 1500L).reason)
        assertEquals("SOFT_IMAGE", selector.decide(
            SmartFrameSelector.signature(IntArray(64 * 48) { 120 }), 2000L).reason)
    }

    @Test fun doesNotInterpretBrightnessChangeAsMovement() {
        val s = sample()
        val brighter = sample(light = 10)
        assertTrue(SmartFrameSelector.imageChange(s, brighter) < 0.01)
    }

    @Test fun minTimeBetweenSavedPhotosAndProgressClamped() {
        val selector = SmartFrameSelector()
        val first = sample()
        selector.decide(first, 100L)
        selector.confirmedSaved(first, 100L)
        val next = sample(shift = 3)
        selector.decide(next, 500L)
        val decision = selector.decide(next, 600L)
        assertFalse(decision.accept)
        assertEquals("COOLDOWN", decision.reason)
        assertTrue(decision.viewProgress in 0f..1f)
    }
}
