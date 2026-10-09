package com.auxz2jz.videogrammetry

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.min

/**
 * Offline, native Android XYZ colored point viewer. Nothing uploaded.
 * Input XYZ units are arbitrary; do not make dimensional measurements.
 */
class SparseCloudView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cloud: PointCloud? = null
    private var yaw = 0.5f
    private var pitch = -0.2f
    private var zoom = 1f
    var pointRadius = 3.5f
        set(v) { field=v.coerceIn(1f,12f); invalidate() }
    var focusCluster = true
        set(v) { field=v; invalidate() }
    var showColors = true
        set(v) { field=v; invalidate() }
    private var lastX=0f
    private var lastY=0f
    private val scaler = ScaleGestureDetector(context,
        object: ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoom = (zoom*detector.scaleFactor).coerceIn(0.15f,30f)
                invalidate()
                return true
            }
        })
    init {
        setBackgroundColor(Color.rgb(12,19,31))
        contentDescription="Sparse 3D point cloud, drag to rotate and pinch to zoom"
    }
    fun load(value: PointCloud) { if (cloud !== value) { cloud=value; reset() } }
    fun reset() {
        yaw=0.5f;pitch=-0.2f;zoom=1f;invalidate()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaler.onTouchEvent(event)
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX=event.x;lastY=event.y;return true }
            MotionEvent.ACTION_POINTER_DOWN -> { lastX=event.x;lastY=event.y;return true }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount==1 && !scaler.isInProgress) {
                    yaw += (event.x-lastX)*0.008f
                    pitch=(pitch+(event.y-lastY)*0.008f).coerceIn(-1.55f,1.55f)
                    invalidate()
                }
                lastX=event.x;lastY=event.y;return true
            }
        }
        return true
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val vertices=cloud?.vertices ?: emptyList()
        if (vertices.isEmpty()) {
            paint.color=Color.LTGRAY;paint.textSize=42f
            canvas.drawText("No sparse points loaded", 35f,height/2f,paint)
            return
        }
        // Median-centered radius limits effect of a single wildly triangulated point.
        fun med(values:List<Float>):Float = values.sorted()[values.size/2]
        val mx=med(vertices.map { it.x })
        val my=med(vertices.map { it.y })
        val mz=med(vertices.map { it.z })
        val ranges=vertices.map {
            val x=(it.x-mx).toDouble();val y=(it.y-my).toDouble()
            val z=(it.z-mz).toDouble();sqrt(x*x+y*y+z*z).toFloat()
        }.sorted()
        val index = if (focusCluster) (ranges.size*0.90).toInt().coerceIn(0,ranges.lastIndex)
            else ranges.lastIndex
        val radius=ranges[index].coerceAtLeast(0.00001f)
        val displayScale = (min(width,height)*0.42f)/radius*zoom
        val centerX=width/2f
        val centerY=height/2f
        val cy=cos(yaw);val sy=sin(yaw);val cp=cos(pitch);val sp=sin(pitch)
        data class Pixel(val px:Float,val py:Float,val depth:Float,val c:Int)
        val plotted=ArrayList<Pixel>(vertices.size)
        for (v in vertices) {
            val x=v.x-mx; val y=v.y-my;val z=v.z-mz
            val distance=sqrt(x*x+y*y+z*z)
            if (focusCluster && distance>radius*1.2f) continue
            val xr=cy*x + sy*z
            val zr=-sy*x + cy*z
            val yr=cp*y-sp*zr
            val depth=sp*y+cp*zr
            val px=centerX+xr*displayScale
            val py=centerY-yr*displayScale
            if (px < -20 || px > width+20 || py < -20 || py > height+20) continue
            val c=if(showColors) Color.rgb(v.r,v.g,v.b) else Color.rgb(145,207,255)
            plotted.add(Pixel(px,py,depth,c))
        }
        // Depth sorting gives nearer points priority where projections overlap.
        plotted.sortBy { it.depth }
        for (dot in plotted) {
            paint.color=dot.c
            paint.style=Paint.Style.FILL
            canvas.drawCircle(dot.px,dot.py,pointRadius*resources.displayMetrics.density,paint)
        }
        paint.color=Color.rgb(195,207,221);paint.textSize=13f*resources.displayMetrics.density
        canvas.drawText("Two-view sparse points | scale unknown",
            10f*resources.displayMetrics.density,21f*resources.displayMetrics.density,paint)
        canvas.drawText("Visible: " + plotted.size + " / " + vertices.size +
            if (focusCluster) " (cluster focus)" else " (all)",
            10f*resources.displayMetrics.density,39f*resources.displayMetrics.density,paint)
    }
}
