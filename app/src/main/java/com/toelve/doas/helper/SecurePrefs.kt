package com.toelve.doas.helper

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "badaipastiberlalu")

class SecurePrefs(private val context: Context) {

    companion object {

        private val ACCESS_TOKEN = stringPreferencesKey("AccessToken")
        private val REFRESH_TOKEN = stringPreferencesKey("RefreshToken")
        private val AES_KEY = stringPreferencesKey("superkey")
        private val IS_LOGIN = stringPreferencesKey("isLogin")

        fun get(context: Context): SecurePrefs {
            return SecurePrefs(context)
        }
    }

    // ===============================
    // SAVE (ASYNC INTERNAL)
    // ===============================
    fun saveAccessToken(token: String) {
        runBlocking {
            context.dataStore.edit {
                it[ACCESS_TOKEN] = token
            }
        }
    }

    fun saveRefreshToken(token: String) {
        runBlocking {
            context.dataStore.edit {
                it[REFRESH_TOKEN] = token
            }
        }
    }

    fun saveAesKey(key: String) {
        runBlocking {
            context.dataStore.edit {
                it[AES_KEY] = key
            }
        }
    }

    fun saveLogin(value: String) {
        runBlocking {
            context.dataStore.edit {
                it[IS_LOGIN] = value
            }
        }
    }

    // ===============================
    // GET (SYNC STYLE)
    // ===============================

    fun getAccessToken(): String? {
        return runBlocking {
            context.dataStore.data.first()[ACCESS_TOKEN]
        }
    }

    fun getRefreshToken(): String? {
        return runBlocking {
            context.dataStore.data.first()[REFRESH_TOKEN]
        }
    }

    fun getAesKey(): String? {
        return runBlocking {
            context.dataStore.data.first()[AES_KEY]
        }
    }

    // ===============================
    // CLEAR
    // ===============================

    fun clear() {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { it.clear() }
        }
    }
}