package com.toelve.doas.helper

import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.toelve.doas.R
import com.toelve.doas.helper.Auto.loadingFoto

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
    }

    fun updateData(newList: List<HistoryModel>) {

        val diff = DiffUtil.calculateDiff(
            HistoryDiff(list, newList)
        )

        list.clear()
        list.addAll(newList)

        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {

        val d = list[position]

        val context = holder.itemView.context

        holder.tvTanggal.text = d.tanggal
        holder.tvKetam.text = "Keterangan : "+d.ketam
        if(d.ketpul.isEmpty()){
            holder.tvKetpul.visibility= View.GONE
        }else{
            holder.tvKetpul.visibility= View.GONE
            holder.tvKetpul.text="Keterangan Pulang : "+d.ketpul
        }

        var ket = d.keterangan
        if (ket.equals("DL", true)) {
            ket = "Dinas Luar"
        }

        holder.tvStatus.text = ket

        val text = if (d.pulang.isEmpty()) {
            "Masuk : ${d.masuk}(${d.statusmasuk})\nPulang: -"
        } else {
            "Masuk : ${d.masuk}(${d.statusmasuk})\nPulang:${d.pulang}-${d.statuspulang}"
        }

        val spannable = SpannableString(text)

// kecilkan status masuk
        val statusMasukStart = text.indexOf(d.statusmasuk)
        if (statusMasukStart != -1) {
            spannable.setSpan(
                RelativeSizeSpan(0.8f), // 80% ukuran
                statusMasukStart,
                statusMasukStart + d.statusmasuk.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

// kecilkan status pulang (kalau ada)
        if (d.pulang.isNotEmpty()) {
            val statusPulangStart = text.indexOf(d.statuspulang)
            if (statusPulangStart != -1) {
                spannable.setSpan(
                    RelativeSizeSpan(0.8f),
                    statusPulangStart,
                    statusPulangStart + d.statuspulang.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        holder.tvJam.text = spannable
        loadingFoto(context, holder.ivFoto, d.foto, d.tanggal)
    }


    override fun getItemCount(): Int = list.size

    class HistoryDiff(
        private val old: List<HistoryModel>, private val new: List<HistoryModel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            return old[oldPos].id == new[newPos].id
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return old[oldPos] == new[newPos]
        }
    }

    override fun getItemId(position: Int): Long {
        return list[position].id.toLong()
    }
}
