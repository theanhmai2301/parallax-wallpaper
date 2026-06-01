package com.galaxywall.app.ui.builder

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.galaxywall.app.R
import com.galaxywall.app.data.local.SettingsManager
import com.galaxywall.app.data.model.ContentType
import com.galaxywall.app.databinding.FragmentResultBinding
import com.galaxywall.app.sensors.ParallaxSensorManager
import com.galaxywall.app.ui.customview.LoopingVideoTexture
import com.galaxywall.app.util.collectWhenStarted
import com.galaxywall.app.wallpaper.LiveWallpaperController
import com.galaxywall.app.wallpaper.VideoWallpaperController
import com.galaxywall.app.wallpaper.WallpaperApplier
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val builderViewModel: BuilderViewModel by activityViewModels()

    @Inject
    lateinit var settingsManager: SettingsManager

    private lateinit var sensorManager: ParallaxSensorManager
    private var progressDialog: Dialog? = null
    private var videoPlayer: LoopingVideoTexture? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.resultRoot) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }

        sensorManager = ParallaxSensorManager(requireContext())
        sensorManager.setListener { x, y -> binding.resultPreview.setOffset(x, y) }
        binding.resultPreview.touchParallaxEnabled = !sensorManager.isAvailable
        binding.resultPreview.setEffectColorFilter(builderViewModel.effect.value.colorFilter())
        val dm = resources.displayMetrics
        // Phone-shaped frame matching the screen aspect; the image fills it (no black side gaps).
        binding.resultPreview.targetAspect = 0f
        (binding.resultCard.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio =
            "W,${dm.widthPixels}:${dm.heightPixels}"

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSetBackground.setOnClickListener { showAdGate() }

        renderPreview()
        setupVideo()

        collectWhenStarted(builderViewModel.working) { message ->
            if (message != null) showProgress(message) else dismissProgress()
        }
        collectWhenStarted(builderViewModel.events) { event ->
            when (event) {
                is BuilderViewModel.Event.Applied -> onApplied(event.success)
            }
        }
        collectWhenStarted(builderViewModel.parallaxDepth) { depth ->
            binding.resultPreview.setParallaxDepth(depth)
        }
        collectWhenStarted(settingsManager.settings) { s ->
            sensorManager.sensitivity = s.sensitivity
            sensorManager.enabled = s.parallaxEnabled
            binding.resultPreview.parallaxEnabled = s.parallaxEnabled
        }
    }

    private fun renderPreview() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.resultPreview.setLayers(builderViewModel.loadRenderLayers())
        }
    }

    /** For a video item, play it (looping, muted) over the thumbnail poster. */
    private fun setupVideo() {
        val isVideo = builderViewModel.currentType() == ContentType.VIDEO
        binding.resultVideo.isVisible = isVideo
        if (!isVideo) return
        if (videoPlayer == null) videoPlayer = LoopingVideoTexture(binding.resultVideo)
        builderViewModel.currentWallpaper()?.sourceUrl?.let { videoPlayer?.play(it) }
    }

    private fun showAdGate() {
        val content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ad_gate, null)
        val dialog = Dialog(requireContext()).apply {
            setContentView(content)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        content.findViewById<View>(R.id.btnWatchAd).setOnClickListener {
            dialog.dismiss()
            watchAdThenApply()
        }
        content.findViewById<View>(R.id.btnSkip).setOnClickListener { dialog.dismiss() }
        dialog.show()
        // Default Dialog windows wrap their content, which makes this card too narrow and forces
        // ugly text wrapping. Pin the width to most of the screen so it matches the rest of the UI.
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun watchAdThenApply() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Simulated rewarded ad — replace with a real AdMob RewardedAd if needed.
            showProgress(getString(R.string.ad_loading))
            delay(1800)
            dismissProgress()
            when (builderViewModel.currentType()) {
                ContentType.IMAGE -> builderViewModel.applyStaticImage(WallpaperApplier.Target.BOTH)
                ContentType.VIDEO -> setVideoWallpaper()
                ContentType.PARALLAX -> applyParallax()
            }
        }
    }

    private fun applyParallax() {
        // Save the composition (layers + depth + effect) for the live wallpaper.
        builderViewModel.saveLiveComposition()
        // Set as a live parallax wallpaper so the 3D tilt works on the home screen.
        try {
            startActivity(LiveWallpaperController.changeIntent(requireContext()))
        } catch (e: Exception) {
            // Device without a live-wallpaper picker → fall back to a static composite.
            builderViewModel.setWallpaper(WallpaperApplier.Target.BOTH)
        }
    }

    private fun setVideoWallpaper() {
        val url = builderViewModel.currentWallpaper()?.sourceUrl
        if (url == null) {
            Snackbar.make(binding.root, R.string.apply_error, Snackbar.LENGTH_SHORT).show()
            return
        }
        VideoWallpaperController.setVideo(requireContext(), url)
        try {
            startActivity(VideoWallpaperController.changeIntent(requireContext()))
        } catch (e: Exception) {
            Snackbar.make(binding.root, R.string.apply_error, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun onApplied(success: Boolean) {
        val message = getString(if (success) R.string.apply_success else R.string.apply_error)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        if (success) findNavController().popBackStack(R.id.homeFragment, false)
    }

    private fun showProgress(message: String) {
        val dialog = progressDialog ?: Dialog(requireContext()).also { progressDialog = it }
        val content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null)
        content.findViewById<TextView>(R.id.progressMessage).text = message
        dialog.setContentView(content)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        if (!dialog.isShowing) dialog.show()
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.start()
        setupVideo()
    }

    override fun onPause() {
        sensorManager.stop()
        videoPlayer?.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        dismissProgress()
        progressDialog = null
        videoPlayer?.stop()
        videoPlayer = null
        _binding = null
        super.onDestroyView()
    }
}
