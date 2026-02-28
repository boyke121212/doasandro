package com.toelve.doas.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toelve.doas.R
import com.toelve.doas.model.UserTerakhir

class UserTerakhirAdapter(private val items: List<UserTerakhir>) :
    RecyclerView.Adapter<UserTerakhirAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvDetail: TextView = view.findViewById(R.id.tvUserDetail)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_terakhir, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = items[position]
        holder.tvName.text = user.name ?: "-"
        holder.tvDetail.text = "${user.pangkat ?: "-"} | ${user.username ?: "-"} | ${user.jabatan ?: "-"} | ${user.subdit ?: "-"}"
        holder.tvTime.text = user.createdDtm ?: "-"
    }

    override fun getItemCount() = items.size
}
