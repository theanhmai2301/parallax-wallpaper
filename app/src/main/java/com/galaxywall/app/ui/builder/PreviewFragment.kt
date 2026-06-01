package com.galaxywall.app.ui.builder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import coil.size.Size
import com.galaxywall.app.R
import com.galaxywall.app.data.local.SettingsManager
import com.galaxywall.app.data.model.ContentType
import com.galaxywall.app.databinding.FragmentPreviewBinding
import com.galaxywall.app.sensors.ParallaxSensorManager
import com.galaxywall.app.ui.customview.ParallaxImageView
import com.galaxywall.app.util.collectWhenStarted
import com.galaxywall.app.util.dp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val builderViewModel: BuilderViewModel by activityViewModels()

    @Inject
    lateinit var settingsManager: SettingsManager

    private lateinit var sensorManager: ParallaxSensorManager

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            builderViewModel.setIndex(position)
            updateVideoPlayback()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.previewRoot) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }

        sensorManager = ParallaxSensorManager(requireContext())
        sensorManager.setListener { x, y -> forwardOffset(x, y) }

        val dm = resources.displayMetrics
        binding.previewPager.adapter = PreviewPagerAdapter(
            items = builderViewModel.previewList,
            scope = viewLifecycleOwner.lifecycleScope,
            // The frame fills the page height (tall); the image covers it.
            loadSize = Size(dm.widthPixels, dm.heightPixels),
            sensorAvailable = sensorManager.isAvailable,
            onClick = { position ->
                if (position >= 0) {
                    builderViewModel.setIndex(position)
                    openSelected()
                }
            }
        )
        // Leave a 5% gap below the "Preview" bar and a 20% gap above the bottom.
        binding.previewPager.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = (dm.heightPixels * 0.05f).toInt()
            bottomMargin = (dm.heightPixels * 0.15f).toInt()
        }
        binding.previewPager.setCurrentItem(builderViewModel.index.value, false)
        binding.previewPager.registerOnPageChangeCallback(pageCallback)
        setupPeek()
        binding.previewPager.post { updateVideoPlayback() }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNext.setOnClickListener { openSelected() }

        collectWhenStarted(settingsManager.settings) { s ->
            sensorManager.sensitivity = s.sensitivity
            sensorManager.enabled = s.parallaxEnabled
        }
    }

    /** Parallax items go to the editor; static images and videos go straight to the result screen. */
    private fun openSelected() {
        if (builderViewModel.currentType() == ContentType.PARALLAX) {
            builderViewModel.prepareLayersFromCurrent()
            findNavController().navigate(R.id.action_preview_to_edit)
        } else {
            findNavController().navigate(R.id.action_preview_to_result)
        }
    }

    /** Forwards the device tilt to every visible parallax page. */
    private fun forwardOffset(x: Float, y: Float) {
        val rv = binding.previewPager.getChildAt(0) as? RecyclerView ?: return
        for (i in 0 until rv.childCount) {
            rv.getChildAt(i).findViewById<ParallaxImageView>(R.id.pagePreview)?.setOffset(x, y)
        }
    }

    /** Plays only the centered page's video; pauses the rest to keep it light. */
    private fun updateVideoPlayback() {
        val rv = binding.previewPager.getChildAt(0) as? RecyclerView ?: return
        val current = binding.previewPager.currentItem
        for (i in 0 until rv.childCount) {
            val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? PreviewPagerAdapter.PageVH
                ?: continue
            if (holder.bindingAdapterPosition == current) holder.playVideo() else holder.stopVideo()
        }
    }

    private fun stopAllVideos() {
        val rv = binding.previewPager.getChildAt(0) as? RecyclerView ?: return
        for (i in 0 until rv.childCount) {
            (rv.getChildViewHolder(rv.getChildAt(i)) as? PreviewPagerAdapter.PageVH)?.stopVideo()
        }
    }

    /** Reveals a peek of the previous/next wallpaper, scaled down so the centered page stands out. */
    private fun setupPeek() {
        val pager = binding.previewPager
        pager.offscreenPageLimit = 1
        val transformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(4.dp))
            addTransformer { page, position ->
                // Centered page (position 0) full size; side pages shrink toward SIDE_SCALE.
                val scale = SIDE_SCALE +
                    (1f - SIDE_SCALE) * (1f - kotlin.math.abs(position).coerceIn(0f, 1f))
                page.scaleX = scale
                page.scaleY = scale
            }
        }
        pager.setPageTransformer(transformer)
        (pager.getChildAt(0) as? RecyclerView)?.apply {
            // Pad each side by 1/6 of the screen so each page (and its frame) is ~2/3 wide.
            val peek = resources.displayMetrics.widthPixels / 14
            setPadding(peek, 0, peek, 0)
            //setPadding(36, 0, 36, 0)
            clipToPadding = false
        }
    }

    private companion object {
        const val SIDE_SCALE = 0.8f
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
        binding.previewPager.unregisterOnPageChangeCallback(pageCallback)
        binding.previewPager.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
