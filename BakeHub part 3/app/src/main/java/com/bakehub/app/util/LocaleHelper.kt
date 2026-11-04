package com.bakehub.app.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language switching.
 *
 * - Android 13+ (API 33): uses the platform per-app language API, so the choice also shows
 *   up in system Settings > App languages (see res/xml/locales_config.xml).
 * - Android 8-12: the chosen tag is stored in SharedPreferences and applied by wrapping the
 *   Activity's base context ([wrap]); the Activity is then recreated.
 *
 * An empty tag means "follow the system language".
 */
object LocaleHelper {
    val SUPPORTED get() = LanguageTags.SUPPORTED

    fun normalizeTag(tag: String?): String = LanguageTags.normalize(tag)

    /** Wraps [base] with the stored locale on Android < 13. Call from attachBaseContext. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = normalizeTag(
            base.getSharedPreferences(AppPreferences.FILE, Context.MODE_PRIVATE)
                .getString(AppPreferences.KEY_LANGUAGE, "")
        )
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** The currently selected tag ("" = system default). */
    fun currentTag(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            normalizeTag(
                context.getSystemService(LocaleManager::class.java)
                    .applicationLocales.takeIf { !it.isEmpty }?.get(0)?.language
            )
        } else {
            normalizeTag(
                context.getSharedPreferences(AppPreferences.FILE, Context.MODE_PRIVATE)
                    .getString(AppPreferences.KEY_LANGUAGE, "")
            )
        }

    fun setLanguage(activity: Activity, tag: String) {
        val normalized = normalizeTag(tag)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (normalized.isEmpty()) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(normalized)   // the system recreates the Activity
        } else {
            activity.getSharedPreferences(AppPreferences.FILE, Context.MODE_PRIVATE)
                .edit().putString(AppPreferences.KEY_LANGUAGE, normalized).apply()
            activity.recreate()
        }
    }
}
