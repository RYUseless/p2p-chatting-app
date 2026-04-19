package ryu.masters_thesis.presentation.component.ui

import androidx.compose.runtime.Composable

@Composable
expect fun QrCodeScanner(onResult: (String) -> Unit)