package com.toelve.doas.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toelve.doas.R
import com.toelve.doas.model.SummarySubditData
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SummarySubditAdapter(private val items: Map<String, SummarySubditData>) :
    RecyclerView.Adapter<SummarySubditAdapter.ViewHolder>() {

    private val list = items.toList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvNamaSubdit)
        val tvDiajukan: TextView = view.findViewById(R.id.tvDiajukan)
        val tvTerserap: TextView = view.findViewById(R.id.tvTerserap)
        val tvPersen: TextView = view.findViewById(R.id.tvPersen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary_subdit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, data) = list[position]
        holder.tvNama.text = name
        holder.tvDiajukan.text = "Diajukan: ${formatNumber(data.diajukan)}"
        holder.tvTerserap.text = "Terserap: ${formatNumber(data.terserap)}"
        holder.tvPersen.text = "${String.format("%.2f", data.persen ?: 0.0)}%"
    }

    override fun getItemCount() = list.size

    private fun formatNumber(number: Double?): String {
        val symbols = DecimalFormatSymbols(Locale("id", "ID"))
        symbols.groupingSeparator = '.'
        val df = DecimalFormat("#,###", symbols)
        return df.format(number ?: 0.0)
    }
}
