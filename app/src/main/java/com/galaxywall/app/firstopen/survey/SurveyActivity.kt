package com.galaxywall.app.firstopen.survey

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ActivitySurveyBinding
import com.galaxywall.app.firstopen.isSurveyDone

/**
 * Survey step 1. Mirrors the source app's SurveyActivity: a 2-column topic grid; tapping a topic
 * advances to [SurveyDupActivity] (the multi-step survey is split so a native ad shows on each).
 * Ad loading is left as the [loadNativeAd] hook.
 */
class SurveyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurveyBinding
    private lateinit var surveyAdapter: SurveyAdapter
    private var surveyList: List<Survey> = emptyList()

    companion object {
        var isShowToolTip = true

        fun start(context: Context, clearTask: Boolean = true) {
            val intent = Intent(context, SurveyActivity::class.java).apply {
                if (clearTask) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }

        /** True if the survey still needs to run (also where the survey native ads are preloaded). */
        fun preloadAds(activity: Activity): Boolean {
            if (!activity.applicationContext.isSurveyDone) {
                // TODO: ADS — preload the survey / surveyDup / survey3 native ads here.
                return true
            }
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createView()
    }

    private fun createView() {
        loadNativeAd()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })
        // Tooltip hint disabled: the bundled pulse animation just looked like a stray dot over the
        // first card. Re-enable with a proper "tap"/"swipe" Lottie later if needed.
        binding.lottieAnimationView.visibility = View.GONE
        applicationContext.isSurveyDone = false

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        surveyList = surveyTopics(this)
        surveyAdapter = SurveyAdapter(
            surveyList,
            onSelectionChanged = { count -> updateNextButton(count) },
            itemClickListener = { _, position ->
                // Tapping a topic advances to step 2 (mirrors the source flow).
                binding.lottieAnimationView.visibility = View.INVISIBLE
                isShowToolTip = false
                val intent = Intent(this, SurveyDupActivity::class.java)
                intent.putExtra("survey_list", ArrayList(surveyList))
                intent.putExtra("selected_position", position)
                startActivity(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
                } else {
                    @Suppress("DEPRECATION") overridePendingTransition(0, 0)
                }
                finish()
            }
        )
        binding.recyclerView.adapter = surveyAdapter
        binding.tvNext.setOnClickListener { showMessage() }
        updateNextButton(0)
    }

    private fun updateNextButton(selectedCount: Int) {
        if (selectedCount < 2) {
            binding.tvNext.isActivated = false
            binding.tvNext.setTextColor(Color.parseColor("#BEBEBE"))
            binding.tvNext.setOnClickListener { showMessage() }
        } else {
            binding.tvNext.isActivated = true
            binding.tvNext.setTextColor(Color.WHITE)
        }
    }

    /** Placeholder: load + show the survey native ad into binding.frAds. */
    private fun loadNativeAd() {
        // showNativeAd(this, frAds = binding.frAds, ...)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val runnableHide = Runnable { binding.tvMessage.visibility = View.INVISIBLE }

    private fun showMessage() {
        handler.removeCallbacks(runnableHide)
        binding.tvMessage.visibility = View.VISIBLE
        handler.postDelayed(runnableHide, 2000)
    }

    override fun onResume() {
        super.onResume()
        // TODO: ADS — reload survey native ad.
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnableHide)
        super.onDestroy()
    }
}

/** The wallpaper-themed survey topics, reusing icons already in the app. Names come from string
 *  resources so they translate with the chosen language. */
internal fun surveyTopics(context: Context): List<Survey> = listOf(
    Survey(context.getString(R.string.survey_parallax), R.drawable.ic_layers),
    Survey(context.getString(R.string.survey_live), R.drawable.ic_live),
    Survey(context.getString(R.string.survey_photos), R.drawable.ic_image),
    Survey(context.getString(R.string.survey_colorful), R.drawable.ic_palette),
    Survey(context.getString(R.string.survey_featured), R.drawable.ic_star),
    Survey(context.getString(R.string.survey_explore), R.drawable.ic_explore)
)
