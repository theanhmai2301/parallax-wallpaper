package com.galaxywall.app.util

import android.content.res.Resources
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.galaxywall.app.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Float.dp: Float get() = this * Resources.getSystem().displayMetrics.density

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.setVisible(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

fun View.fadeIn() {
    visibility = View.VISIBLE
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
}

/** Collects a Flow only while [this] owner is at least STARTED, cancelling when STOPPED. */
fun <T> LifecycleOwner.collectWhenStarted(flow: Flow<T>, action: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}
