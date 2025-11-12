package com.gvtlaiko.tengokaraoke.data.models.response

data class VideoResponse(
    val etag: String,
    val items: List<Item>,
    val kind: String,
    val nextPageToken: String,
    val pageInfo: PageInfo,
    val regionCode: String
)