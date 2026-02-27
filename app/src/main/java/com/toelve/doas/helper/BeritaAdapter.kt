package com.toelve.doas.helper

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import com.toelve.doas.BuildConfig
import com.toelve.doas.R
import com.toelve.doas.helper.Auto.downloadPdfVolley
import com.toelve.doas.helper.Auto.downloadPdfWithAuth
import com.toelve.doas.soasa.DeviceSecurityHelper
import com.toelve.doas.view.Home
import com.toelve.doas.view.PdfViewerActivity

class BeritaAdapter(
    private val list: ArrayList<BeritaModel>
) : RecyclerView.Adapter<BeritaAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvJudul: TextView = v.findViewById(R.id.tvNama)
        val tvIsi: TextView = v.findViewById(R.id.tvNo)
        val ivFoto: ImageView = v.findViewById(R.id.ivFoto)
        val btDownload: Button = v.findViewById(R.id.btDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.rowberita, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {

        val d = list[position]
        val context = holder.itemView.context

        holder.tvJudul.text = d.judul
        holder.tvIsi.text = RenderHtml.htmlPreviewClean(d.isi)

        val token = SecurePrefs(context).getAccessToken()


        val url = BuildConfig.BASE_URL + "api/media/berita/" + d.foto

        holder.ivFoto.load(url) {
            placeholder(R.drawable.doas2)
            error(R.drawable.logodit)
            crossfade(true)

            addHeader("Authorization", "Bearer $token")
            addHeader("X-Device-Hash", DeviceSecurityHelper.getDeviceHash(context))
            addHeader("X-App-Signature", DeviceSecurityHelper.getAppSignatureHash(context))
        }


        if (d.pdf.isEmpty()) {
            holder.btDownload.visibility = View.GONE
        } else {
            holder.btDownload.visibility = View.VISIBLE
        }

        holder.btDownload.setOnClickListener {

            val context = holder.itemView.context
            val token = SecurePrefs.get(context).getAccessToken() ?: return@setOnClickListener

            val url = BuildConfig.BASE_URL + "api/media/pdf/" + d.pdf

            if (!Auto.isInternetAvailable(context)) {
                Toast.makeText(context, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            downloadPdfVolley(context, url, d.pdf, token)
        }

    }
}
