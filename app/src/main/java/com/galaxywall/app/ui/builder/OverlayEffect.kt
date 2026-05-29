package com.galaxywall.app.ui.builder

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

/** In-app overlay effects applied to the composed wallpaper via a [ColorMatrix]. */
enum class OverlayEffect(val label: String) {
    NONE("Original"),
    VIVID("Vivid"),
    WARM("Warm"),
    COOL("Cool"),
    MONO("Mono"),
    VINTAGE("Vintage"),
    NOIR("Noir");

    fun colorFilter(): ColorMatrixColorFilter? {
        val matrix = when (this) {
            NONE -> return null
            VIVID -> ColorMatrix().apply { setSaturation(1.6f) }
            WARM -> ColorMatrix(
                floatArrayOf(
                    1.15f, 0f, 0f, 0f, 12f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.85f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            COOL -> ColorMatrix(
                floatArrayOf(
                    0.88f, 0f, 0f, 0f, 0f,
                    0f, 1.0f, 0f, 0f, 4f,
                    0f, 0f, 1.18f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            MONO -> ColorMatrix().apply { setSaturation(0f) }
            VINTAGE -> ColorMatrix(
                floatArrayOf(
                    0.9f, 0.5f, 0.1f, 0f, 0f,
                    0.3f, 0.8f, 0.1f, 0f, 0f,
                    0.2f, 0.3f, 0.6f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            NOIR -> ColorMatrix().apply {
                setSaturation(0f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.35f, 0f, 0f, 0f, -25f,
                            0f, 1.35f, 0f, 0f, -25f,
                            0f, 0f, 1.35f, 0f, -25f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
        }
        return ColorMatrixColorFilter(matrix)
    }
}
