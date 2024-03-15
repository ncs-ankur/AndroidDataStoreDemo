package com.ncs.poc.androiddatastoredemo.sqlite

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private val DATABASE_VERSION = 1
        val DATABASE_NAME = "userDB"
        private val TABLE_USERS = "users"
        private val KEY_ID = "id"
        private val KEY_NAME = "name"
        private val KEY_AGE = "age"
        private val SINGLE_USER_ID = 1 // Fixed ID for the single user
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_USERS_TABLE = ("CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_AGE + " INTEGER" + ")")
        db?.execSQL(CREATE_USERS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    fun addUser(name: String, age: Int) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_NAME, name)
        values.put(KEY_AGE, age)

        // Using replace to insert or update existing row
        values.put(KEY_ID, SINGLE_USER_ID) // Use the single user ID
        db.replace(TABLE_USERS, null, values)
        db.close()
    }

    fun getUser(): User? {
        val selectQuery = "SELECT * FROM $TABLE_USERS WHERE $KEY_ID = $SINGLE_USER_ID"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            val user = User(cursor.getInt(0), cursor.getString(1), cursor.getInt(2))
            cursor.close()
            return user
        }
        cursor.close()
        return null
    }
}

data class User(val id: Int, val name: String, val age: Int)
