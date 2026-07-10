package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences

/**
 * Base URL is configurable at runtime (Settings screen) rather than a
 * hardcoded constant: the emulator default (10.0.2.2, which forwards to the
 * host machine) only works in the emulator — a real device on the same
 * Wi-Fi needs the host's LAN IP instead, and there's no way to know that
 * ahead of time.
 */
object AppSettings {
    private const val PREFS = "app_settings"
    private const val KEY_BASE_URL = "baseUrl"
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(v) {
            val normalized = v.trim().ifBlank { DEFAULT_BASE_URL }
            prefs.edit().putString(KEY_BASE_URL, if (normalized.endsWith("/")) normalized else "$normalized/").apply()
        }
}
