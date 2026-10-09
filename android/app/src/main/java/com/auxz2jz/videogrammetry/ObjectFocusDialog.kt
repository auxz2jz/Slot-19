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
import java.io.File

/**
 * Large STEP-BY-STEP photo selector. Only one image is onscreen at a time.
 * Drag ONE finger in Draw mode. Next Photo advances only after a valid ROI.
 */
@Composable
fun ObjectFocusDialog(photos:Pair<File,File>,onDismiss:()->Unit,
    onConfirm:(FocusRect,FocusRect)->Unit) {
    val context=LocalContext.current
    val first=remember(photos.first.absolutePath) {
        ObjectRegionView(context).apply { load(photos.first) }
    }
    val second=remember(photos.second.absolutePath) {
        ObjectRegionView(context).apply { load(photos.second) }
    }
    var step by remember { mutableIntStateOf(0) }
    var moving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Drag ONE finger around the object in Photo 1.") }
    val view=if(step==0)first else second
    first.selectionFinished={ roi ->
        if(step==0)message=if(roi==null) "Box too small. Drag across the object."
            else "Photo 1 selected. Tap NEXT PHOTO to continue."
    }
    second.selectionFinished={ roi ->
        if(step==1)message=if(roi==null) "Box too small. Drag across the object."
            else "Photo 2 selected. Tap CREATE OBJECT PLY."
    }
    Dialog(onDismissRequest=onDismiss,
        properties=DialogProperties(usePlatformDefaultWidth=false)) {
        OutlinedCard(Modifier.fillMaxWidth().fillMaxHeight(0.96f).padding(6.dp)) {
            Column(Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("Object Focus — Photo " + (step+1) + " of 2",
                    style=MaterialTheme.typography.titleLarge)
                Text(if(step==0)
                    "Select the object in the FIRST reconstruction photo."
                    else "Select that SAME object in the SECOND reconstruction photo.")
                Text("DRAW: drag ONE finger to make the green box. " +
                    "MOVE: drag to pan when zoomed. Use + or − to zoom.",
                    style=MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Button(onClick={moving=false;view.setPanMode(false)},
                        modifier=Modifier.weight(1f)) {
                        Text(if(!moving)"✓ Draw box" else "Draw box")
                    }
                    OutlinedButton(onClick={moving=true;view.setPanMode(true)},
                        modifier=Modifier.weight(1f)) {
                        Text(if(moving)"✓ Move" else "Move")
                    }
                    OutlinedButton(onClick={view.zoomBy(1.5f)}) {Text("+")}
                    OutlinedButton(onClick={view.zoomBy(1f/1.5f)}) {Text("−")}
                }
                // No verticalScroll ancestor: one-finger drags reach the native view.
                // The photo occupies nearly all remaining available screen height.
                key(step) {
                    AndroidView(factory={view},
                        modifier=Modifier.fillMaxWidth().weight(1f))
                }
                Text(message,style=MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick={
                        if(step==0)onDismiss()
                        else {
                            step=0;moving=false;first.setPanMode(false)
                            message="Photo 1: adjust the box or tap NEXT PHOTO."
                        }
                    },modifier=Modifier.weight(1f)) {
                        Text(if(step==0)"Cancel" else "Back")
                    }
                    OutlinedButton(onClick={
                        view.resetView();moving=false
                    }) {Text("Reset view")}
                    Button(onClick={
                        if(step==0) {
                            if(first.selected==null)message="Draw a box on Photo 1 first."
                            else {
                                step=1;moving=false;second.setPanMode(false)
                                message=if(second.selected!=null)
                                    "Photo 2 selected. Tap CREATE, or draw again."
                                    else "Now draw ONE box on Photo 2."
                            }
                        } else {
                            val a=first.selected
                            val b=second.selected
                            if(a==null || b==null)
                                message="Draw around the object in BOTH photographs first."
                            else onConfirm(a,b)
                        }
                    },modifier=Modifier.weight(1.35f)) {
                        Text(if(step==0)"Next Photo →" else "Create Object PLY")
                    }
                }
                Text("Filtering existing points only; the original full-scene PLY is preserved.",
                    style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
