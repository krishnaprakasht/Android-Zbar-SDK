package com.bluebird.zbar.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OverlayViewBounded extends View {

    private List<BarcodeData> barcodes = new ArrayList<>();

    public OverlayViewBounded(Context context) {
        super(context);
    }

    public OverlayViewBounded(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlayViewBounded(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBarcodes(List<BarcodeData> barcodes) {
        this.barcodes = barcodes;
        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL); // Fill the dot
        paint.setAntiAlias(true); // Smooth edges

        for (BarcodeData barcode : barcodes) {
            // Draw a small dot at the top-left corner of the barcode
            float x = barcode.bounds.left;
            float y = barcode.bounds.top;
            float radius = 10; // Dot size
            canvas.drawCircle(x, y, radius, paint);
        }
    }

//    For rectangle
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        Paint paint = new Paint();
//        paint.setColor(Color.RED);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(5);
//
//        for (BarcodeData barcode : barcodes) {
//            canvas.drawRect(barcode.bounds, paint);
//        }
//    }
}