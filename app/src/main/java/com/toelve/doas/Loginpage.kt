package com.toelve.doas

import android.os.Bundle
import android.widget.Toast
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityLoginpageBinding
import com.toelve.doas.helper.SecurePrefs
import com.toelve.doas.helper.VolleyHelper
import com.toelve.doas.helper.logbound
import com.toelve.doas.helper.setupDoubleBackExit
/*ini adalah remark coba cek deh*/

class Loginpage : Boyke() {
    private lateinit var binding: ActivityLoginpageBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginpageBinding.inflate(layoutInflater)
        setupDoubleBackExit()
        setContentView(binding.root)
        binding.btMasuk.setOnClickListener {
            val username=binding.etUsername.text.toString()
            val password=binding.etPassword.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan Password harus diisi", Toast.LENGTH_SHORT).show()
            } else {
                val url = BuildConfig.BASE_URL + "cekdata"

                VolleyHelper.getInstance(this).login(
                    url = url,
                    username = username,
                    password = password,
                    onSuccess = { json ->
                        if (json.optString("status") == "success") {

                            val accessToken = json.getString("access_token")
                            val refreshToken = json.getString("refresh_token")

                            // 🔐 SIMPAN TOKEN (PAKAI SecurePrefs)
                            val securePrefs = SecurePrefs(this)
                            securePrefs.saveAccessToken(accessToken)
                            securePrefs.saveRefreshToken(refreshToken)
                            logbound(binding)
                        } else {

                            Toast.makeText(
                                this,
                                json.optString("message", "Login gagal"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onError = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

    }

}
