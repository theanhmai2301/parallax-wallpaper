package com.galaxywall.app.ui.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.galaxywall.app.R

/** Premium app bar: brand mark + title + two glass action buttons. */
class PremiumToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val title: TextView
    private val folder: ImageView
    private val settings: ImageView

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_premium_toolbar, this, true)
        title = findViewById(R.id.toolbarTitle)
        folder = findViewById(R.id.actionFolder)
        settings = findViewById(R.id.actionSettings)
    }

    fun setTitle(text: CharSequence) { title.text = text }

    fun onFolderClick(action: () -> Unit) { folder.setOnClickListener { action() } }

    fun onSettingsClick(action: () -> Unit) { settings.setOnClickListener { action() } }
}
