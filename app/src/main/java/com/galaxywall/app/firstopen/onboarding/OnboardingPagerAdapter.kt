package com.galaxywall.app.firstopen.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

const val PAGE_ONBOARDING_ADS_FULL_SCREEN = 100
const val PAGE_ONBOARDING_ADS_FULL_SCREEN_1 = 101

/** Driven by each onboarding page's "Next" button, mirroring the source app. */
interface OnActionNextOnboarding {
    fun onNext()
    fun onComplete()
}

/**
 * Onboarding pager. Mirrors the source app's OnboardingPagerAdapter, including the optional
 * full-screen ad pages inserted between content pages. The two flags below decide whether those ad
 * pages are present; they default to false (ads off) so only the 3 content pages show. Flip them
 * (e.g. from a remote-config flag) when wiring ads.
 */
class OnboardingPagerAdapter(
    activity: FragmentActivity,
    private val listener: OnActionNextOnboarding
) : FragmentStateAdapter(activity) {

    // TODO: ADS — drive these from your remote-config flags (showNativeOBFull / *_1).
    private val isFullScreenEnabled = false
    private val isFullScreenEnabled1 = false

    override fun getItemCount(): Int = when {
        isFullScreenEnabled && isFullScreenEnabled1 -> 5
        isFullScreenEnabled1 -> 4
        isFullScreenEnabled -> 4
        else -> 3
    }

    override fun createFragment(position: Int): Fragment {
        val fragment: Fragment = when {
            !isFullScreenEnabled && !isFullScreenEnabled1 -> when (position) {
                0 -> OnboardingFullFragment.newInstance(0)
                1 -> OnboardingFullFragment.newInstance(1)
                2 -> OnboardingFullFragment.newInstance(2)
                else -> OnboardingFullFragment.newInstance(0)
            }

            isFullScreenEnabled1 && !isFullScreenEnabled -> when (position) {
                0 -> OnboardingFullFragment.newInstance(0)
                1 -> OnboardingAdsFullScreenFragment1.newInstance(PAGE_ONBOARDING_ADS_FULL_SCREEN_1)
                2 -> OnboardingFullFragment.newInstance(1)
                3 -> OnboardingFullFragment.newInstance(2)
                else -> OnboardingFullFragment.newInstance(0)
            }

            !isFullScreenEnabled1 && isFullScreenEnabled -> when (position) {
                0 -> OnboardingFullFragment.newInstance(0)
                1 -> OnboardingFullFragment.newInstance(1)
                2 -> OnboardingAdsFullScreenFragment.newInstance(PAGE_ONBOARDING_ADS_FULL_SCREEN)
                3 -> OnboardingFullFragment.newInstance(2)
                else -> OnboardingFullFragment.newInstance(0)
            }

            else -> when (position) {
                0 -> OnboardingFullFragment.newInstance(0)
                1 -> OnboardingAdsFullScreenFragment1.newInstance(PAGE_ONBOARDING_ADS_FULL_SCREEN_1)
                2 -> OnboardingFullFragment.newInstance(1)
                3 -> OnboardingAdsFullScreenFragment.newInstance(PAGE_ONBOARDING_ADS_FULL_SCREEN)
                4 -> OnboardingFullFragment.newInstance(2)
                else -> OnboardingFullFragment.newInstance(0)
            }
        }
        (fragment as? OnboardingFullFragment)?.onActionNextOnboarding = listener
        (fragment as? OnboardingAdsFullScreenFragment)?.onActionNextOnboarding = listener
        (fragment as? OnboardingAdsFullScreenFragment1)?.onActionNextOnboarding = listener
        return fragment
    }
}
