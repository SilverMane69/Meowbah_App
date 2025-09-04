package com.kawaii.meowbah.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kawaii.meowbah.data.VideoContentProviderContract.VideoColumns

class VideoDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "meowbah_videos.db"
        private const val DATABASE_VERSION = 1

        // SQL statement to create the videos table
        private const val SQL_CREATE_VIDEOS_TABLE = """
            CREATE TABLE ${VideoColumns.TABLE_NAME} (
                ${VideoColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${VideoColumns.COLUMN_VIDEO_ID} TEXT UNIQUE NOT NULL,
                ${VideoColumns.COLUMN_TITLE} TEXT NOT NULL,
                ${VideoColumns.COLUMN_DESCRIPTION} TEXT,
                ${VideoColumns.COLUMN_CACHED_THUMBNAIL_PATH} TEXT,
                ${VideoColumns.COLUMN_PUBLISHED_AT} TEXT
            )
            """

        // SQL statement to delete the videos table (for upgrades or downgrades)
        private const val SQL_DELETE_VIDEOS_TABLE = "DROP TABLE IF EXISTS ${VideoColumns.TABLE_NAME}"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_VIDEOS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply discard the data and start over. 
        // More complex migration logic would be needed for user-generated data.
        db?.execSQL(SQL_DELETE_VIDEOS_TABLE)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}
