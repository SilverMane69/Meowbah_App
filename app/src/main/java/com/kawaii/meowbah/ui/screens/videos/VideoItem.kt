package com.kawaii.meowbah.ui.screens.videos

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoItem(
    val id: String,
    val snippet: VideoSnippet,
    val statistics: VideoStatistics
    // Add other relevant properties here
) : Parcelable

@Parcelize
data class VideoSnippet(
    val title: String,
    val channelTitle: String,
    val localized: LocalizedSnippet, // Added for full structure, if needed
    val thumbnails: Thumbnails,
    val publishedAt: String? // Added: Make nullable for safety
) : Parcelable

// Represents localized title and description
@Parcelize
data class LocalizedSnippet(
    val title: String,
    val description: String
) : Parcelable

@Parcelize
data class Thumbnails(
    val default: Thumbnail,
    val medium: Thumbnail,
    val high: Thumbnail,
    val standard: Thumbnail? = null, // Optional, sometimes present
    val maxres: Thumbnail? = null    // Optional, sometimes present
) : Parcelable

@Parcelize
data class Thumbnail(
    val url: String,
    val width: Int,
    val height: Int
) : Parcelable

@Parcelize
data class VideoStatistics(
    val viewCount: String?, // CHANGED to nullable
    // Common statistics, add as needed
    val likeCount: String? = null,
    val dislikeCount: String? = null, // Note: dislikeCount is often not available anymore
    val favoriteCount: String? = null,
    val commentCount: String? = null
) : Parcelable

fun formatViewCount(count: Long): String {
    return when {
        count < 1000 -> count.toString()
        count < 1_000_000 -> String.format("%.1fK", count / 1000.0)
        count < 1_000_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        else -> String.format("%.1fB", count / 1_000_000_000.0)
    }
}
