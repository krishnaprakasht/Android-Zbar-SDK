package com.bluebird.zbar.camera;


import android.graphics.Rect;

public class BarcodeData {
    Rect bounds; // Barcode bounding box
    String data; // Barcode data
    int type;    // Barcode type

    BarcodeData(Rect bounds, String data, int type) {
        this.bounds = bounds;
        this.data = data;
        this.type = type;
    }
}
