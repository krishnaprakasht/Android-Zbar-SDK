package com.bluebird.zbar.sample.roomdb

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class BarcodeViewModel(val repository: BarcodeRepository) : ViewModel() {

    private val _barcodes = MutableLiveData<List<Barcode>>()
    val barcodes: LiveData<List<Barcode>> get() = _barcodes

    fun fetchBarcodes() {
        viewModelScope.launch {
            val data = repository.getAllBarcodes()
            _barcodes.postValue(data)
        }
    }

    fun insertBarcode(barcode: Barcode) {
        viewModelScope.launch {
            repository.insertBarcode(barcode)
        }
    }

    fun deleteBarcode(barcode: Barcode) {
        viewModelScope.launch {
            repository.deleteBarcode(barcode)
        }
    }

    fun deleteBarcodeBySNo(sno: Int) {
        viewModelScope.launch {
            repository.deleteBarcodeBySNo(sno)
        }
    }

    fun findBarcode(barcode: String, callback: (Barcode?) -> Unit) {
        viewModelScope.launch {
            val result = repository.findBarcode(barcode)
            callback(result)
        }
    }

    fun deleteAllBarcodes() {
        viewModelScope.launch {
            repository.deleteAllBarcodes()
        }
    }


//    fun findBarcodeInMulti(barcode: String, callback: (Barcode?) -> Unit) {
//        viewModelScope.launch {
//            val result = repository.findBarcodeInMulti(barcode)
//            callback(result)
//        }
//    }

    fun getAllBarcodes(callback: (List<Barcode>) -> Unit) {
        viewModelScope.launch {
            val result = repository.getAllBarcodes()
            callback(result)
        }
    }
}
