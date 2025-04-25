package com.bluebird.zbar.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;


public class CameraPreview extends FrameLayout implements SurfaceHolder.Callback {

    private CameraManager mCameraManager;
    private CameraScanAnalysis mPreviewCallback;
    private SurfaceView mSurfaceView;

    private OverlayView overlayView;



    public CameraPreview(Context context) {
        this(context, null);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCameraManager = new CameraManager(context);
        mPreviewCallback = new CameraScanAnalysis();
    }

    /**
     * Set Scan results callback.
     *
     * @param callback {@link ScanCallback}.
     */
    public void setScanCallback(ScanCallback callback) {
        mPreviewCallback.setScanCallback(callback);
    }

    public void setMode(int mode) {
        mPreviewCallback.setMode(mode);
    }

    public void setTargetMode(int mode) {
        mPreviewCallback.setTargetMode(mode);
    }

    /**
     * Camera start preview.
     */
    public boolean start() {
        try {
            mCameraManager.openDriver();
        } catch (Exception e) {
            return false;
        }
        mPreviewCallback.startTime = System.currentTimeMillis();
        Log.d("FocusTime", "start: "+mPreviewCallback.startTime);
        mPreviewCallback.onStart();
        if (mSurfaceView == null) {
            mSurfaceView = new SurfaceView(getContext());
            addView(mSurfaceView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            SurfaceHolder holder = mSurfaceView.getHolder();
            holder.addCallback(this);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            Log.d("TAG", "start: Holder is null");
        }

        startCameraPreview(mSurfaceView.getHolder());

        return true;
    }

    /**
     * Camera stop preview.
     */
    public void stop() {
        removeCallbacks(mAutoFocusTask);
        mPreviewCallback.onStop();
        mCameraManager.stopPreview();
        mCameraManager.closeDriver();
    }

    private void startCameraPreview(SurfaceHolder holder) {
        if (holder == null || !holder.getSurface().isValid()) {
            Log.e("CameraPreview", "Invalid SurfaceHolder.");
            return;
        }
        try {
            mCameraManager.startPreview(holder, mPreviewCallback);
            mCameraManager.autoFocus(mFocusCallback);
        } catch (Exception e) {
            Log.e("CameraPreview", "Failed to start camera preview.", e);
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }
        try {
            mCameraManager.stopPreview();
        } catch (Exception e) {
            Log.e("CameraPreview", "Failed to stop preview.", e);
        }
        startCameraPreview(holder);
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    // In CameraPreview class
    private Camera.AutoFocusCallback mFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(success) {
                mPreviewCallback.focusState = true;
                Log.d("FocusTime", "onAutoFocus: try ");
            }
            if (!success) {
                postDelayed(() -> mCameraManager.autoFocus(this), 1000);
            }
        }
    };
    private Runnable mAutoFocusTask = new Runnable() {
        public void run() {
            mCameraManager.autoFocus(mFocusCallback);
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    public void getFocus(){
        mCameraManager.autoFocus(mFocusCallback);
    }

    public boolean toggleFlash(){
        return mCameraManager.toggleFlashlight();

    }


    public void setOverlayView(OverlayView overlayView,OverlayViewBounded overlayViewBounded,Context context){
        this.overlayView = overlayView;
        mPreviewCallback.setOverlayView(overlayView,overlayViewBounded,context);
    }

}