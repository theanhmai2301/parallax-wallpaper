package com.galaxywall.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.galaxywall.app.R
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.model.WallpaperCategory
import com.galaxywall.app.databinding.FragmentHomeBinding
import com.galaxywall.app.ui.adapter.WallpaperAdapter
import com.galaxywall.app.ui.builder.BuilderViewModel
import com.galaxywall.app.util.UiState
import com.galaxywall.app.util.collectWhenStarted
import com.galaxywall.app.util.setVisible
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val builderViewModel: BuilderViewModel by activityViewModels()

    private val adapter by lazy {
        WallpaperAdapter(onClick = ::openPreview, onFavorite = viewModel::toggleFavorite)
    }

    private var firstLoadAnimated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyInsets()
        setupToolbar()
        setupChips()
        setupRecycler()
        setupSwipeRefresh()
        setupBottomBar()
        observeState()
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeRoot) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeBottomBar) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                updateMargins(bottom = bottom + (8 * resources.displayMetrics.density).toInt())
            }
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setTitle(getString(R.string.home_title))
        binding.toolbar.onFolderClick {
            findNavController().navigate(R.id.action_home_to_favorite)
        }
        binding.toolbar.onSettingsClick {
            findNavController().navigate(R.id.action_home_to_settings)
        }
    }

    private fun setupChips() {
        val group = binding.categoryChips
        viewModel.categories.forEach { category ->
            val chip = LayoutInflater.from(requireContext())
                .inflate(R.layout.chip_category, group, false) as Chip
            chip.id = View.generateViewId()
            chip.text = category.label
            chip.tag = category
            chip.isChecked = category == WallpaperCategory.ALL
            group.addView(chip)
        }
        group.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(id)
            (chip.tag as? WallpaperCategory)?.let { viewModel.setCategory(it) }
        }
    }

    private fun setupRecycler() {
        binding.recycler.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                .apply { gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE }
            adapter = this@HomeFragment.adapter
            setHasFixedSize(false)
            setItemViewCacheSize(20)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_purple, R.color.brand_blue)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    }

    private fun setupBottomBar() {
        binding.tabParallax.setOnClickListener { binding.recycler.smoothScrollToPosition(0) }
        binding.tabDiy.setOnClickListener {
            builderViewModel.startBlank()
            findNavController().navigate(R.id.action_home_to_edit)
        }
    }

    private fun observeState() {
        collectWhenStarted(viewModel.uiState) { renderState(it) }
    }

    private fun renderState(state: UiState<List<Wallpaper>>) {
        val loading = state is UiState.Loading
        binding.shimmerLayout.setVisible(loading)
        if (loading) binding.shimmerLayout.startShimmer() else binding.shimmerLayout.stopShimmer()

        when (state) {
            is UiState.Success -> {
                binding.swipeRefresh.isRefreshing = false
                binding.emptyState.emptyRoot.setVisible(false)
                binding.recycler.setVisible(true)
                submitAndAnimate(state.data)
            }
            is UiState.Empty -> {
                binding.swipeRefresh.isRefreshing = false
                binding.recycler.setVisible(false)
                showEmpty(getString(R.string.empty_home))
            }
            is UiState.Error -> {
                binding.swipeRefresh.isRefreshing = false
                binding.recycler.setVisible(false)
                showEmpty(state.message)
            }
            UiState.Loading -> binding.emptyState.emptyRoot.setVisible(false)
        }
    }

    private fun submitAndAnimate(data: List<Wallpaper>) {
        adapter.submitList(data) {
            if (!firstLoadAnimated && data.isNotEmpty()) {
                firstLoadAnimated = true
                binding.recycler.layoutAnimation =
                    AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_fall_down)
                binding.recycler.scheduleLayoutAnimation()
            }
        }
    }

    private fun showEmpty(title: String) {
        binding.emptyState.emptyRoot.setVisible(true)
        binding.emptyState.emptyTitle.text = title
        binding.emptyState.emptySubtitle.text = ""
    }

    private fun openPreview(wallpaper: Wallpaper, sharedView: View) {
        val list = adapter.currentList
        val index = list.indexOfFirst { it.id == wallpaper.id }.coerceAtLeast(0)
        builderViewModel.startFrom(list, index)
        findNavController().navigate(R.id.action_home_to_preview)
    }

    override fun onDestroyView() {
        binding.recycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
