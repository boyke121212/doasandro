package com.toelve.doas.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager.widget.ViewPager
import com.toelve.doas.BuildConfig
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityDoasBinding
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.helper.Auto.autoSlide
import com.toelve.doas.helper.Auto.downloadPdfVolley
import com.toelve.doas.helper.BeritaPagerAdapter
import com.toelve.doas.helper.RenderHtml.htmlPreviewClean
import com.toelve.doas.helper.SecurePrefs
import com.toelve.doas.soasa.CryptoAES
import com.toelve.doas.soasa.DeviceSecurityHelper

class Doas : Boyke(),
    BeritaPagerAdapter.OnBeritaChangeListener {
    private lateinit var binding: ActivityDoasBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        AuthManager(this, "api/getdoas").checkAuth(
            onLoading = { loading ->
                if (loading) showLoading()
                else hideLoading()
            },

            onSuccess = { json ->
                try {
                    val aesKey = json.getString("aes_key")
                    val status = json.getString("status")
                    if (status == "ok") {
                        val listBerita = ArrayList<BeritaItem>()
                        if (json.has("berita")) {
                            val beritaArray = json.optJSONArray("berita")

                            if (beritaArray != null && beritaArray.length() > 0) {
                                for (i in 0 until beritaArray.length()) {
                                    val obj = beritaArray.getJSONObject(i)

                                    listBerita.add(
                                        BeritaItem(
                                            id = obj.optString("id"),
                                            judul = CryptoAES.decrypt(obj.optString("judul"), aesKey),
                                            isi = CryptoAES.decrypt(obj.optString("isi"), aesKey),
                                            tanggal = obj.optString("tanggal"),
                                            foto = CryptoAES.decrypt(obj.optString("foto"), aesKey),
                                            pdf = CryptoAES.decrypt(obj.optString("pdf"), aesKey)
                                        )
                                    )
                                }
                            } else {
                            }
                        } else {
                        }




                        val viewPager = binding.pager

                        if (listBerita.isNotEmpty()) {
                            val adapter = BeritaPagerAdapter(
                                context = this,
                                items = listBerita,
                                listener = this // 🔥 PASANG LISTENER
                            )
                            viewPager.adapter = adapter

                            // SET AWAL
                            onBeritaChanged(listBerita[0])

                            // UPDATE SAAT SWIPE
                            viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                                override fun onPageSelected(position: Int) {
                                    onBeritaChanged(listBerita[position])
                                }
                            })

                            autoSlide(viewPager, listBerita.size)
                        }

                        val judul = CryptoAES.decrypt(json.getString("judul"), aesKey)
                        val isi =CryptoAES.decrypt(json.getString("isi"), aesKey)
                        val pdf =CryptoAES.decrypt(json.getString("pdf"), aesKey)
                        binding.tvAtas.text = judul
                        binding.tvDoas.text = htmlPreviewClean(isi, 2000)
                        binding.btDownload.setOnClickListener {
                            val token = SecurePrefs.get(this@Doas).getAccessToken()

                            val url = BuildConfig.BASE_URL + "api/media/pdf/" + pdf
                            downloadPdfVolley(this@Doas, url, pdf,token!!)
                        }
                    }


                } catch (e: Exception) {
                }
            },

            onLogout = { message ->
                when {
                    message == "__TIMEOUT__" ||
                            message == "__NO_INTERNET__" -> {

                        Toast.makeText(this, "Connecion Time Out Error", Toast.LENGTH_LONG).show()

                        startActivity(Intent(this, Home::class.java))
                        finish()
                    }
                    message.contains("Verification failed") -> {
                        startActivity(Intent(this, Home::class.java))
                        finish()
                    }
                    message.contains("Checking Device") -> {
                        startActivity(Intent(this, Home::class.java))
                        finish()
                    }

                    else -> {
                        startActivity(Intent(this, Home::class.java))
                        finish()
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
        binding.btAbsen.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
        }
        binding.btLog.setOnClickListener {
            startActivity(Intent(this@Doas, History::class.java))
            finishAffinity()
        }
        binding.btStatus.setOnClickListener {
            startActivity(Intent(this@Doas, Statuses::class.java))
            finishAffinity()
        }
        binding.btInfo.setOnClickListener {
            startActivity(Intent(this@Doas, Berita::class.java))
            finishAffinity()
        }
        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@Doas, Home::class.java))
            finishAffinity()
        }
    }

    override fun onBeritaChanged(berita: BeritaItem) {
        binding.tvJudul.text = berita.judul
        binding.tvIsi.text = htmlPreviewClean(berita.isi, 20)
    }
}