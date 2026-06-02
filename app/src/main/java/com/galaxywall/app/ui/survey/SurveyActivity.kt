package com.galaxywall.app.ui.survey

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ActivitySurveyBinding
import com.galaxywall.app.ui.MainActivity

class SurveyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurveyBinding
    private lateinit var adapter: SurveyAdapter

    private val topics = mutableListOf(
        SurveyModel("😜", "Cartoons"),
        SurveyModel("🤣", "Memes"),
        SurveyModel("🧨", "Explosions"),
        SurveyModel("🎏", "Fun"),
        SurveyModel("🕹️", "Arcade"),
        SurveyModel("🛋️", "Relaxing")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = SurveyAdapter(topics) { count -> updateNext(count) }
        binding.recyclerViewSurvey.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSurvey.adapter = adapter
        updateNext(0)

        binding.btnNextToOB.setOnClickListener {
            if (topics.count { it.isSelected } >= MIN_TOPICS) {
                MainActivity.setOnboardingDone(this)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    /** Next is enabled (purple) only when at least [MIN_TOPICS] topics are selected. */
    private fun updateNext(count: Int) {
        val ready = count >= MIN_TOPICS
        binding.btnNextToOB.isEnabled = ready
        binding.btnNextToOB.backgroundTintList = ColorStateList.valueOf(
            if (ready) ContextCompat.getColor(this, R.color.brand_purple)
            else Color.parseColor("#8A8A8A")
        )
    }

    private companion object {
        const val MIN_TOPICS = 2
    }
}
