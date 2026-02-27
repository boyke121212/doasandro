package com.toelve.doas.helper

import android.app.Dialog
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
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
import org.json.JSONObject
import kotlin.text.lowercase

fun Home.dos(  binding: ActivityHomeBinding){
    AuthManager(this, "api/auth-check").checkAuth(
        onLoading = { loading ->
            if (loading) showLoading()
            else hideLoading()
        },

        onSuccess = { json ->
            try {
                val aesKey = json.getString("aes_key")
                val status = json.getString("status")
                if(status=="ok"){
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
                }


            } catch (e: Exception) {
            }
        },

        onLogout = { message ->
            when {
                message == "__TIMEOUT__" || message == "__NO_INTERNET__" -> {

                    Toast.makeText(this, "Connecion Time Out Error", Toast.LENGTH_LONG).show()

                }
                message.contains("Verification failed") -> {
                    Toast.makeText(this, "Connecion Time Out Error", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, Home::class.java))
                    finish()
                }
                message.contains("Checking Device") -> {
                    Toast.makeText(this, "Connecion Time Out Error", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, Home::class.java))
                    finish()
                }

                else -> {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    )
}

fun Home.go(binding:ActivityHomeBinding){
    d = Dialog(this@go)
    d.setContentView(R.layout.dialog)
    d.setCancelable(true)

    // PENTING: background transparan biar CardView rapi
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
                        Toast.makeText(
                            this@go,
                            absen,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@go,
                        "Terjadi kesalahan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },

            onLogout = { message ->
                if (d.isShowing) d.dismiss()

                when (message) {
                    "Verification failed","__TIMEOUT__", "__NO_INTERNET__" -> {
                        Toast.makeText(
                            this@go,
                            message,
                            Toast.LENGTH_LONG
                        ).show()

                        // 🔴 tutup aplikasi
                    }

                    else -> {
                        Toast.makeText(
                            this@go,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
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
        params = params,   // 🔥 kirim ke backend

        onLoading = { loading ->
            if (loading) showLoading()
            else hideLoading()
        },

        onSuccess = { json ->
            try {

                val absen = json.optString("absen")
                val ketam = json.optString("ketam")
              /*  Log.e("ketam",ketam)
                Log.e("isiabsen",absen)
                Log.e("dari",dari)*/
                when (dari.lowercase()) {
                    "status" -> startStatus(absen,ketam)
                    "dik" -> startAbsen(absen, AbsenDik::class.java,ketam)
                    "sakit" -> startAbsen(absen, AbsenSakit::class.java,ketam)
                    "bko" -> startAbsen(absen, AbsenBko::class.java,ketam)
                    "cuti" -> startAbsen(absen, AbsenCuti::class.java,ketam)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@cekabsen,
                    "Terjadi kesalahan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },

        onLogout = { message ->
            Toast.makeText(
                this@cekabsen,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    )
}
fun Home.startStatus(absen: String,ketam: String) {
    if (absen.equals("belum", true)) {

        Toast.makeText(
            this,
            "Anda Belum Absen",
            Toast.LENGTH_SHORT
        ).show()
    } else {
        val intent = Intent(this, Statuses::class.java)
        intent.putExtra("ketam", ketam)
        startActivity(intent)
        finish()
    }
}


fun Home.startAbsen(absen: String, target: Class<*>,ketam: String) {
    if (absen.equals("belum", true)) {

        val intent = Intent(this, target)
        intent.putExtra("ketam", ketam)
        startActivity(intent)
        finishAffinity()
    } else {
        Toast.makeText(
            this,
            absen,
            Toast.LENGTH_SHORT
        ).show()

    }
}



