package com.bluebird.zbar.camera;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {
    private Rect roiRect;
    private Paint roiPaint;
    private Paint overlayPaint;
    private Path overlayPath;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        roiPaint = new Paint();
        roiPaint.setColor(Color.TRANSPARENT);
        roiPaint.setStyle(Paint.Style.STROKE);
        roiPaint.setStrokeWidth(5);

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#A0000000"));
        overlayPaint.setStyle(Paint.Style.FILL);

        overlayPath = new Path();
    }

    public void setROI(int x, int y, int width, int height) {
        roiRect = new Rect(x, y, x + width, y + height);
        invalidate();
    }

    public void clearOverlay() {
        roiRect = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (roiRect != null) {
            // Create a path covering the whole screen
            overlayPath.reset();
            overlayPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);

            // Cut out the ROI area
            overlayPath.addRect(roiRect.left, roiRect.top, roiRect.right, roiRect.bottom, Path.Direction.CCW);

            // Draw the overlay with a cut-out ROI
            canvas.drawPath(overlayPath, overlayPaint);

            // Draw the ROI border
            canvas.drawRect(roiRect, roiPaint);
        }
    }
}
