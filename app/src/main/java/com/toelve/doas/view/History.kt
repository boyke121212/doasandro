package com.toelve.doas.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.toelve.doas.R
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityHistoryBinding
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.helper.Auto.autoSlide
import com.toelve.doas.helper.BeritaPagerAdapter
import com.toelve.doas.helper.HistoryAdapter
import com.toelve.doas.helper.HistoryModel
import com.toelve.doas.helper.RenderHtml.htmlPreviewClean
import com.toelve.doas.soasa.CryptoAES
import org.json.JSONObject

class History : Boyke(),
    BeritaPagerAdapter.OnBeritaChangeListener {
    var currentFilter = HashMap<String,String>()

    var selectedStatus = ""
    var selectedTanggal = ""
    lateinit var binding: ActivityHistoryBinding
    val listHistory = ArrayList<HistoryModel>()
    lateinit var adapter: HistoryAdapter
    var lastId = ""
    var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = HistoryAdapter(listHistory)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        binding.swipe.setOnRefreshListener {

            if (isLoading) {
                binding.swipe.isRefreshing = false
                return@setOnRefreshListener
            }

            lastId = ""
            listHistory.clear()
            adapter.notifyDataSetChanged()   // WAJIB

            loadHistory()

            binding.swipe.isRefreshing = false
        }

        binding.btDoas.setOnClickListener {
            startActivity(Intent(this, Doas::class.java))
            finish()
        }
        binding.btFilter.setOnClickListener {
             selectedStatus = ""
             selectedTanggal = ""
            val view = layoutInflater.inflate(R.layout.bottom_filter, null)
            val dialog = BottomSheetDialog(this)
            dialog.setContentView(view)
            dialog.show()

            val btApply = view.findViewById<Button>(R.id.btTerapkan)
            val btTanggal = view.findViewById<TextView>(R.id.btTanggal)


            val radios = listOf(
                view.findViewById<RadioButton>(R.id.rSemua),
                view.findViewById<RadioButton>(R.id.rHadir),
                view.findViewById<RadioButton>(R.id.rTerlambat),
                view.findViewById<RadioButton>(R.id.rTK),
                view.findViewById<RadioButton>(R.id.rLD),
                view.findViewById<RadioButton>(R.id.rCuti),
                view.findViewById<RadioButton>(R.id.rDIK),
                view.findViewById<RadioButton>(R.id.rBKO),
                view.findViewById<RadioButton>(R.id.rDinas),
                view.findViewById<RadioButton>(R.id.rSakit),
                view.findViewById<RadioButton>(R.id.rIzin)
            )

            radios.forEach { rb ->
                rb.setOnClickListener {
                    radios.forEach { it.isChecked = false }
                    rb.isChecked = true

                    selectedStatus = when (rb.id) {
                        R.id.rHadir -> "HADIR"
                        R.id.rSemua -> ""
                        R.id.rTerlambat -> "TERLAMBAT"
                        R.id.rTK -> "TK"
                        R.id.rLD -> "LD"
                        R.id.rCuti -> "CUTI"
                        R.id.rDIK -> "DIK"
                        R.id.rBKO -> "BKO"
                        R.id.rDinas -> "DINAS"
                        R.id.rSakit -> "SAKIT"
                        R.id.rIzin -> "IZIN"
                        else -> ""
                    }

                }
            }


            btTanggal.setOnClickListener {
                val dp = DatePickerDialog(this)
                dp.setOnDateSetListener { _, y, m, d ->
                    selectedTanggal = "%04d-%02d-%02d".format(y, m + 1, d)
                    btTanggal.text=selectedTanggal.toString()
                }
                dp.show()
            }

            btApply.setOnClickListener {

                if (isLoading) return@setOnClickListener

                currentFilter.clear()

                if (selectedStatus.isNotEmpty()) {
                    currentFilter["status"] = selectedStatus
                }

                if (selectedTanggal.isNotEmpty()) {
                    currentFilter["tanggal"] = selectedTanggal
                }

                lastId = ""
                listHistory.clear()
                adapter.notifyDataSetChanged()

                loadHistory(currentFilter)

                dialog.dismiss()
            }


        }
        binding.rvHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {

                if (dy <= 0) return

                val lm = rv.layoutManager as LinearLayoutManager
                if (!isLoading && lm.findLastVisibleItemPosition() >= listHistory.size - 2) {
                    loadHistory(currentFilter)
                }
            }
        })


        loadHistory()
        binding.btAbsen.setOnClickListener {
            startActivity(Intent(this@History, Home::class.java))
            finishAffinity()
        }
        onBackPressedDispatcher.addCallback(this) {
            startActivity(Intent(this@History, Home::class.java))
            finishAffinity()
        }

        binding.btStatus.setOnClickListener {
            startActivity(Intent(this@History, Statuses::class.java))
            finishAffinity()
        }
        binding.btInfo.setOnClickListener {
            startActivity(Intent(this@History, Berita::class.java))
            finishAffinity()
        }
    }



    private fun loadHistory(filterParams: Map<String,String>? = null) {

        if (isLoading) return
        isLoading = true

        val params = HashMap<String, String>()
        params["lastId"] = lastId
        filterParams?.let { params.putAll(it) }


        AuthManager(this, "api/sejarah").checkAuth(
            params = params,
            onLoading = { },
            onSuccess = { json ->
                val aesKey = json.getString("aes_key")
                val arr = json.getJSONArray("data")

                if (arr.length() == 0) {
                    isLoading = false
                    return@checkAuth
                }

                val startPos = listHistory.size

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)

                    val id = obj.getString("id")
                    val ket = decryptField(obj,"keterangan",aesKey)
                    val statusmasuk = decryptField(obj,"statusmasuk",aesKey)
                    listHistory.add(
                        HistoryModel(
                            id,
                            decryptField(obj,"masuk",aesKey),
                            decryptField(obj,"pulang",aesKey),
                            decryptField(obj,"selesai",aesKey),
                            ket,
                            decryptField(obj,"latitude",aesKey),
                            decryptField(obj,"longitude",aesKey),
                            decryptField(obj,"ketam",aesKey),
                            decryptField(obj,"tipeizin",aesKey),
                            decryptField(obj,"namapimpinan",aesKey),
                            decryptField(obj,"jabatan",aesKey),
                            decryptField(obj,"pangkat",aesKey),
                            decryptField(obj,"foto",aesKey),
                            decryptField(obj,"foto2",aesKey),
                            statusmasuk,
                            decryptField(obj,"statuspulang",aesKey),
                            decryptField(obj,"fotopulang",aesKey),
                            decryptField(obj,"fotopulang2",aesKey),
                            decryptField(obj,"subdit",aesKey),
                            decryptField(obj,"tanggal",aesKey),
                            decryptField(obj,"ketpul",aesKey)

                        )
                    )

                    lastId = id
                }

                adapter.notifyItemRangeInserted(startPos, arr.length())

                isLoading = false
            },
            onLogout = {
                isLoading = false
            }
        )
    }


    fun decryptField(obj: JSONObject, field: String, key: String): String {
        val value = obj.optString(field, "")
        return if (value.isNotEmpty() && value != "null") {
            CryptoAES.decrypt(value, key)
        } else {
            ""
        }
    }


    override fun onResume() {
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
        binding.tvIsi.text = htmlPreviewClean(berita.isi, 20)
    }

}
