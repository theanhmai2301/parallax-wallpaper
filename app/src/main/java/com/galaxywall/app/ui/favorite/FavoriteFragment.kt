package com.galaxywall.app.ui.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.galaxywall.app.R
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.databinding.FragmentFavoriteBinding
import com.galaxywall.app.ui.adapter.WallpaperAdapter
import com.galaxywall.app.ui.builder.BuilderViewModel
import com.galaxywall.app.util.UiState
import com.galaxywall.app.util.collectWhenStarted
import com.galaxywall.app.util.setVisible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()
    private val builderViewModel: BuilderViewModel by activityViewModels()

    private val adapter by lazy {
        WallpaperAdapter(onClick = ::openPreview, onFavorite = viewModel::toggleFavorite)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.favoriteRoot) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top)
            insets
        }
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        setupBottomNav()
        binding.recycler.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = this@FavoriteFragment.adapter
            setHasFixedSize(false)
        }
        binding.emptyState.emptyTitle.text = getString(R.string.empty_favorites)
        binding.emptyState.emptySubtitle.text = getString(R.string.empty_favorites_hint)

        collectWhenStarted(viewModel.uiState) { state ->
            when (state) {
                is UiState.Success -> {
                    binding.emptyState.emptyRoot.setVisible(false)
                    binding.recycler.setVisible(true)
                    adapter.submitList(state.data)
                }
                else -> {
                    adapter.submitList(emptyList())
                    binding.recycler.setVisible(false)
                    binding.emptyState.emptyRoot.setVisible(state is UiState.Empty)
                }
            }
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.navHome.setOnClickListener { findNavController().navigateUp() }
        binding.bottomNav.navCategory.setOnClickListener { findNavController().navigateUp() }
        binding.bottomNav.navSetting.setOnClickListener {
            findNavController().navigate(R.id.action_global_settings)
        }
        // Favorite is the current screen → highlight it; the rest inactive.
        val active = ContextCompat.getColor(requireContext(), R.color.brand_purple)
        val inactive = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
        binding.bottomNav.navHomeIcon.setColorFilter(inactive)
        binding.bottomNav.navHomeText.setTextColor(inactive)
        binding.bottomNav.navCategoryIcon.setColorFilter(inactive)
        binding.bottomNav.navCategoryText.setTextColor(inactive)
        binding.bottomNav.navSettingIcon.setColorFilter(inactive)
        binding.bottomNav.navSettingText.setTextColor(inactive)
        binding.bottomNav.navFavoriteIcon.setColorFilter(active)
        binding.bottomNav.navFavoriteText.setTextColor(active)
    }

    private fun openPreview(wallpaper: Wallpaper, sharedView: View) {
        val list = adapter.currentList.filter { it.type == wallpaper.type }
        val index = list.indexOfFirst { it.id == wallpaper.id }.coerceAtLeast(0)
        builderViewModel.startFrom(list, index)
        findNavController().navigate(R.id.action_favorite_to_preview)
    }

    override fun onDestroyView() {
        binding.recycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
