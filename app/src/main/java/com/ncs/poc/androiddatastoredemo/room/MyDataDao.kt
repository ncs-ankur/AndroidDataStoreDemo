package com.ncs.poc.androiddatastoredemo.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MyDataDao {
    // Use REPLACE strategy to ensure that inserting a user with an existing ID replaces the old user
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: MyData) // Assuming MyData is your entity

    // Optional: Method to retrieve the single user if you're using a fixed ID for the user.
    // This method assumes you have a fixed ID (e.g., 1) for your single user.
    @Query("SELECT * FROM mydata WHERE id = 1 LIMIT 1")
    suspend fun loadUser(): MyData?
}
