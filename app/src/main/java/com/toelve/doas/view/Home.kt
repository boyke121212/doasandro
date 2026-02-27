package com.toelve.doas.view

import android.app.Dialog
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.helper.BeritaPagerAdapter
import com.toelve.doas.R
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityHomeBinding
import com.toelve.doas.helper.Auto.cleanTempPhotos
import com.toelve.doas.helper.RenderHtml.htmlPreviewClean
import com.toelve.doas.helper.cekabsen
import com.toelve.doas.helper.dos
import com.toelve.doas.helper.go
import com.toelve.doas.helper.setupDoubleBackExit
import java.io.File

class Home : Boyke(),

    BeritaPagerAdapter.OnBeritaChangeListener {
    lateinit var binding: ActivityHomeBinding
    private lateinit var tvJudul: TextView
    private lateinit var tvIsi: TextView
    lateinit var d: Dialog
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private var isRequestRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupDoubleBackExit()
        binding.btHadir.setOnClickListener { go(binding) }
        monitorInternet()
        val mapAbsen = mapOf(
            binding.btStatus to "status",
            binding.btDik to "dik",
            binding.btSakit to "sakit",
            binding.btBko to "bko",
            binding.btCUti to "cuti"
        )

        mapAbsen.forEach { (button, value) ->
            button.setOnClickListener {
                cekabsen(binding, value)
            }
        }
        binding.btLog.setOnClickListener {
            startActivity(Intent(this, History::class.java))
            finish()
        }
        binding.btDoas.setOnClickListener {
            startActivity(Intent(this, Doas::class.java))
            finish()
        }
        binding.btInfo.setOnClickListener {
            startActivity(Intent(this@Home, Berita::class.java))
            finishAffinity()
        }

    }
    override fun onBeritaChanged(berita: BeritaItem) {
        binding.tvJudul.text = berita.judul
        binding.tvIsi.text = htmlPreviewClean(berita.isi, 20)
    }

    private fun monitorInternet() {
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    refreshData()
                }
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback)
    }



    override fun onPause() {
        super.onPause()
        if (::d.isInitialized && d.isShowing) {
            d.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
        cleanTempPhotos(this)
    }


    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    private fun refreshData() {
        if (isRequestRunning) return

        isRequestRunning = true

        dos(binding)

        Handler(Looper.getMainLooper()).postDelayed({
            isRequestRunning = false
        }, 3000) // asumsi request max 3 detik
    }


}
