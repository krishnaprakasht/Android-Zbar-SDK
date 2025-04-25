package com.bluebird.zbar.sample.roomdb

class BarcodeRepository(private val barcodeDao: BarcodeDao) {
    suspend fun insertBarcode(barcode: Barcode) = barcodeDao.insertBarcode(barcode)
    suspend fun deleteBarcode(barcode: Barcode) = barcodeDao.deleteBarcode(barcode)
    suspend fun deleteBarcodeBySNo(sno: Int) = barcodeDao.deleteBarcodeBySNo(sno)
    suspend fun findBarcode(barcode: String) = barcodeDao.findBarcode(barcode)
    suspend fun deleteAllBarcodes() = barcodeDao.deleteAllBarcodes()
    suspend fun getAllBarcodes() = barcodeDao.getAllBarcodes()
}
