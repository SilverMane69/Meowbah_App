package com.kawaii.meowbah.data

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import com.kawaii.meowbah.data.VideoContentProviderContract.AUTHORITY
import com.kawaii.meowbah.data.VideoContentProviderContract.CONTENT_TYPE_VIDEOS_DIR
import com.kawaii.meowbah.data.VideoContentProviderContract.CONTENT_TYPE_VIDEOS_ITEM
import com.kawaii.meowbah.data.VideoContentProviderContract.PATH_VIDEOS
import com.kawaii.meowbah.data.VideoContentProviderContract.VideoColumns

class VideoContentProvider : ContentProvider() {

    private lateinit var dbHelper: VideoDatabaseHelper

    companion object {
        private const val TAG = "VideoContentProvider"

        private const val VIDEOS = 100 // URI Matcher code for all videos
        private const val VIDEO_ID = 101 // URI Matcher code for a single video by its DB _ID

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_VIDEOS, VIDEOS)
            addURI(AUTHORITY, "$PATH_VIDEOS/#", VIDEO_ID) // For specific item by _ID
        }
    }

    override fun onCreate(): Boolean {
        dbHelper = VideoDatabaseHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor?
        val match = uriMatcher.match(uri)

        cursor = when (match) {
            VIDEOS -> {
                db.query(
                    VideoColumns.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null, // groupBy
                    null, // having
                    sortOrder ?: "${VideoColumns.COLUMN_PUBLISHED_AT} DESC" // Default sort by published_at
                )
            }
            VIDEO_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(
                    VideoColumns.TABLE_NAME,
                    projection,
                    "${VideoColumns._ID}=?",
                    arrayOf(id.toString()),
                    null,
                    null,
                    sortOrder
                )
            }
            else -> {
                Log.e(TAG, "Query: Unknown URI: $uri")
                null
            }
        }
        // Set notification URI on the Cursor, so it knows what content URI to watch for changes
        cursor?.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            VIDEOS -> CONTENT_TYPE_VIDEOS_DIR
            VIDEO_ID -> CONTENT_TYPE_VIDEOS_ITEM
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val match = uriMatcher.match(uri)
        var insertedId: Long = -1

        when (match) {
            VIDEOS -> {
                // Allow null column hack for cases like empty ContentValues for autoincrement ID
                insertedId = db.insertWithOnConflict(VideoColumns.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            else -> {
                Log.e(TAG, "Insert: Operation not supported for URI: $uri")
                return null // Or throw IllegalArgumentException
            }
        }

        return if (insertedId > 0) {
            val insertedUri = ContentUris.withAppendedId(VideoContentProviderContract.CONTENT_URI_VIDEOS, insertedId)
            context!!.contentResolver.notifyChange(uri, null) // Notify observers of the original URI
            Log.d(TAG, "Inserted row with ID $insertedId, URI: $insertedUri")
            insertedUri
        } else {
            Log.e(TAG, "Insert: Failed to insert row for URI: $uri")
            null
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        val match = uriMatcher.match(uri)
        val rowsDeleted: Int

        rowsDeleted = when (match) {
            VIDEOS -> {
                // If selection is null, this deletes all rows
                db.delete(VideoColumns.TABLE_NAME, selection, selectionArgs)
            }
            VIDEO_ID -> {
                val id = ContentUris.parseId(uri)
                val newSelection = "${VideoColumns._ID}=?"
                val newSelectionArgs = arrayOf(id.toString())
                db.delete(VideoColumns.TABLE_NAME, newSelection, newSelectionArgs)
            }
            else -> {
                Log.e(TAG, "Delete: Operation not supported for URI: $uri")
                0 // Or throw IllegalArgumentException
            }
        }

        if (rowsDeleted > 0) {
            context!!.contentResolver.notifyChange(uri, null)
            Log.d(TAG, "Deleted $rowsDeleted rows for URI: $uri")
        }
        return rowsDeleted
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        val match = uriMatcher.match(uri)
        val rowsUpdated: Int

        rowsUpdated = when (match) {
            VIDEOS -> {
                // This would update multiple rows based on selection
                db.updateWithOnConflict(VideoColumns.TABLE_NAME, values, selection, selectionArgs, SQLiteDatabase.CONFLICT_NONE)
            }
            VIDEO_ID -> {
                val id = ContentUris.parseId(uri)
                val newSelection = "${VideoColumns._ID}=?"
                val newSelectionArgs = arrayOf(id.toString())
                db.updateWithOnConflict(VideoColumns.TABLE_NAME, values, newSelection, newSelectionArgs, SQLiteDatabase.CONFLICT_NONE)
            }
            else -> {
                Log.e(TAG, "Update: Operation not supported for URI: $uri")
                0 // Or throw IllegalArgumentException
            }
        }

        if (rowsUpdated > 0) {
            context!!.contentResolver.notifyChange(uri, null)
            Log.d(TAG, "Updated $rowsUpdated rows for URI: $uri")
        }
        return rowsUpdated
    }

    override fun bulkInsert(uri: Uri, valuesArray: Array<out ContentValues>): Int {
        val db = dbHelper.writableDatabase
        val match = uriMatcher.match(uri)
        var numInserted = 0

        if (match == VIDEOS) {
            db.beginTransaction()
            try {
                for (values in valuesArray) {
                    val id = db.insertWithOnConflict(VideoColumns.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                    if (id != -1L) {
                        numInserted++
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            if (numInserted > 0) {
                context!!.contentResolver.notifyChange(uri, null)
                Log.d(TAG, "Bulk inserted $numInserted rows for URI: $uri")
            }
            return numInserted
        } else {
            return super.bulkInsert(uri, valuesArray)
        }
    }
}
