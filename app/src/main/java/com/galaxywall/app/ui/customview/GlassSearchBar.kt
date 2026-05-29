package com.galaxywall.app.ui.customview

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import com.galaxywall.app.R

/** A frosted-glass search field with a leading icon and a clear button. */
class GlassSearchBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val input: EditText
    private val clearIcon: ImageView

    var onQueryChanged: ((String) -> Unit)? = null

    val query: String get() = input.text?.toString().orEmpty()

    init {
        LayoutInflater.from(context).inflate(R.layout.view_glass_search, this, true)
        background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_search)
        clipToOutline = true
        input = findViewById(R.id.searchInput)
        clearIcon = findViewById(R.id.clearIcon)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                clearIcon.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
                onQueryChanged?.invoke(text)
            }
        })

        clearIcon.setOnClickListener { input.setText("") }
    }

    fun setText(text: String) {
        input.setText(text)
        input.setSelection(input.text?.length ?: 0)
    }
}
