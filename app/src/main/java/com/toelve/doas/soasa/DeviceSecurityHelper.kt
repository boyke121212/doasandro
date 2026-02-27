package com.toelve.doas.soasa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.content.pm.ApplicationInfo
import android.location.LocationManager
import android.widget.Toast
import java.security.MessageDigest
import java.util.UUID
import java.io.File
import kotlin.system.exitProcess
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit

object DeviceSecurityHelper {

    // =================================================
    // SHA-256
    // =================================================
    fun sha256(input: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // =================================================
    // SSAID (Android ID)
    // =================================================
    /**
     * Android ID (SSAID)
     *
     * Aman digunakan sejak Android 8+ karena:
     * - App-scoped
     * - Tidak bisa tracking lintas aplikasi
     * - Digunakan hanya untuk device binding internal
     */
    private fun getSSaid(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""
    }


    // =================================================
    // FALLBACK UUID (PERSISTENT)
    // =================================================
    private fun getFallbackId(context: Context): String {
        val prefs = context.getSharedPreferences("device_security", Context.MODE_PRIVATE)

        return prefs.getString("fallback_id", null) ?: run {
            val id = UUID.randomUUID().toString()
            prefs.edit {
                putString("fallback_id", id)
            }
            id
        }
    }

    // =================================================
    // FINAL DEVICE HASH
    // =================================================
    fun getDeviceHash(context: Context): String {
        val ssaid = getSSaid(context)
        return if (ssaid.isNotEmpty()) {
            sha256(ssaid)
        } else {
            sha256(getFallbackId(context))
        }
    }

    // =================================================
    // APP SIGNATURE HASH (ANTI CLONE / RESIGN)
    // =================================================
    fun getAppSignatureHash(context: Context): String {
        val pm = context.packageManager

        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        }

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            val signingInfo = packageInfo.signingInfo
                ?: return ""   // ⛔ jika null → anggap APK tidak valid

            when {
                signingInfo.hasMultipleSigners() ->
                    signingInfo.apkContentsSigners

                else ->
                    signingInfo.signingCertificateHistory
            }

        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        if (signatures.isNullOrEmpty()) return ""

        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(signatures[0].toByteArray())

        return hash.joinToString("") { "%02x".format(it) }
    }


    // =================================================
    // DEBUG APK CHECK
    // =================================================
    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    // =================================================
    // INSTALLER CHECK (PLAY STORE ONLY)
    // =================================================
    fun isValidInstaller(context: Context): Boolean {
        val pm = context.packageManager
        val pkg = context.packageName

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // ✅ API 30+ (RESMI, tidak deprecated)
                val sourceInfo = pm.getInstallSourceInfo(pkg)

                val installer = sourceInfo.installingPackageName
                installer == "com.android.vending" ||
                        installer == "com.google.android.apps.work.clouddpc"

            } else {
                // ⚠️ API < 30 (fallback legacy)
                @Suppress("DEPRECATION")
                val installer = pm.getInstallerPackageName(pkg)
                installer == "com.android.vending"
            }
        } catch (e: Exception) {
            false
        }
    }

    // =================================================
    // ROOT DETECTION
    // =================================================
    fun isDeviceRooted(): Boolean {
        return checkBuildTags() || checkSuBinary()
    }

    private fun checkBuildTags(): Boolean {
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }

    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/app/Superuser.apk",
            "/system/app/Magisk.apk",
            "/system/bin/magisk"
        )
        return paths.any { File(it).exists() }
    }

    // =================================================
    // FAKE GPS / MOCK LOCATION DETECTION
    // =================================================
    @Suppress("DEPRECATION")
    fun isUsingMockLocation(context: Context): Boolean {
        return try {

            val hasFine = context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCoarse = context.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                return false
            }

            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.allProviders ?: return false

            for (provider in providers) {
                val location = lm.getLastKnownLocation(provider) ?: continue

                // 1️⃣ Deprecated flag → masih valid sebagai sinyal
                if (location.isFromMockProvider) {
                    return true
                }

                // 2️⃣ Heuristic tambahan
                if (location.accuracy > 100) {
                    return true
                }

                if (location.speed > 50) {
                    return true
                }
            }

            false
        } catch (e: Exception) {
            false
        }
    }


    // =================================================
    // FORCE EXIT WITH TOAST
    // =================================================
    private fun forceExit(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        Handler(Looper.getMainLooper()).postDelayed({
            exitProcess(0)
        }, 2000)
    }

    // =================================================
    // EMULATOR DETECTION
    // =================================================
    fun isEmulator(): Boolean {
        return (
                Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk", true)
                        || Build.MODEL.contains("Emulator", true)
                        || Build.MODEL.contains("Android SDK built for x86", true)
                        || Build.MANUFACTURER.contains("Genymotion", true)
                        || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                        || Build.PRODUCT.contains("sdk", true)
                        || Build.PRODUCT.contains("emulator", true)
                        || Build.PRODUCT.contains("simulator", true)
                        || Build.HARDWARE.contains("goldfish", true)
                        || Build.HARDWARE.contains("ranchu", true)
                )
    }

    // =================================================
    // FINAL VALIDATION (CALL THIS!)
    // =================================================
    fun validateDeviceOrExit(context: Context) {
       /* if (isEmulator()) {
            forceExit(
                context,
                "Aplikasi tidak dapat dijalankan di emulator."
            )
            return
        }*/
        if (isDeviceRooted()) {
            forceExit(
                context,
                "Perangkat terdeteksi ROOT.\nAplikasi ditutup."
            )
            return
        }

        if (isUsingMockLocation(context)) {
            forceExit(
                context,
                "Fake GPS terdeteksi.\nMatikan lokasi palsu."
            )
            return
        }

     /*   if (isDebuggable(context)) {
            forceExit(
                context,
                "Aplikasi tidak valid.\\nSilakan instal versi resmi dari Play Store."
            )
            return
        }

        if (!isValidInstaller(context)) {
            forceExit(
                context,
                "Aplikasi harus diinstal dari Play Store resmi."
            )
            return
        }*/
    }
}
