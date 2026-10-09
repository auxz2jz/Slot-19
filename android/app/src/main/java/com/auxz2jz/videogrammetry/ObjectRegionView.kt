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

/** Image-aligned rectangle: user must draw on BOTH source photos explicitly. */
class ObjectRegionView(context: Context) : View(context) {
    private var photo: Bitmap? = null
    var selected: FocusRect? = null
        private set
    private val brush=Paint(Paint.ANTI_ALIAS_FLAG)
    private var anchorX=0f
    private var anchorY=0f
    private var active=false

    fun load(file: File) {
        val opts=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        require(opts.outWidth>0 && opts.outHeight>0) { "Cannot decode saved source photo" }
        var sample=1
        while(opts.outWidth/sample>900 || opts.outHeight/sample>900)sample*=2
        val decoded=BitmapFactory.decodeFile(file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize=sample })
            ?: error("Photo decode failed")
        photo?.recycle()
        photo=decoded
        selected=null
        invalidate()
    }

    private fun imageBox(): RectF {
        val bmp=photo ?: return RectF()
        val scale=min(width.toFloat()/bmp.width,height.toFloat()/bmp.height)
        val w=bmp.width*scale
        val h=bmp.height*scale
        return RectF((width-w)/2f,(height-h)/2f,(width+w)/2f,(height+h)/2f)
    }
    private fun normalized(x:Float,y:Float):Pair<Double,Double> {
        val r=imageBox()
        return ((x-r.left)/r.width()).coerceIn(0f,1f).toDouble() to
            ((y-r.top)/r.height()).coerceIn(0f,1f).toDouble()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(Color.rgb(15,20,29))
        val bmp=photo ?: return
        val r=imageBox()
        brush.style=Paint.Style.FILL
        brush.color=Color.WHITE
        c.drawBitmap(bmp,null,r,brush)
        val box=selected
        if(box!=null) {
            val highlight=RectF(
                r.left+(r.width()*box.left).toFloat(),
                r.top+(r.height()*box.top).toFloat(),
                r.left+(r.width()*box.right).toFloat(),
                r.top+(r.height()*box.bottom).toFloat())
            brush.color=Color.argb(55,15,245,125)
            brush.style=Paint.Style.FILL
            c.drawRect(highlight,brush)
            brush.color=Color.rgb(5,240,120)
            brush.style=Paint.Style.STROKE
            brush.strokeWidth=3f*resources.displayMetrics.density
            c.drawRect(highlight,brush)
            brush.style=Paint.Style.FILL
        }
    }

    override fun onTouchEvent(event: MotionEvent):Boolean {
        if(photo==null)return false
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val box=imageBox()
                if(!box.contains(event.x,event.y))return false
                anchorX=event.x;anchorY=event.y;active=true
                selected=null;invalidate();return true
            }
            MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP -> {
                if(!active)return false
                val a=normalized(anchorX,anchorY)
                val b=normalized(event.x,event.y)
                val choice=FocusRect(min(a.first,b.first),min(a.second,b.second),
                    max(a.first,b.first),max(a.second,b.second))
                selected=choice.takeIf { it.valid() }
                invalidate()
                if(event.actionMasked==MotionEvent.ACTION_UP)active=false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {active=false;return true}
        }
        return true
    }
    override fun onDetachedFromWindow() {
        // Photos belong to the app's saved run; this is just a bounded preview.
        photo?.recycle()
        photo=null
        super.onDetachedFromWindow()
    }
}
