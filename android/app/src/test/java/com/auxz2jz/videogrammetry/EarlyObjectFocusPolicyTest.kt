package com.auxz2jz.videogrammetry
import org.junit.Assert.*
import org.junit.Test

class EarlyObjectFocusPolicyTest {
    @Test fun pairChosenBeforeAny3dRun() {
        assertEquals(0 to 6, EarlyObjectFocusPolicy.sourcePair(300))
        assertEquals(0 to 6, EarlyObjectFocusPolicy.sourcePair(7))
        assertEquals(0 to 1, EarlyObjectFocusPolicy.sourcePair(2))
        assertEquals(0 to 2, EarlyObjectFocusPolicy.sourcePair(3))
    }
    @Test fun requiresTwoSavedPhotographs() {
        try { EarlyObjectFocusPolicy.sourcePair(1); fail("Invalid") }
        catch (_:IllegalArgumentException) {}
    }
    @Test fun acceptsTwoValidButNotInvalidRois() {
        val ok=FocusRect(.2,.2,.8,.8)
        assertTrue(EarlyObjectFocusPolicy.selectionValid(ok,ok))
        assertFalse(EarlyObjectFocusPolicy.selectionValid(ok,FocusRect(.9,.3,.92,.9)))
    }
    @Test fun noFabricatedObjectWhenMissing3dTracks() {
        assertEquals("INCONCLUSIVE",EarlyObjectFocusPolicy.candidateLabel(0))
        assertEquals("EARLY_OBJECT_FOCUS_CANDIDATE",
            EarlyObjectFocusPolicy.candidateLabel(35))
    }
}
