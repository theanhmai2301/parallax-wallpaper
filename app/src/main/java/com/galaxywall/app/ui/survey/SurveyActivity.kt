package com.galaxywall.app.ui.survey

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ActivitySurveyBinding
import com.galaxywall.app.ui.home.HomeFragment
import kotlin.jvm.java

class SurveyActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySurveyBinding

    private lateinit var adapter: SurveyAdapter

    private var selectedLanguage: SurveyModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySurveyBinding.inflate(layoutInflater)

        setContentView(binding.root)


        // trạng thái nút next ban đầu
        binding.btnNextToOB.isActivated = false

        // data list
        val languageList = mutableListOf(

            SurveyModel(
                R.drawable.flag_vn_vietnam,
                "Cartoon"
            ),

            SurveyModel(
                R.drawable.flag_in_hindi,
                "Meme"
            ),

            SurveyModel(
                R.drawable.flag_fr_france,
                "Fun"
            ),

            SurveyModel(
                R.drawable.flag_pt_portugal,
                "Relax"
            )
        )

        // adapter
        adapter = SurveyAdapter(languageList) { survey ->

            selectedLanguage = survey

            binding.btnNextToOB.isActivated = true

        }

        // recyclerview
        binding.recyclerViewSurvey.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerViewSurvey.adapter =
            adapter

        // click next
        binding.btnNextToOB.setOnClickListener {

            // chưa chọn ngôn ngữ
            if (selectedLanguage == null) {


            } else {

                // chuyển màn hình
                startActivity(
                    Intent(
                        this, HomeFragment::class.java
                    )
                )

                finish()
            }
        }
    }
}