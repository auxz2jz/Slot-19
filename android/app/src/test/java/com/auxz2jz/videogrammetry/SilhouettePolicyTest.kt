package com.auxz2jz.videogrammetry
import org.junit.Test
import org.junit.Assert.*
class SilhouettePolicyTest {
 @Test fun threeRegisteredViewsRequired() {
  assertFalse(SilhouettePolicy.validMasks(2,true))
  assertFalse(SilhouettePolicy.validMasks(5,false))
  assertTrue(SilhouettePolicy.validMasks(3,true))
 }
 @Test fun voxelsNeedConsistentCameraProjectionsAndMasks() {
  val c=VoxelCamera.anchor(0,100,100)
  assertEquals(.5,c.project(0.0,0.0,1.0)!!.first,.02)
  assertNull(c.project(0.0,0.0,-1.0))
  val pixels=ByteArray(10000){255.toByte()}
  val full=VoxelMask(c,pixels,100,100)
  assertTrue(full.contains(0.0,0.0,1.0))
  assertFalse(full.contains(0.0,0.0,-1.0))
  val points=listOf(SparseVertex(0.0,0.0,1.0,1,2,3))
  assertEquals(1,SilhouettePolicy.carveSparse(points,listOf(full,full,full)).size)
  assertTrue(SilhouettePolicy.carveSparse(points,listOf(full,full)).isEmpty())
 }
 @Test fun volumeStartsFromManualObjectSilhouetteRaysNotCheckerboardDots() {
  val first=VoxelCamera.anchor(0,100,100)
  val other=VoxelCamera(6,100,100,first.focal,first.cx,first.cy,
   first.rotation,listOf(-1.0,0.0,0.0))
  val a=FocusRect(.40,.40,.60,.60)
  val b=FocusRect(.15,.40,.35,.60)
  val bounds=SilhouetteBounds.fromTwoSilhouettes(a,b,first,other)
  assertNotNull(bounds)
  assertTrue(bounds!!.valid())
  assertTrue(bounds.z0>0)
  assertTrue(bounds.x0<0 && bounds.x1>0)
  assertNull(SilhouetteBounds.fromTwoSilhouettes(a,a,first,first))
 }
 @Test fun texturelessObjectCanHaveSilhouetteHullFromCamerasOnly() {
  val first=VoxelCamera.anchor(0,100,100)
  val other=VoxelCamera(6,100,100,first.focal,first.cx,first.cy,
   first.rotation,listOf(-1.0,0.0,0.0))
  val third=VoxelCamera(9,100,100,first.focal,first.cx,first.cy,
   first.rotation,listOf(.1,0.0,0.0))
  val mask=ByteArray(10000){255.toByte()}
  val views=listOf(VoxelMask(first,mask,100,100),
   VoxelMask(other,mask,100,100),VoxelMask(third,mask,100,100))
  val box=VoxelBounds(-.1,.1,-.1,.1,1.0,2.0)
  assertTrue(SilhouettePolicy.carveVoxels(emptyList(),views,box).isNotEmpty())
  assertTrue(SilhouettePolicy.carveVoxels(emptyList(),views).isEmpty())
 }
 @Test fun voxelBudgetBounded() {
  assertEquals(21952,SilhouettePolicy.MAX_VOXELS)
 }
}