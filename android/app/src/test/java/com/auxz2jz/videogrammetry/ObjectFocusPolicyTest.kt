package com.auxz2jz.videogrammetry
import org.junit.Assert.*
import org.junit.Test
class ObjectFocusPolicyTest {
 private val box=FocusRect(0.2,0.2,0.8,0.8)
 @Test fun dualSourceFilter() {
  val points=listOf(FocusProjection(0.5,0.5,0.5,0.5),FocusProjection(0.5,0.5,0.99,0.5),FocusProjection(0.0,0.0,0.5,0.5))
  assertEquals(listOf(0),ObjectFocusPolicy.retainedIndices(points,box,box))
 }
 @Test fun acceptsBoundary() {
  assertEquals(listOf(0),ObjectFocusPolicy.retainedIndices(listOf(FocusProjection(0.2,0.8,0.8,0.2)),box,box))
 }
 @Test fun rejectsInvalidBox() {
  try { ObjectFocusPolicy.retainedIndices(emptyList(),FocusRect(0.5,0.5,0.51,0.9),box); fail() }
  catch (_:IllegalArgumentException) {}
 }
 @Test fun noAutomaticCentralGuess() {
  assertTrue(ObjectFocusPolicy.retainedIndices(listOf(FocusProjection(0.0,0.0,0.0,0.0)),box,box).isEmpty())
 }
}
