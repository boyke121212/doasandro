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
import com.toelve.doas.databinding.ActivityAbsenSakitBinding
import com.toelve.doas.helper.AbsensiManager
import com.toelve.doas.helper.AuthManager
import java.io.File
import java.util.UUID
import com.toelve.doas.helper.PreviewFotoActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AbsenSakit : Boyke() {

    private lateinit var binding: ActivityAbsenSakitBinding
    private lateinit var absensiManager: AbsensiManager
    private val handler = Handler(Looper.getMainLooper())
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
        binding = ActivityAbsenSakitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        absensiManager = AbsensiManager(
            activity = this,
            previewView = binding.ivFoto
        )
        
        updateButtonState(false)
        
        binding.tvLiveTime.visibility = View.GONE
        absensiManager.prepare()
        
        val ketam = intent.getStringExtra("ketam") ?: ""
        binding.etAlasan.setText(ketam)
        
        absensiManager.onGpsStatusChanged = { acc ->
            runOnUiThread {
                val gpsReady = acc != null && acc <= 120f && absensiManager.lat != null && absensiManager.lon != null

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
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }

        binding.btMulai.setOnClickListener {
            binding.tvLiveTime.visibility = View.VISIBLE
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

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@AbsenSakit, Home::class.java))
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
            Toast.makeText(this, "Minimal 2 foto bukti", Toast.LENGTH_SHORT).show()
            return
        }
        val alasan = binding.etAlasan.text.toString().trim()
        if (alasan.isEmpty()) {
            Toast.makeText(this, "Keterangan wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        val params = mutableMapOf(
            "menu" to "Sakit",
            "ketam" to alasan,
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
                    startActivity(Intent(this, MainActivity::class.java))
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
