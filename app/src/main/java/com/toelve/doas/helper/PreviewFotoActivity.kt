package com.toelve.doas.helper

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.ImageButton
import com.toelve.doas.R
import com.toelve.doas.boyi.Boyke
import coil.imageLoader
import coil.request.ImageRequest
import com.github.chrisbanes.photoview.PhotoView
import com.toelve.doas.soasa.DeviceSecurityHelper
import androidx.core.net.toUri

class PreviewFotoActivity : Boyke() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_foto)

        val photoView = findViewById<PhotoView>(R.id.photoView)
        val btTutup = findViewById<ImageButton>(R.id.btTutup)

        val uriString = intent.getStringExtra("foto_uri")

        uriString?.let { data ->
            if (data.startsWith("http")) {
                val token = SecurePrefs(this).getAccessToken()

                val request = ImageRequest.Builder(this)
                    .data(data)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader(
                        "X-Device-Hash",
                        DeviceSecurityHelper.getDeviceHash(this)
                    )
                    .addHeader(
                        "X-App-Signature",
                        DeviceSecurityHelper.getAppSignatureHash(this)
                    )
                    .target { drawable ->
                        val bitmap = (drawable as BitmapDrawable).bitmap
                        photoView.setImageBitmap(bitmap)
                    }
                    .build()

                imageLoader.enqueue(request)
            } else {
                // Untuk URI lokal (file:// atau content://)
                photoView.setImageURI(data.toUri())
            }
        }

        btTutup.setOnClickListener {
            finish()
        }
    }
}
