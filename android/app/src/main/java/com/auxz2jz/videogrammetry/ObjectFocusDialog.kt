package com.auxz2jz.videogrammetry
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

@Composable
fun ObjectFocusDialog(photos:Pair<File,File>,onDismiss:()->Unit,
    onConfirm:(FocusRect,FocusRect)->Unit) {
 val ctx=LocalContext.current
 val first=remember(photos.first.absolutePath) { ObjectRegionView(ctx).apply{load(photos.first)} }
 val second=remember(photos.second.absolutePath) { ObjectRegionView(ctx).apply{load(photos.second)} }
 var message by remember { mutableStateOf("Drag a box around the SAME object in both photos.") }
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  OutlinedCard(Modifier.fillMaxWidth().padding(8.dp)) {
   Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
    verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("Object Focus — two real photos",style=MaterialTheme.typography.titleLarge)
    Text("Draw a green rectangle around the object in each image. No box is assumed.")
    Text("First reconstruction photo")
    AndroidView(factory={first},modifier=Modifier.fillMaxWidth().height(220.dp))
    Text("Second reconstruction photo")
    AndroidView(factory={second},modifier=Modifier.fillMaxWidth().height(220.dp))
    Text(message)
    Button(onClick={
      val a=first.selected; val b=second.selected
      if(a==null || b==null)message="Select the object in BOTH images."
      else onConfirm(a,b)
    },modifier=Modifier.fillMaxWidth()) {Text("Create Object-Focused PLY")}
    Button(onClick=onDismiss,modifier=Modifier.fillMaxWidth()) {Text("Cancel")}
    Text("Coarse rectangle filter only. Not a full object mesh or measured 3D model.")
   }
  }
 }
}
