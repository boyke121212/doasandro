package com.toelve.doas.helper

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.toelve.doas.BuildConfig
import com.toelve.doas.R
import com.toelve.doas.helper.Auto.downloadPdfVolley
import com.toelve.doas.soasa.DeviceSecurityHelper
import com.toelve.doas.view.BeritaDetailActivity

class BeritaAdapter(
    private val list: ArrayList<BeritaModel>
) : RecyclerView.Adapter<BeritaAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvJudul: TextView = v.findViewById(R.id.tvNama)
        val tvIsi: TextView = v.findViewById(R.id.tvNo)
        val ivFoto: ImageView = v.findViewById(R.id.ivFoto)
        val pembungkus : LinearLayout = v.findViewById(R.id.pembungkus)
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

        // Click pembungkus untuk buka detail
        holder.pembungkus.setOnClickListener {
            val intent = Intent(context, BeritaDetailActivity::class.java).apply {
                putExtra("id", d.id)
                putExtra("judul", d.judul)
                putExtra("isi", d.isi)
                putExtra("tanggal", d.tanggal)
                putExtra("foto", d.foto)
                putExtra("pdf", d.pdf)
            }
            context.startActivity(intent)
        }

        if (d.pdf.isEmpty()) {
            holder.btDownload.visibility = View.GONE
        } else {
            holder.btDownload.visibility = View.VISIBLE
        }

        holder.btDownload.setOnClickListener {
            val tokenDownload = SecurePrefs.get(context).getAccessToken() ?: return@setOnClickListener
            val urlPdf = BuildConfig.BASE_URL + "api/media/pdf/" + d.pdf

            if (!Auto.isInternetAvailable(context)) {
                Toast.makeText(context, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            downloadPdfVolley(context, urlPdf, d.pdf, tokenDownload)
        }
    }
}
