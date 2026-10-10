package com.auxz2jz.videogrammetry
import org.junit.Test
import org.junit.Assert.*
class MaskEnginePolicyTest {
 @Test fun noMaskIsClaimedByWrongConsensus() {
  assertTrue(MaskEnginePolicy.selectMask(true,true,true,MaskEnginePolicy.CONSENSUS))
  assertFalse(MaskEnginePolicy.selectMask(true,true,false,MaskEnginePolicy.CONSENSUS))
  assertFalse(MaskEnginePolicy.selectMask(false,true,true,MaskEnginePolicy.CONSENSUS))
  assertTrue(MaskEnginePolicy.selectMask(true,false,false,MaskEnginePolicy.BOX))
 }
 @Test fun invalidFractionsFailAndIouIsStable() {
  assertFalse(MaskEnginePolicy.acceptable(0,1000))
  assertFalse(MaskEnginePolicy.acceptable(995,1000))
  assertTrue(MaskEnginePolicy.acceptable(500,1000))
  assertEquals(0.5,MaskEnginePolicy.iou(20,30,30),1e-9)
 }
 @Test fun unregisteredDifferentCoordinateFramesNeverFuse() {
  assertFalse(MaskEnginePolicy.canFuse3d(true,false,true,true))
  assertFalse(MaskEnginePolicy.canFuse3d(true,true,false,true))
  assertFalse(MaskEnginePolicy.canFuse3d(false,true,true,true))
  assertTrue(MaskEnginePolicy.canFuse3d(true,true,true,true))
 }
}