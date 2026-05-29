package com.galaxywall.app.ui.builder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.galaxywall.app.R
import com.galaxywall.app.data.local.SettingsManager
import com.galaxywall.app.databinding.FragmentEditBinding
import com.galaxywall.app.sensors.ParallaxSensorManager
import com.galaxywall.app.util.collectWhenStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    private val builderViewModel: BuilderViewModel by activityViewModels()

    @Inject
    lateinit var settingsManager: SettingsManager

    private lateinit var sensorManager: ParallaxSensorManager
    private var renderJob: Job? = null

    private var pickingSlot: BuilderViewModel.Slot? = null
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val slot = pickingSlot ?: return@registerForActivityResult
        if (uri != null) builderViewModel.pickLayer(slot, uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val basePanelBottom = binding.panel.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.editRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top)
            // Lift the panel (depth slider) above the system navigation bar so they don't overlap.
            binding.panel.updatePadding(bottom = basePanelBottom + bars.bottom)
            insets
        }

        sensorManager = ParallaxSensorManager(requireContext())
        sensorManager.setListener { x, y -> binding.editPreview.setOffset(x, y) }
        binding.editPreview.touchParallaxEnabled = !sensorManager.isAvailable
        val dm = resources.displayMetrics
        binding.editPreview.targetAspect = dm.widthPixels.toFloat() / dm.heightPixels

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNext.setOnClickListener { findNavController().navigate(R.id.action_edit_to_result) }

        // Gallery picking is only for DIY; when viewing an API wallpaper the slots are read-only.
        val diy = builderViewModel.isDiy
        binding.slotBottomHint.isVisible = diy
        binding.slotTopHint.isVisible = diy
        binding.slotBottomCard.isClickable = diy
        binding.slotTopCard.isClickable = diy
        if (diy) {
            binding.slotBottomCard.setOnClickListener { launchPicker(BuilderViewModel.Slot.BOTTOM) }
            binding.slotTopCard.setOnClickListener { launchPicker(BuilderViewModel.Slot.TOP) }
        }

        binding.sliderDepth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) builderViewModel.setParallaxDepth(value)
        }

        collectWhenStarted(builderViewModel.layers) { state ->
            binding.slotBottomImage.loadOrClear(state.bottom)
            binding.slotTopImage.loadOrClear(state.top)
            renderPreview()
        }
        collectWhenStarted(builderViewModel.parallaxDepth) { depth ->
            binding.editPreview.setParallaxDepth(depth)
            if (kotlin.math.abs(binding.sliderDepth.value - depth) > 0.001f) {
                binding.sliderDepth.value = depth.coerceIn(0f, 1f)
            }
        }
        collectWhenStarted(settingsManager.settings) { s ->
            sensorManager.sensitivity = s.sensitivity
            sensorManager.enabled = s.parallaxEnabled
            binding.editPreview.parallaxEnabled = s.parallaxEnabled
        }
    }

    private fun renderPreview() {
        renderJob?.cancel()
        renderJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.editPreview.setLayers(builderViewModel.loadRenderLayers())
        }
    }

    private fun launchPicker(slot: BuilderViewModel.Slot) {
        pickingSlot = slot
        pickImage.launch("image/*")
    }

    private fun android.widget.ImageView.loadOrClear(uri: String?) {
        if (uri == null) setImageDrawable(null) else load(uri)
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
        renderJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
