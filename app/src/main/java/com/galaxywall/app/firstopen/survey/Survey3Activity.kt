package com.galaxywall.app.firstopen.survey

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import com.galaxywall.app.firstopen.onboarding.OnBoardingFullFragmentActivity

/**
 * Survey step 3 (final). Mirrors the source app's Survey3Activity: "Next" (with at least 2 topics
 * selected) marks the survey complete and opens onboarding; otherwise it nudges the user. Ad loading
 * is left as the [loadNativeAd] hook.
 */
@Suppress("UNCHECKED_CAST")
class Survey3Activity : AppCompatActivity() {

    private lateinit var binding: ActivitySurveyBinding
    private var surveyAdapter: SurveyAdapter? = null
    private var surveyList: List<Survey> = emptyList()
    private var selectedPosition: Int = -1
    private var isNext = false

    companion object {
        fun start(context: Context, clearTask: Boolean = true) {
            val intent = Intent(context, Survey3Activity::class.java).apply {
                if (clearTask) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
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
        applicationContext.isSurveyDone = false
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.lottieAnimationView.visibility = View.INVISIBLE

        surveyList = (intent?.getSerializableExtra("survey_list") as? ArrayList<Survey>) ?: surveyTopics(this)
        selectedPosition = intent.getIntExtra("selected_position", -1)
        if (selectedPosition != -1) surveyList[selectedPosition].isSelected = true

        surveyAdapter = SurveyAdapter(
            surveyList,
            onSelectionChanged = { updateNextButton() },
            itemClickListener = { _, _ -> }
        )
        binding.recyclerView.adapter = surveyAdapter

        binding.tvNext.setOnClickListener {
            if (isNext) {
                applicationContext.isSurveyDone = true
                OnBoardingFullFragmentActivity.start(this)
                finish()
            } else {
                showMessage()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })
        updateNextButton()
    }

    private fun updateNextButton() {
        val selectedCount = surveyAdapter?.getSelectedItems()?.size ?: 0
        if (selectedCount < 2) {
            binding.tvNext.setBackgroundResource(R.drawable.bg_next_survey_disable)
            binding.tvNext.setTextColor(Color.parseColor("#BEBEBE"))
            isNext = false
        } else {
            isNext = true
            binding.tvNext.setBackgroundResource(R.drawable.bg_next_survey_enable)
            binding.tvNext.setTextColor(Color.WHITE)
        }
    }

    private fun loadNativeAd() {
        // showNativeAd(this, frAds = binding.frAds, ...)
    }

    private fun showMessage() {
        binding.tvMessage.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvMessage.visibility = View.INVISIBLE
        }, 2000)
    }

    override fun onResume() {
        super.onResume()
        // TODO: ADS — reload survey3 native ad.
    }
}
