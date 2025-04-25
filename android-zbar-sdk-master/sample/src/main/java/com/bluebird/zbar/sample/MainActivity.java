package com.bluebird.zbar.sample;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bluebird.zbar.sample.databinding.ActivityQrScanBinding;
import com.bluebird.zbar.sample.history.HistoryFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ActivityQrScanBinding binding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setClickListeners();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    private void setClickListeners() {
        binding.cardViewSingleScan.setOnClickListener(
                v -> {
                    if (PermissionHelper.checkCameraPermission(this)) {
                        Log.d(TAG, "setClickListeners: Open Single Scan");
                        Fragment myFragment = PreviewHolderFragment.Companion.newInstance(0);
                        replaceFragment(getSupportFragmentManager(), R.id.fragment_container, myFragment, true);

                    } else {
                        PermissionHelper.requestCameraPermission(this);
                    }
                }
        );
        binding.cardViewContinuousScan.setOnClickListener(
                v -> {
                    if (PermissionHelper.checkCameraPermission(this)) {
                        Fragment myFragment = PreviewHolderFragment.Companion.newInstance(1);
                        replaceFragment(getSupportFragmentManager(), R.id.fragment_container, myFragment, true);
                    } else {
                        PermissionHelper.requestCameraPermission(this);
                    }
                }
        );

        binding.cardViewHistory.setOnClickListener(
                v -> {
                    replaceFragment(getSupportFragmentManager(), R.id.fragment_container, new HistoryFragment(), true);
                }
        );

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Handle Home navigation
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                return true;
            }
            if (itemId == R.id.nav_history) {
                // Handle Home navigation
                replaceFragment(getSupportFragmentManager(), R.id.fragment_container, new HistoryFragment(), true);
                return true;
            }
            if (itemId == R.id.nav_settings) {
                // Handle Home navigation
                return true;
            }

            return false;
        });

    }

    public static void replaceFragment(FragmentManager fragmentManager, int containerId, Fragment fragment, boolean addToBackStack) {
        if (fragmentManager == null || fragment == null) {
            throw new IllegalArgumentException("FragmentManager and Fragment cannot be null.");
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(containerId, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            // Check if the user has already been shown the settings dialog
            PermissionHelper.showSettingsDialog(this);
        }
    }

    public void hideNavAndScanSpeedLayouts(){
        binding.bottomNavigation.setVisibility(View.GONE);
        binding.cardViewAvgSpeed.setVisibility(View.GONE);
    }

    public void ViewNavAndScanSpeedLayouts(){
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        binding.cardViewAvgSpeed.setVisibility(View.VISIBLE);
    }


}
//
//    private ConstraintLayout mScanCropView;
//    private ImageView mScanLine;
//
//    private CameraPreview mPreviewView;
//
//    // Arul added
//    private long time;
//    private long contScanTime;
//
//    private int mCount = 0;
//
//    private int mode = 0;
//    private static final String TAG = MainActivity.class.getSimpleName();
//
//    private ConstraintLayout resultView;
//
//    private Button restart;
//
//    TextView totalTimeTextView;
//    ToggleButton startStopToggle;
//
//    Boolean scanStop = false;
//
//    TextView barcodeId;
//    TextView decodeTime;
//    TextView count;
//
//    private Boolean scanComplete = false;
//
//    private boolean isPermissionRequested = false;
//
//    //Arul End
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_qr_scan);
//
//        // Toolbar
//        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(mToolbar);
//
//        mPreviewView = findViewById(R.id.capture_preview);
//        mScanCropView = findViewById(R.id.capture_crop_view);
//        resultView = findViewById(R.id.resultView);
//        restart = findViewById(R.id.capture_restart_scan);
//        startStopToggle = findViewById(R.id.startStopToggle);
//        totalTimeTextView = findViewById(R.id.totalTime);
//        barcodeId = findViewById(R.id.barcodeId);
//        decodeTime = findViewById(R.id.totalTime);
//        count = findViewById(R.id.count);
////        mScanLine = (ImageView) findViewById(R.id.capture_scan_line);
//        mPreviewView.setScanCallback(resultCallback);
//
//        restart.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ConstraintLayout resultView = findViewById(R.id.resultView);
//                resultView.setVisibility(View.INVISIBLE);
//                startScanUnKnowPermission(1);
//            }
//        });
//
//        TabLayout tabLayout = findViewById(R.id.bottomNavigationView);
//
//        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//            @Override
//            public void onTabSelected(@NonNull TabLayout.Tab tab) {
//                mode = tab.getPosition();
//                Log.d(TAG, "onTabSelected: " + mode);
//                if (mode == 1) {
//                    registerStartStop();
//                    mPreviewView.setMode(1);
//                    decodeTime.setVisibility(View.VISIBLE);
//                } else {
//                    if(startStopToggle.isChecked()){
//                        startStopToggle.setChecked(false);
//                        startStopToggle.setCompoundDrawablesWithIntrinsicBounds(
//                                R.drawable.icon_play,
//                                0,
//                                0,
//                                0
//                        );
//                    }
//                    unregisterStartStop();
//                    mPreviewView.setMode(0);
//                    restart.setVisibility(View.VISIBLE);
//                }
//            }
//
//            @Override
//            public void onTabUnselected(@NonNull TabLayout.Tab tab) {
//                // Handle tab unselected event (optional)
//            }
//
//            @Override
//            public void onTabReselected(@NonNull TabLayout.Tab tab) {
//                // Handle tab reselected event (optional)
//            }
//        });
//
//    }
//
//    /**
//     * Accept scan result.
//     */
//    private ScanCallback resultCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(String result) {
//            if(result=="focus"){
//                mPreviewView.getFocus();
//                return;
//            }
//            if ((startStopToggle.isChecked() == false && mode == 1)) {
//                stopScan(1);
//                return;
//            }
//            long elapsedTime = System.currentTimeMillis() - time; // Corrected elapsed time calculation
//            // END
//            addResultToUI(result, elapsedTime, ++mCount);
//            if (mode == 0) {
//                scanStop = true;
//                stopScan(2);
//                restart.setVisibility(View.VISIBLE);
//
//            }
//            Log.d(TAG, "onScanResult: Focus Time : "+mPreviewView.focustime+" and "+(mPreviewView.focustime)/1000.0);
////            else {
////                startScanWithPermission();
////            }
//        }
//    };
//
//    private void registerStartStop() {
//        stopScan(3);
//        startStopToggle.setVisibility(View.VISIBLE);
//        resultView.setVisibility(View.VISIBLE);
//        restart.setVisibility(View.INVISIBLE);
//        startStopToggle.setOnClickListener(v -> {
//            if (startStopToggle.isChecked()) {
//                startScanUnKnowPermission(2);
//                startStopToggle.setCompoundDrawablesWithIntrinsicBounds(
//                        R.drawable.icon_pause,
//                        0,
//                        0,
//                        0
//                );
//                mCount = 0;
//                contScanTime = System.currentTimeMillis();
//                totalTimeTextView.setVisibility(View.VISIBLE);
//            } else {
//                stopScan(4);
//                startStopToggle.setCompoundDrawablesWithIntrinsicBounds(
//                        R.drawable.icon_play,
//                        0,
//                        0,
//                        0
//                );
//
//
//            }
//        });
//    }
//    private void unregisterStartStop() {
//        startStopToggle.setVisibility(View.INVISIBLE);
//        resultView.setVisibility(View.INVISIBLE);
//        startStopToggle.setOnClickListener(null);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        isPermissionRequested = false;
//        if (mode == 0 && !scanStop) {
//            startScanUnKnowPermission(3);
//            Log.d(TAG, "onResume: 1 ");
//        }
//        Log.d(TAG, "onResume: ");
//    }
//
//
//    @Override
//    public void onPause() {
//        stopScan(5);
//        super.onPause();
//    }
//
//    @SuppressLint("SetTextI18n")
//    private void addResultToUI(String result, long elapsedTime, int cnt) {
//        resultView.setVisibility(View.VISIBLE);
//        barcodeId.setText("Data : " + result);
//        decodeTime.setText("Total Time : " + elapsedTime + "ms");
//        count.setText("Count : " + cnt);
//
//    }
//
//    /**
//     * Do not have permission to request for permission and start scanning.
//     */
//    private void startScanUnKnowPermission(int i) {
//        Log.d(TAG, "startScanUnKnowPermission: "+i);
//        restart.setVisibility(View.INVISIBLE);
//        mCount = 0;
//        scanStop = false;
//
//        // Request camera permission using PermissionHelper
//        if (!PermissionHelper.checkCameraPermission(this)) {
//            // Camera permission is not granted, request it
//            PermissionHelper.requestCameraPermission(this);
//        } else {
//            // Permission already granted, start scanning
//            startScanWithPermission();
//        }
//    }
//
//    // Override onRequestPermissionsResult to handle the result of the permission request
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
//            // Check if the user has already been shown the settings dialog
//            if (!isPermissionRequested) {
//                isPermissionRequested = true; // Set flag to prevent multiple dialogs
//                showSettingsDialog(this);
//            }
//        }
//    }
//
//    private static boolean isSettingsDialogShown = false;
//
//    private static void showSettingsDialog(Context context) {
//        if (isSettingsDialogShown) {
//            return; // Prevent showing the dialog if it has already been shown
//        }
//
//        isSettingsDialogShown = true; // Set flag to true when the dialog is shown
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle("Permissions Needed")
//                .setMessage("To grant permission, go to Settings -> App -> Your App -> Permissions and enable the required permissions or use the settings button below to redirect to permission.")
//                .setPositiveButton("Settings", (dialog, which) -> {
//                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
//                    intent.setData(uri);
//                    context.startActivity(intent);
//                })
//                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
//                .create()
//                .show();
//    }
//
//    /**
//     * There is a camera when the direct scan.
//     */
//    private void startScanWithPermission() {
//        time = System.currentTimeMillis();
//        //End
//        Log.d(TAG, "startScanWithPermission: Starting Preview");
//        if (mPreviewView.start()) {
////            mScanAnimator.start();
//            Log.d(TAG, "startScanWithPermission: Success");
//        } else {
//            new AlertDialog.Builder(this)
//                    .setTitle(R.string.camera_failure)
//                    .setMessage(R.string.camera_hint)
//                    .setCancelable(false)
//                    .setPositiveButton(R.string.ok, (dialog, which) -> finish())
//                    .show();
//        }
//    }
//
//    /**
//     * Stop scan.
//     */
//    private void stopScan(int i) {
////        mScanAnimator.cancel();
//        mPreviewView.stop();
//        Log.d(TAG, "stopScan: Stoppingggg"+i);
//    }
//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
////            int height = mScanCropView.getMeasuredHeight() - 25;
////            mScanAnimator = ObjectAnimator.ofFloat(mScanLine, "translationY", 0F, height).setDuration(3000);
////            mScanAnimator.setInterpolator(new LinearInterpolator());
////            mScanAnimator.setRepeatCount(ValueAnimator.INFINITE);
////            mScanAnimator.setRepeatMode(ValueAnimator.REVERSE);
//
////        startScanUnKnowPermission(4);
//    }
//}
//
