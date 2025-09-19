package com.kawaii.meowbah.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kawaii.meowbah.data.model.RssPost
import kotlinx.coroutines.flow.Flow

@Dao
interface RssPostDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPost(post: RssPost)

    @Query("SELECT * FROM rss_posts ORDER BY pubDateEpochSeconds DESC")
    fun getAllPosts(): Flow<List<RssPost>>

    @Query("SELECT * FROM rss_posts WHERE guid = :guid")
    suspend fun getPostByGuid(guid: String): RssPost?
}
