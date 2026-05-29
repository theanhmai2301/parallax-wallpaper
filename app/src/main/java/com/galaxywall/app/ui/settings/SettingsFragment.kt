package com.galaxywall.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import com.galaxywall.app.BuildConfig
import com.galaxywall.app.R
import com.galaxywall.app.databinding.FragmentSettingsBinding
import com.galaxywall.app.util.collectWhenStarted
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsContent) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }

        binding.textVersion.text = BuildConfig.VERSION_NAME
        setupControls()
        observe()
    }

    private fun setupControls() {
        binding.switchDarkMode.setOnClickListener {
            val enabled = binding.switchDarkMode.isChecked
            viewModel.setDarkMode(enabled)
            AppCompatDelegate.setDefaultNightMode(
                if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.switchDynamic.setOnClickListener {
            val enabled = binding.switchDynamic.isChecked
            val job = viewModel.setDynamicColor(enabled)
            viewLifecycleOwner.lifecycleScope.launch {
                job.join()
                requireActivity().recreate()
            }
        }

        binding.switchParallax.setOnClickListener {
            viewModel.setParallax(binding.switchParallax.isChecked)
        }

        binding.sliderSensitivity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setSensitivity(value)
        }

        binding.rowClearCache.setOnClickListener { clearCache() }
        binding.rowRate.setOnClickListener { openPlayStore() }
        binding.rowPrivacy.setOnClickListener { showPrivacy() }
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

    private fun clearCache() {
        val context = requireContext()
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
        File(context.cacheDir, "shared").deleteRecursively()
        Snackbar.make(binding.root, R.string.cache_cleared, Snackbar.LENGTH_SHORT).show()
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
