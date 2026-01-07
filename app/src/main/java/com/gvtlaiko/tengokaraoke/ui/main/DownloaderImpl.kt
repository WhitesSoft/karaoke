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

        // Añadir cabeceras que pide NewPipe
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

    // Método helper para convertir byte[] a RequestBody si es necesario
    private fun ByteArray.toRequestBody(): RequestBody {
        return RequestBody.create(null, this)
    }

//    companion object {
//        const val USER_AGENT =
//            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
//
//        private var instance: DownloaderImpl? = null
//
//        fun getInstance(): DownloaderImpl {
//            if (instance == null) {
//                instance = DownloaderImpl(OkHttpClient.Builder())
//            }
//            return instance!!
//        }
//    }
//
//    val client: OkHttpClient = builder
//        .readTimeout(30, TimeUnit.SECONDS)
//        .connectTimeout(30, TimeUnit.SECONDS)
//        .build()
//
//    // Cliente ESPECÍFICO para ExoPlayer con headers forzados
//    fun getClientForExoPlayer(): OkHttpClient {
//        return client.newBuilder()
//            .addInterceptor { chain ->
//                val original = chain.request()
//                val request = original.newBuilder()
//                    .header("User-Agent", USER_AGENT)
//                    .header("Accept", "*/*")
//                    .header("Accept-Language", "es-US,es;q=0.9,en;q=0.8")
//                    .header("Accept-Encoding", "identity") // ⭐ IMPORTANTE
//                    .header("Range", "bytes=0-") // ⭐ IMPORTANTE para streaming
//                    .removeHeader("Transfer-Encoding") // Evita problemas
//                    .build()
//
//                Log.i("DownloaderImpl", "✅ Headers aplicados: ${request.headers}")
//                chain.proceed(request)
//            }
//            .followRedirects(true) // ⭐ Seguir redirects automáticamente
//            .followSslRedirects(true)
//            .build()
//    }
//
//    @Throws(IOException::class, ReCaptchaException::class)
//    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
//        val httpMethod = request.httpMethod()
//        val url = request.url()
//        val headers = request.headers()
//        val dataToSend = request.dataToSend()
//
//        val requestBuilder = Request.Builder()
//            .url(url)
//            .method(httpMethod, dataToSend?.toRequestBody())
//
//        headers.forEach { (key, list) ->
//            list.forEach { value ->
//                requestBuilder.addHeader(key, value)
//            }
//        }
//
//        requestBuilder.header("User-Agent", USER_AGENT)
//        requestBuilder.header("Accept-Language", "es-US,es;q=0.9")
//
//        val response = client.newCall(requestBuilder.build()).execute()
//
//        if (response.code == 429) {
//            response.close()
//            throw ReCaptchaException("reCaptcha Challenge requested", url)
//        }
//
//        return Response(
//            response.code,
//            response.message,
//            response.headers.toMultimap(),
//            response.body?.string(),
//            response.request.url.toString()
//        )
//    }
}

//class DownloaderImpl private constructor(builder: OkHttpClient.Builder) : Downloader() {
//
//    companion object {
//
//        const val USER_AGENT =
//            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
//
//        private var instance: DownloaderImpl? = null
//
//        fun getInstance(): DownloaderImpl {
//            if (instance == null) {
//                instance = DownloaderImpl(OkHttpClient.Builder())
//            }
//            return instance!!
//        }
//    }
//
//    val client: OkHttpClient = builder
//        .readTimeout(30, TimeUnit.SECONDS)
//        .build()
//
//    fun getClientForExoPlayer(): OkHttpClient {
//        return client
//    }
//
//    @Throws(IOException::class, ReCaptchaException::class)
//    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
//        val httpMethod = request.httpMethod()
//        val url = request.url()
//        val headers = request.headers()
//        val dataToSend = request.dataToSend()
//
//        val requestBuilder = Request.Builder()
//            .url(url)
//            .method(httpMethod, dataToSend?.toRequestBody())
//
//        headers.forEach { (key, list) ->
//            list.forEach { value ->
//                requestBuilder.addHeader(key, value)
//            }
//        }
//
//        requestBuilder.header("User-Agent", USER_AGENT)
//        requestBuilder.header("Referer", "https://www.youtube.com/")
//
//        val response = client.newCall(requestBuilder.build()).execute()
//
//        if (response.code == 429) {
//            response.close()
//            throw ReCaptchaException("reCaptcha Challenge requested", url)
//        }
//
//        return Response(
//            response.code,
//            response.message,
//            response.headers.toMultimap(),
//            response.body?.string(),
//            response.request.url.toString()
//        )
//    }
//}