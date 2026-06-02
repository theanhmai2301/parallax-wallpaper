package com.galaxywall.app.firstopen

import android.content.Context

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
