package com.galaxywall.app.ui.customview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import com.galaxywall.app.util.dp

/**
 * A [MotionLayout] that can also translate registered child layers by a normalized depth offset,
 * giving foreground UI (titles, badges) a subtle parallax that matches the hero image.
 */
class DepthMotionLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MotionLayout(context, attrs, defStyle) {

    private val depthLayers = LinkedHashMap<View, Float>()
    private val maxTranslate = 16f.dp

    fun registerDepthLayer(view: View, depth: Float) {
        depthLayers[view] = depth
    }

    fun clearDepthLayers() {
        depthLayers.clear()
    }

    fun setDepthOffset(x: Float, y: Float) {
        depthLayers.forEach { (view, depth) ->
            view.translationX = x * maxTranslate * depth
            view.translationY = y * maxTranslate * depth
        }
    }
}
