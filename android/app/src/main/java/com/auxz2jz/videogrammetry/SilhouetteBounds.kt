package com.auxz2jz.videogrammetry
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class VoxelBounds(val x0:Double,val x1:Double,val y0:Double,
    val y1:Double,val z0:Double,val z1:Double) {
    fun valid():Boolean=listOf(x0,x1,y0,y1,z0,z1).all{it.isFinite()} &&
        x1>x0 && y1>y0 && z1>z0
}

/** Experimental start volume from the actual USER-SELECTED silhouettes,
 *  not from a few textured letters or background feature points. */
object SilhouetteBounds {
    private fun dot(a:DoubleArray,b:DoubleArray):Double =
        a.indices.sumOf{a[it]*b[it]}
    private fun norm(x:DoubleArray):DoubleArray {
        val length=sqrt(dot(x,x))
        return x.map{it/length}.toDoubleArray()
    }
    private fun cameraOrigin(c:VoxelCamera):DoubleArray {
        val R=c.rotation;val t=c.translation
        return DoubleArray(3){world ->
            -(0..2).sumOf{camera->R[camera*3+world]*t[camera]}
        }
    }
    private fun ray(c:VoxelCamera,u:Double,v:Double):DoubleArray {
        val local=doubleArrayOf((u*c.width-c.cx)/c.focal,
            (v*c.height-c.cy)/c.focal,1.0)
        val R=c.rotation
        return norm(DoubleArray(3){world ->
            (0..2).sumOf{camera->R[camera*3+world]*local[camera]}
        })
    }
    fun fromTwoSilhouettes(first:FocusRect,second:FocusRect,
        a:VoxelCamera,b:VoxelCamera):VoxelBounds? {
        if(!first.valid() || !second.valid())return null
        val u0=(first.left+first.right)/2
        val v0=(first.top+first.bottom)/2
        val u1=(second.left+second.right)/2
        val v1=(second.top+second.bottom)/2
        val c0=cameraOrigin(a);val c1=cameraOrigin(b)
        val d0=ray(a,u0,v0);val d1=ray(b,u1,v1)
        val w=DoubleArray(3){c0[it]-c1[it]}
        val aa=dot(d0,d0);val bb=dot(d0,d1);val cc=dot(d1,d1)
        val dd=dot(d0,w);val ee=dot(d1,w)
        val det=aa*cc-bb*bb
        if(det<.0003)return null // almost parallel; no reliable center
        val length0=(bb*ee-cc*dd)/det
        val length1=(aa*ee-bb*dd)/det
        if(!length0.isFinite() || !length1.isFinite() ||
            length0<=.05 || length1<=.05)return null
        val p=DoubleArray(3){
            (c0[it]+length0*d0[it]+c1[it]+length1*d1[it])/2
        }
        val error=sqrt((0..2).sumOf{
            val v=c0[it]+length0*d0[it]-c1[it]-length1*d1[it]
            v*v
        })
        val width=(first.right-first.left)*a.width/a.focal*length0
        val height=(first.bottom-first.top)*a.height/a.focal*length0
        val size=max(width,height)
        if(size<=0.0 || error>size*1.5)return null
        val halfWidth=max(.025,size*.1)+width*.65
        val halfHeight=max(.025,size*.1)+height*.65
        val halfDepth=max(.025,size*1.05)
        val bounds=VoxelBounds(p[0]-halfWidth,p[0]+halfWidth,
            p[1]-halfHeight,p[1]+halfHeight,
            max(.02,p[2]-halfDepth),p[2]+halfDepth)
        return bounds.takeIf{it.valid()}
    }
}
