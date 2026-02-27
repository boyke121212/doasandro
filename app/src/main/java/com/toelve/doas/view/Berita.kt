package com.toelve.doas.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.toelve.doas.R
import com.toelve.doas.boyi.Boyke
import com.toelve.doas.databinding.ActivityBeritaBinding
import com.toelve.doas.helper.AuthManager
import com.toelve.doas.helper.Auto.autoSlide
import com.toelve.doas.helper.BeritaAdapter
import com.toelve.doas.helper.BeritaModel
import com.toelve.doas.helper.BeritaPagerAdapter
import com.toelve.doas.helper.RenderHtml.htmlPreviewClean
import com.toelve.doas.soasa.CryptoAES

class Berita : Boyke(),
    BeritaPagerAdapter.OnBeritaChangeListener {
    private lateinit var binding: ActivityBeritaBinding
    val listBerita = ArrayList<BeritaModel>()
    lateinit var adapter: BeritaAdapter

    var lastId = ""
    var isLoading = false
    var isLastPage = false

    private var userScrolled = false
    private var lastLoadTime = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityBeritaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btAbsen.setOnClickListener {
            startActivity(Intent(this@Berita, Home::class.java))
            finishAffinity()
        }
        binding.btProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        onBackPressedDispatcher.addCallback(this) {
            val intent = Intent(this@Berita, Home::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        binding.btStatus.setOnClickListener {
            startActivity(Intent(this@Berita, Statuses::class.java))
            finishAffinity()
        }
        binding.btLog.setOnClickListener {
            startActivity(Intent(this@Berita, History::class.java))
            finishAffinity()
        }

        binding.btDoas.setOnClickListener {
            startActivity(Intent(this@Berita, Doas::class.java))
            finishAffinity()
        }


        adapter = BeritaAdapter(listBerita)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
        // scroll pagination
        binding.rvHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {

                if (dy <= 0) return
                if (binding.swipe.isRefreshing) return

                val lm = rv.layoutManager as LinearLayoutManager
                val total = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()

                if (!isLoading && !isLastPage && lastVisible >= total - 2) {
                    loadBerita()
                }
            }

        })
        binding.swipe.setOnRefreshListener {

            lastId = ""
            isLastPage = false

            listBerita.clear()
            adapter.notifyDataSetChanged()   // WAJIB

            loadBerita()

            binding.swipe.isRefreshing = false
        }


        loadBerita()

    }


    private fun loadBerita() {

        if (isLoading) return
        if (isLastPage) return

        isLoading = true
        val params = HashMap<String, String>()
        params["lastId"] = lastId


        AuthManager(this, "api/berita").checkAuth(
            params = params,
            onLoading = { },

            onSuccess = { json ->
                try {

                    val aesKey = json.getString("aes_key")
                    val jumlah = json.getInt("jumlah")
                    val arr = json.getJSONArray("data")


                    // kalau data habis
                    if (jumlah == 0) {
                        isLastPage = true
                        isLoading = false
                        return@checkAuth
                    }

                    val startPos = listBerita.size

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)

                        val id = obj.getString("id")

                        listBerita.add(
                            BeritaModel(
                                id,
                                CryptoAES.decrypt(obj.getString("judul"), aesKey),
                                CryptoAES.decrypt(obj.getString("isi"), aesKey),
                                obj.getString("tanggal"),
                                CryptoAES.decrypt(obj.getString("foto"), aesKey),
                                CryptoAES.decrypt(obj.getString("pdf"), aesKey)
                            )
                        )
                    }

                    // update lastId dari response
                    lastId = json.optString("last_id", lastId)

                    adapter.notifyItemRangeInserted(startPos, arr.length())

                } catch (e: Exception) {
                }

                isLoading = false
            },

            onLogout = {
                isLoading = false
            }
        )
    }





    override fun onResume() {
        AuthManager(this, "api/ambil_absen").checkAuth(
            onLoading = { loading ->
                if (loading) showLoading()
                else hideLoading()
            },

            onSuccess = { json ->
                val status = json.getString("status")
                if(status=="ok"){
                    try {
                        val aesKey = json.getString("aes_key")
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
                    } catch (e: Exception) {
                    }

                }

            }
            ,

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