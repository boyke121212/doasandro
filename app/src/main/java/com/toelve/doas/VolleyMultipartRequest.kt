package com.toelve.doas


import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

abstract class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val errorListener: Response.ErrorListener
) : Request<NetworkResponse>(method, url, errorListener) {

    private val boundary = "apiclient-${System.currentTimeMillis()}"
    private val mimeType = "multipart/form-data;boundary=$boundary"

    override fun getBodyContentType(): String = mimeType

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return try {
            Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            Response.error(ParseError(e))
        }
    }

    override fun deliverResponse(response: NetworkResponse) {
        // di-handle manual di subclass
    }

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        try {
            // TEXT PARAMS
            params?.forEach { (key, value) ->
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes("Content-Disposition: form-data; name=\"$key\"\r\n\r\n")
                dos.writeBytes(value)
                dos.writeBytes("\r\n")
            }

            // FILE PARAMS
            getByteData().forEach { (key, dataPart) ->
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes(
                    "Content-Disposition: form-data; name=\"$key\"; filename=\"${dataPart.fileName}\"\r\n"
                )
                dos.writeBytes("Content-Type: ${dataPart.type}\r\n\r\n")
                dos.write(dataPart.content)
                dos.writeBytes("\r\n")
            }

            dos.writeBytes("--$boundary--\r\n")

        } catch (e: IOException) {
            throw AuthFailureError("Multipart error")
        }

        return bos.toByteArray()
    }

    abstract fun getByteData(): Map<String, DataPart>

    data class DataPart(
        val fileName: String,
        val content: ByteArray,
        val type: String = "image/jpeg"
    )
}
