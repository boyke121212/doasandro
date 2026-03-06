package com.toelve.doas.helper


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import coil.load
import com.toelve.doas.BuildConfig
import com.toelve.doas.R
import com.toelve.doas.soasa.DeviceSecurityHelper
import com.toelve.doas.view.BeritaItem

class BeritaPagerAdapter(
    private val context: Context,
    items: List<BeritaItem>,
    private val listener: OnBeritaChangeListener
) : PagerAdapter() {

    // 🛠️ BERSIHKAN SEMUA DATA DI AWAL (SEKALI PROSES)
    val items: List<BeritaItem> = items.map { item ->
        item.copy(
            judul = RenderHtml.htmlPreviewClean(item.judul, 50), // Judul max 50 kata/karakter sesuai fungsi
            isi = RenderHtml.htmlPreviewClean(item.isi, 20)      // Isi max 20 kata
        )
    }

    interface OnBeritaChangeListener {
        fun onBeritaChanged(berita: BeritaItem)
    }

    override fun getCount(): Int = items.size

    override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_pager_image, container, false)

        val img = view.findViewById<ImageView>(R.id.imgBanner)
        val berita = items[position]

        // LOAD IMAGE
        val token = SecurePrefs(context).getAccessToken()
        val url = BuildConfig.BASE_URL + "api/media/berita/" + berita.foto

        img.load(url) {
            placeholder(R.drawable.doas2)
            error(R.drawable.logodit)
            crossfade(true)

            addHeader("Authorization", "Bearer $token")
            addHeader("X-Device-Hash", DeviceSecurityHelper.getDeviceHash(context))
            addHeader("X-App-Signature", DeviceSecurityHelper.getAppSignatureHash(context))
        }

        // ⚠️ JANGAN panggil listener.onBeritaChanged di sini karena instantiateItem 
        // dipanggil untuk halaman yang belum terlihat (offscreen). 
        // Biarkan Activity yang menangani via onPageSelected.

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }
}
