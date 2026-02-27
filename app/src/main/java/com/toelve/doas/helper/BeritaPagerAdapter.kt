package com.toelve.doas.helper


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import coil.load
import coil.request.ImageRequest
import com.toelve.doas.BuildConfig
import com.toelve.doas.R
import com.toelve.doas.soasa.DeviceSecurityHelper
import com.toelve.doas.view.BeritaItem

class BeritaPagerAdapter(
    private val context: Context,
    private val items: List<BeritaItem>,
    private val listener: OnBeritaChangeListener
) : PagerAdapter() {

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

        // LOAD IMAGE (kode kamu tetap)
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

        // 🔔 KIRIM DATA KE ACTIVITY (SAAT ITEM DIBUAT)
        listener.onBeritaChanged(berita)

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }
}
