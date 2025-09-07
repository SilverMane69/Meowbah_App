package com.kawaii.meowbah.data

// Main response object for youtube/v3/videos endpoint
data class YoutubeVideoDetailResponse(
    val items: List<VideoDetailApiItem>?
)

data class VideoDetailApiItem(
    val id: String?,
    val snippet: VideoDetailSnippet?,
    val contentDetails: VideoDetailContentDetails?,
    val statistics: VideoDetailStatistics?
)

data class VideoDetailSnippet(
    val publishedAt: String?,
    val channelId: String?,
    val title: String?,
    val description: String?,
    val thumbnails: VideoDetailThumbnails?,
    val channelTitle: String?
    // Add other snippet fields if needed, e.g., tags, categoryId
)

data class VideoDetailThumbnails(
    val default: VideoDetailThumbnail?,
    val medium: VideoDetailThumbnail?,
    val high: VideoDetailThumbnail?,
    val standard: VideoDetailThumbnail?,
    val maxres: VideoDetailThumbnail?
)

data class VideoDetailThumbnail(
    val url: String?,
    val width: Int?,
    val height: Int?
)

data class VideoDetailContentDetails(
    val duration: String?, // ISO 8601 duration, e.g., "PT15M33S"
    val dimension: String?,
    val definition: String?,
    val caption: String?,
    val licensedContent: Boolean?
    // Add other contentDetails fields if needed
)

data class VideoDetailStatistics(
    val viewCount: String?,
    val likeCount: String?,
    // val dislikeCount: String?, // Often unavailable
    val favoriteCount: String?, // Usually 0
    val commentCount: String?
)
