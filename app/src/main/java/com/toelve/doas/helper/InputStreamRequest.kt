package com.toelve.doas.helper

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayInputStream
import java.io.InputStream

class InputStreamRequest(
    method: Int,
    url: String,
    private val listener: Response.Listener<InputStream>,
    errorListener: Response.ErrorListener,
    private val headers: Map<String, String>? = null
) : Request<InputStream>(method, url, errorListener) {

    override fun parseNetworkResponse(response: NetworkResponse): Response<InputStream> {
        return Response.success(
            ByteArrayInputStream(response.data),
            HttpHeaderParser.parseCacheHeaders(response)
        )
    }

    override fun deliverResponse(response: InputStream) {
        listener.onResponse(response)
    }

    override fun getHeaders(): MutableMap<String, String> {
        return headers?.toMutableMap() ?: HashMap()
    }
}
