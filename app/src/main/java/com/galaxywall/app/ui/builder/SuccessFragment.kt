package com.galaxywall.app.ui.builder

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.galaxywall.app.R
import com.galaxywall.app.databinding.FragmentSuccessBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Confirmation screen shown after a wallpaper is applied. Uses the same blurred-wallpaper backdrop
 * as the preview/result screens. Returns to Home when the user taps the button or automatically
 * after a short delay (whichever happens first).
 */
class SuccessFragment : Fragment() {

    private var _binding: FragmentSuccessBinding? = null
    private val binding get() = _binding!!

    private val builderViewModel: BuilderViewModel by activityViewModels()

    /** Guards against navigating twice (auto-return + button / back press racing). */
    private var navigated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.successRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        // Blurred wallpaper thumbnail backdrop, matching the preview/result screens.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.successBg.setRenderEffect(
                RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.CLAMP)
            )
        }
        builderViewModel.currentWallpaper()?.thumbUri?.let { binding.successBg.load(it) }

        binding.btnHome.setOnClickListener { goHome() }

        // Auto-return to Home after a short confirmation moment.
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2200)
            goHome()
        }
    }

    private fun goHome() {
        if (navigated) return
        navigated = true
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
