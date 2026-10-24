package com.example.data

import android.content.Context
import android.content.SharedPreferences

class CalendyPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("calendy_prefs", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var activeFamilyId: String?
        get() = prefs.getString(KEY_FAMILY_ID, null)
        set(value) = prefs.edit().putString(KEY_FAMILY_ID, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_MODE, false) // Default to false for Mode Clair
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_MODE, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasValidSession(): Boolean {
        return !accessToken.isNullOrBlank() && !userId.isNullOrBlank()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_FAMILY_ID = "active_family_id"
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
    }
}
