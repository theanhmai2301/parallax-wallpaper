package com.galaxywall.app.firstopen.language

import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ActivityFirstOpenLanguageNewBinding
import com.galaxywall.app.firstopen.LocaleHelper
import com.galaxywall.app.firstopen.languageCode
import com.galaxywall.app.firstopen.survey.SurveyActivity
import com.galaxywall.app.ui.MainActivity

/**
 * Shared logic for the two language screens, mirroring the source app's BaseLanguageActivity:
 * builds the language list, moves the system / saved language to the top, drives the adapter, and
 * applies the chosen language. FO1 and FO2 both use [ActivityFirstOpenLanguageNewBinding].
 */
abstract class BaseLanguageActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityFirstOpenLanguageNewBinding
    protected lateinit var languageAdapter: LanguageAdapter

    var listLanguages: ArrayList<Language> = arrayListOf()
    var languageCodeSelected = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstOpenLanguageNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createView()
    }

    abstract fun createView()
    abstract fun setAdapter()

    open fun onClickLanguageItem(language: Language, position: Int) {}

    private fun initAdapter() {
        languageAdapter = LanguageAdapter(this, object : LanguageAdapter.OnLanguageClickListener {
            override fun onClickItemListener(language: Language, position: Int) {
                languageCodeSelected = language.locale
                onClickLanguageItem(language, position)
            }
        })
        languageAdapter.setItems(listLanguages)
        setAdapter()
    }

    protected open fun getDataLanguage(isOpenFromMain: Boolean) {
        initData(isOpenFromMain)
        val locale = Resources.getSystem().configuration.locales.get(0)
        var languageSystem: Language? = null
        for (language in listLanguages) {
            if (isOpenFromMain) {
                if (applicationContext.languageCode != null &&
                    applicationContext.languageCode == language.locale
                ) {
                    languageSystem = language
                    languageCodeSelected = languageSystem.locale
                }
            } else if (language.locale == locale.language) {
                languageSystem = language
                languageCodeSelected = locale.language
            }
        }
        if (languageSystem != null) {
            listLanguages.remove(languageSystem)
            listLanguages.add(0, languageSystem)
        }
        initAdapter()
    }

    /** Persists + applies the chosen language, then routes (survey on first open, home from main). */
    protected open fun changeLanguage(isOpenFromMain: Boolean) {
        applicationContext.languageCode = languageCodeSelected
        LocaleHelper.changeLang(languageCodeSelected)
        if (!isOpenFromMain) {
            SurveyActivity.start(this, true)
        } else {
            MainActivity.startMain(this)
        }
    }

    private fun initData(isOpenFromMain: Boolean) {
        listLanguages = ArrayList()
        listLanguages.add(Language(R.drawable.flag_en, "English", "en"))
        listLanguages.add(Language(R.drawable.flag_es_spain, "Spanish", "es"))
        listLanguages.add(Language(R.drawable.flag_pt_portugal, "Portuguese", "pt"))
        listLanguages.add(Language(R.drawable.flag_id_indonesia, "Indonesian", "in"))
        listLanguages.add(Language(R.drawable.flag_de_deutsh, "German", "de"))
        listLanguages.add(Language(R.drawable.flag_fr_france, "French", "fr"))

        if (isOpenFromMain) {
            listLanguages.add(Language(R.drawable.flag_ko_korea, "Korean", "ko"))
            listLanguages.add(Language(R.drawable.flag_jp_japan, "Japanese", "ja"))
            listLanguages.add(Language(R.drawable.flag_cn_china, "Chinese", "zh"))
            listLanguages.add(Language(R.drawable.flag_in_hindi, "Hindi", "hi"))
            listLanguages.add(Language(R.drawable.flag_vn_vietnam, "Vietnamese", "vi"))
            listLanguages.add(Language(R.drawable.flag_it_italian, "Italian", "it"))
            listLanguages.add(Language(R.drawable.flag_ru_russia, "Russian", "ru"))
            listLanguages.add(Language(R.drawable.flag_tr_turkish, "Turkish", "tr"))
            listLanguages.add(Language(R.drawable.flag_mr_marathi, "Marathi", "mr"))
            listLanguages.add(Language(R.drawable.flag_ta_tamil, "Tamil", "ta"))
            listLanguages.add(Language(R.drawable.flag_bn_bengali, "Bengali", "bn"))
            listLanguages.add(Language(R.drawable.flag_th_thailan, "Thai", "th"))
            listLanguages.add(Language(R.drawable.flag_ar_arabic, "Arabic", "ar"))
            listLanguages.add(Language(R.drawable.flag_ur_urdu, "Urdu", "ur"))
        }
    }
}
