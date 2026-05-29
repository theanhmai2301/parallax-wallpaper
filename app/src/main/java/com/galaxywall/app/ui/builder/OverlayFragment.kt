package com.galaxywall.app.ui.builder

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.galaxywall.app.R
import com.galaxywall.app.data.local.SettingsManager
import com.galaxywall.app.databinding.FragmentOverlayBinding
import com.galaxywall.app.sensors.ParallaxSensorManager
import com.galaxywall.app.util.collectWhenStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayFragment : Fragment() {

    private var _binding: FragmentOverlayBinding? = null
    private val binding get() = _binding!!

    private val builderViewModel: BuilderViewModel by activityViewModels()

    @Inject
    lateinit var settingsManager: SettingsManager

    private lateinit var sensorManager: ParallaxSensorManager
    private lateinit var effectAdapter: EffectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.overlayRoot) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }

        sensorManager = ParallaxSensorManager(requireContext())
        sensorManager.setListener { x, y -> binding.overlayPreview.setOffset(x, y) }
        binding.overlayPreview.touchParallaxEnabled = !sensorManager.isAvailable
        val dm = resources.displayMetrics
        binding.overlayPreview.targetAspect = dm.widthPixels.toFloat() / dm.heightPixels

        val previewUri = builderViewModel.layers.value.ordered().firstOrNull()
        effectAdapter = EffectAdapter(previewUri, onSelect = builderViewModel::setEffect)
        binding.effectRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.effectRecycler.adapter = effectAdapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSave.setOnClickListener {
            findNavController().navigate(R.id.action_overlay_to_result)
        }

        renderPreview()

        collectWhenStarted(builderViewModel.effect) { effect ->
            binding.overlayPreview.setEffectColorFilter(effect.colorFilter())
            effectAdapter.setSelected(effect)
        }
        collectWhenStarted(builderViewModel.parallaxDepth) { depth ->
            binding.overlayPreview.setParallaxDepth(depth)
        }
        collectWhenStarted(settingsManager.settings) { s ->
            sensorManager.sensitivity = s.sensitivity
            sensorManager.enabled = s.parallaxEnabled
            binding.overlayPreview.parallaxEnabled = s.parallaxEnabled
        }
    }

    private fun renderPreview() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.overlayPreview.setLayers(builderViewModel.loadRenderLayers())
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.start()
    }

    override fun onPause() {
        sensorManager.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.effectRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
