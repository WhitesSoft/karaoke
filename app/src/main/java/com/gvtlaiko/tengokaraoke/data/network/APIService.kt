package com.gvtlaiko.tengokaraoke.data.network

import com.gvtlaiko.tengokaraoke.data.models.response.VideoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface APIService {

    @GET("search?part=snippet&type=video&key=AIzaSyBFaVSE-n5rgORlvwn8iMTSKPgCKB9c1R0&videoEmbeddable=true&safeSearch=strict&regionCode=BO")
    suspend fun getVideos(
        @Query("q") busquedaUsuario: String,
        @Query("maxResults") maxResults: Int
    ): Response<VideoResponse>

}