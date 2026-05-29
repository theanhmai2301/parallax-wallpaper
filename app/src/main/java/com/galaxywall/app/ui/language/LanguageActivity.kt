package com.galaxywall.app.ui.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ActivityLanguage1Binding
import com.galaxywall.app.databinding.ActivityLanguage2Binding
import com.galaxywall.app.databinding.ActivityLanguageBinding
import com.galaxywall.app.ui.survey.SurveyActivity

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter
    private var selectedLanguage: LanguageModel? = null
    private lateinit var layoutEmpty: ActivityLanguage1Binding
    private lateinit var layoutSelected: ActivityLanguage2Binding
    private var selectedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityLanguageBinding.inflate(layoutInflater)

        setContentView(binding.root)
        initView()
        setupRecyclerView()
        setupClick()
    }

    private fun initView() {

        layoutEmpty = binding.layoutEmpty

        layoutSelected = binding.layoutSelected

        showEmptyState()
    }

    private fun setupRecyclerView() {

        val languageList = mutableListOf(

            LanguageModel(
                R.drawable.flag_en,
                "English"
            ),

            LanguageModel(
                R.drawable.flag_in_hindi,
                "Hindi"
            ),

            LanguageModel(
                R.drawable.flag_id_indonesia,
                "Indonesian"
            ),

            LanguageModel(
                R.drawable.flag_pt_portugal,
                "Portuguese"
            ),

            LanguageModel(
                R.drawable.flag_fr_france,
                "French"
            ),

            LanguageModel(
                R.drawable.flag_es_spain,
                "Spanish"
            ),

            LanguageModel(
                R.drawable.flag_ar_arabic,
                "Arabic"
            ),

            LanguageModel(
                R.drawable.flag_bn_bengali,
                "Bengali"
            ),

            LanguageModel(
                R.drawable.flag_cn_china,
                "Chinese"
            ),

            LanguageModel(
                R.drawable.flag_de_deutsh,
                "German"
            ),

            LanguageModel(
                R.drawable.flag_it_italian,
                "Italian"
            ),

            LanguageModel(
                R.drawable.flag_jp_japan,
                "Japanese"
            ),

            LanguageModel(
                R.drawable.flag_ko_korea,
                "Korean"
            ),

            LanguageModel(
                R.drawable.flag_mr_marathi,
                "Marathi"
            ),

            LanguageModel(
                R.drawable.flag_ru_russia,
                "Russian"
            ),

            LanguageModel(
                R.drawable.flag_ta_tamil,
                "Tamil"
            ),

            LanguageModel(
                R.drawable.flag_th_thailan,
                "Thai"
            ),

            LanguageModel(
                R.drawable.flag_tr_turkish,
                "Turkish"
            ),

            LanguageModel(
                R.drawable.flag_ur_urdu,
                "Urdu"
            ),

            LanguageModel(
                R.drawable.flag_vn_vietnam,
                "Vietnamese"
            )
        )

        adapter = LanguageAdapter(languageList) { language, position ->

            selectedLanguage = language
            selectedPosition = position

            layoutEmpty.tvWarning.visibility = View.GONE

            showSelectedState()
        }

        // EMPTY
        layoutEmpty.recyclerViewLanguage.apply {

            layoutManager = LinearLayoutManager(
                this@LanguageActivity
            )

            adapter = this@LanguageActivity.adapter

            setHasFixedSize(true)
        }

        // SELECTED
        layoutSelected.recyclerViewLanguage.apply {

            layoutManager = LinearLayoutManager(
                this@LanguageActivity
            )

            adapter = this@LanguageActivity.adapter

            setHasFixedSize(true)
        }
    }

    private fun setupClick() {

        binding.btnBack.setOnClickListener { finish() }

        // NEXT EMPTY
        layoutEmpty.btnNext.setOnClickListener {

            if (selectedLanguage == null) {

                layoutEmpty.tvWarning.visibility = View.VISIBLE

                layoutEmpty.tvWarning.postDelayed({

                    layoutEmpty.tvWarning.visibility = View.GONE

                }, 3000)
            }
        }

        // NEXT SELECTED
        layoutSelected.btnNext.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SurveyActivity::class.java
                )
            )

            finish()
        }
    }

    private fun showEmptyState() {

        layoutEmpty.root.visibility = View.VISIBLE

        layoutSelected.root.visibility = View.GONE
    }

    private fun showSelectedState() {

        layoutEmpty.root.visibility = View.GONE

        layoutSelected.root.visibility = View.VISIBLE

        layoutSelected.recyclerViewLanguage.scrollToPosition(selectedPosition)
    }
}

