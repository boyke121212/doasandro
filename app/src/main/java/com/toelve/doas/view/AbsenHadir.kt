package com.toelve.doas.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.toelve.doas.R
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityAbsenHadirBinding
import com.toelve.doas.helper.AbsensiManager
import com.toelve.doas.helper.AuthManager
import java.io.File
import java.util.UUID
import com.github.chrisbanes.photoview.PhotoView
import com.toelve.doas.helper.PreviewFotoActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AbsenHadir : Boyke() {

    private lateinit var binding: ActivityAbsenHadirBinding
    private lateinit var absensiManager: AbsensiManager
    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            val waktu = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            binding.tvLiveTime.text = "Live Camera time "+waktu
            handler.postDelayed(this, 1000)
        }
    }
    private var menu = "mulai"
    private var jam: String? = "00:00"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbsenHadirBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        absensiManager = AbsensiManager(
            activity = this,
            previewView = binding.ivFoto
        )
        binding.btMulai.isEnabled = false
        binding.btMulai.setBackgroundColor(0xFFBDBDBD.toInt()) // abu-abu
        binding.btMulai.setTextColor(0xFF777777.toInt())
        binding.tvLiveTime.visibility= View.GONE
        absensiManager.prepare()
        val ketam = intent.getStringExtra("ketam") ?: ""
        binding.etAlasan.setText(ketam)
        absensiManager.onGpsStatusChanged = { acc ->

            runOnUiThread {

                val gpsReady =
                    acc != null &&
                            acc <= 120f &&
                            absensiManager.lat != null &&
                            absensiManager.lon != null

                if (!gpsReady) {

                    binding.btMulai.isEnabled = false
                    binding.btMulai.setBackgroundColor(0xFFBDBDBD.toInt())
                    binding.btMulai.setTextColor(0xFF777777.toInt())

                    if (acc == null) {
                        binding.tvGpsStatus.text =
                            "Mencari posisi anda saat ini..."
                    } else {
                        binding.tvGpsStatus.text =
                            "Posisi belum akurat (${acc.toInt()}m), mohon tunggu..."
                    }

                    binding.imgGpsStatus.setColorFilter(
                        0xFFFFC107.toInt()
                    )
                    return@runOnUiThread
                }

                binding.btMulai.isEnabled = true
                binding.btMulai.setBackgroundColor(0xFFE0E0E0.toInt())
                binding.btMulai.setTextColor(0xFF000000.toInt())

                binding.tvGpsStatus.text =
                    "Posisi berhasil didapatkan... anda bisa memulai absen"

                binding.imgGpsStatus.setColorFilter(
                    0xFF4CAF50.toInt()
                )
            }
        }



        // default state
        binding.btnSwitch.visibility = View.GONE
        binding.btMulai.text = "Mulai"

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

        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@AbsenHadir, Home::class.java))
            finishAffinity()
        }
    }

    /* ================= FLOW ================= */

    private fun startFlow() {

        absensiManager.authenticate {

            binding.ivResult.visibility = View.GONE
            binding.ivFoto.visibility = View.VISIBLE

            absensiManager.prepare()

            menu = "capture"
            binding.btMulai.text = "Capture"
            binding.btnSwitch.visibility = View.VISIBLE
        }
    }

    private fun captureFlow() {

        absensiManager.capture { uri, jumlah ->

            val fileBaru = absensiManager.capturedFiles.last()

            addThumbnail(uri, fileBaru)

            Toast.makeText(
                this,
                "Foto ke-$jumlah",
                Toast.LENGTH_SHORT
            ).show()

            if (jumlah >= 2) {
                binding.btMulai.isEnabled = false
                binding.btMulai.text = "Maksimal"
            }
        }
    }

    /* ================= THUMBNAIL ================= */

    private fun addThumbnail(uri: Uri, file: File) {

        val img = ImageView(this)

        val size = (70 * resources.displayMetrics.density).toInt()
        val params = LinearLayout.LayoutParams(size, size)
        params.marginEnd = 12
        img.layoutParams = params

        img.scaleType = ImageView.ScaleType.CENTER_CROP
        img.setImageURI(uri)

        // klik untuk preview besar
        img.setOnClickListener {
            val intent = Intent(this, PreviewFotoActivity::class.java)
            intent.putExtra("foto_uri", uri.toString())
            startActivity(intent)

        }


        // long click untuk hapus
        img.setOnLongClickListener {

            absensiManager.capturedFiles.remove(file)
            binding.layoutThumb.removeView(img)

            binding.btMulai.isEnabled = true
            binding.btMulai.text = "Capture"

            Toast.makeText(
                this,
                "Foto dihapus",
                Toast.LENGTH_SHORT
            ).show()

            true
        }

        binding.layoutThumb.addView(img)
    }

    /* ================= SUBMIT ================= */

    private fun onKirim() {

        val files = absensiManager.capturedFiles

        if (files.size < 2) {
            Toast.makeText(this, "Foto harus 2", Toast.LENGTH_SHORT).show()
            return
        }
        val alasan = binding.etAlasan.text.toString().trim()

        if (alasan.isEmpty()) {
            Toast.makeText(this, "Alasan wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }


        val params = mutableMapOf(
            "menu" to "ADL",
            "ketam" to alasan,
            "latitude" to absensiManager.lat.toString(),
            "longitude" to absensiManager.lon.toString()
        )

        // anti replay
        params["__ts"] = (System.currentTimeMillis() / 1000).toString()
        params["__nonce"] = UUID.randomUUID().toString()

        AuthManager(this, "api/absen").uploadFoto(
            files = files,   // ⬅️ MULTI FILE
            params = params,

            onLoading = { loading ->
                if (loading) showLoading()
                else hideLoading()
            },

            onSuccess = { json ->

                val status = json.optString("status")
                val message = json.optString("message", "Berhasil")

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                if (status == "ok") {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            },

            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            },

            onLogout = { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
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
