package com.bluebird.zbar.sample;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.Map;

public class PermissionHelper {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1002;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1003;
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 1004;

    private static ActivityResultLauncher<Intent> requestEnableBluetooth;
    private static ActivityResultLauncher<String[]> requestMultiplePermissions;

    public static void initialize(FragmentActivity activity) {
        requestEnableBluetooth = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Log.d("MyTag", "Bluetooth enabled");
            } else {
                Log.d("MyTag", "Bluetooth enabling denied");
                requestBluetooth(activity);
            }
        });

        requestMultiplePermissions = activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allGranted = true;
            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                Log.d("MyTag", entry.getKey() + " = " + entry.getValue());
                if (!entry.getValue()) {
                    allGranted = false;
                }
            }
            if (!allGranted) {
                showSettingsDialog(activity);
            }
        });
    }

    // Request location permissions from a Fragment
    public static void requestLocationPermission(Fragment fragment) {
        fragment.requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_PERMISSION_REQUEST_CODE);
    }

    // Request location permissions from a FragmentActivity
    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_PERMISSION_REQUEST_CODE);
    }

    // Request storage permissions from a Fragment
    public static void requestStoragePermission(Fragment fragment) {
        fragment.requestPermissions(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, STORAGE_PERMISSION_REQUEST_CODE);
    }

    // Request storage permissions from a FragmentActivity
    public static void requestStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, STORAGE_PERMISSION_REQUEST_CODE);
    }

    // Check if location permissions are granted
    public static boolean isLocationPermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Check if storage permissions are granted
    public static boolean isStoragePermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean checkCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    // Show settings dialog to manually enable permissions
    static void showSettingsDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Permissions Needed")
                .setMessage("To grant permission, go to Settings -> App -> Your App -> Permissions and enable the required permissions or use the settings button below to redirect to permission.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    // Request Bluetooth permissions
    public static void requestBluetooth(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isBluetoothPermissionGranted(activity)) {
                // Permission already granted
                Log.d("MyTag", "Bluetooth permissions granted");
            } else {
                // Request permissions
                requestMultiplePermissions.launch(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                });
            }
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            requestEnableBluetooth.launch(enableBtIntent);
        }
    }

    // Check if Bluetooth permissions are granted
    public static boolean isBluetoothPermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }


    public static void requestCameraPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    // Handle permission results
    public static void handlePermissionsResult(int requestCode, int[] grantResults, Context context) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale((FragmentActivity) context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // User selected "Don't ask again", show settings dialog
                        showSettingsDialog(context);
                    }
                }
                break;

            case STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 &&
                        (grantResults[0] == PackageManager.PERMISSION_DENIED ||
                                grantResults[1] == PackageManager.PERMISSION_DENIED)) {

                    boolean showRationaleRead = ActivityCompat.shouldShowRequestPermissionRationale((FragmentActivity) context, Manifest.permission.READ_EXTERNAL_STORAGE);
                    boolean showRationaleWrite = ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    if (!showRationaleRead || !showRationaleWrite) {
                        // User selected "Don't ask again" for either permission, show settings dialog
                        showSettingsDialog(context);
                    }
                }
                break;

            case BLUETOOTH_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    showSettingsDialog(context);
                }
                break;

        }
    }
}
