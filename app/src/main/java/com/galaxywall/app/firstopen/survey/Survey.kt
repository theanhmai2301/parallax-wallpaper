package com.galaxywall.app.firstopen.survey

import java.io.Serializable

/** A survey topic. Mirrors the source app's Survey model (Serializable so it can be passed between
 *  the survey steps via Intent extras). [imageResId] is the topic icon/illustration. */
data class Survey(
    val name: String,
    val imageResId: Int,
    var isSelected: Boolean = false
) : Serializable
