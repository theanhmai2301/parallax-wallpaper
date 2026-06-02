package com.galaxywall.app.firstopen.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.galaxywall.app.R
import com.galaxywall.app.databinding.FragmentOnboardingFullFragmentBinding

/**
 * A content onboarding page. Mirrors the source app's OnboardingFullFragment: renders the page for
 * its position and forwards Next/Complete. The last page shows an interstitial (hook) before
 * completing; each page loads a native ad into frAds (hook). A swipe tooltip overlay (flSwipe) is
 * included but hidden — show it from [onActionNextOnboarding] driven flags when wiring ads.
 */
class OnboardingFullFragment : Fragment() {

    private var _binding: FragmentOnboardingFullFragmentBinding? = null
    private val binding get() = _binding!!

    private var pagePosition = 0
    var onActionNextOnboarding: OnActionNextOnboarding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingFullFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagePosition = requireArguments().getInt(ON_BOARDING_POSITION)
        binding.flSwipe.visibility = View.GONE
        setupViewFile()

        binding.tvNext.setOnClickListener {
            if (pagePosition == LAST_PAGE) {
                // TODO: ADS — show the onboarding interstitial here, then call onComplete() from its
                //  onClosed/onFailed callback. For now complete immediately.
                onActionNextOnboarding?.onComplete()
            } else {
                onActionNextOnboarding?.onNext()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO: ADS — load + show this page's native ad into binding.frAds.
        // loadNativeAd()
    }

    private fun loadNativeAd() {
        // showNativeAd(activity = requireActivity(), frAds = binding.frAds, ...)
    }

    private fun setupViewFile() {
        val (titleRes, desRes, iconRes) = when (pagePosition) {
            0 -> Triple(R.string.txt_onboarding_1_title, R.string.txt_onboarding_1_des, R.drawable.ic_layers)
            1 -> Triple(R.string.txt_onboarding_2_title, R.string.txt_onboarding_2_des, R.drawable.ic_live)
            else -> Triple(R.string.txt_onboarding_3_title, R.string.txt_onboarding_3_des, R.drawable.ic_palette)
        }
        binding.tvTitleOnboard.setText(titleRes)
        binding.tvDesOnboard.setText(desRes)
        binding.imgOnboarding.setImageResource(iconRes)
        binding.tvNext.setText(if (pagePosition == LAST_PAGE) R.string.action_start else R.string.action_next)

        // Page indicator dots.
        val density = resources.displayMetrics.density
        listOf(binding.dot0 to 0, binding.dot1 to 1, binding.dot2 to 2).forEach { (dot, index) ->
            val active = index == pagePosition
            dot.setBackgroundResource(if (active) R.drawable.dot_active else R.drawable.dot_inactive)
            dot.layoutParams = dot.layoutParams.apply {
                width = ((if (active) 22 else 8) * density).toInt()
                height = (8 * density).toInt()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ON_BOARDING_POSITION = "on_boarding_position_1"
        private const val LAST_PAGE = 2

        fun newInstance(position: Int): OnboardingFullFragment =
            OnboardingFullFragment().apply {
                arguments = Bundle().apply { putInt(ON_BOARDING_POSITION, position) }
            }
    }
}
