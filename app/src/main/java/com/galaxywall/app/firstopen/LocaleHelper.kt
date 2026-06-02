package com.galaxywall.app.firstopen

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Applies the chosen UI language, mirroring the source app's LanguageUtil.changeLang().
 * Uses AppCompat's per-app locales API (auto-persisted and re-applied by the framework), so the
 * choice survives process restarts without manual Configuration juggling.
 */
object LocaleHelper {

    /** Apply [languageCode] (e.g. "en", "vi") as the app's locale. */
    fun changeLang(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }
}
