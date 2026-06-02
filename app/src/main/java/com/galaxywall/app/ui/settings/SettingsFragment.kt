package com.galaxywall.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.galaxywall.app.R
import com.galaxywall.app.databinding.FragmentSettingsBinding
import com.galaxywall.app.firstopen.language.LanguageFO2Activity
import com.galaxywall.app.util.collectWhenStarted
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        setupBottomNav()
        setupControls()
        observe()
    }

    private fun setupBottomNav() {
        // Home / Category return to the home screen; Favorite switches over; Setting is current.
        binding.bottomNav.navHome.setOnClickListener { findNavController().navigateUp() }
        binding.bottomNav.navCategory.setOnClickListener { findNavController().navigateUp() }
        binding.bottomNav.navFavorite.setOnClickListener {
            findNavController().navigate(R.id.action_global_favorite)
        }
        // Highlight the Setting tab as active; the rest inactive.
        val active = ContextCompat.getColor(requireContext(), R.color.brand_purple)
        val inactive = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
        binding.bottomNav.navHomeIcon.setColorFilter(inactive)
        binding.bottomNav.navHomeText.setTextColor(inactive)
        binding.bottomNav.navCategoryIcon.setColorFilter(inactive)
        binding.bottomNav.navCategoryText.setTextColor(inactive)
        binding.bottomNav.navFavoriteIcon.setColorFilter(inactive)
        binding.bottomNav.navFavoriteText.setTextColor(inactive)
        binding.bottomNav.navSettingIcon.setColorFilter(active)
        binding.bottomNav.navSettingText.setTextColor(active)
    }

    private fun setupControls() {
        binding.switchDarkMode.setOnClickListener {
            val enabled = binding.switchDarkMode.isChecked
            // Persist FIRST, then apply the night mode. Applying night mode recreates the activity,
            // and App.onCreate re-reads the stored value; if the write hasn't flushed yet the old
            // value is read back and the switch "jumps". Joining the write removes that race.
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.setDarkMode(enabled).join()
                AppCompatDelegate.setDefaultNightMode(
                    if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        binding.switchDynamic.setOnClickListener {
            val enabled = binding.switchDynamic.isChecked
            // Same ordering as dark mode: persist before recreate so the dynamic-color precondition
            // (and the dark-mode read) see the committed values and neither switch flips back.
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.setDynamicColor(enabled).join()
                requireActivity().recreate()
            }
        }

        binding.switchParallax.setOnClickListener {
            viewModel.setParallax(binding.switchParallax.isChecked)
        }

        binding.sliderSensitivity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setSensitivity(value)
        }

        binding.rowLanguage.setOnClickListener { openLanguage() }
        binding.rowShare.setOnClickListener { shareApp() }
        binding.rowFeedback.setOnClickListener { sendFeedback() }
        binding.rowRate.setOnClickListener { openPlayStore() }
        binding.rowPrivacy.setOnClickListener { showPrivacy() }
    }

    /** Opens the language list from settings (full list + back, returns to Home on confirm). */
    private fun openLanguage() {
        LanguageFO2Activity.start(requireContext(), selectedPosition = 0, isOpenFromMain = true)
    }

    private fun shareApp() {
        val link = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.app_name) + "\n" + link)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.detail_share)))
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@galaxywall.app"))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - Feedback")
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.apply_error, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observe() {
        collectWhenStarted(viewModel.state) { settings ->
            if (binding.switchDarkMode.isChecked != settings.darkMode) {
                binding.switchDarkMode.isChecked = settings.darkMode
            }
            if (binding.switchDynamic.isChecked != settings.dynamicColor) {
                binding.switchDynamic.isChecked = settings.dynamicColor
            }
            if (binding.switchParallax.isChecked != settings.parallaxEnabled) {
                binding.switchParallax.isChecked = settings.parallaxEnabled
            }
            if (abs(binding.sliderSensitivity.value - settings.sensitivity) > 0.001f) {
                binding.sliderSensitivity.value = settings.sensitivity.coerceIn(0f, 1f)
            }
        }
    }

    private fun openPlayStore() {
        val packageName = requireContext().packageName
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        try {
            startActivity(market)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun showPrivacy() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_privacy)
            .setMessage(R.string.privacy_policy_text)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
