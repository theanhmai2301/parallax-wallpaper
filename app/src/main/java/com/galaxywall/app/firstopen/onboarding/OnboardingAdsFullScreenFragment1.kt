package com.galaxywall.app.firstopen.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.galaxywall.app.databinding.FragmentOnboardingAdsFullScreenBinding

/**
 * Second full-screen native-ad onboarding page (placeholder). Mirrors the source app's
 * OnboardingAdsFullScreenFragment1 — identical to [OnboardingAdsFullScreenFragment] but kept as a
 * separate class so the two ad slots can use different ad units. The ad is a [loadFullScreenAd] hook.
 */
class OnboardingAdsFullScreenFragment1 : Fragment() {

    private var _binding: FragmentOnboardingAdsFullScreenBinding? = null
    private val binding get() = _binding!!

    var onActionNextOnboarding: OnActionNextOnboarding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingAdsFullScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFullScreenAd()
        binding.btnNext.setOnClickListener { onActionNextOnboarding?.onNext() }
        binding.btnClose.setOnClickListener { onActionNextOnboarding?.onNext() }
    }

    /** Placeholder: load + show the full-screen native ad into binding.frAds. */
    private fun loadFullScreenAd() {
        // showNativeAd(activity = requireActivity(), frAds = binding.frAds, ...)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_PAGE = "page"
        fun newInstance(page: Int): OnboardingAdsFullScreenFragment1 =
            OnboardingAdsFullScreenFragment1().apply {
                arguments = Bundle().apply { putInt(ARG_PAGE, page) }
            }
    }
}
