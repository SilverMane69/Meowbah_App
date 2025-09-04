package com.kawaii.meowbah.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CachedVideoInfo(
    val id: String,
    val title: String,
    val description: String?,
    val cachedThumbnailPath: String?, // Local file path to the cached thumbnail
    val publishedAt: String?
) : Parcelable
