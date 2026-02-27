package com.toelve.doas.helper

import android.content.Context
import android.content.Intent
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.toelve.doas.BuildConfig
import com.toelve.doas.Loginpage
import java.io.File

class AuthManager(
    private val context: Context,
    private val purnomo: String
) {

    private val prefs = SecurePrefs(context)
    private val volley = VolleyHelper.getInstance(context)

    /* =========================================================
     * ANTI RACE CONDITION (PENTING)
     * ========================================================= */

    private var isRefreshing = false
    private val waitingQueue = mutableListOf<() -> Unit>()

    private fun refreshTokenSafe(
        onSuccess: () -> Unit,
        onLogout: (String) -> Unit
    ) {

        // kalau sedang refresh → antre saja
        if (isRefreshing) {
            waitingQueue.add(onSuccess)
            return
        }

        isRefreshing = true

        refreshTokenOnly(

            onSuccess = {

                isRefreshing = false

                // jalankan request utama
                onSuccess()

                // jalankan semua yg antre
                waitingQueue.forEach { it() }
                waitingQueue.clear()
            },

            onLogout = { msg ->

                isRefreshing = false
                waitingQueue.clear()

                onLogout(msg)
            }
        )
    }

    /* =========================================================
     * PUBLIC API
     * ========================================================= */

    fun checkAuth(
        params: Map<String, String>? = null,
        onSuccess: (JSONObject) -> Unit,
        onLogout: (String) -> Unit,
        onLoading: (Boolean) -> Unit
    ) {
        if (!ensureInternet(onLoading, onLogout)) return

        val accessToken = prefs.getAccessToken()
        val refreshToken = prefs.getRefreshToken()

        if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
            onLogout("Sesi tidak valid")
            return
        }

        volley.authCheck(
            url = BuildConfig.BASE_URL + purnomo,
            accessToken = accessToken,
            params = params,
            onLoading = onLoading,

            onSuccess = { response ->
                onSuccess(response)
            },

            onUnauthorized = {

                onLoading(false)

                // 🔥 pakai SAFE VERSION
                refreshTokenSafe(

                    onSuccess = {
                        checkAuth(params, onSuccess, onLogout, onLoading)
                    },

                    onLogout = { message ->
                        onLogout(message)
                    }
                )
            },

            onError = { errorMessage ->
                onLoading(false)

                Toast.makeText(
                    context,
                    errorMessage ?: "Gangguan jaringan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    fun uploadFoto(
        files: List<File>?,
        params: Map<String, String>,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit,
        onLogout: (String) -> Unit,
        onLoading: (Boolean) -> Unit
    ) {
        if (!ensureInternet(onLoading, onLogout)) return

        val accessToken = prefs.getAccessToken()

        if (accessToken.isNullOrEmpty()) {
            onLogout("Sesi tidak valid")
            return
        }

        val realFiles = if (files != null && files.isEmpty()) null else files

        volley.uploadMultipartAuth(
            url = BuildConfig.BASE_URL + purnomo,
            accessToken = accessToken,
            params = params,
            files = realFiles,
            onLoading = onLoading,

            onSuccess = { response ->
                onSuccess(response)
            },

            onUnauthorized = {

                // 🔥 pakai SAFE VERSION
                refreshTokenSafe(

                    onSuccess = {
                        uploadFoto(
                            files,
                            params,
                            onSuccess,
                            onError,
                            onLogout,
                            onLoading
                        )
                    },

                    onLogout = { message ->
                        onLogout(message)
                    }
                )
            },

            onError = { message ->
                onError(message ?: "Terjadi kesalahan jaringan")
            }
        )
    }

    fun forceLogout() {
        prefs.clear()
        context.startActivity(
            Intent(context, Loginpage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    /* =========================================================
     * REFRESH TOKEN ORIGINAL
     * ========================================================= */

    private fun refreshTokenOnly(
        onSuccess: () -> Unit,
        onLogout: (String) -> Unit
    ) {

        if (!Auto.isInternetAvailable(context)) {
            Toast.makeText(context, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
            return
        }

        val refreshToken = prefs.getRefreshToken()

        if (refreshToken.isNullOrEmpty()) {
            onLogout("Refresh token tidak ditemukan")
            return
        }

        volley.refreshToken(
            url = BuildConfig.BASE_URL + BuildConfig.refresh,
            refreshToken = refreshToken,

            onSuccess = { json ->

                val newAccess = json.optString("access_token")
                val newRefresh = json.optString("refresh_token")

                if (newAccess.isBlank() || newRefresh.isBlank()) {
                    onLogout("Refresh token tidak valid")
                    return@refreshToken
                }

                prefs.saveAccessToken(newAccess)
                prefs.saveRefreshToken(newRefresh)

                onSuccess()
            },

            onError = { message ->

                if (message.startsWith("401")) {

                    val cleanMessage = message.substringAfter(":")

                    prefs.clear()
                    onLogout(cleanMessage)

                } else {

                    Toast.makeText(
                        context,
                        "Gangguan jaringan, silakan coba lagi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    /* =========================================================
     * UTIL
     * ========================================================= */

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context.applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun ensureInternet(
        onLoading: ((Boolean) -> Unit)? = null,
        onLogout: ((String) -> Unit)? = null
    ): Boolean {
        if (!Auto.isInternetAvailable(context)) {
            onLoading?.invoke(false)
            onLogout?.invoke("Tidak ada koneksi internet")
            return false
        }
        return true
    }
}