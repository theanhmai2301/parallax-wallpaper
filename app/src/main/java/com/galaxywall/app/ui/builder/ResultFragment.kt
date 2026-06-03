package com.galaxywall.app.ui.builder

import android.app.Activity
import android.app.Dialog
import android.app.WallpaperManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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
import coil.load
import com.galaxywall.app.sensors.ParallaxSensorManager
import com.galaxywall.app.ui.customview.LoopingVideoTexture
import com.galaxywall.app.util.NetworkMonitor
import com.galaxywall.app.util.collectWhenStarted
import com.galaxywall.app.wallpaper.LiveWallpaperController
import com.galaxywall.app.wallpaper.ParallaxWallpaperService
import com.galaxywall.app.wallpaper.VideoWallpaperController
import com.galaxywall.app.wallpaper.VideoWallpaperService
import com.galaxywall.app.wallpaper.WallpaperApplier
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
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

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private lateinit var sensorManager: ParallaxSensorManager
    private var progressDialog: Dialog? = null
    private var videoPlayer: LoopingVideoTexture? = null
    private var renderJob: Job? = null

    /** The preview content (layers / image / video) must finish loading before it can be applied. */
    private var contentReady = false
    private var wasOffline = false

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

        // Blurred wallpaper thumbnail as the background (like the preview screen).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.resultBg.setRenderEffect(
                RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.CLAMP)
            )
        }
        builderViewModel.currentWallpaper()?.thumbUri?.let { binding.resultBg.load(it) }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        // Can only set the wallpaper once the preview content has fully loaded.
        binding.btnSetBackground.setOnClickListener { if (contentReady) showAdGate() }

        setContentReady(false)
        renderPreview()
        setupVideo()

        // When the network comes back after dropping, reload the preview right away.
        collectWhenStarted(networkMonitor.observe()) { online ->
            if (!online) {
                wasOffline = true
            } else if (wasOffline) {
                wasOffline = false
                if (!contentReady) {
                    renderPreview()
                    setupVideo()
                }
            }
        }

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

    /** Toggles the loading spinner and enables/disables the "set wallpaper" button accordingly. */
    private fun setContentReady(ready: Boolean) {
        contentReady = ready
        _binding?.let {
            it.resultLoading.isVisible = !ready
            it.btnSetBackground.isEnabled = ready
            it.btnSetBackground.alpha = if (ready) 1f else 0.5f
        }
    }

    private fun renderPreview() {
        val isVideo = builderViewModel.currentType() == ContentType.VIDEO
        if (!isVideo) setContentReady(false)
        renderJob?.cancel()
        renderJob = viewLifecycleOwner.lifecycleScope.launch {
            val layers = builderViewModel.loadRenderLayers()
            binding.resultPreview.setLayers(layers)
            // Image/parallax become ready when their bitmaps load; video readiness comes from the
            // player's onReady callback below (the image here is just its poster underneath).
            if (!isVideo) setContentReady(layers.isNotEmpty())
        }
    }

    /** For a video item, play it (looping, muted) over the thumbnail poster. */
    private fun setupVideo() {
        val isVideo = builderViewModel.currentType() == ContentType.VIDEO
        binding.resultVideo.isVisible = isVideo
        if (!isVideo) return
        if (videoPlayer == null) videoPlayer = LoopingVideoTexture(binding.resultVideo)
        videoPlayer?.onReady = { setContentReady(true) }
        videoPlayer?.onError = { setContentReady(false) }
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
            // One unified "set wallpaper" action for every type; all end on the success screen.
            applyWallpaper()
        }
    }

    private fun applyWallpaper() {
        when (builderViewModel.currentType()) {
            ContentType.IMAGE -> builderViewModel.applyStaticImage(WallpaperApplier.Target.BOTH)
            ContentType.PARALLAX -> applyParallax()
            ContentType.VIDEO -> applyVideo()
        }
    }

    private fun applyParallax() {
        // The layers are already loaded (the button is gated on that), so the live service renders
        // the real parallax. Keep the live tilt effect via the system live-wallpaper picker.
        builderViewModel.saveLiveComposition()
        try {
            liveWallpaperLauncher.launch(LiveWallpaperController.changeIntent(requireContext()))
        } catch (e: Exception) {
            // Device without a live-wallpaper picker → fall back to a static composite.
            builderViewModel.setWallpaper(WallpaperApplier.Target.BOTH)
        }
    }

    private fun applyVideo() {
        val url = builderViewModel.currentWallpaper()?.sourceUrl
        if (url == null) {
            Snackbar.make(binding.root, R.string.apply_error, Snackbar.LENGTH_SHORT).show()
            return
        }
        // Keep the looping video via the system live-wallpaper picker.
        VideoWallpaperController.setVideo(requireContext(), url)
        try {
            liveWallpaperLauncher.launch(VideoWallpaperController.changeIntent(requireContext()))
        } catch (e: Exception) {
            // No live-wallpaper picker → fall back to the video's poster frame as a static image.
            val poster = builderViewModel.currentWallpaper()?.thumbUri
            if (poster != null) {
                builderViewModel.applyStaticFromUrl(poster, WallpaperApplier.Target.BOTH)
            } else {
                Snackbar.make(binding.root, R.string.apply_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    /** Launches the system live-wallpaper picker and, on return, treats it as success if our service
     *  became the active wallpaper. Result codes are unreliable across devices, and the system also
     *  updates WallpaperManager.wallpaperInfo a moment AFTER the picker closes, so we accept an
     *  explicit RESULT_OK and otherwise poll wallpaperInfo briefly before giving up. */
    private val liveWallpaperLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (result.resultCode == Activity.RESULT_OK) {
                    onApplied(true)
                    return@launch
                }
                repeat(8) {
                    if (isOurLiveWallpaperActive()) {
                        onApplied(true)
                        return@launch
                    }
                    delay(300)
                }
            }
        }

    private fun isOurLiveWallpaperActive(): Boolean {
        val info = WallpaperManager.getInstance(requireContext()).wallpaperInfo ?: return false
        val cls = info.component?.className ?: return false
        return cls == ParallaxWallpaperService::class.java.name ||
            cls == VideoWallpaperService::class.java.name
    }

    private fun onApplied(success: Boolean) {
        if (success) {
            // Show the success screen; it returns to Home (auto or via its button). Guard against
            // a late callback (e.g. the wallpaperInfo poll) firing after we already navigated away.
            if (findNavController().currentDestination?.id == R.id.resultFragment) {
                findNavController().navigate(R.id.action_result_to_success)
            }
        } else {
            Snackbar.make(binding.root, R.string.apply_error, Snackbar.LENGTH_SHORT).show()
        }
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
        renderJob?.cancel()
        renderJob = null
        videoPlayer?.stop()
        videoPlayer = null
        _binding = null
        super.onDestroyView()
    }
}
