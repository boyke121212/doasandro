package com.toelve.doas.view

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.toelve.doas.helper.BeritaPagerAdapter
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityHomeBinding
import com.toelve.doas.helper.Auto.cleanTempPhotos
import com.toelve.doas.helper.RenderHtml.htmlPreviewClean
import com.toelve.doas.helper.cekabsen
import com.toelve.doas.helper.dos
import com.toelve.doas.helper.go
import com.toelve.doas.helper.setupDoubleBackExit
import com.toelve.doas.model.DashboardData
import com.toelve.doas.ui.home.SummarySubditAdapter
import com.toelve.doas.ui.home.UserTerakhirAdapter

class Home : Boyke(),
    BeritaPagerAdapter.OnBeritaChangeListener {
    lateinit var binding: ActivityHomeBinding
    lateinit var d: Dialog
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    var isRequestRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
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
        binding.btProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        refreshData()
        cleanTempPhotos(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun refreshData() {
        if (isRequestRunning) return
        isRequestRunning = true
        dos(binding)
    }

    fun renderDashboard(data: DashboardData?) {
        binding.apply {
            tvTahunAktif.text = "Tahun Aktif: ${data?.tahunAktif ?: "-"}"
            tvBulanAwal.text = "Bulan Awal: ${data?.bulanAwal ?: "-"}"

            rvSummarySubdit.layoutManager = LinearLayoutManager(this@Home)
            rvSummarySubdit.adapter = SummarySubditAdapter(data?.summarySubdit ?: emptyMap())

            rvUserTerakhir.layoutManager = LinearLayoutManager(this@Home)
            rvUserTerakhir.adapter = UserTerakhirAdapter(data?.userTerakhir ?: emptyList())

            setupChart(data)
        }
    }

    fun setupChart(data: DashboardData?) {
        val dataMap = data?.dataMap
        if (dataMap.isNullOrEmpty()) {
            binding.chartSubdit.clear()
            return
        }

        val lineData = LineData()
        val colors = listOf(Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN)
        var colorIndex = 0

        dataMap.forEach { (subdit, months) ->
            val entries = months.mapNotNull { (bulanStr, details) ->
                val xValue = bulanStr.toFloatOrNull() ?: return@mapNotNull null
                val terserap = details["anggaran_terserap"]?.toFloatOrNull() ?: 0f
                Entry(xValue, terserap)
            }.sortedBy { it.x }

            if (entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, subdit).apply {
                    color = colors[colorIndex % colors.size]
                    setCircleColor(color)
                    lineWidth = 2f
                    circleRadius = 4f
                    setDrawValues(false)
                }
                lineData.addDataSet(dataSet)
                colorIndex++
            }
        }

        binding.chartSubdit.apply {
            this.data = lineData
            description.isEnabled = false
            xAxis.granularity = 1f
            animateX(1000)
            invalidate()
        }
    }
}
