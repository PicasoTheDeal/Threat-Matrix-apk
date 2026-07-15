package com.picasothedeal.threatmatrix.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object TokenManager {
    private const val PREFS_NAME = "threat_matrix_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USERNAME = "username"

    fun saveToken(context: Context, token: String, username: String) {
        // The KTX edit block automatically calls apply() for you at the end
        getPrefs(context).edit {
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_USERNAME, username)
        }
    }

    fun getToken(context: Context): String? =
        getPrefs(context).getString(KEY_AUTH_TOKEN, null)

    fun getUsername(context: Context): String? =
        getPrefs(context).getString(KEY_USERNAME, null)

    fun clear(context: Context) {
        // The KTX edit block automatically calls apply() for you at the end
        getPrefs(context).edit {
            clear()
        }
    }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}