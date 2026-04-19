package ryu.masters_thesis.core.configuration

import java.util.Locale

actual fun detectDefaultLanguage(): AppLanguage {
    val tag = Locale.getDefault().language
    return when (tag) {
        "cs" -> AppLanguage.CZECH
        "de" -> AppLanguage.GERMAN
        "pl" -> AppLanguage.POLISH
        else -> AppLanguage.ENGLISH
    }
}