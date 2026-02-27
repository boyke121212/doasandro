package com.toelve.doas.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.toelve.doas.R
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityAbsenPulangBinding
import com.toelve.doas.databinding.ActivityStatusesBinding
import com.toelve.doas.helper.AbsensiManager
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.helper.PreviewFotoActivity
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

            binding.tvLiveTime.text = "Live Camera time "+waktu
            handler.postDelayed(this, 1000)
        }
    }
    private var menu = "mulai"
    private var jam: String? = "00:00"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAbsenPulangBinding.inflate(layoutInflater)
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

        absensiManager.onGpsStatusChanged = { acc ->

            runOnUiThread {

                val gpsReady =
                    acc != null &&
                            acc <= 120F &&
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
        bataspulang=intent.getStringExtra("bataspulang")
        ket=intent.getStringExtra("ket")
        jamserver=intent.getStringExtra("jamserver")
        val kepulangan=cekBatasPulang()
        statuspulang=kepulangan
        when (kepulangan) {
            "OVERTIME" -> {
                binding.tvLabelAlasan.visibility=View.VISIBLE
                binding.etAlasan.visibility=View.VISIBLE
                binding.inputLayoutAlasan.visibility=View.VISIBLE
                binding.tvLabelAlasan.text="Jelaskan Alasan Anda Pulang Lewat Waktu"
                binding.etAlasan.hint="Jelaskan Alasan Anda Pulang Lewat Waktu"

            }
            "TEPAT WAKTU" -> {
                binding.tvLabelAlasan.visibility=View.GONE
                binding.etAlasan.visibility=View.GONE
                binding.inputLayoutAlasan.visibility=View.GONE
            }
            "PULANG CEPAT" -> {
                binding.inputLayoutAlasan.visibility=View.VISIBLE
                binding.tvLabelAlasan.visibility=View.VISIBLE
                binding.etAlasan.visibility=View.VISIBLE
                binding.tvLabelAlasan.text="Jelaskan Alasan Anda Pulang Cepat"
                binding.etAlasan.hint="Jelaskan Alasan Anda Pulang Pulang Cepat"
            }
        }



        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@AbsenPulang, Statuses::class.java))
            finishAffinity()
        }
    }
    private fun cekBatasPulang(): String {

        if (bataspulang.isNullOrEmpty() || jamserver.isNullOrEmpty()) {
            return "-"
        }

        fun toMinutes(time: String): Int {
            val parts = time.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            return hour * 60 + minute
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
        val alasan = if (statuspulang.equals("TEPAT WAKTU", true))
            "TEPAT WAKTU"
        else
            binding.etAlasan.text.toString().trim()


        val params = mutableMapOf(
            "menu" to "PULANG",
            "statuspulang" to statuspulang,
            "ketpul" to alasan,
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
                    startActivity(Intent(this, Statuses::class.java))
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