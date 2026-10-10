package com.auxz2jz.videogrammetry

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import kotlin.math.min

/** The original reconstruction's 2D feature measurements drawn on their source JPEG. */
class SourcePhotoOverlayView(ctx:Context): View(ctx) {
    private var bitmap:Bitmap?=null
    private var fileName:String?=null
    private var positions:List<PhotoPoint> = emptyList()
    var photoOpacity:Float=0.40f
        set(value) { field=value.coerceIn(0f,1f); invalidate() }
    var dotRadius:Float=5f
        set(value) { field=value.coerceIn(1f,18f);invalidate() }
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    fun showPhoto(file:File,coords:List<PhotoPoint>) {
        if(fileName!=file.absolutePath) {
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            BitmapFactory.decodeFile(file.absolutePath,bounds)
            require(bounds.outWidth>0 && bounds.outHeight>0) { "Cannot decode photo" }
            var sample=1
            while(bounds.outWidth/sample>1200 || bounds.outHeight/sample>1200)sample*=2
            val loaded=BitmapFactory.decodeFile(file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize=sample })
                ?: error("Source photograph unavailable")
            bitmap?.recycle()
            bitmap=loaded
            fileName=file.absolutePath
        }
        positions=coords
        invalidate()
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(16,22,30))
        val photo=bitmap?:return
        val fit=min(width.toFloat()/photo.width,height.toFloat()/photo.height)
        val drawWidth=photo.width*fit
        val drawHeight=photo.height*fit
        val target=RectF((width-drawWidth)/2f,(height-drawHeight)/2f,
            (width+drawWidth)/2f,(height+drawHeight)/2f)
        paint.style=Paint.Style.FILL
        paint.color=Color.WHITE
        paint.alpha=(photoOpacity*255).toInt()
        canvas.drawBitmap(photo,null,target,paint)
        paint.alpha=255
        val r=dotRadius*resources.displayMetrics.density
        for(point in positions) {
            val x=target.left+target.width()*point.x
            val y=target.top+target.height()*point.y
            paint.color=Color.BLACK
            canvas.drawCircle(x,y,r+1.3f*resources.displayMetrics.density,paint)
            paint.color=Color.rgb(0,255,160)
            canvas.drawCircle(x,y,r,paint)
        }
    }
    fun release() {bitmap?.recycle();bitmap=null;fileName=null}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPointOverlayDialog(run:ScanRun,
    onDismiss:()->Unit,onEvent:(String,String,Int)->Unit) {
    val context=LocalContext.current
    val viewer=remember(run.id) { SourcePhotoOverlayView(context) }
    val loader=remember(run.id) {PhotoPointOverlayLoader()}
    val types=listOf(CloudArtifacts.SCENE_PLY,
        CloudArtifacts.FILTERED_SCENE_PLY,CloudArtifacts.ROI_RECONSTRUCTED_PLY)
        .filter { File(run.directory,it).isFile }
    var selection by remember(run.id) {
        mutableStateOf(if(CloudArtifacts.SCENE_PLY in types)CloudArtifacts.SCENE_PLY
            else types.firstOrNull().orEmpty())
    }
    var step by remember(run.id) { mutableIntStateOf(0) }
    var photoOpacity by remember { mutableFloatStateOf(.45f) }
    var pointSize by remember { mutableFloatStateOf(4f) }
    var showingChoices by remember { mutableStateOf(false) }
    val loaded=remember(run.id,selection) {
        runCatching {loader.load(run,selection)}.getOrNull()
    }
    val error=remember(run.id,selection) {
        runCatching { loader.load(run,selection) }.exceptionOrNull()?.javaClass?.simpleName
    }
    DisposableEffect(viewer) { onDispose {viewer.release()} }
    val model=loaded
    val image=remember(run.id,selection,step) {
        if(model==null)null
        else runCatching { loader.sourceFile(run,if(step==0)
            model.sourceIndices.first else model.sourceIndices.second) }.getOrNull()
    }
    Dialog(onDismissRequest=onDismiss,
        properties=DialogProperties(usePlatformDefaultWidth=false)) {
        OutlinedCard(Modifier.fillMaxWidth().fillMaxHeight(.96f).padding(6.dp)) {
            Column(Modifier.fillMaxSize().padding(9.dp),
                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text("Photo ↔ 3D Point Correspondence",
                    style=MaterialTheme.typography.titleLarge)
                Text("These dots are the exact original matched IMAGE features "+
                    "used for 3D vertices, not arbitrary projected camera poses. "+
                    "Only source photos with saved correspondences are available.",
                    style=MaterialTheme.typography.bodySmall)
                Box {
                    Button(onClick={showingChoices=true},enabled=types.isNotEmpty()) {
                        Text(when(selection) {
                            CloudArtifacts.SCENE_PLY -> "Full Scene ▾"
                            CloudArtifacts.FILTERED_SCENE_PLY -> "Filtered Scene ▾"
                            else -> "ROI-First Reconstruction ▾"
                        })
                    }
                    DropdownMenu(expanded=showingChoices,onDismissRequest={showingChoices=false}) {
                        for(name in types)
                            DropdownMenuItem(text={Text(when(name) {
                                CloudArtifacts.SCENE_PLY -> "Full Scene"
                                CloudArtifacts.FILTERED_SCENE_PLY -> "Filtered Scene"
                                else -> "ROI-First Reconstruction"
                            })},onClick={
                                selection=name;step=0;showingChoices=false
                                onEvent("SELECT_CLOUD",name,0)
                            })
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    Button(onClick={step=0;onEvent("PHOTO_VIEW",selection,1)},
                        enabled=step!=0 && model!=null) {Text("Photo 1")}
                    Button(onClick={step=1;onEvent("PHOTO_VIEW",selection,2)},
                        enabled=step!=1 && model!=null) {Text("Photo 2")}
                }
                if(model!=null && image!=null) {
                    Text(model.name+" · frame "+
                        (if(step==0)model.sourceIndices.first else model.sourceIndices.second)+
                        " · "+model.pointsA.size+" marked 3D correspondences",
                        style=MaterialTheme.typography.bodySmall)
                    AndroidView(factory={viewer},modifier=Modifier.fillMaxWidth().weight(1f),
                        update={v ->
                            v.showPhoto(image,if(step==0)model.pointsA else model.pointsB)
                            v.photoOpacity=photoOpacity
                            v.dotRadius=pointSize
                        })
                } else {
                    Box(Modifier.weight(1f)) {
                        Text("Overlay unavailable for this scan. "+
                            (error?:"Missing valid original photo or source feature map.")+
                            ". Run sparse analysis again to save aligned features.")
                    }
                }
                Text("Photo opacity: "+(photoOpacity*100).toInt()+"%")
                Slider(value=photoOpacity,onValueChange={photoOpacity=it},
                    valueRange=0f..1f)
                Text("Dot size: "+pointSize.toInt())
                Slider(value=pointSize,onValueChange={pointSize=it},valueRange=1f..14f)
                Text("Photo frames 1/2 are actual reconstruction source views. "+
                    "The multi-view model's later frames are not projected here "+
                    "until their estimated poses and per-point tracks are saved.",
                    style=MaterialTheme.typography.bodySmall)
                Button(onClick=onDismiss,modifier=Modifier.fillMaxWidth()) {
                    Text("Close Photo Overlay")
                }
            }
        }
    }
}
