package com.galaxywall.app.firstopen.language

import android.content.res.Resources
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
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
        enableEdgeToEdge()
        binding = ActivityFirstOpenLanguageNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Push the title row below the status bar. Reading the status-bar height directly is
        // deterministic (the inset listener can fire too late / report 0 on some OEM skins, which
        // left the title clipped under the bar).
        //val statusId = resources.getIdentifier("status_bar_height", "dimen", "android")
        //val statusBar = if (statusId > 0) resources.getDimensionPixelSize(statusId) else 0
       // binding.llTitle.updatePadding(top = statusBar + (1 * resources.displayMetrics.density).toInt())
        createView()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        // Hide the navigation bar (immersive); a swipe from the edge reveals it transiently.
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
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
        // All languages that have a flag asset are selectable (each has a translation; untranslated
        // strings fall back to English). [isOpenFromMain] no longer trims the list.
        listLanguages = ArrayList()
        listLanguages.add(Language(R.drawable.flag_en, "English", "en"))
        listLanguages.add(Language(R.drawable.flag_es_spain, "Spanish", "es"))
        listLanguages.add(Language(R.drawable.flag_pt_portugal, "Portuguese", "pt"))
        listLanguages.add(Language(R.drawable.flag_id_indonesia, "Indonesian", "in"))
        listLanguages.add(Language(R.drawable.flag_de_deutsh, "German", "de"))
        listLanguages.add(Language(R.drawable.flag_fr_france, "French", "fr"))
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
