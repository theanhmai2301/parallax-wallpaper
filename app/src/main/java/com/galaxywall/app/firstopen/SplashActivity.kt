package com.galaxywall.app.firstopen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.galaxywall.app.firstopen.language.LanguageFO1Activity
import com.galaxywall.app.firstopen.onboarding.OnBoardingFullFragmentActivity
import com.galaxywall.app.firstopen.survey.SurveyActivity
import com.galaxywall.app.ui.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Launcher / orchestrator for the first-open flow, mirroring the source app's SplashActivity:
 * it preloads the next screen's ads (hooks) and routes to the first incomplete step:
 * Language -> Survey -> Onboarding -> Permission -> Main.
 *
 * Ads: the source loads a native + interstitial "splash" ad and routes only once it finishes (or
 * times out). That is left as the [loadSplashAds] hook; here we just wait a short delay.
 */
class SplashActivity : AppCompatActivity() {

    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // SINGLE splash: keep the Android-12 system splash (app icon on
        // @color/first_open_bg) on screen for the whole load, then route straight
        // to the first screen. We deliberately do NOT inflate a second in-app
        // splash layout, so the user only ever sees one splash.
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !completed }
        super.onCreate(savedInstanceState)
        createView()
    }

    private fun createView() {
        // Re-apply the previously chosen language before entering the flow / app.
        applicationContext.languageCode?.let { LocaleHelper.changeLang(it) }

        preloadAds()

        // TODO: ADS — load the splash native/interstitial here, then call completeSplash() when it
        //  finishes or times out (instead of the fixed delay below).
        // loadSplashAds()

        lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            completeSplash()
        }
    }

    /** Mirrors the source preload chain: warm up the ads for whichever screen comes next. */
    private fun preloadAds() {
        if (!LanguageFO1Activity.preload(this)) {
            if (!SurveyActivity.preloadAds(this)) {
                if (!OnBoardingFullFragmentActivity.preloadAds(this)) {
                    if (!PermissionActivity.preloadAds(this)) {
                        // TODO: ADS — MainActivity ads preload
                    }
                }
            }
        }
    }

    /** Placeholder for splash ad loading; call [completeSplash] from its callbacks/timeout. */
    @Suppress("unused")
    private fun loadSplashAds() {
        // AdsProvider.nativeSplash.loadAds(this); AdsProvider.interSplash.loadAds(this) ...
    }

    private fun completeSplash() {
        if (completed || isFinishing) return
        completed = true
        startNextScreen()
        finish()
    }

    /** Routes to the first incomplete step of the first-open flow (mirrors the source). */
    private fun startNextScreen() {
        val ctx = applicationContext
        when {
            ctx.languageCode == null -> LanguageFO1Activity.start(this)
            !ctx.isSurveyDone -> SurveyActivity.start(this)
            !ctx.isOnboardingDone -> {
                ctx.languageCode?.let { LocaleHelper.changeLang(it) }
                OnBoardingFullFragmentActivity.start(this)
            }
            !PermissionActivity.isNotificationPermissionGranted(this) -> PermissionActivity.start(this)
            else -> MainActivity.startMain(this)
        }
    }

    private companion object {
        const val SPLASH_DELAY_MS = 1200L
    }
}
