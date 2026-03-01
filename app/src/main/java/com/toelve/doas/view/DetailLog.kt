package com.toelve.doas.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.toelve.doas.R
import com.toelve.doas.databinding.ActivityDetailLogBinding
import com.toelve.doas.helper.Auto
import com.toelve.doas.helper.Auto.loadingFoto

class DetailLog : AppCompatActivity() {

    private lateinit var binding: ActivityDetailLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Ambil data dari Intent
        val tanggal = intent.getStringExtra("tanggal") ?: ""
        val masuk = intent.getStringExtra("masuk") ?: ""
        val pulang = intent.getStringExtra("pulang") ?: ""
        val keterangan = intent.getStringExtra("keterangan") ?: ""
        val statusmasuk = intent.getStringExtra("statusmasuk") ?: ""
        val statuspulang = intent.getStringExtra("statuspulang") ?: ""
        val ketam = intent.getStringExtra("ketam") ?: ""
        val ketpul = intent.getStringExtra("ketpul") ?: ""
        val foto = intent.getStringExtra("foto") ?: ""
        val foto2 = intent.getStringExtra("foto2") ?: ""
        val fotopulang = intent.getStringExtra("fotopulang") ?: ""
        val fotopulang2 = intent.getStringExtra("fotopulang2") ?: ""
        val lat = intent.getStringExtra("lat") ?: ""
        val lng = intent.getStringExtra("lng") ?: ""
        val latpulang = intent.getStringExtra("latpulang") ?: ""
        val lonpulang = intent.getStringExtra("lonpulang") ?: ""
        val subdit = intent.getStringExtra("subdit") ?: ""
        val nip = intent.getStringExtra("nip") ?: ""
        val jabatan = intent.getStringExtra("jabatan") ?: ""
        val pangkat = intent.getStringExtra("pangkat") ?: ""
        val nama = intent.getStringExtra("nama") ?: ""

        // Tampilkan data
        binding.tvTanggal.text = tanggal
        binding.tvKet.text = if (keterangan.equals("DL", true)) "Dinas Luar" else keterangan
        binding.tvSubdit.text = "Nama : "+nama+"\nSubdit : "+subdit+"\nNIP : "+nip +"\n"+ "Jabatan : "+jabatan  +"\nPangkat : "+ pangkat
        binding.tvJam.text = "$masuk ($statusmasuk)"
        binding.tvKetam.text = ketam

        if (pulang.isNotEmpty()) {
            binding.tvPulang.text = "$pulang ($statuspulang)"
            binding.tvLabelKRP.visibility = View.VISIBLE
            binding.tvKepul.visibility = View.VISIBLE
            binding.tvKepul.text = ketpul
        } else {
            binding.tvPulang.text = "Belum Absen"
        }

        // Lokasi Masuk
        if (lat.isNotEmpty() && lng.isNotEmpty()) {
            binding.tvLat.text = "Koordinat: $lat, $lng"
            Auto.getAddressFromLatLng(this, lat.toDouble(), lng.toDouble()) { alamat ->
                binding.tvAlamat.text = alamat
            }
        }

        // Lokasi Pulang
        if (latpulang.isNotEmpty() && lonpulang.isNotEmpty()) {
            binding.tvLatPulang.text = "Koordinat: $latpulang, $lonpulang"
            Auto.getAddressFromLatLng(this, latpulang.toDouble(), lonpulang.toDouble()) { alamat ->
                binding.tvAlamatPulang.text = alamat
            }
        }

        // Foto Masuk
        if (foto.isNotEmpty()) {
            loadingFoto(this, binding.imgFoto, foto, tanggal)
        }
        if (foto2.isNotEmpty()) {
            loadingFoto(this, binding.imgFoto2, foto2, tanggal)
        }

        // Foto Pulang
        if (fotopulang.isNotEmpty()) {
            binding.layoutThumbPulang.visibility = View.VISIBLE
            loadingFoto(this, binding.imgFotoPulang, fotopulang, tanggal)
        }
        if (fotopulang2.isNotEmpty()) {
            binding.layoutThumbPulang.visibility = View.VISIBLE
            loadingFoto(this, binding.imgFotoPulang2, fotopulang2, tanggal)
        }
    }
}
