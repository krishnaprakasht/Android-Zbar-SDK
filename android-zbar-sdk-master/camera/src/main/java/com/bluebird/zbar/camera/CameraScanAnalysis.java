package com.bluebird.zbar.camera;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.bluebird.zbar.Config;
import com.bluebird.zbar.Image;
import com.bluebird.zbar.ImageScanner;
import com.bluebird.zbar.Symbol;
import com.bluebird.zbar.SymbolSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraScanAnalysis implements Camera.PreviewCallback {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ImageScanner mImageScanner;
    private Handler mHandler;
    private OverlayView overlayView;
    private OverlayViewBounded overlayViewBounded;
    private ScanCallback mCallback;
    private boolean allowAnalysis = true;
    private Image barcode;
    private int mode = 0;
    private int targetMode = 0;
    private Context context;
    private List<BarcodeData> detectedBarcodes = new ArrayList<>();
    Boolean focusState = false;
    long startTime = 0L;

    CameraScanAnalysis() {
        mImageScanner = new ImageScanner();
        mImageScanner.setConfig(0, Config.X_DENSITY, 3);
        mImageScanner.setConfig(0, Config.Y_DENSITY, 3);
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (mCallback != null) mCallback.onScanResult((String) msg.obj);
            }
        };

    }

    void setScanCallback(ScanCallback callback) {
        this.mCallback = callback;
    }

    void onStop() {
        this.allowAnalysis = false;
        executorService.shutdownNow();
        overlayView.clearOverlay();
    }

    // In CameraScanAnalysis class
    void onStart() {
        if (executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }
        this.allowAnalysis = true;
    }

    public void setOverlayView(OverlayView overlayView, OverlayViewBounded overlayViewBounded, Context context) {
        this.overlayView = overlayView;
        this.overlayViewBounded = overlayViewBounded;
        this.context = context;
    }

    void setMode(int mode) {
        this.mode = mode;
    }

    void setTargetMode(int mode) {
        this.targetMode = mode;
    }

    public static Bitmap byteArrayToBitmap(byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0) {
            return null; // Return null if the byte array is empty
        }
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (data == null || camera == null) {
            Log.d("TAG", "onPreviewFrame: Null");
            return;
        }
        if (allowAnalysis) {
            Camera.Size size = camera.getParameters().getPreviewSize();
            final int frameWidth = size.width;
            final int frameHeight = size.height;
            Log.d("TAG", "onPreviewFrame: Target Mode" + targetMode);
            if (targetMode == 1) {
                makeDir();

                // Define ROI
                int roiX = (frameWidth / 4) + 88;
                final int roiY = frameHeight / 4;
                int roiWidth = frameWidth / 6;
                final int roiHeight = frameHeight / 2;

                // Ensure ROI is within bounds
                if (roiX + roiWidth > frameWidth || roiY + roiHeight > frameHeight) {
                    Log.e("ROIError", "ROI is out of bounds!");
                    return;
                }

                // Log ROI coordinates for debugging
//                Log.d("ROICoordinates", "ROI: (" + roiX + ", " + roiY + ", " + roiWidth + ", " + roiHeight + ")");

                // Extract Y plane from NV21 data
                byte[] yPlane = new byte[frameWidth * frameHeight];
                System.arraycopy(data, 0, yPlane, 0, yPlane.length);


                byte[] croppedYPlane = new byte[roiWidth * roiHeight];

                overlayView.setROI(roiY, roiX, roiHeight, roiWidth);
                roiX = roiX + 88;

                int finalRoiX = roiX;

                executorService.execute(() -> {
                    // Manually crop the Y plane
                    for (int y = 0; y < roiHeight; y++) {
                        for (int x = 0; x < roiWidth; x++) {
                            // Calculate the source index based on ROI
                            int srcIndex = (roiY + y) * frameWidth + (finalRoiX + x);
                            int destIndex = y * roiWidth + x;
                            croppedYPlane[destIndex] = yPlane[srcIndex];
                        }
                    }

                    // Create Image object with Y800 format and cropped data
                    barcode = new Image(roiWidth, roiHeight, "Y800");
                    barcode.setData(croppedYPlane);
                });

//                Save image for debugging
//                File croppedImageFile = new File(mDir, "cropped_y_plane.jpg");
//                saveBarcodeImage(barcode.getData(), roiWidth, roiHeight, croppedImageFile);
//                Log.d("BarcodeImageSize", "Barcode image width: " + barcode.getWidth() + ", height: " + barcode.getHeight());
            }
//            else if (targetMode == 1) {
//
//                detectedBarcodes.clear();
//                barcode = new Image(frameWidth, frameHeight, "Y800");
//                barcode.setData(data);
//
//                // Scan the entire frame for barcodes
//                int result = mImageScanner.scanImage(barcode);
//                Log.d("TAG", "onPreviewFrame: "+result);
//                if (result != 0) {
//                    SymbolSet symSet = mImageScanner.getResults();
//                    for (Symbol sym : symSet) {
//                        // Get barcode bounds as an array
//                        int[] boundsArray = sym.getBounds();
//
//                        if (boundsArray != null && boundsArray.length == 4) {
//                            int left = boundsArray[0];
//                            int top = boundsArray[1];
//                            int right = boundsArray[2];
//                            int bottom = boundsArray[3];
//                            if (right <= 0 || bottom <= 0) {
//                                continue;
//                            }
//                            //             Right->Width  bottom ->height left -> X top ->Y
//                            Log.d("Barcode", "onPreviewFrame: X" + left + " Y " + top + " Height " + right + " Width " + bottom);
//                            // Create a Rect object
//                            Rect bounds = new Rect(left, top, left+1, top+1);
//                            // Store detected barcode
//                            detectedBarcodes.add(new BarcodeData(bounds, sym.getData(), sym.getType()));
//                        }
//                    }
//                }
//                overlayViewBounded.setBarcodes(detectedBarcodes);
//
//            }
            else {
                overlayView.clearOverlay();
                barcode = new Image(size.width, size.height, "Y800");
                barcode.setData(data);
            }
            // Execute analysis task
            executorService.execute(mAnalysisTask);
        }
    }



        public static void saveBarcodeImage(byte[] barcodeData, int barcodeWidth, int barcodeHeight, File outputFile) {
        try {
            // Create a grayscale bitmap
            Bitmap barcodeBitmap = Bitmap.createBitmap(barcodeWidth, barcodeHeight, Bitmap.Config.ARGB_8888);
            int[] pixels = new int[barcodeWidth * barcodeHeight];
            for (int y = 0; y < barcodeHeight; y++) {
                for (int x = 0; x < barcodeWidth; x++) {
                    int index = y * barcodeWidth + x;
                    int luminance = barcodeData[index] & 0xFF;
                    pixels[index] = 0xFF000000 | (luminance << 16) | (luminance << 8) | luminance;
                }
            }
            barcodeBitmap.setPixels(pixels, 0, barcodeWidth, 0, 0, barcodeWidth, barcodeHeight);

            // Save the bitmap as a JPEG
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                barcodeBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }
            Log.d("CameraScanAnalysis", "Barcode image saved: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("CameraScanAnalysis", "Error saving barcode image", e);
        }
    }


    private Boolean focusRequest = false;

    private int fcsReqCnt = 0;

    private String mLogDir = "/bbrfid/sled";
    private File mDir;

    private void makeDir() {
        String storage = Environment.getExternalStorageDirectory().getPath();
        mDir = new File(storage + mLogDir);
        if (!mDir.exists()) {
            mDir.mkdirs();
        }
    }


    private Runnable mAnalysisTask = new Runnable() {
        @Override
        public void run() {
            // Check if analysis is still allowed before processing.
            if (!allowAnalysis) return;
            try {
                int result = mImageScanner.scanImage(barcode);
                String resultStr = null;
                String type = "";
                String decoderTime = "";
                if (result != 0) {
                    SymbolSet symSet = mImageScanner.getResults();
                    for (Symbol sym : symSet) {
                        resultStr = sym.getData();
                        type = sym.getSymbolName().trim();
                        decoderTime = sym.getElapsedTime();
                    }
                    if (mode == 0) {
                        allowAnalysis = false;
                    }
                }
// logic is done like if the processed image has no value that might be because of no focus so request focus if more than 5 frames have null.
// If this is observed "focus" is return in callback to MainActivity from there using the camera preview focus is being obtained.
                if (resultStr == null && !focusRequest) {
                    if (fcsReqCnt < 10) {
                        fcsReqCnt++;
                        return;
                    }
                    Message message = mHandler.obtainMessage();
                    message.obj = "focus";
                    message.sendToTarget();
                    focusRequest = true;
                }

                if (!TextUtils.isEmpty(resultStr)) {
                    focusRequest = false;
                    fcsReqCnt = 0;
                    long focusTime = 0L;
                    if (focusState) {
                        focusTime = System.currentTimeMillis()- startTime;
                        focusState = false;
                    }
                    else{
                        Log.e("FocusTime", "invalid focus duration: " + focusTime);
                    }
                    Message message = mHandler.obtainMessage();
                    message.obj = resultStr + ";" + type + ";" + decoderTime + ";" + focusTime;
                    message.sendToTarget();
                } else {
                    allowAnalysis = true;
                    focusRequest = false;
                }

            } catch (Exception e) {
                Log.e("CameraScanAnalysis", "Error scanning image", e);
            }
        }
    };

}