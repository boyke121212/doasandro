package com.toelve.doas.view

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.toelve.doas.BuildConfig
import com.toelve.doas.R
import com.toelve.doas.databinding.ActivityBeritaDetailBinding
import com.toelve.doas.helper.Auto
import com.toelve.doas.helper.SecurePrefs
import com.toelve.doas.soasa.DeviceSecurityHelper

class BeritaDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBeritaDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBeritaDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set Toolbar as ActionBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Ambil data dari Intent
        val id = intent.getStringExtra("id") ?: ""
        val judul = intent.getStringExtra("judul") ?: ""
        val isi = intent.getStringExtra("isi") ?: ""
        val tanggal = intent.getStringExtra("tanggal") ?: ""
        val foto = intent.getStringExtra("foto") ?: ""
        val pdf = intent.getStringExtra("pdf") ?: ""

        // Tampilkan Data
        binding.tvDetailJudul.text = judul
        binding.tvDetailTanggal.text = tanggal
        
        // Format HTML untuk Isi Berita
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.tvDetailIsi.text = Html.fromHtml(isi, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            binding.tvDetailIsi.text = Html.fromHtml(isi)
        }

        // Load Foto
        val token = SecurePrefs(this).getAccessToken()
        val urlFoto = BuildConfig.BASE_URL + "api/media/berita/" + foto

        binding.ivDetailFoto.load(urlFoto) {
            placeholder(R.drawable.doas2)
            error(R.drawable.logodit)
            crossfade(true)
            addHeader("Authorization", "Bearer $token")
            addHeader("X-Device-Hash", DeviceSecurityHelper.getDeviceHash(this@BeritaDetailActivity))
            addHeader("X-App-Signature", DeviceSecurityHelper.getAppSignatureHash(this@BeritaDetailActivity))
        }

        // Handle PDF Button
        if (pdf.isNotEmpty()) {
            binding.btnDownloadPdf.visibility = View.VISIBLE
            binding.btnDownloadPdf.setOnClickListener {
                if (!Auto.isInternetAvailable(this)) {
                    Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val urlPdf = BuildConfig.BASE_URL + "api/media/pdf/" + pdf
                Auto.downloadPdfVolley(this, urlPdf, pdf, token ?: "")
            }
        } else {
            binding.btnDownloadPdf.visibility = View.GONE
        }
    }
}
