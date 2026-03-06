package com.toelve.doas.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.viewpager.widget.ViewPager
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityStatusesBinding
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.helper.Auto.autoSlide
import com.toelve.doas.helper.Auto.renderAbsensi
import com.toelve.doas.helper.Auto.resetView
import com.toelve.doas.helper.BeritaPagerAdapter
import com.toelve.doas.soasa.CryptoAES

class Statuses : Boyke(),
    BeritaPagerAdapter.OnBeritaChangeListener {
    private lateinit var binding: ActivityStatusesBinding
    private var bataspulang:String?=null
    private var jamserver:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatusesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.btAbsen.setOnClickListener {
            startActivity(Intent(this@Statuses, Home::class.java))
            finishAffinity()
        }
        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@Statuses, Home::class.java))
            finishAffinity()
        }


        binding.btDoas.setOnClickListener {
            startActivity(Intent(this, Doas::class.java))
            finish()
        }
        binding.btLog.setOnClickListener {
            startActivity(Intent(this@Statuses, History::class.java))
            finishAffinity()
        }
        binding.btInfo.setOnClickListener {
            startActivity(Intent(this@Statuses, Berita::class.java))
            finishAffinity()
        }
    }



    override fun onResume() {
        resetView(binding)
        AuthManager(this, "api/ambil_absen").checkAuth(
            onLoading = { loading ->
                if (loading) showLoading()
                else hideLoading()
            },

            onSuccess = { json ->
                val status = json.getString("status")
                if (status == "ok") {
                    bataspulang=json.getString("pulang")
                    jamserver=json.getString("jamserver")
                    try {
                        val aesKey = json.getString("aes_key")
                        val arr = json.optJSONArray("dataabsen")
                        if ((json.optJSONArray("dataabsen")?.length() ?: 0) == 0) {
                            Toast.makeText(this, "Anda Belum Absen Hari ini", Toast.LENGTH_SHORT)
                                .show()
                            startActivity(Intent(this, Home::class.java))
                            finishAffinity()
                        }
                        if (arr != null) {
                            if (arr.length() > 0) {
                                val obj = arr.getJSONObject(0)
                                renderAbsensi(binding, obj, aesKey, bataspulang,jamserver)
                            }
                        }
                        val listBerita = ArrayList<BeritaItem>()

                        if (json.has("berita")) {
                            val beritaArray = json.optJSONArray("berita")

                            if (beritaArray != null && beritaArray.length() > 0) {
                                for (i in 0 until beritaArray.length()) {
                                    val obj = beritaArray.getJSONObject(i)

                                    listBerita.add(
                                        BeritaItem(
                                            id = obj.optString("id"),
                                            judul = CryptoAES.decrypt(
                                                obj.optString("judul"),
                                                aesKey
                                            ),
                                            isi = CryptoAES.decrypt(obj.optString("isi"), aesKey),
                                            tanggal = obj.optString("tanggal"),
                                            foto = CryptoAES.decrypt(obj.optString("foto"), aesKey),
                                            pdf = CryptoAES.decrypt(obj.optString("pdf"), aesKey)
                                        )
                                    )
                                }
                            }
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
                            if (adapter.items.isNotEmpty()) {
                                onBeritaChanged(adapter.items[0])
                            }

                            // UPDATE SAAT SWIPE
                            viewPager.addOnPageChangeListener(object :
                                ViewPager.SimpleOnPageChangeListener() {
                                override fun onPageSelected(position: Int) {
                                    onBeritaChanged(adapter.items[position])
                                }
                            })

                            autoSlide(viewPager, listBerita.size)
                        }
                    } catch (e: Exception) {
                    }

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
        super.onResume()
    }


    override fun onBeritaChanged(berita: BeritaItem) {
        binding.tvJudul.text = berita.judul
        binding.tvIsi.text = berita.isi
    }

}
