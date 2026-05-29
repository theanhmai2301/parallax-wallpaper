package com.galaxywall.app.ui.customview

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView

/**
 * A card that can blur its own content on Android 12+ via [RenderEffect]. Used to frost a
 * background image placed inside it; on older versions a pre-blurred bitmap is supplied instead.
 */
class BlurCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MaterialCardView(context, attrs, defStyle) {

    val supportsRuntimeBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun setBlurRadius(radius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(
                if (radius <= 0f) null
                else RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            )
        }
    }
}
