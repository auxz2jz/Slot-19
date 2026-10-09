package com.auxz2jz.videogrammetry

data class FocusRect(val left: Double, val top: Double, val right: Double, val bottom: Double) {
 fun valid(): Boolean = listOf(left,top,right,bottom).all { it.isFinite() } && left>=0.0 && top>=0.0 && right<=1.0 && bottom<=1.0 && right-left>=0.05 && bottom-top>=0.05
 fun contains(x:Double,y:Double):Boolean=x.isFinite() && y.isFinite() && x>=left && x<=right && y>=top && y<=bottom
}
data class FocusProjection(val firstX:Double,val firstY:Double,val secondX:Double,val secondY:Double)
object ObjectFocusPolicy {
 fun retainedIndices(points:List<FocusProjection>,first:FocusRect,second:FocusRect):List<Int> {
  require(first.valid() && second.valid()) { "Select an object box in both images" }
  return points.indices.filter { val p=points[it]; first.contains(p.firstX,p.firstY) && second.contains(p.secondX,p.secondY) }
 }
}
