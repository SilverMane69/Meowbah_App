package com.kawaii.meowbah.ui.screens.videos

data class PlaceholderVideoDetailItem(
    val id: String,
    val snippet: Snippet,
    val contentDetails: ContentDetails
) {
    data class Snippet(
        val title: String,
        val description: String,
        val thumbnails: Thumbnails,
        val channelTitle: String
    )

    data class Thumbnails(
        val default: Thumbnail,
        val medium: Thumbnail,
        val high: Thumbnail
    )

    data class Thumbnail(
        val url: String // Placeholder, assuming it just needs a URL
    )

    data class ContentDetails(
        val duration: String
    )
}
