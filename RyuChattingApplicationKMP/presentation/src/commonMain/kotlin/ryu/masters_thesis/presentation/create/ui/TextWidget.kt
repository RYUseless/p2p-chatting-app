package ryu.masters_thesis.presentation.create.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun TextWidget(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text      = "$text:",
        style     = MaterialTheme.typography.titleMedium,
        color     = MaterialTheme.colorScheme.onSurface,   // ← z theme
        modifier  = modifier.fillMaxWidth(),
        textAlign = TextAlign.Left
    )
}