package com.gvtlaiko.tengokaraoke.data.models.response

//data class Thumbnails(
//    val default: Default,
//    val high: High,
//    val medium: Medium
//)
data class Thumbnails(
    val high: Thumbnail,
    val medium: Thumbnail? = null,
    val default: Thumbnail? = null
)