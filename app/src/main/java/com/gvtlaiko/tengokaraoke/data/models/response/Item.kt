package com.gvtlaiko.tengokaraoke.data.models.response

data class Item(
    val etag: String,
    val id: Id,
    val kind: String,
    val snippet: Snippet
)