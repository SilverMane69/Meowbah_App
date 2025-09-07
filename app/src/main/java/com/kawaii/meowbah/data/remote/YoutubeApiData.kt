package com.kawaii.meowbah.data.remote

// Data Transfer Objects for YouTube API

data class YoutubeId(
    val kind: String?,
    val videoId: String?
)

data class YoutubeSnippet(
    val title: String?,
    val channelTitle: String?,
    val description: String?,
    val thumbnails: YoutubeThumbnails?,
    val publishedAt: String?
)

data class YoutubeThumbnails(
    val default: YoutubeThumbnail?,
    val medium: YoutubeThumbnail?,
    val high: YoutubeThumbnail?
    // val standard: YoutubeThumbnail?, // Add if needed
    // val maxres: YoutubeThumbnail?  // Add if needed
)

data class YoutubeThumbnail(
    val url: String?,
    val width: Int?, // Making these nullable for safety, though API usually provides them
    val height: Int?
)

data class YoutubeVideoItem(
    val id: YoutubeId?, // Changed from PlaceholderId to YoutubeId
    val snippet: YoutubeSnippet? // Changed from PlaceholderSnippet to YoutubeSnippet
)

data class YoutubeVideoListResponse(
    val items: List<YoutubeVideoItem> // Changed from PlaceholderYoutubeVideoItem
)
