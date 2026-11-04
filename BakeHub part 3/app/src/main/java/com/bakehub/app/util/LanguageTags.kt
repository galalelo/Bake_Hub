package com.bakehub.app.util

/** Pure language-tag logic (no Android dependencies, so it is unit-tested on the JVM). */
object LanguageTags {
    /** Languages BakeHub ships translations for. Keep in sync with res/values-* and res/xml/locales_config.xml. */
    val SUPPORTED = listOf("en", "es", "fr", "ar")

    /** Returns the bare language of [tag] if BakeHub supports it, otherwise "" (= system default). */
    fun normalize(tag: String?): String {
        val language = tag?.trim()?.substringBefore('-')?.substringBefore('_')?.lowercase().orEmpty()
        return if (language in SUPPORTED) language else ""
    }
}
