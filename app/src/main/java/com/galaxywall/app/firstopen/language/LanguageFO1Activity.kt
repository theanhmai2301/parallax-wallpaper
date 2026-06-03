package com.galaxywall.app.firstopen.language

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.galaxywall.app.databinding.ActivityLanguageFo1Binding
import com.galaxywall.app.firstopen.languageCode

/**
 * First language screen. Mirrors the source app's LanguageFO1Activity: tapping a language opens
 * [LanguageFO2Activity] (pre-selected) — the two-screen split exists so a native ad can be shown on
 * each. Ad loading is left as the [loadNativeAd] / preload hooks below.
 */
class LanguageFO1Activity : BaseLanguageActivity<ActivityLanguageFo1Binding>() {

    override fun inflateBinding(inflater: LayoutInflater) =
        ActivityLanguageFo1Binding.inflate(inflater)

    companion object {
        fun start(context: Context, clearTask: Boolean = true) {
            val intent = Intent(context, LanguageFO1Activity::class.java).apply {
                if (clearTask) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }

        /** Mirrors the source preload: true if this screen is the next one (no language chosen yet),
         *  which is also where the FO1/FO2 native ads would be preloaded. */
        fun preload(activity: Activity): Boolean {
            if (activity.applicationContext.languageCode == null) {
                // TODO: ADS — load the FO1 + FO2 native ads here.
                return true
            }
            return false
        }
    }

    override fun createView() {
        loadNativeAd()
        getDataLanguage(false)
        setClickView()
        // TODO: ADS — SurveyActivity.preloadAds(this)
    }

    override fun setAdapter() {
        binding.recyclerViewLanguage.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLanguage.setHasFixedSize(true)
        binding.recyclerViewLanguage.setItemViewCacheSize(languageAdapter.itemCount)
        binding.recyclerViewLanguage.adapter = languageAdapter
    }

    override fun onClickLanguageItem(language: Language, position: Int) {
        if (languageAdapter.currentSelect == position) return
        LanguageFO2Activity.start(this, position)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        finish()
    }

    private fun setClickView() {
        binding.imgConfirm.setOnClickListener { showMessage() }
    }

    /** Placeholder: load + show the FO1 native ad into binding.frAds. */
    private fun loadNativeAd() {
        // showNativeAd(activity = this, frAds = binding.frAds, adLayout = ...)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val runnableHide = Runnable { binding.tvMessage.visibility = View.INVISIBLE }

    private fun showMessage() {
        binding.tvMessage.visibility = View.VISIBLE
        handler.postDelayed(runnableHide, 2000)
    }

    override fun onResume() {
        super.onResume()
        // TODO: ADS — reload FO1 native ad.
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnableHide)
        super.onDestroy()
    }
}
