package com.auxz2jz.videogrammetry

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import java.io.File
import kotlin.math.min
import kotlin.math.max

/**
 * A source-JPEG aligned, large, one-finger rectangle editor.
 * DRAW uses one finger to select; MOVE uses one finger to pan an enlarged photo.
 * All boxes remain normalized to original decoded image pixels at any zoom.
 */
class ObjectRegionView(context: Context): View(context) {
    private var photo: Bitmap? = null
    var selected: FocusRect? = null
        private set
    private var draft: FocusRect? = null
    private val brush=Paint(Paint.ANTI_ALIAS_FLAG)
    private var startX=0f
    private var startY=0f
    private var lastX=0f
    private var lastY=0f
    private var dragging=false
    private var zoom=1f
    private var panX=0f
    private var panY=0f
    private var panMode=false
    var selectionFinished: ((FocusRect?)->Unit)? = null

    fun load(file: File) {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeFile(file.absolutePath,bounds)
        require(bounds.outWidth>0 && bounds.outHeight>0) { "Cannot decode saved photo" }
        var sample=1
        while(bounds.outWidth/sample>1200 || bounds.outHeight/sample>1200)sample*=2
        val bitmap=BitmapFactory.decodeFile(file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize=sample })
            ?: error("Saved photo decode failed")
        photo?.recycle()
        photo=bitmap
        selected=null
        draft=null
        panX=0f;panY=0f;zoom=1f
        invalidate()
    }
    fun isPanMode():Boolean=panMode
    fun setPanMode(pan:Boolean) {
        dragging=false
        draft=null
        panMode=pan
        invalidate()
    }
    fun zoomBy(factor:Float) {
        zoom=(zoom*factor).coerceIn(1f,5f)
        clampPan()
        invalidate()
    }
    fun resetView() {
        zoom=1f
        panX=0f
        panY=0f
        panMode=false
        dragging=false
        draft=null
        invalidate()
    }
    fun clearSelection() {
        selected=null
        draft=null
        selectionFinished?.invoke(null)
        invalidate()
    }
    private fun photoBox():RectF {
        val bmp=photo ?: return RectF()
        if(width<=0 || height<=0)return RectF()
        val fit=min(width.toFloat()/bmp.width,height.toFloat()/bmp.height)
        val widthPx=bmp.width*fit*zoom
        val heightPx=bmp.height*fit*zoom
        return RectF((width-widthPx)/2f+panX,(height-heightPx)/2f+panY,
            (width+widthPx)/2f+panX,(height+heightPx)/2f+panY)
    }
    private fun clampPan() {
        val box=photoBox()
        if(box.isEmpty)return
        val boundX=max(0f,(box.width()-width)/2f)
        val boundY=max(0f,(box.height()-height)/2f)
        panX=panX.coerceIn(-boundX,boundX)
        panY=panY.coerceIn(-boundY,boundY)
    }
    private fun normalize(x:Float,y:Float):Pair<Double,Double> {
        val box=photoBox()
        return ((x-box.left)/box.width()).coerceIn(0f,1f).toDouble() to
            ((y-box.top)/box.height()).coerceIn(0f,1f).toDouble()
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(12,20,33))
        val bmp=photo ?: return
        val box=photoBox()
        brush.style=Paint.Style.FILL
        brush.color=Color.WHITE
        canvas.drawBitmap(bmp,null,box,brush)
        val highlight=draft ?: selected
        if(highlight!=null) {
            val selectedBox=RectF(
                box.left+(box.width()*highlight.left).toFloat(),
                box.top+(box.height()*highlight.top).toFloat(),
                box.left+(box.width()*highlight.right).toFloat(),
                box.top+(box.height()*highlight.bottom).toFloat())
            brush.style=Paint.Style.FILL
            brush.color=Color.argb(62,15,245,125)
            canvas.drawRect(selectedBox,brush)
            brush.style=Paint.Style.STROKE
            brush.strokeWidth=3.5f*resources.displayMetrics.density
            brush.color=Color.rgb(9,250,121)
            canvas.drawRect(selectedBox,brush)
            brush.style=Paint.Style.FILL
        }
    }
    fun releasePhoto() {
        dragging=false
        draft=null
        selectionFinished=null
        photo?.recycle()
        photo=null
    }
    override fun onTouchEvent(event: MotionEvent):Boolean {
        if(photo==null)return false
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // The former vertical-scroll dialog stole one-finger drags.
                // Take ownership of this gesture so DRAW works reliably.
                parent?.requestDisallowInterceptTouchEvent(true)
                dragging=true
                startX=event.x
                startY=event.y
                lastX=event.x
                lastY=event.y
                if(!panMode) {
                    val b=photoBox()
                    if(!b.contains(event.x,event.y)) {
                        dragging=false
                        parent?.requestDisallowInterceptTouchEvent(false)
                        return true
                    }
                    draft=null
                    selected=null
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Two fingers are NOT needed and must never create a box.
                dragging=false
                draft=null
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if(!dragging || event.pointerCount!=1)return true
                if(panMode) {
                    panX+=event.x-lastX
                    panY+=event.y-lastY
                    clampPan()
                } else {
                    val start=normalize(startX,startY)
                    val finish=normalize(event.x,event.y)
                    draft=FocusRect(min(start.first,finish.first),
                        min(start.second,finish.second),
                        max(start.first,finish.first),
                        max(start.second,finish.second))
                }
                lastX=event.x;lastY=event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if(dragging && !panMode) {
                    val start=normalize(startX,startY)
                    val finish=normalize(event.x,event.y)
                    val box=FocusRect(min(start.first,finish.first),
                        min(start.second,finish.second),
                        max(start.first,finish.first),
                        max(start.second,finish.second))
                    if(box.valid()) {
                        selected=box
                        selectionFinished?.invoke(box)
                    } else {
                        selectionFinished?.invoke(null)
                    }
                }
                dragging=false
                draft=null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging=false
                draft=null
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return true
    }
}