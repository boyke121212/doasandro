package com.toelve.doas.helper

import android.app.Dialog
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.google.gson.Gson
import com.toelve.doas.R
import com.toelve.doas.databinding.ActivityHomeBinding
import com.toelve.doas.helper.Auto.autoSlide
import com.toelve.doas.soasa.CryptoAES
import com.toelve.doas.view.AbsenBko
import com.toelve.doas.view.AbsenCuti
import com.toelve.doas.view.AbsenDik
import com.toelve.doas.view.BeritaItem
import com.toelve.doas.view.Home
import com.toelve.doas.view.AbsenHadir
import com.toelve.doas.view.AbsenSakit
import com.toelve.doas.view.Statuses

import com.toelve.doas.model.DashboardData
import org.json.JSONObject
import kotlin.text.lowercase

fun Home.dos(binding: ActivityHomeBinding) {
    AuthManager(this, "api/auth-check").checkAuth(
        onLoading = { loading ->
            if (loading) showLoading()
            else hideLoading()
        },

        onSuccess = { json ->
            Log.e("json di auth", json.toString())
            isRequestRunning = false

            try {
                val aesKey = json.getString("aes_key")
                val status = json.getString("status")
                
                // Render Dashboard Data
                if (json.has("dashboard")) {
                    val dashboardJson = json.getJSONObject("dashboard").toString()
                    val dashboardData = Gson().fromJson(dashboardJson, DashboardData::class.java)
                    renderDashboard(dashboardData)
                }

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
                        }
                    }

                    val viewPager = binding.pager
                    if (listBerita.isNotEmpty()) {
                        val adapter = BeritaPagerAdapter(
                            context = this,
                            items = listBerita,
                            listener = this
                        )
                        viewPager.adapter = adapter

                        onBeritaChanged(listBerita[0])

                        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                            override fun onPageSelected(position: Int) {
                                onBeritaChanged(listBerita[position])
                            }
                        })

                        autoSlide(viewPager, listBerita.size)
                    }
                }
            } catch (e: Exception) {
                Log.e("DOS_ERROR", "Error parsing data", e)
            }
        },

        onLogout = { message ->
            isRequestRunning = false
            when {
                message == "__TIMEOUT__" || message == "__NO_INTERNET__" -> {
                    Toast.makeText(this, "Connection Time Out Error", Toast.LENGTH_LONG).show()
                }
                message.contains("Verification failed") -> {
                    Toast.makeText(this, "Verification failed", Toast.LENGTH_LONG).show()
                    // Restart logic if needed
                }
                else -> {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    )
}

fun Home.go(binding: ActivityHomeBinding) {
    d = Dialog(this@go)
    d.setContentView(R.layout.dialog)
    d.setCancelable(true)
    d.window?.setBackgroundDrawableResource(android.R.color.transparent)

    val tvTanya: TextView = d.findViewById(R.id.tvTitle)
    val tvMessage: TextView = d.findViewById(R.id.tvMessage)
    val btYa: Button = d.findViewById(R.id.btYa)
    val btTidak: Button = d.findViewById(R.id.btTidak)

    tvTanya.text = "Konfirmasi Apel"
    tvMessage.text = "Anda Wajib Apel, Kami Akan Cek Kehadiran Apel Anda"
    btYa.text = "Setuju"
    btTidak.text = "Tidak"

    btYa.setOnClickListener {
        AuthManager(this@go, "api/cekabsen").checkAuth(
            onLoading = { loading ->
                if (loading) showLoading()
                else hideLoading()
            },
            onSuccess = { json ->
                if (d.isShowing) d.dismiss()
                try {
                    val absen = json.optString("absen")
                    if (absen.equals("belum", true)) {
                        val intent = Intent(this@go, AbsenHadir::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@go, absen, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@go, "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
                }
            },
            onLogout = { message ->
                if (d.isShowing) d.dismiss()
                Toast.makeText(this@go, message, Toast.LENGTH_LONG).show()
            }
        )
    }
    btTidak.setOnClickListener { d.dismiss() }
    d.show()
    val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
    d.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
}

fun Home.cekabsen(binding: ActivityHomeBinding, dari: String) {
    val params = HashMap<String, String>()
    params["jenis"] = when (dari.lowercase()) {
        "dik" -> "DIK"
        "sakit" -> "SAKIT"
        "bko" -> "BKO"
        "cuti" -> "CUTI"
        else -> ""
    }

    AuthManager(this@cekabsen, "api/cekabsen").checkAuth(
        params = params,
        onLoading = { loading ->
            if (loading) showLoading()
            else hideLoading()
        },
        onSuccess = { json ->
            try {
                val absen = json.optString("absen")
                val ketam = json.optString("ketam")
                when (dari.lowercase()) {
                    "status" -> startStatus(absen, ketam)
                    "dik" -> startAbsen(absen, AbsenDik::class.java, ketam)
                    "sakit" -> startAbsen(absen, AbsenSakit::class.java, ketam)
                    "bko" -> startAbsen(absen, AbsenBko::class.java, ketam)
                    "cuti" -> startAbsen(absen, AbsenCuti::class.java, ketam)
                }
            } catch (e: Exception) {
                Toast.makeText(this@cekabsen, "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
            }
        },
        onLogout = { message ->
            Toast.makeText(this@cekabsen, message, Toast.LENGTH_LONG).show()
        }
    )
}

fun Home.startStatus(absen: String, ketam: String) {
    if (absen.equals("belum", true)) {
        Toast.makeText(this, "Anda Belum Absen", Toast.LENGTH_SHORT).show()
    } else {
        val intent = Intent(this, Statuses::class.java)
        intent.putExtra("ketam", ketam)
        startActivity(intent)
        finish()
    }
}

fun Home.startAbsen(absen: String, target: Class<*>, ketam: String) {
    if (absen.equals("belum", true)) {
        val intent = Intent(this, target)
        intent.putExtra("ketam", ketam)
        startActivity(intent)
        finishAffinity()
    } else {
        Toast.makeText(this, absen, Toast.LENGTH_SHORT).show()
    }
}
