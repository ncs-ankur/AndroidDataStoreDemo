package com.ncs.poc.androiddatastoredemo.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri


class MyDataProvider : ContentProvider() {
    private var db: SQLiteDatabase? = null
    private var dbHelper: MyDatabaseHelper? = null

    override fun onCreate(): Boolean {
        val context = context
        dbHelper = MyDatabaseHelper(context)
        db = dbHelper?.writableDatabase
        return (db != null)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // Check if a user already exists
        val cursor = db?.query("myData", arrayOf("_id"), null, null, null, null, null)
        val exists = cursor?.moveToFirst() == true
        cursor?.close()

        if (exists) {
            // Update the existing user instead of inserting a new one
            db?.update("myData", values, "_id = 1", null)
        } else {
            // Insert the new user with a fixed ID to ensure single user
            values?.put("_id", 1) // Ensure we use a fixed ID for the user
            db?.insert("myData", null, values)
        }

        val _uri = ContentUris.withAppendedId(CONTENT_URI, 1) // Always return URI with ID 1
        context?.contentResolver?.notifyChange(_uri, null)
        return _uri
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val qb = SQLiteQueryBuilder()
        qb.tables = "myData"
        // Modify query builder to always select the single user
        val modifiedSelection = "_id = 1" // Ensure we always target the single user

        val c = qb.query(db, projection, modifiedSelection, null, null, null, sortOrder)
        c.setNotificationUri(context?.contentResolver, uri)
        return c
    }

    // Implement the delete() and update() methods as required for your use case, ensuring they respect the single-user constraint
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        // Optional: Implement delete method, potentially resetting the user data or preventing deletion
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        // Update the single user, ignoring selection and selectionArgs
        val count = db?.update("myData", values, "_id = 1", null) ?: 0
        context?.contentResolver?.notifyChange(uri, null)
        return count
    }

    override fun getType(uri: Uri): String? {
        // No change needed here
        return when (uriMatcher.match(uri)) {
            MYDATA -> "vnd.android.cursor.dir/vnd.example.myData"
            MYDATA_ID -> "vnd.android.cursor.item/vnd.example.myData"
            else -> throw IllegalArgumentException("Unsupported URI: $uri")
        }
    }

    companion object {
        const val PROVIDER_NAME: String = "com.example.app.MyDataProvider"
        const val URL: String = "content://" + PROVIDER_NAME + "/myData"
        val CONTENT_URI: Uri = Uri.parse(URL)

        const val MYDATA: Int = 1
        const val MYDATA_ID: Int = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            uriMatcher.addURI(PROVIDER_NAME, "myData", MYDATA)
            uriMatcher.addURI(PROVIDER_NAME, "myData/#", MYDATA_ID)
        }
    }
}
