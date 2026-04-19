package ryu.masters_thesis.presentation.component.ui

import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import ryu.masters_thesis.core.qrCode.implementation.QrCodeReaderImpl

@Composable
actual fun QrCodeScanner(onResult: (String) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var reader: QrCodeReaderImpl? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose { reader?.stop() }
    }

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