package com.toelve.doas.helper

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.viewpager.widget.ViewPager
import coil.load
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.toelve.doas.BuildConfig
import com.toelve.doas.R
import com.toelve.doas.databinding.ActivityStatusesBinding
import com.toelve.doas.soasa.CryptoAES
import com.toelve.doas.soasa.DeviceSecurityHelper
import com.toelve.doas.view.AbsenPulang
import com.toelve.doas.view.Statuses
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import kotlin.text.equals

object Auto {
    fun autoSlide(viewPager: ViewPager, size: Int) {
        val handler = Handler(Looper.getMainLooper())
        var index = 0

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (size == 0) return
                index = (index + 1) % size
                viewPager.currentItem = index
                handler.postDelayed(this, 4000)
            }
        }, 4000)
    }

    fun downloadPdfWithAuth(
        context: Context,
        url: String,
        fileName: String
    ) {
        Thread {

            try {
                val token = SecurePrefs.get(context).getAccessToken()

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty(
                    "X-Device-Hash",
                    DeviceSecurityHelper.getDeviceHash(context)
                )
                connection.setRequestProperty(
                    "X-App-Signature",
                    DeviceSecurityHelper.getAppSignatureHash(context)
                )

                connection.connect()

                val input = BufferedInputStream(connection.inputStream)

                val file = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ),
                    fileName
                )

                val output = FileOutputStream(file)

                val buffer = ByteArray(1024)
                var count: Int

                while (input.read(buffer).also { count = it } != -1) {
                    output.write(buffer, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "PDF tersimpan di Download", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Download gagal", Toast.LENGTH_LONG).show()
                }
            }

        }.start()
    }

    fun downloadPdfVolley(
        context: Context,
        url: String,
        fileName: String,
        accessToken: String
    ) {

        // CEK INTERNET DULU
        if (!Auto.isInternetAvailable(context)) {
            Toast.makeText(context, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
            return
        }

        val request = object : Request<ByteArray>(
            Method.GET,
            url,
            Response.ErrorListener { error ->

                val message = when (error) {
                    is com.android.volley.NoConnectionError -> "Tidak ada koneksi internet"
                    is com.android.volley.TimeoutError -> "Koneksi timeout"
                    is com.android.volley.AuthFailureError -> "Akses ditolak"
                    is com.android.volley.ServerError -> "Server error"
                    else -> "Download gagal"
                }

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        ) {

            override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray> {
                return Response.success(
                    response.data,
                    HttpHeaderParser.parseCacheHeaders(response)
                )
            }

            override fun deliverResponse(data: ByteArray) {

                if (data.isEmpty()) {
                    Toast.makeText(context, "File kosong atau gagal diunduh", Toast.LENGTH_SHORT)
                        .show()
                    return
                }

                try {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                        // =========================
                        // ANDROID 10+
                        // =========================
                        val resolver = context.contentResolver

                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }

                        val uri = resolver.insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            values
                        )

                        if (uri != null) {

                            resolver.openOutputStream(uri).use { outputStream ->
                                outputStream?.write(data)
                            }

                            values.clear()
                            values.put(MediaStore.Downloads.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)

                            openPdf(context, uri)

                        } else {
                            Toast.makeText(context, "Gagal membuat file", Toast.LENGTH_SHORT).show()
                        }

                    } else {

                        // =========================
                        // ANDROID 9 KE BAWAH
                        // =========================
                        val dir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        )

                        if (!dir.exists()) dir.mkdirs()

                        val file = File(dir, fileName)

                        val fos = FileOutputStream(file)
                        fos.write(data)
                        fos.flush()
                        fos.close()

                        val uri = FileProvider.getUriForFile(
                            context,
                            context.packageName + ".provider",
                            file
                        )

                        openPdf(context, uri)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                }
            }

            override fun getHeaders(): MutableMap<String, String> {

                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val nonce = UUID.randomUUID().toString()

                return hashMapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Accept" to "application/json",
                    "X-Device-Hash" to DeviceSecurityHelper.getDeviceHash(context),
                    "X-App-Signature" to DeviceSecurityHelper.getAppSignatureHash(context),
                    "Platform" to "android",
                    "X-Request-Timestamp" to timestamp,
                    "X-Request-Nonce" to nonce
                )
            }
        }

        // TIMEOUT + RETRY
        request.retryPolicy = DefaultRetryPolicy(
            15000, // 15 detik
            1,     // retry 1x
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        VolleySingleton.getInstance(context).addToRequestQueue(request)
    }


    fun openPdf(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            Toast.makeText(context, "PDF berhasil didownload", Toast.LENGTH_LONG).show()

            context.startActivity(Intent.createChooser(intent, "Buka PDF dengan"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi untuk membuka PDF", Toast.LENGTH_LONG)
                .show()
        }
    }

    fun cleanTempPhotos(context: Context) {

        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "DOAS"
        )

        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                try {
                    file.delete()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun Statuses.renderAbsensi(
        binding: ActivityStatusesBinding,
        obj: JSONObject,
        aesKey: String,
        batpul: String?,
        jamserver: String?
    ) {

        resetView(binding)

        val tanggal = obj.dec("tanggal", aesKey)
        val selesai = obj.dec("selesai", aesKey)
        val masuk = obj.dec("masuk", aesKey)
        val pulang = obj.dec("pulang", aesKey)
        val ket = obj.dec("keterangan", aesKey).uppercase()
        val lat = obj.dec("latitude", aesKey)
        val lng = obj.dec("longitude", aesKey)
        val ketam = obj.dec("ketam", aesKey)
        val tipeizin = obj.dec("tipeizin", aesKey)
        val namaPimpinan = obj.dec("namapimpinan", aesKey)
        val jabatan = obj.dec("jabatan", aesKey)
        val pangkat = obj.dec("pangkat", aesKey)
        val foto = obj.dec("foto", aesKey)
        val foto2 = obj.dec("foto2", aesKey)
        val fotopulang = obj.dec("fotopulang2", aesKey)
        val fotopulang2 = obj.dec("fotopulang2", aesKey)
        val latpulang = obj.dec("latpulang", aesKey)
        val lonpulang = obj.dec("lonpulang", aesKey)
        val statuspulang = obj.dec("statuspulang", aesKey)
        val ketpul = obj.dec("ketpul", aesKey)
        val subdit = obj.dec("subdit", aesKey)
        val statusmasuk = obj.dec("statusmasuk", aesKey)

        binding.cardStatusHariIni.visibility = View.VISIBLE
        binding.tvTanggal.text = tanggal
        binding.tvKet.text = ket


        binding.tvJam.text = masuk + " (" + statusmasuk + ")"



        binding.tvKetam.text = ketam
        binding.tvKetam.visibility = View.VISIBLE
        binding.tvLabelKR.visibility = View.VISIBLE

        if (lat.isNotEmpty() && lng.isNotEmpty()) {

            binding.tvLat.visibility = View.VISIBLE
            binding.tvLat.text = "Latitude: $lat | Longitude: $lng"

            binding.tvAlamat.visibility = View.VISIBLE
            binding.tvAlamat.text = "Mencari alamat..."

            getAddressFromLatLng(
                binding.root.context,
                lat.toDouble(),
                lng.toDouble()
            ) { alamat ->

                binding.tvAlamat.text = alamat
            }
        }



        if (foto.isNotEmpty()) {
            binding.imgFoto.visibility = View.VISIBLE
            loadFoto(binding, binding.imgFoto, foto, tanggal)


        }
        if (foto2.isNotEmpty()) {
            binding.imgFoto2.visibility = View.VISIBLE
            loadFoto(binding, binding.imgFoto2, foto2, tanggal)
        }



        if (subdit.isNotEmpty()) {
            binding.tvSubdit.visibility = View.VISIBLE
            binding.tvSubdit.text = "($subdit)"
        }


        // kalau sudah pulang
        if (pulang.isNotBlank()) {
            binding.tvPulang.text = pulang + " (" + statuspulang + ")"
            if (fotopulang.isNotEmpty()) {
                binding.imgFotoPulang.visibility = View.VISIBLE
                loadFoto(binding, binding.imgFotoPulang, fotopulang, tanggal)
            }
            if (fotopulang2.isNotEmpty()) {
                binding.imgFotoPulang2.visibility = View.VISIBLE
                loadFoto(binding, binding.imgFotoPulang2, fotopulang2, tanggal)
            }
            if (latpulang.isNotEmpty() && lonpulang.isNotEmpty()) {

                binding.tvLatPulang.visibility = View.VISIBLE
                binding.tvLatPulang.text = "Latitude: $latpulang | Longitude: $lonpulang"

                binding.tvAlamatPulang.visibility = View.VISIBLE
                binding.tvAlamatPulang.text = "Mencari alamat..."

                getAddressFromLatLng(
                    binding.root.context,
                    latpulang.toDouble(),
                    lonpulang.toDouble()
                ) { alamat ->

                    binding.tvAlamatPulang.text = alamat
                }
            }
            binding.tvLabelR.visibility = View.VISIBLE
            binding.btnPulang.visibility = View.GONE
            binding.tvPulang.visibility = View.VISIBLE
            binding.tvKepul.visibility = View.VISIBLE
            binding.tvKepul.text = ketpul
            binding.tvLabelKRP.visibility = View.VISIBLE
        } else {
            binding.tvLabelR.visibility = View.GONE
            binding.tvLabelKRP.visibility = View.GONE
            binding.tvKepul.visibility = View.GONE
            binding.tvLatPulang.visibility = View.GONE
            binding.tvAlamatPulang.visibility = View.GONE
            binding.imgFotoPulang2.visibility = View.GONE
            binding.imgFotoPulang.visibility = View.GONE
            binding.tvLabelR.visibility = View.GONE
            binding.btnPulang.visibility = View.VISIBLE
        }
        if (ket.equals("DL", true)) {
            binding.tvKet.text = "Dinas Luar"
        }
        binding.btnPulang.setOnClickListener {
            if (batpul.isNullOrEmpty()) {
                Toast.makeText(this, "Gagal Ambil Data", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, AbsenPulang::class.java)
                intent.putExtra("bataspulang", batpul)
                intent.putExtra("jamserver", jamserver)
                intent.putExtra("ket", ket)
                startActivity(intent)
                finish()
            }

        }
    }


    // =========================
// RESET VIEW
// =========================
    fun resetView(b: ActivityStatusesBinding) {

        b.tvLat.visibility = View.GONE
        b.tvLabelR.visibility = View.GONE
        b.tvLabelR.visibility = View.GONE
        b.tvLabelKR.visibility = View.GONE
        b.tvKetam.visibility = View.GONE
        b.tvPulang.visibility = View.GONE
        b.layoutThumbPulang.visibility = View.GONE
        b.tvLatPulang.visibility = View.GONE
        b.tvLabelKR.visibility = View.GONE
        b.tvKetam.visibility = View.GONE
        b.imgFoto.visibility = View.GONE
        b.btnPulang.visibility = View.GONE
        b.tvSubdit.visibility = View.GONE
        b.tvPulang.visibility = View.GONE
        b.tvKepul.visibility = View.GONE
        b.cardStatusHariIni.visibility = View.GONE
    }

    // =========================
// LOAD FOTO
// =========================
    fun Statuses.loadFoto(
        binding: ActivityStatusesBinding,
        img: ImageView,
        file: String,
        tanggal: String   // format: 2026-02-19
    ) {

        val token = SecurePrefs(this).getAccessToken()

        // 2026-02-19 → 2026/02/19
        val folderPath = tanggal.replace("-", "/")

        val fullPath = "$folderPath/$file"

        // WAJIB encode karena ada slash
        val encodedPath = java.net.URLEncoder.encode(fullPath, "UTF-8")

        val url = BuildConfig.BASE_URL +
                "api/media/absensi?file=" + encodedPath


        img.load(url) {
            placeholder(R.drawable.doas2)
            error(R.drawable.logodit)
            crossfade(true)

            addHeader("Authorization", "Bearer $token")
            addHeader("X-Device-Hash", DeviceSecurityHelper.getDeviceHash(this@loadFoto))
            addHeader("X-App-Signature", DeviceSecurityHelper.getAppSignatureHash(this@loadFoto))
        }

        img.setOnClickListener {
            val intent = Intent(this, PreviewFotoActivity::class.java)
            intent.putExtra("foto_uri", url)
            startActivity(intent)

        }
    }

    fun loadingFoto(
        context: Context,
        img: ImageView,
        file: String,
        tanggal: String   // format: 2026-02-19
    ) {

        val token = SecurePrefs(context).getAccessToken()

        // 2026-02-19 → 2026/02/19
        val folderPath = tanggal.replace("-", "/")

        val fullPath = "$folderPath/$file"

        // WAJIB encode karena ada slash
        val encodedPath = java.net.URLEncoder.encode(fullPath, "UTF-8")

        val url = BuildConfig.BASE_URL +
                "api/media/absensi?file=" + encodedPath


        img.load(url) {

            placeholder(R.drawable.doas2)
            error(R.drawable.logodit)
            crossfade(true)

            addHeader("Authorization", "Bearer $token")
            addHeader("X-Device-Hash", DeviceSecurityHelper.getDeviceHash(context))
            addHeader("X-App-Signature", DeviceSecurityHelper.getAppSignatureHash(context))
        }

        img.setOnClickListener {
            val intent = Intent(context, PreviewFotoActivity::class.java)
            intent.putExtra("foto_uri", url)
            context.startActivity(intent)
        }
    }

    // =========================
// HELPER DECRYPT JSON
// =========================
    fun JSONObject.dec(field: String, key: String): String {
        val v = optString(field)
        return if (v.isNotEmpty() && v != "null") CryptoAES.decrypt(v, key) else ""
    }


    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getAddressFromLatLng(
        context: Context,
        lat: Double,
        lon: Double,
        callback: (String) -> Unit
    ) {

        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            geocoder.getFromLocation(lat, lon, 1) { addresses ->

                val address = if (addresses.isNotEmpty()) {
                    addresses[0].getAddressLine(0)
                } else {
                    "Alamat tidak ditemukan"
                }

                callback(address ?: "-")
            }

        } else {

            // fallback untuk Android lama (SDK 21 Anda masih aman)
            try {
                val addresses = geocoder.getFromLocation(lat, lon, 1)

                val address = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else {
                    "Alamat tidak ditemukan"
                }

                callback(address ?: "-")

            } catch (e: Exception) {
                callback("Gagal mengambil alamat")
            }
        }
    }

}

