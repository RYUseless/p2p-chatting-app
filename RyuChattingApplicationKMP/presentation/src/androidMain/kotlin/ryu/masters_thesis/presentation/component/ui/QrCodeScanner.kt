package ryu.masters_thesis.presentation.component.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import ryu.masters_thesis.core.qrCode.implementation.QrCodeReaderImpl

@Composable
actual fun QrCodeScanner(onResult: (String) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted: Boolean -> permissionGranted = granted },
    )

    LaunchedEffect(Unit) {
        if (!permissionGranted) launcher.launch(Manifest.permission.CAMERA)
    }

    var reader: QrCodeReaderImpl? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose { reader?.stop() }
    }

    if (permissionGranted) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                PreviewView(ctx).also { previewView ->
                    reader = QrCodeReaderImpl(
                        context        = context,
                        previewView    = previewView,
                        lifecycleOwner = lifecycleOwner,
                    )
                    reader?.start { result ->
                        Log.d("QrCodeScanner", "Scanned: $result")
                        onResult(result)
                    }
                }
            }
        )
    }
}