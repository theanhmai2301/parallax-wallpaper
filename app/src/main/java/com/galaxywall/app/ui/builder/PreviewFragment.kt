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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.galaxywall.app.R
import com.galaxywall.app.databinding.FragmentPreviewBinding
import com.galaxywall.app.util.dp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val builderViewModel: BuilderViewModel by activityViewModels()

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            builderViewModel.setIndex(position)
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

        binding.previewPager.adapter = PreviewPagerAdapter(builderViewModel.previewList) { position ->
            if (position >= 0) {
                builderViewModel.setIndex(position)
                openEdit()
            }
        }
        binding.previewPager.setCurrentItem(builderViewModel.index.value, false)
        binding.previewPager.registerOnPageChangeCallback(pageCallback)
        setupPeek()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNext.setOnClickListener { openEdit() }
    }

    private fun openEdit() {
        builderViewModel.prepareLayersFromCurrent()
        findNavController().navigate(R.id.action_preview_to_edit)
    }

    /** Makes the pager reveal a peek of the previous/next wallpaper on the left and right. */
    private fun setupPeek() {
        val pager = binding.previewPager
        pager.offscreenPageLimit = 1
        pager.setPageTransformer(MarginPageTransformer(14.dp))
        (pager.getChildAt(0) as? RecyclerView)?.apply {
            val peek = 36.dp
            setPadding(peek, 0, peek, 0)
            clipToPadding = false
        }
    }

    override fun onDestroyView() {
        binding.previewPager.unregisterOnPageChangeCallback(pageCallback)
        binding.previewPager.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
