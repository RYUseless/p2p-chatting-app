package ryu.masters_thesis.presentation.component.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.core.qrCode.implementation.QrCodeGeneratorImpl

@Composable
actual fun QrCodeImage(
    content:  String,
    sizeDp:   Int,
    modifier: Modifier,
) {
    val qrResult = remember(content) {
        QrCodeGeneratorImpl().generate(content = content, sizePx = 512)
    }

    if (qrResult != null) {
        val bitmap = remember(qrResult) {
            BitmapFactory.decodeByteArray(qrResult.imageBytes, 0, qrResult.imageBytes.size)
        }
        if (bitmap != null) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier           = modifier.size(sizeDp.dp),
            )
            return
        }
    }

    Box(
        modifier         = modifier
            .size(sizeDp.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text      = "QR generation failed",
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}