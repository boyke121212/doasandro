package com.toelve.doas.view

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.toelve.doas.R
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btBack: ImageView

    private var renderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        imageView = findViewById(R.id.imagePdf)
        btBack = findViewById(R.id.btBack)

        btBack.setOnClickListener {
            finish()
        }

        val path = intent.getStringExtra("path") ?: return
        openPdf(File(path))
    }

    private fun openPdf(file: File) {
        try {
            if (!file.exists()) {
                return
            }

            val fileDescriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

            renderer = PdfRenderer(fileDescriptor)

            if (renderer!!.pageCount > 0) {
                showPage(0)
            }

        } catch (e: Exception) {
        }
    }

    private fun showPage(index: Int) {
        currentPage?.close()
        currentPage = renderer?.openPage(index)

        val width = currentPage!!.width * 2
        val height = currentPage!!.height * 2

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // background putih supaya teks kelihatan
        bitmap.eraseColor(Color.WHITE)

        currentPage!!.render(
            bitmap,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        imageView.setImageBitmap(bitmap)
    }

    override fun onDestroy() {
        currentPage?.close()
        renderer?.close()
        super.onDestroy()
    }
}
