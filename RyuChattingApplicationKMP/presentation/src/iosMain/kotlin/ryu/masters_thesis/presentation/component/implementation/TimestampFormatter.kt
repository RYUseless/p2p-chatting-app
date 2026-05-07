package ryu.masters_thesis.presentation.component.implementation

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970


actual fun formatTimestamp(epochMs: Long): String {
    val date      = NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "dd.MM."
    return formatter.stringFromDate(date)
}