package com.galaxywall.app.firstopen.survey

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
import com.galaxywall.app.firstopen.onboarding.OnBoardingFullFragmentActivity

/**
 * Survey step 2. Mirrors the source app's SurveyDupActivity: receives the list + the topic tapped on
 * step 1 (pre-selected); tapping another topic advances to [Survey3Activity], while "Next" (with at
 * least one selected) completes the survey and opens onboarding. Ad loading is left as a hook.
 */
@Suppress("UNCHECKED_CAST", "DEPRECATION")
class SurveyDupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurveyBinding
    private lateinit var surveyAdapter: SurveyAdapter
    private var surveyList: List<Survey> = emptyList()
    private var selectedPosition: Int = -1
    private var isNext = false

    companion object {
        fun start(context: Context, clearTask: Boolean = true) {
            val intent = Intent(context, SurveyDupActivity::class.java).apply {
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
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })
        applicationContext.isSurveyDone = false
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.lottieAnimationView.visibility = View.INVISIBLE

        surveyList = (intent?.getSerializableExtra("survey_list") as? ArrayList<Survey>) ?: surveyTopics(this)
        selectedPosition = intent.getIntExtra("selected_position", -1)
        if (selectedPosition != -1) surveyList[selectedPosition].isSelected = true

        surveyAdapter = SurveyAdapter(
            surveyList,
            onSelectionChanged = { count -> updateNextButton(count) },
            itemClickListener = { _, position ->
                if (position == selectedPosition) {
                    selectedPosition = -1
                    return@SurveyAdapter
                }
                val intent = Intent(this, Survey3Activity::class.java)
                intent.putExtra("survey_list", ArrayList(surveyList))
                intent.putExtra("selected_position", position)
                startActivity(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
                } else {
                    overridePendingTransition(0, 0)
                }
                finish()
            }
        )
        binding.recyclerView.adapter = surveyAdapter

        binding.tvNext.setOnClickListener {
            if (isNext) {
                applicationContext.isSurveyDone = true
                OnBoardingFullFragmentActivity.start(this)
                finish()
            } else {
                // Fewer than 2 selected (including exactly 1) → warn, same as the empty case.
                showMessage()
            }
        }
        updateNextButton(surveyAdapter.getSelectedItems().size)
        // TODO: ADS — OnBoardingFullFragmentActivity.preloadAds(this)
    }

    private fun updateNextButton(selectedCount: Int) {
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

    private val handler = Handler(Looper.getMainLooper())
    private val runnableHide = Runnable { binding.tvMessage.visibility = View.INVISIBLE }

    private fun showMessage() {
        handler.removeCallbacks(runnableHide)
        binding.tvMessage.visibility = View.VISIBLE
        handler.postDelayed(runnableHide, 2000)
    }

    override fun onResume() {
        super.onResume()
        // TODO: ADS — reload surveyDup native ad.
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnableHide)
        super.onDestroy()
    }
}
