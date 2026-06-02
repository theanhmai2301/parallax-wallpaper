package com.galaxywall.app.firstopen.language

/** A selectable UI language. Mirrors the source app's base.language.Language model. */
data class Language(
    val flag: Int,
    val title: String,
    val locale: String,
    var isChoose: Boolean = false
)
