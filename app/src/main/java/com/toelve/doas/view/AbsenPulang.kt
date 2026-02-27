package com.toelve.doas.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityAbsenPulangBinding
import com.toelve.doas.helper.AbsensiManager
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.helper.PreviewFotoActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AbsenPulang : Boyke() {
    private  var bataspulang: String?=null
    private  var ket: String?=null
    private  var jamserver: String?=null
    private lateinit var binding: ActivityAbsenPulangBinding
    private lateinit var absensiManager: AbsensiManager
    private val handler = Handler(Looper.getMainLooper())
    var statuspulang : String=""

    private val timeRunnable = object : Runnable {
        override fun run() {
            val waktu = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            binding.tvLiveTime.text = waktu
            handler.postDelayed(this, 1000)
        }
    }
    private var menu = "mulai"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAbsenPulangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        absensiManager = AbsensiManager(
            activity = this,
            previewView = binding.ivFoto
        )
        
        updateButtonState(false)
        
        binding.tvLiveTime.visibility= View.GONE
        absensiManager.prepare()

        absensiManager.onGpsStatusChanged = { acc ->
            runOnUiThread {
                val gpsReady = acc != null && acc <= 120F && absensiManager.lat != null && absensiManager.lon != null

                if (!gpsReady) {
                    updateButtonState(false)
                    binding.tvGpsStatus.text = if (acc == null) "Mencari posisi..." else "Akurasi rendah (${acc.toInt()}m)..."
                    binding.imgGpsStatus.setColorFilter(0xFFFFC107.toInt())
                } else {
                    updateButtonState(true)
                    binding.tvGpsStatus.text = "Lokasi terdeteksi"
                    binding.imgGpsStatus.setColorFilter(0xFF4CAF50.toInt())
                }
            }
        }

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, Statuses::class.java))
            finishAffinity()
        }

        binding.btMulai.setOnClickListener {
            binding.tvLiveTime.visibility= View.VISIBLE
            when (menu) {
                "mulai" -> startFlow()
                "capture" -> captureFlow()
            }
        }

        binding.btnSwitch.setOnClickListener {
            absensiManager.switchCamera()
        }

        binding.btKirim.setOnClickListener {
            onKirim()
        }
        bataspulang=intent.getStringExtra("bataspulang")
        ket=intent.getStringExtra("ket")
        jamserver=intent.getStringExtra("jamserver")
        val kepulangan=cekBatasPulang()
        statuspulang=kepulangan
        
        when (kepulangan) {
            "OVERTIME" -> {
                binding.inputCard.visibility=View.VISIBLE
                binding.tvLabelAlasan.text="Jelaskan Alasan Anda Pulang Lewat Waktu"
                binding.etAlasan.hint="Alasan pulang lewat waktu..."
            }
            "TEPAT WAKTU" -> {
                binding.inputCard.visibility=View.GONE
            }
            "PULANG CEPAT" -> {
                binding.inputCard.visibility=View.VISIBLE
                binding.tvLabelAlasan.text="Jelaskan Alasan Anda Pulang Cepat"
                binding.etAlasan.hint="Alasan pulang cepat..."
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@AbsenPulang, Statuses::class.java))
            finishAffinity()
        }
    }

    private fun updateButtonState(enabled: Boolean) {
        binding.btMulai.isEnabled = enabled
        if (!enabled) {
            binding.btMulai.alpha = 0.5f
        } else {
            binding.btMulai.alpha = 1.0f
        }
    }

    private fun cekBatasPulang(): String {
        if (bataspulang.isNullOrEmpty() || jamserver.isNullOrEmpty()) return "-"
        fun toMinutes(time: String): Int {
            val parts = time.split(":")
            return parts[0].toInt() * 60 + parts[1].toInt()
        }
        val batasMenit = toMinutes(bataspulang!!)
        val serverMenit = toMinutes(jamserver!!)
        val batasNormal = batasMenit + 5
        return when {
            serverMenit < batasMenit -> "PULANG CEPAT"
            serverMenit <= batasNormal -> "TEPAT WAKTU"
            else -> "OVERTIME"
        }
    }

    private fun startFlow() {
        absensiManager.authenticate {
            binding.ivResult.visibility = View.GONE
            binding.ivFoto.visibility = View.VISIBLE
            absensiManager.prepare()
            menu = "capture"
            binding.btMulai.text = "Ambil Foto"
            binding.btnSwitch.visibility = View.VISIBLE
        }
    }

    private fun captureFlow() {
        absensiManager.capture { uri, jumlah ->
            val fileBaru = absensiManager.capturedFiles.last()
            addThumbnail(uri, fileBaru)
            if (jumlah >= 2) {
                binding.btMulai.isEnabled = false
                binding.btMulai.text = "Selesai"
                binding.btMulai.alpha = 0.5f
            }
        }
    }

    private fun addThumbnail(uri: Uri, file: File) {
        val img = ImageView(this)
        val size = (80 * resources.displayMetrics.density).toInt()
        val params = LinearLayout.LayoutParams(size, size)
        params.marginEnd = 12
        img.layoutParams = params
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        img.setImageURI(uri)
        img.setOnClickListener {
            val intent = Intent(this, PreviewFotoActivity::class.java)
            intent.putExtra("foto_uri", uri.toString())
            startActivity(intent)
        }
        img.setOnLongClickListener {
            absensiManager.capturedFiles.remove(file)
            binding.layoutThumb.removeView(img)
            binding.btMulai.isEnabled = true
            binding.btMulai.alpha = 1.0f
            binding.btMulai.text = "Ambil Foto"
            true
        }
        binding.layoutThumb.addView(img)
    }

    private fun onKirim() {
        val files = absensiManager.capturedFiles
        if (files.size < 2) {
            Toast.makeText(this, "Foto harus 2", Toast.LENGTH_SHORT).show()
            return
        }
        val alasan = if (statuspulang.equals("TEPAT WAKTU", true)) "TEPAT WAKTU" else binding.etAlasan.text.toString().trim()
        if (alasan.isEmpty() && !statuspulang.equals("TEPAT WAKTU", true)) {
            Toast.makeText(this, "Alasan wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val params = mutableMapOf(
            "menu" to "PULANG",
            "statuspulang" to statuspulang,
            "ketpul" to alasan,
            "latitude" to absensiManager.lat.toString(),
            "longitude" to absensiManager.lon.toString()
        )
        params["__ts"] = (System.currentTimeMillis() / 1000).toString()
        params["__nonce"] = UUID.randomUUID().toString()

        AuthManager(this, "api/absen").uploadFoto(
            files = files,
            params = params,
            onLoading = { loading -> if (loading) showLoading() else hideLoading() },
            onSuccess = { json ->
                val status = json.optString("status")
                if (status == "ok") {
                    Toast.makeText(this, "Absensi berhasil terkirim", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Statuses::class.java))
                    finishAffinity()
                }
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_LONG).show() },
            onLogout = { message -> Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
        )
    }

    override fun onResume() {
        super.onResume()
        handler.post(timeRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        absensiManager.stop()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timeRunnable)
    }
}
