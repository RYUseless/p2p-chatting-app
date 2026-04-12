package ryu.masters_thesis.presentation.create.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

// Sdílený label widget pro form fieldy
@Composable
fun TextWidget(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "$text:",
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Left
    )
}