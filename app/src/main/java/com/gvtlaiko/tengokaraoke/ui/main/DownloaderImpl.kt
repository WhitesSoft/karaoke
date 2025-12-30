package com.gvtlaiko.tengokaraoke.ui.main

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloaderImpl private constructor(builder: OkHttpClient.Builder) : Downloader() {

    private val client: OkHttpClient = builder
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private var instance: DownloaderImpl? = null

        fun getInstance(): DownloaderImpl {
            if (instance == null) {
                instance = DownloaderImpl(OkHttpClient.Builder())
            }
            return instance!!
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = Request.Builder()
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
}