package com.kawaii.meowbah.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CachedVideoInfo(
    val id: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?, // URL to the video thumbnail
    val publishedAt: String?
) : Parcelable
