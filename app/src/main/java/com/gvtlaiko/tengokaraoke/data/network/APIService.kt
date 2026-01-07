package com.gvtlaiko.tengokaraoke.data.network

import com.gvtlaiko.tengokaraoke.data.models.response.VideoResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface APIService {

    // ya no se usa
    @GET("search?part=snippet&type=video&videoEmbeddable=true&safeSearch=strict&videoCategoryId=10")
    suspend fun getVideos(
        @Query("q") busquedaUsuario: String,
        @Query("maxResults") maxResults: Int,
        @Query("key") apikey: String
    ): Response<VideoResponse>

    @GET
    suspend fun getAutocomplete(
        @Url url: String = "http://suggestqueries.google.com/complete/search?client=firefox&ds=yt",
        @Query("q") query: String
    ): ResponseBody

}