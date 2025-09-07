package com.kawaii.meowbah.data

import android.net.Uri
import android.provider.BaseColumns

object VideoContentProviderContract {
    // Authority must be unique and is typically the package name
    const val AUTHORITY = "com.kawaii.meowbah.provider"

    // Base content URI
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

    // Path for the videos table
    const val PATH_VIDEOS = "videos"

    // Content URI for the videos table
    val CONTENT_URI_VIDEOS: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_VIDEOS).build()

    // MIME types
    const val CONTENT_TYPE_VIDEOS_DIR = "vnd.android.cursor.dir/$AUTHORITY.$PATH_VIDEOS"
    const val CONTENT_TYPE_VIDEOS_ITEM = "vnd.android.cursor.item/$AUTHORITY.$PATH_VIDEOS"

    // Column names for the videos table (matching CachedVideoInfo fields)
    object VideoColumns : BaseColumns { // Implements BaseColumns for _ID
        const val _ID = BaseColumns._ID // Explicitly define _ID
        const val TABLE_NAME = PATH_VIDEOS // Table name often matches path
        const val COLUMN_VIDEO_ID = "video_id" // Actual YouTube video ID
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_CACHED_THUMBNAIL_PATH = "cached_thumbnail_path"
        const val COLUMN_PUBLISHED_AT = "published_at"
    }
}
