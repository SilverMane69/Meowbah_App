package com.kawaii.meowbah.ui.screens.videos

data class PlaceholderVideoDetailItem(
    val id: String,
    val snippet: Snippet,
    val contentDetails: ContentDetails,
    val statistics: Statistics? // This being nullable is good
) {
    data class Snippet(
        val title: String,
        val description: String,
        val thumbnails: Thumbnails,
        val channelTitle: String,
        val publishedAt: String 
    )

    data class Thumbnails(
        val default: Thumbnail,
        val medium: Thumbnail,
        val high: Thumbnail
    )

    data class Thumbnail(
        val url: String
    )

    data class ContentDetails(
        val duration: String
    )

    data class Statistics( 
        val viewCount: String?, // CHANGED to nullable
        val likeCount: String?  // CHANGED to nullable
    )
}
