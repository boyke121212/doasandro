package com.toelve.doas.helper

import android.os.Handler
import android.os.Looper
import android.text.Html
import androidx.viewpager.widget.ViewPager

object RenderHtml {
    fun htmlPreviewClean(
        html: String,
        maxWords: Int = 20
    ): String {

        // HTML → plain text
        val plainText = Html.fromHtml(
            html,
            Html.FROM_HTML_MODE_LEGACY
        ).toString()

        // 🔥 HILANGKAN BARIS KOSONG & SPASI BERLEBIH
        val cleaned = plainText
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        val words = cleaned.split("\\s+".toRegex())

        return if (words.size <= maxWords) {
            cleaned
        } else {
            words.take(maxWords).joinToString(" ") + "..."
        }
    }
}

