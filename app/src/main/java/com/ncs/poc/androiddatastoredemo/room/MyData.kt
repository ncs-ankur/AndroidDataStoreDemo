package com.ncs.poc.androiddatastoredemo.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class MyData {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 1

    @ColumnInfo(name = "data")
    var data: String? = null
}
