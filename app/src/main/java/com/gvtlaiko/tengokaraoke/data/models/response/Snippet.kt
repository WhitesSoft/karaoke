package com.gvtlaiko.tengokaraoke.data.models.response

//data class Snippet(
//    val channelId: String,
//    val channelTitle: String,
//    val description: String,
//    val liveBroadcastContent: String,
//    val publishTime: String,
//    val publishedAt: String,
//    val thumbnails: Thumbnails,
//    val title: String
//)\
data class Snippet(val title: String, val thumbnails: Thumbnails, val channelTitle: String, val description: String)