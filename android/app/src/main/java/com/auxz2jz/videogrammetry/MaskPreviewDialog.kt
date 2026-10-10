package com.auxz2jz.videogrammetry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONObject
import java.io.File

/** Inspect engine silhouettes immediately after selection, BEFORE any 3D solve. */
@Composable
fun MaskPreviewDialog(run:ScanRun,onDismiss:()->Unit) {
    val context=LocalContext.current
    val viewer=remember(run.id){SourcePhotoOverlayView(context)}
    val manifest=remember(run.id) {
        runCatching {
            JSONObject(File(run.directory,"early_object_focus_selection.json").readText())
                .getJSONArray("sourcePair")
        }.getOrNull()
    }
    var side by remember {mutableIntStateOf(0)}
    var engine by remember {mutableStateOf(MaskEnginePolicy.GRABCUT)}
    var opacity by remember {mutableFloatStateOf(.55f)}
    var selection by remember {mutableStateOf(false)}
    DisposableEffect(viewer){onDispose{viewer.release()}}
    val index=if(manifest==null)-1 else manifest.optInt(side,-1)
    val photo=remember(run.id,index) {
        if(index<0)null else runCatching {
            PhotoPointOverlayLoader().sourceFile(run,index)
        }.getOrNull()
    }
    val mask=if(index<0)null else ObjectMaskProcessor()
        .maskFile(run,index,engine).takeIf{it.isFile}
    Dialog(onDismissRequest=onDismiss,
        properties=DialogProperties(usePlatformDefaultWidth=false)) {
        OutlinedCard(Modifier.fillMaxWidth().fillMaxHeight(.96f).padding(7.dp)){
            Column(Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Foreground Engine Comparison",style=MaterialTheme.typography.titleLarge)
                Text("ORANGE = guessed object silhouette. Inspect carefully: "+
                    "checkerboard squares or empty object surfaces mean this "+
                    "engine should NOT be trusted for reconstruction.")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={side=0},enabled=side!=0){Text("Photo 1")}
                    Button(onClick={side=1},enabled=side!=1){Text("Photo 2")}
                }
                Box {
                    OutlinedButton(onClick={selection=true}) {
                        Text("Mask engine: "+engine+" ▾")
                    }
                    DropdownMenu(expanded=selection,onDismissRequest={selection=false}) {
                        for(name in MaskEnginePolicy.engineNames)
                            DropdownMenuItem(text={Text(name)},onClick={
                                engine=name;selection=false
                            })
                    }
                }
                if(photo!=null && mask!=null) {
                    AndroidView(factory={viewer},
                        modifier=Modifier.fillMaxWidth().weight(1f),
                        update={it.showPhoto(photo,emptyList(),mask).also {
                            viewer.photoOpacity=opacity
                        }})
                } else {
                    Box(Modifier.weight(1f)){
                        Text("No generated mask available for this photo. "+
                            "Complete object selection and compare mask engines first.")
                    }
                }
                Text("Photograph opacity: "+(opacity*100).toInt()+"%")
                Slider(value=opacity,onValueChange={opacity=it},valueRange=0f..1f)
                Text("Comparing masks doesn't prove geometry. Next: choose "+
                    "the best mask engine, rerun comparison with that engine, "+
                    "then Analyze Sparse 3D and Multi-View.")
                Button(onClick=onDismiss,modifier=Modifier.fillMaxWidth()){
                    Text("Close Mask Comparison")
                }
            }
        }
    }
}
