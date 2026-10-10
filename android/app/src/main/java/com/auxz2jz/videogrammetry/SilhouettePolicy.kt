package com.auxz2jz.videogrammetry

import kotlin.math.max
import kotlin.math.min

data class VoxelCamera(val frame:Int,val width:Int,val height:Int,val focal:Double,
    val cx:Double,val cy:Double,val rotation:List<Double>,val translation:List<Double>) {
    init {
        require(width>0 && height>0 && focal>0.0)
        require(rotation.size==9 && translation.size==3)
        require(rotation.all{it.isFinite()} && translation.all{it.isFinite()})
    }
    fun project(x:Double,y:Double,z:Double):Pair<Double,Double>? {
        val xx=rotation[0]*x+rotation[1]*y+rotation[2]*z+translation[0]
        val yy=rotation[3]*x+rotation[4]*y+rotation[5]*z+translation[1]
        val zz=rotation[6]*x+rotation[7]*y+rotation[8]*z+translation[2]
        if(!zz.isFinite() || zz<=0.01)return null
        val u=(focal*xx/zz+cx)/width
        val v=(focal*yy/zz+cy)/height
        if(!u.isFinite() || !v.isFinite() || u<0 || u>=1 || v<0 || v>=1)
            return null
        return u to v
    }
    companion object {
        fun anchor(frame:Int,w:Int,h:Int):VoxelCamera=VoxelCamera(frame,w,h,
            .95*max(w,h),(w-1)/2.0,(h-1)/2.0,
            listOf(1.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,1.0),
            listOf(0.0,0.0,0.0))
    }
}
data class VoxelMask(val camera:VoxelCamera,val pixels:ByteArray,val w:Int,val h:Int) {
    init{require(pixels.size==w*h && w>0 && h>0)}
    fun contains(x:Double,y:Double,z:Double):Boolean {
        val xy=camera.project(x,y,z) ?: return false
        val xx=(xy.first*w).toInt().coerceIn(0,w-1)
        val yy=(xy.second*h).toInt().coerceIn(0,h-1)
        return (pixels[yy*w+xx].toInt() and 255)>127
    }
}
/** Space carving is only meaningful when >=3 independently registered views have masks. */
object SilhouettePolicy {
    const val GRID=28
    const val MIN_REGISTERED_MASK_VIEWS=3
    const val MAX_REGISTERED_MASK_VIEWS=12
    const val MAX_VOXELS=GRID*GRID*GRID
    fun validMasks(count:Int,sceneAligned:Boolean):Boolean =
        sceneAligned && count in MIN_REGISTERED_MASK_VIEWS..MAX_REGISTERED_MASK_VIEWS
    fun carveSparse(volume:List<SparseVertex>,masks:List<VoxelMask>):List<SparseVertex> {
        if(masks.size<MIN_REGISTERED_MASK_VIEWS)return emptyList()
        return volume.filter {v->masks.all{it.contains(v.x,v.y,v.z)}}
    }
    fun carveVoxels(baseline:List<SparseVertex>,masks:List<VoxelMask>):List<SparseVertex> {
        if(!validMasks(masks.size,true) || baseline.size<24)return emptyList()
        fun ext(values:List<Double>):Pair<Double,Double> {
            val sorted=values.sorted()
            val lo=sorted[(sorted.size*.05).toInt().coerceIn(0,sorted.lastIndex)]
            val hi=sorted[(sorted.size*.95).toInt().coerceIn(0,sorted.lastIndex)]
            val span=max(0.02,hi-lo)
            return lo-.30*span to hi+.30*span
        }
        val bounds=listOf(ext(baseline.map{it.x}),ext(baseline.map{it.y}),
            ext(baseline.map{it.z}))
        val carved=ArrayList<SparseVertex>()
        for(zi in 0 until GRID)for(yi in 0 until GRID)for(xi in 0 until GRID) {
            val x=bounds[0].first+(xi+.5)*(bounds[0].second-bounds[0].first)/GRID
            val y=bounds[1].first+(yi+.5)*(bounds[1].second-bounds[1].first)/GRID
            val z=bounds[2].first+(zi+.5)*(bounds[2].second-bounds[2].first)/GRID
            if(masks.all{it.contains(x,y,z)})
                carved.add(SparseVertex(x,y,z,70,180,250))
        }
        return carved
    }
}
