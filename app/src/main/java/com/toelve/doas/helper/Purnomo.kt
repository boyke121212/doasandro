package com.toelve.doas.helper

import android.content.Intent
import android.widget.Toast
import androidx.activity.addCallback
import com.toelve.doas.Loginpage
import com.toelve.doas.databinding.ActivityLoginpageBinding
import com.toelve.doas.soasa.CryptoAES
import com.toelve.doas.view.BeritaItem
import com.toelve.doas.view.Home

fun Loginpage.logbound(
    binding: ActivityLoginpageBinding
) {
    AuthManager(this, "api/auth-check").checkAuth(
        onSuccess = { json ->
            try {
                Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()

                val aesKey = json.getString("aes_key")


                SecurePrefs(this).saveAesKey(aesKey)
                val listBerita = ArrayList<BeritaItem>()

                if (json.has("berita")) {
                    val beritaArray = json.optJSONArray("berita")

                    if (beritaArray != null && beritaArray.length() > 0) {

                        for (i in 0 until beritaArray.length()) {
                            val obj = beritaArray.getJSONObject(i)

                            listBerita.add(
                                BeritaItem(
                                    id = obj.optString("id"),
                                    judul = CryptoAES.decrypt(
                                        obj.optString(
                                            "judul"
                                        ), aesKey
                                    ),
                                    isi = CryptoAES.decrypt(
                                        obj.optString("isi"),
                                        aesKey
                                    ),
                                    tanggal = obj.optString("tanggal"),
                                    foto = CryptoAES.decrypt(
                                        obj.optString("foto"),
                                        aesKey
                                    ),
                                    pdf = CryptoAES.decrypt(
                                        obj.optString("pdf"),
                                        aesKey
                                    )
                                )
                            )
                        }

                    } else {
                        // 👉 ARRAY ADA TAPI KOSONG
                    }

                } else {
                    // 👉 FIELD "berita" TIDAK ADA
                }

                val intent = Intent(this, Home::class.java)
                intent.putParcelableArrayListExtra("berita_list", listBerita)
                startActivity(intent)
                finish()


            } catch (e: Exception) {
                Toast.makeText(this, "Login Gagal", Toast.LENGTH_SHORT).show()
            }
        },
        onLoading = { loading ->
            if (loading) showLoading()
            else hideLoading()
        },
        onLogout = { message ->

            when (message) {
                "Verification failed", "__TIMEOUT__", "__NO_INTERNET__" -> {
                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    // 🔴 Tutup aplikasi (sesuai maumu)
                    finishAffinity()
                }

                else -> {
                    Toast.makeText(
                        this,
                        message,   // ⬅️ pesan asli dari server
                        Toast.LENGTH_LONG
                    ).show()

                    // Login gagal → tetap di halaman login
                }
            }
        }

    )
     var lastBackPressed = 0L

    onBackPressedDispatcher.addCallback(this) {
        val now = System.currentTimeMillis()

        if (now - lastBackPressed < 2000) {
            finishAffinity()
        } else {
            lastBackPressed = now
            Toast.makeText(
                this@logbound,
                "Tekan sekali lagi untuk Keluar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
fun Loginpage.setupDoubleBackExit() {

    var lastBackPressed = 0L

    onBackPressedDispatcher.addCallback(
        this@setupDoubleBackExit
    ) {
        val now = System.currentTimeMillis()

        if (now - lastBackPressed < 2000) {
            finishAffinity()
        } else {
            lastBackPressed = now
            Toast.makeText(
                this@setupDoubleBackExit,
                "Tekan sekali lagi untuk Keluar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}





