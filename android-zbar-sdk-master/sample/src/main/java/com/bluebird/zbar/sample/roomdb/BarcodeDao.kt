package com.bluebird.zbar.sample.roomdb

import androidx.room.*

@Dao
interface BarcodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: Barcode)

    @Delete
    suspend fun deleteBarcode(barcode: Barcode)

    @Query("DELETE FROM barcode_table WHERE sno = :sno")
    suspend fun deleteBarcodeBySNo(sno: Int)

    @Query("SELECT * FROM barcode_table WHERE barcode = :barcode LIMIT 1")
    suspend fun findBarcode(barcode: String): Barcode?

    @Query("SELECT * FROM barcode_table")
    suspend fun getAllBarcodes(): List<Barcode>

    @Query("DELETE FROM barcode_table")
    suspend fun deleteAllBarcodes()

}
