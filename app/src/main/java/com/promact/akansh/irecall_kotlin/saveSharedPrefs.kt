package com.promact.akansh.irecall_kotlin

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.content.SharedPreferences.Editor

class saveSharedPrefs {
    private val PREF_ID_TOKEN: String = "idToken"
    private val PREF_USERNAME: String = "username"
    private val PREF_EMAIL: String = "email"
    private val PREF_PHOTO_URI = "photoUri"
    private val PREF_USERID = "userId"

    fun getSharedPreferences(context: Context): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

    fun setPrefs(context: Context, idToken: String, username: String, email: String, photoUri: String, userId: String) {
        val editor: Editor = getSharedPreferences(context).edit()
        editor.putString(PREF_ID_TOKEN, idToken)
        editor.putString(PREF_USERNAME, username)
        editor.putString(PREF_EMAIL, email)
        editor.putString(PREF_PHOTO_URI, photoUri)
        editor.putString(PREF_USERID, userId)

        editor.apply()
    }

    fun getToken(context: Context): String = getSharedPreferences(context).getString(PREF_ID_TOKEN, "")

    fun getUsername(context: Context): String = getSharedPreferences(context).getString(PREF_USERNAME, "")

    fun getEmail(context: Context): String = getSharedPreferences(context).getString(PREF_EMAIL, "")

    fun getPhotoUri(context: Context): String = getSharedPreferences(context).getString(PREF_PHOTO_URI, "")

    fun getUserId(context: Context): String = getSharedPreferences(context).getString(PREF_USERID, "")
}