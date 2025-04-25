package com.bluebird.zbar.sample.roomdb

data class ContinuousScan(
    val barcode: String,
    val symbol: String,
    var count: Int
)