package com.galaxywall.app.firstopen.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.galaxywall.app.databinding.ActivityOnboardingFullFragmentBinding
import com.galaxywall.app.firstopen.PermissionActivity
import com.galaxywall.app.firstopen.PermissionActivity.Companion.isNotificationPermissionGranted
import com.galaxywall.app.firstopen.isOnboardingDone
import com.galaxywall.app.ui.MainActivity

/**
 * Hosts the onboarding pager. Mirrors the source app's OnBoardingFullFragmentActivity: "Next"
 * advances; the last page completes onboarding and moves on to the notification-permission screen
 * (or straight to the app if already granted). Per-page ad preloads are left as marked hooks.
 */
class OnBoardingFullFragmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingFullFragmentBinding
    private var currentPosition = 0

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, OnBoardingFullFragmentActivity::class.java))
        }

        /** True if onboarding still needs to run (also where the OB native/inter ads are preloaded). */
        fun preloadAds(activity: Activity): Boolean {
            if (!activity.applicationContext.isOnboardingDone) {
                // TODO: ADS — preload nativeOBD1/2/3, full-screen native + onboarding interstitial.
                return true
            }
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingFullFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createView()
    }

    private fun createView() {
        val adapter = OnboardingPagerAdapter(this, object : OnActionNextOnboarding {
            override fun onNext() {
                binding.viewPager2.setCurrentItem(currentPosition + 1, true)
            }

            override fun onComplete() {
                applicationContext.isOnboardingDone = true
                goNext()
            }
        })
        binding.viewPager2.adapter = adapter
        binding.viewPager2.offscreenPageLimit = 5
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                when (position) {
                    0 -> { /* TODO: ADS — load OBD2 + full-screen native */ }
                    1 -> { /* TODO: ADS — load OBD3 + onboarding interstitial */ }
                    2 -> preloadNext()
                }
            }
        })
    }

    /** Preloads the screen that follows onboarding (permission or home), mirroring the source. */
    private fun preloadNext() {
        if (isNotificationPermissionGranted(this)) {
            // TODO: ADS — MainActivity ads preload
        } else {
            PermissionActivity.preloadAds(this)
        }
    }

    private fun goNext() {
        if (!isNotificationPermissionGranted(this)) {
            PermissionActivity.start(this)
        } else {
            MainActivity.startMain(this)
        }
        finish()
    }

    override fun onDestroy() {
        binding.viewPager2.adapter = null
        super.onDestroy()
    }
}
