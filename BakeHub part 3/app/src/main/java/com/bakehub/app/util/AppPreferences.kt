package com.bakehub.app.util

import android.content.Context
import android.content.SharedPreferences

/** Small non-sensitive, per-device settings (notification opt-in, language). */
object AppPreferences {
    const val FILE = "bakehub_prefs"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    const val KEY_LANGUAGE = "language_tag"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    var notificationsEnabled: Boolean
        get() = prefs?.getBoolean(KEY_NOTIFICATIONS, true) ?: true
        set(value) { prefs?.edit()?.putBoolean(KEY_NOTIFICATIONS, value)?.apply() }
}
