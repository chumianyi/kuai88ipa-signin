package com.kuai88ipa.signin

import android.content.Context
import android.content.SharedPreferences

/**
 * 会话管理：保存邮箱、记住密码选项
 */
object SessionManager {
    private const val PREFS_NAME = "session_prefs"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PASSWORD = "user_password"
    private const val KEY_REMEMBER = "remember_password"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCredentials(context: Context, email: String, password: String, remember: Boolean) {
        val editor = prefs(context).edit()
        editor.putBoolean(KEY_REMEMBER, remember)
        if (remember) {
            editor.putString(KEY_EMAIL, email)
            editor.putString(KEY_PASSWORD, password)
        } else {
            editor.remove(KEY_EMAIL)
            editor.remove(KEY_PASSWORD)
        }
        editor.apply()
    }

    fun getSavedEmail(context: Context): String {
        return prefs(context).getString(KEY_EMAIL, "") ?: ""
    }

    fun getSavedPassword(context: Context): String {
        return prefs(context).getString(KEY_PASSWORD, "") ?: ""
    }

    fun isRememberEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_REMEMBER, false)
    }

    fun clearCredentials(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
