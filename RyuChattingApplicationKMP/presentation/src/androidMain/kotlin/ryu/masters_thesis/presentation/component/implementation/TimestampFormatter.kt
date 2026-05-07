package ryu.masters_thesis.presentation.component.implementation

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatTimestamp(epochMs: Long): String {
    return SimpleDateFormat("dd.MM.", Locale.getDefault()).format(Date(epochMs))
}