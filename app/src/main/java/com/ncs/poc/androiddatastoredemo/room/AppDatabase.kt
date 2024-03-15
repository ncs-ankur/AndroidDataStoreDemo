package com.ncs.poc.androiddatastoredemo.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MyData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun myDataDao(): MyDataDao
}