package com.galaxywall.app.firstopen.language

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager

/**
 * Second language screen, pre-selected with the row tapped on FO1. Mirrors the source app's
 * LanguageFO2Activity: confirm applies the language and advances (survey on first open, home when
 * opened from the in-app settings). Ad loading is left as the [loadNativeAd] hook.
 */
class LanguageFO2Activity : BaseLanguageActivity() {

    companion object {
        const val KEY_POSITION_SELECTED = "KEY_POSITION_SELECTED"
        const val KEY_OPEN_FROM_MAIN = "isOpenFromMain"

        fun start(context: Context, selectedPosition: Int = 0, isOpenFromMain: Boolean = false) {
            val intent = Intent(context, LanguageFO2Activity::class.java).apply {
                putExtra(KEY_POSITION_SELECTED, selectedPosition)
                putExtra(KEY_OPEN_FROM_MAIN, isOpenFromMain)
            }
            context.startActivity(intent)
        }
    }

    private var positionSelected = 0
    private var openFromMain = false

    override fun createView() {
        positionSelected = intent.getIntExtra(KEY_POSITION_SELECTED, 0)
        openFromMain = intent.getBooleanExtra(KEY_OPEN_FROM_MAIN, false)

        loadNativeAd()
        getDataLanguage(openFromMain)
        setClickView()


        languageAdapter.selectItem(positionSelected)
        languageCodeSelected = languageAdapter.getAt(positionSelected).locale
        // Open scrolled to the row chosen on FO1 so the list doesn't appear to reset to the top.
        binding.recyclerViewLanguage.scrollToPosition(positionSelected)
    }

    override fun setAdapter() {
        binding.recyclerViewLanguage.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLanguage.setHasFixedSize(true)
        binding.recyclerViewLanguage.setItemViewCacheSize(languageAdapter.itemCount)
        binding.recyclerViewLanguage.adapter = languageAdapter
    }

    override fun onStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        super.onStart()
    }

    override fun onClickLanguageItem(language: Language, position: Int) {
        positionSelected = position
        languageCodeSelected = languageAdapter.getAt(positionSelected).locale
    }

    private fun setClickView() {
        binding.imgConfirm.setOnClickListener { changeLanguage(openFromMain) }
    }

    /** Placeholder: load + show the FO2 native ad into binding.frAds. */
    private fun loadNativeAd() {
        // showNativeAd(activity = this, frAds = binding.frAds, adLayout = ...)
    }

    override fun onResume() {
        super.onResume()
        // TODO: ADS — reload FO2 native ad.
    }
}
