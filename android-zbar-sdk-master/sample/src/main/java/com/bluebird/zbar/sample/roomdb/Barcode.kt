package com.bluebird.zbar.sample.roomdb

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "barcode_table")
data class Barcode(
    @PrimaryKey(autoGenerate = true) val sno: Int = 0, // Auto-generated serial number
    val mode: Int,
    val barcode: String,
    val symbol: String,
    var count: Int,
    var timeStamp: String
)
