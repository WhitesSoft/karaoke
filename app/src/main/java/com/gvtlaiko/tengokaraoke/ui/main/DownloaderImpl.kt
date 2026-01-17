package com.gvtlaiko.tengokaraoke.ui.main

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloaderImpl private constructor(builder: OkHttpClient.Builder) : Downloader() {

    companion object {
        private var instance: DownloaderImpl? = null

        fun getInstance(): DownloaderImpl {
            if (instance == null) {
                instance = DownloaderImpl(OkHttpClient.Builder())
            }
            return instance!!
        }
    }

    val client: OkHttpClient = builder
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .url(url)
            .method(httpMethod, dataToSend?.toRequestBody())

        headers.forEach { (key, list) ->
            list.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body?.string(),
            response.request.url.toString()
        )
    }

    // para convertir byte[] a RequestBody
    private fun ByteArray.toRequestBody(): RequestBody {
        return RequestBody.create(null, this)
    }
}