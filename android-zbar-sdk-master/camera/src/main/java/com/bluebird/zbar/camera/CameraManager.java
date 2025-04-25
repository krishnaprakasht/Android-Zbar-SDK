
package com.bluebird.zbar.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;


public final class CameraManager {

    private final CameraConfiguration mConfiguration;

    private Camera mCamera;

    private boolean isFocusing = false;

    private boolean isFlashOn = false;

    private Context context;

    public CameraManager(Context context) {
        this.context = context;
        this.mConfiguration = new CameraConfiguration(context);
    }

    /**
     * Opens the mCamera driver and initializes the hardware parameters.
     *
     * @throws Exception ICamera open failed, occupied or abnormal.
     */
    public synchronized void openDriver() throws Exception {
        if (mCamera != null) return;

        mCamera = Camera.open();
        if (mCamera == null) throw new IOException("The camera is occupied.");

        mConfiguration.initFromCameraParameters(mCamera);

        Camera.Parameters parameters = mCamera.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten();
        try {
            mConfiguration.setDesiredCameraParameters(mCamera, false);
        } catch (RuntimeException re) {
            if (parametersFlattened != null) {
                parameters = mCamera.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    mCamera.setParameters(parameters);
                    mConfiguration.setDesiredCameraParameters(mCamera, true);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Closes the camera driver if still in use.
     */
    public synchronized void closeDriver() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Camera is opened.
     *
     * @return true, other wise false.
     */
    public boolean isOpen() {
        return mCamera != null;
    }

    /**
     * Get camera configuration.
     *
     * @return {@link CameraConfiguration}.
     */
    public CameraConfiguration getConfiguration() {
        return mConfiguration;
    }

    /**
     * Camera start preview.
     *
     * @param holder          {@link SurfaceHolder}.
     * @param previewCallback {@link Camera.PreviewCallback}.
     * @throws IOException if the method fails (for example, if the surface is unavailable or unsuitable).
     */
    public void startPreview(SurfaceHolder holder, Camera.PreviewCallback previewCallback) throws IOException {
        if (mCamera != null) {
            // Get the current device rotation
            int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay()
                    .getRotation();

            // Calculate the orientation for the camera preview
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }

            // Get the camera info to adjust for the orientation
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
            int result;
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (cameraInfo.orientation + degrees) % 360;
                result = (360 - result) % 360; // Compensate for the mirror effect
            } else { // Back-facing camera
                result = (cameraInfo.orientation - degrees + 360) % 360;
            }

            mCamera.setDisplayOrientation(result);

            // Set the preview display and callback
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(previewCallback);
            mCamera.startPreview();
            isFocusing = false;
            Log.d("TAG", "startPreview: True");
        } else {
            Log.d("TAG", "startPreview: false");
        }
    }


    /**
     * Camera stop preview.
     */
    public void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception ignored) {
                // nothing.
            }
            try {
                mCamera.setPreviewDisplay(null);
            } catch (IOException ignored) {
                // nothing.
            }
        }
    }

    /**
     * Focus on, make a scan action.
     *
     * @param callback {@link Camera.AutoFocusCallback}.
     */
    public void autoFocus(final Camera.AutoFocusCallback callback) {
        if (mCamera != null && !isFocusing) {
            try {
                isFocusing = true; // Mark focusing state
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        isFocusing = false;
                        Log.d("FocusTime", "onAutoFocus: "+success);
                        if (callback != null) {
                            callback.onAutoFocus(success, camera);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                isFocusing = false;
            }
        }
    }

    public void setFlashlight(boolean enable) {
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                if (enable) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    isFlashOn = true;
                } else {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    isFlashOn = false;
                }
                mCamera.setParameters(parameters);
                Log.d("Flashlight", "Flashlight turned " + (enable ? "ON" : "OFF"));
            } catch (Exception e) {
                Log.e("Flashlight", "Error changing flashlight state: " + e.getMessage());
            }
        }
    }

    /**
     * Toggle flashlight state.
     */
    public boolean toggleFlashlight() {
        setFlashlight(!isFlashOn);
        return isFlashOn;
    }
}
