package com.toelve.doas.helper

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toelve.doas.R
import com.toelve.doas.helper.Auto.loadingFoto
import com.toelve.doas.view.DetailLog

class HistoryAdapter(private val list: ArrayList<HistoryModel>) :
    RecyclerView.Adapter<HistoryAdapter.VH>() {
    init {
        setHasStableIds(true)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvJam: TextView = view.findViewById(R.id.tvJam)
        val tvKetam: TextView = view.findViewById(R.id.tvKetam)
        val tvKetpul: TextView = view.findViewById(R.id.tvKetpul)
        val ivFoto: ImageView = view.findViewById(R.id.ivFoto)
        val pembungkus: LinearLayout = view.findViewById(R.id.pembungkus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = list[position]
        val context = holder.itemView.context

        holder.tvTanggal.text = d.tanggal
        holder.tvKetam.text = "Keterangan : " + d.ketam
        
        if (d.ketpul.isEmpty()) {
            holder.tvKetpul.visibility = View.GONE
        } else {
            holder.tvKetpul.visibility = View.VISIBLE
            holder.tvKetpul.text = "Keterangan Pulang : " + d.ketpul
        }

        var ket = d.keterangan
        if (ket.equals("DL", true)) {
            ket = "Dinas Luar"
        }
        holder.tvStatus.text = ket

        val text = if (d.pulang.isEmpty()) {
            "Masuk : ${d.masuk}(${d.statusmasuk})\nPulang: -"
        } else {
            "Masuk : ${d.masuk}(${d.statusmasuk})\nPulang: ${d.pulang}(${d.statuspulang})"
        }

        val spannable = SpannableString(text)
        val statusMasukStart = text.indexOf("(${d.statusmasuk})")
        if (statusMasukStart != -1) {
            spannable.setSpan(RelativeSizeSpan(0.8f), statusMasukStart, statusMasukStart + d.statusmasuk.length + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (d.pulang.isNotEmpty()) {
            val statusPulangStart = text.indexOf("(${d.statuspulang})")
            if (statusPulangStart != -1) {
                spannable.setSpan(RelativeSizeSpan(0.8f), statusPulangStart, statusPulangStart + d.statuspulang.length + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        holder.tvJam.text = spannable
        loadingFoto(context, holder.ivFoto, d.foto, d.tanggal)

        // Click listener untuk buka DetailLog
        holder.pembungkus.setOnClickListener {
            val intent = Intent(context, DetailLog::class.java).apply {
                putExtra("tanggal", d.tanggal)
                putExtra("masuk", d.masuk)
                putExtra("pulang", d.pulang)
                putExtra("keterangan", d.keterangan)
                putExtra("statusmasuk", d.statusmasuk)
                putExtra("statuspulang", d.statuspulang)
                putExtra("ketam", d.ketam)
                putExtra("ketpul", d.ketpul)
                putExtra("foto", d.foto)
                putExtra("foto2", d.foto2)
                putExtra("fotopulang", d.fotopulang)
                putExtra("fotopulang2", d.fotopulang2)
                putExtra("lat", d.latitude)
                putExtra("lng", d.longitude)
                putExtra("latpulang", d.latpulang)
                putExtra("lonpulang", d.lonpulang)
                putExtra("subdit", d.subdit)
                putExtra("nip", d.nip)
                putExtra("pangkat", d.pangkat)
                putExtra("jabatan", d.jabatan)
                putExtra("nama", d.nama)

            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun getItemId(position: Int): Long {
        return list[position].id.hashCode().toLong()
    }
}
