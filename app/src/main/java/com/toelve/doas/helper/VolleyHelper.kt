package com.toelve.doas.helper

import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.toelve.doas.VolleyMultipartRequest
import com.toelve.doas.soasa.DeviceSecurityHelper
import org.json.JSONObject
import java.io.File
import java.util.UUID


class VolleyHelper private constructor(context: Context) {

    var currentFilter = HashMap<String,String>()

    private val appContext = context.applicationContext
    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(appContext)
    }

    companion object {
        @Volatile
        private var INSTANCE: VolleyHelper? = null

        fun getInstance(context: Context): VolleyHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleyHelper(context).also { INSTANCE = it }
            }
    }
    fun uploadMultipartAuth(
        url: String,
        accessToken: String,
        params: Map<String, String>,
        files: List<File>?,   // ⬅️ GANTI
        onSuccess: (JSONObject) -> Unit,
        onUnauthorized: (String?) -> Unit,
        onError: (String) -> Unit,
        onLoading: (Boolean) -> Unit
    ) {

        val deviceHash = DeviceSecurityHelper.getDeviceHash(appContext)

        onLoading(true)

        val request = object : VolleyMultipartRequest(
            Method.POST,
            url,
            Response.ErrorListener { error ->
                onLoading(false)

                val statusCode = error.networkResponse?.statusCode
                val message = parseError(error)

                when (statusCode) {
                    401 -> onUnauthorized(message)
                    else -> onError(message ?: "Terjadi kesalahan jaringan")
                }
            }
        ) {

            override fun getHeaders(): MutableMap<String, String> =
                hashMapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Accept" to "application/json",
                    "X-Device-Hash" to deviceHash,
                    "X-App-Signature" to DeviceSecurityHelper.getAppSignatureHash(appContext),
                    "Platform" to "android"
                )

            override fun getParams(): MutableMap<String, String> =
                params.toMutableMap()

            override fun getByteData(): Map<String, DataPart> {

                val map = HashMap<String, DataPart>()

                files?.forEachIndexed { index, file ->

                    if (file.exists()) {

                        val mime = when {
                            file.name.endsWith(".png", true) -> "image/png"
                            file.name.endsWith(".webp", true) -> "image/webp"
                            else -> "image/jpeg"
                        }

                        map["foto${index + 1}"] = DataPart(
                            file.name,
                            file.readBytes(),
                            mime
                        )
                    }
                }

                return map
            }

            override fun deliverResponse(response: NetworkResponse) {
                onLoading(false)

                try {
                    val body = String(response.data)
                    val json = JSONObject(body)

                    // hapus file setelah sukses upload
                    files?.forEach { file ->
                        try {
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (_: Exception) {}
                    }

                    onSuccess(json)

                } catch (e: Exception) {
                    onError("Response server tidak valid")
                }
            }

        }

        request.retryPolicy = DefaultRetryPolicy(
            20000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }

    fun authCheck(
        url: String,
        accessToken: String,
        params: Map<String, String>? = null,
        onLoading: (Boolean) -> Unit,
        onSuccess: (JSONObject) -> Unit,
        onUnauthorized: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        onLoading(true)

        val request = object : StringRequest(
            Method.POST,
            url,

            // ================= SUCCESS =================
            StringRequest@{ response ->
                onLoading(false)

                if (response.isBlank()) {
                    onError("Response kosong dari server")
                    return@StringRequest   // ⬅️ WAJIB
                }

                try {
                    val json = JSONObject(response)
                    val status = json.optString("status")

                    if (status == "ok") {
                        onSuccess(json)
                    } else {
                        onError(
                            json.optString("message", "Terjadi kesalahan")
                        )
                    }
                } catch (e: Exception) {
                    onError("Response server tidak valid")
                }
            },

            // ================= ERROR =================
            { error ->
                onLoading(false)

                val statusCode = error.networkResponse?.statusCode
                val message = parseError(error)



                when (statusCode) {
                    401 -> onUnauthorized(message)
                    else -> onError(message ?: "Terjadi kesalahan jaringan")
                }
            }
        ) {

            override fun getHeaders(): MutableMap<String, String> {

                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val nonce = UUID.randomUUID().toString()

                return hashMapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Accept" to "application/json",
                    "X-Device-Hash" to DeviceSecurityHelper.getDeviceHash(appContext),
                    "X-App-Signature" to DeviceSecurityHelper.getAppSignatureHash(appContext),
                    "Platform" to "android",

                    // 🔐 ANTI REPLAY
                    "X-Request-Timestamp" to timestamp,
                    "X-Request-Nonce" to nonce
                )
            }

            override fun getParams(): MutableMap<String, String> =
                params?.toMutableMap() ?: HashMap()
        }

        request.retryPolicy = DefaultRetryPolicy(
            15000,
            0, // ⬅️ PENTING: JANGAN RETRY 401
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }



    fun refreshToken(
        url: String,
        refreshToken: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    onSuccess(JSONObject(response))
                } catch (e: Exception) {
                    onError("Response server tidak valid")
                }
            },
            { error ->

                val statusCode = error.networkResponse?.statusCode
                val errorMessage = parseError(error)

                if (statusCode != null) {
                    // kirim status code supaya bisa dibedakan di AuthManager
                    onError("$statusCode:$errorMessage")
                } else {
                    // biasanya timeout / no connection
                    onError(errorMessage ?: "Terjadi kesalahan jaringan")
                }
            }
        ) {

            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Bearer $refreshToken",
                    "Accept" to "application/json",
                    "X-Device-Hash" to DeviceSecurityHelper.getDeviceHash(appContext),
                    "X-App-Signature" to DeviceSecurityHelper.getAppSignatureHash(appContext),
                    "Platform" to "android"
                )
            }
        }

        // Retry policy refresh token sebaiknya tidak retry banyak
        request.retryPolicy = DefaultRetryPolicy(
            15000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }


    fun post(
        url: String,
        token: String,
        params: Map<String, String>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                onSuccess(response)
            },
            { error ->
                onError(parseError(error))
            }
        ) {

            override fun getParams(): MutableMap<String, String> {
                return params.toMutableMap()
            }

            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Bearer $token",
                    "Accept" to "application/json",
                    "X-Device-Hash" to DeviceSecurityHelper.getDeviceHash(appContext),
                    "X-App-Signature" to DeviceSecurityHelper.getAppSignatureHash(appContext),
                    "Platform" to "android"
                )
            }
        }

        request.retryPolicy = DefaultRetryPolicy(
            15000, // timeout 15 detik
            0,     // tidak retry
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }

    fun parseError(error: VolleyError): String {

        // 1️⃣ IDEAL PATH: pesan dari server
        error.networkResponse?.data?.let { data ->
            try {
                val json = JSONObject(String(data))
                return json.optJSONObject("messages")?.optString("error")
                    ?: json.optString("message")
                    ?: "Terjadi kesalahan"
            } catch (_: Exception) {
                // lanjut fallback
            }
        }

        // 2️⃣ REALITY PATH: Volley buang body
        return when (error) {
            is AuthFailureError -> "Verification failed"
            is TimeoutError -> "__TIMEOUT__"
            is NoConnectionError -> "__NO_INTERNET__"
            else -> "Sesi kamu sudah berakhir, silakan login ulang"
        }
    }


    fun login(
        url: String,
        username: String,
        password: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val isRooted = DeviceSecurityHelper.isDeviceRooted()
        val isEmulator = DeviceSecurityHelper.isEmulator()
        val isFakeGps = DeviceSecurityHelper.isUsingMockLocation(appContext)
        val isDebug = DeviceSecurityHelper.isDebuggable(appContext)
        val isInstallerValid = DeviceSecurityHelper.isValidInstaller(appContext)
        val request = object : StringRequest(
            Method.POST,
            url,
            { response ->
                try {
                    val json = JSONObject(response)
                    onSuccess(json)
                } catch (e: Exception) {
                    onError("Response server tidak valid")
                }
            },
            { error ->
                onError(parseError(error))
            }
        ) {

            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    // ===== LOGIN =====
                    "username" to username,
                    "password" to password,

                    // ===== DEVICE BINDING =====
                    "device_hash" to DeviceSecurityHelper.getDeviceHash(appContext),
                    "app_signature" to DeviceSecurityHelper.getAppSignatureHash(appContext),
                    // ===== SECURITY FLAGS =====
                    "is_rooted" to if (isRooted) "1" else "0",
                    "is_emulator" to if (isEmulator) "1" else "0",
                    "is_fake_gps" to if (isFakeGps) "1" else "0",
                    "is_debug" to if (isDebug) "1" else "0",
                    "is_installer_valid" to if (isInstallerValid) "1" else "0",
                    // ===== INFO TAMBAHAN =====
                    "platform" to "android"
                )
            }
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Accept" to "application/json"
                )
            }
        }

        request.retryPolicy = DefaultRetryPolicy(
            15000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }






}
