package ryu.masters_thesis.presentation.component.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrCodeImage(
    content:  String,
    sizeDp:   Int,
    modifier: Modifier = Modifier,
)