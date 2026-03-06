package com.toelve.doas.helper

import android.text.Html

object RenderHtml {
    fun htmlPreviewClean(
        html: String?,
        maxWords: Int = 20
    ): String {
        if (html.isNullOrEmpty()) return ""

        // 1. Ubah entitas seperti &lt; menjadi < dan &amp; menjadi &
        val decoded = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()

        // 2. Buang semua tag HTML yang tersisa menggunakan Regex (paling ampuh)
        val noTags = decoded.replace(Regex("<[^>]*>"), "")

        // 3. Bersihkan &nbsp; dan whitespace berlebih
        val cleaned = noTags
            .replace("&nbsp;", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        // 4. Potong berdasarkan jumlah kata
        val words = cleaned.split(" ")

        return if (words.size <= maxWords) {
            cleaned
        } else {
            words.take(maxWords).joinToString(" ") + "..."
        }
    }
}
