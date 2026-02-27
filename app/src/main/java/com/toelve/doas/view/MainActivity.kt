package com.toelve.doas.view

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.Loginpage
import com.toelve.doas.R
import com.toelve.doas.helper.SecurePrefs
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityMainBinding
import com.toelve.doas.helper.Auto.cleanTempPhotos
import com.toelve.doas.soasa.CryptoAES

class MainActivity : Boyke() {

    public lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔒 HIDE BUTTON LOGIN DULU
        binding.btLogin.visibility = View.GONE
        val akses=SecurePrefs(this).getAccessToken()
        val refresh=SecurePrefs(this).getRefreshToken()
        if(akses.isNullOrEmpty() || refresh.isNullOrEmpty()){
            binding.btLogin.visibility = View.VISIBLE

        }else{

           startActivity(Intent(this, Home::class.java))
            finishAffinity()
        }

        binding.btLogin.setOnClickListener {
            startActivity(Intent(this, Loginpage::class.java))
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        cleanTempPhotos(this)
        super.onResume()
    }
}
