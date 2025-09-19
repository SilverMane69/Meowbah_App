package com.kawaii.meowbah.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss_posts")
data class RssPost(
    @PrimaryKey
    val guid: String,
    val title: String,
    val link: String,
    val pubDateEpochSeconds: Long, // Store as epoch seconds for sorting
    val description: String,
    val imageUrl: String?
)
