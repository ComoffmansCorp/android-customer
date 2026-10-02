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
    // :8000, not :8080 -- the backend (`app`) no longer publishes a host
    // port directly, the nginx gateway in front of it is now the only
    // entry point (see docker-compose.yml: `app` has `expose: 8080`, no
    // `ports:`; `gateway` publishes `8000:80`).
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var darkTheme: Boolean
        get() = prefs.getBoolean("darkTheme", true)
        set(value) { prefs.edit().putBoolean("darkTheme", value).apply() }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(v) {
            val normalized = v.trim().ifBlank { DEFAULT_BASE_URL }
            prefs.edit().putString(KEY_BASE_URL, if (normalized.endsWith("/")) normalized else "$normalized/").apply()
        }

    /**
     * Resolves a relative media path (e.g. "/media/masters/master1.jpg", as
     * returned by MasterProfileResponse.avatarUrl) against the configured
     * server -- images are served from the same gateway as the API, not a
     * separate CDN, so this is just baseUrl + path with the duplicate slash
     * collapsed.
     */
    fun mediaUrl(path: String): String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
