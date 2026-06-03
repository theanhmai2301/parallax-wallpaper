package com.galaxywall.app.firstopen

import android.content.Context
import java.io.File

/**
 * Persists the first-open flow state, mirroring the source app's SharedPreferenceUtils keys:
 * which language was chosen, and whether the survey / onboarding were completed.
 *
 * The SplashActivity reads these to decide the next screen:
 * Splash -> Language -> Survey -> Onboarding -> Main.
 *
 * Exposed as top-level Context extension properties so any Activity can read/write them directly.
 */
private const val PREFS = "first_open"
private const val KEY_LANGUAGE = "language_code"
private const val KEY_SURVEY_DONE = "complete_survey"
private const val KEY_ONBOARDING_DONE = "complete_onboarding"

private fun Context.firstOpenPrefs() =
    getSharedPreferences(PREFS, Context.MODE_PRIVATE)

/** Selected language code (e.g. "en"), or null if the user hasn't picked one yet. */
var Context.languageCode: String?
    get() = firstOpenPrefs().getString(KEY_LANGUAGE, null)
    set(value) {
        firstOpenPrefs().edit().putString(KEY_LANGUAGE, value).apply()
    }

var Context.isSurveyDone: Boolean
    get() = firstOpenPrefs().getBoolean(KEY_SURVEY_DONE, false)
    set(value) {
        firstOpenPrefs().edit().putBoolean(KEY_SURVEY_DONE, value).apply()
    }

var Context.isOnboardingDone: Boolean
    get() = firstOpenPrefs().getBoolean(KEY_ONBOARDING_DONE, false)
    set(value) {
        firstOpenPrefs().edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
    }

/** Marker file kept in no-backup storage; its presence means this install has
 *  already run (or skipped) the first-open flow. It is NEVER backed up, so it is
 *  absent on every genuinely new install. */
private const val FRESH_INSTALL_MARKER = "first_open.initialized"

/**
 * Guarantees the first-open funnel runs once per *install*, not per *backup*.
 *
 * Android Auto Backup and device-transfer (phone clone) can restore the
 * "first_open" SharedPreferences onto a brand-new install, which would wrongly
 * skip Language/Survey/Onboarding and drop the user straight on Permission.
 *
 * We detect a fresh install via a marker in [Context.getNoBackupFilesDir] (which
 * is never backed up or restored). If the marker is missing, we clear any
 * restored first-open flags and recreate it. Call this once, early, before the
 * Splash routing reads the flags.
 */
fun Context.ensureFreshInstallResetsFirstOpen() {
    val marker = File(noBackupFilesDir, FRESH_INSTALL_MARKER)
    if (marker.exists()) return
    // Fresh install (or backup restored onto one): start the intro from scratch.
    firstOpenPrefs().edit().clear().commit()
    runCatching { marker.createNewFile() }
}
