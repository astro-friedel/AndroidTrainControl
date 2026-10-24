package com.controls.train.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.controls.train.BluefruitApplication;
import com.controls.train.R;
import com.controls.train.ble.BleUtils;
import com.controls.train.ble.central.BleManager;
import com.controls.train.ble.central.BlePeripheral;
import com.controls.train.utils.DialogUtils;
import com.controls.train.utils.PermissionsUtils;

public class MainActivity extends AppCompatActivity implements ScannerFragment.ScannerFragmentListener {

    // Constants
    private final static String TAG = MainActivity.class.getSimpleName();

    // Permission requests
    private final static int PERMISSION_REQUEST_BLUETOOTHSCANNING = 1;

    // Activity request codes (used for onActivityResult)
    private static final int kActivityRequestCode_EnableBluetooth = 1;
    public static final int kActivityRequestCode_PlayServicesAvailability = 2;

    // Data
    private MainScreenFragment mMainFragment;
    private AlertDialog mRequestLocationDialog;
    private boolean hasUserAlreadyBeenAskedAboutBluetoothStatus = false;
    private boolean isRequestingPermissions = false;

    // region Activity Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            // Set mainmenu fragment
            mMainFragment = MainScreenFragment.newInstance();
            fragmentManager.beginTransaction()
                    .add(R.id.contentLayout, mMainFragment, "Main")
                    .commit();

        } else {
            hasUserAlreadyBeenAskedAboutBluetoothStatus = savedInstanceState.getBoolean("hasUserAlreadyBeenAskedAboutBluetoothStatus");
            mMainFragment = (MainScreenFragment) fragmentManager.findFragmentByTag("Main");
        }

        // Back navigation listener
        fragmentManager.addOnBackStackChangedListener(() -> {
            if (fragmentManager.getBackStackEntryCount() == 0) {        // Check if coming back
                try {
                    mMainFragment.disconnectAllPeripherals();
                } catch (SecurityException e) {
                    Log.d(TAG, "Security exception: " + e);
                }
            }
        });
    }

    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("hasUserAlreadyBeenAskedAboutBluetoothStatus", hasUserAlreadyBeenAskedAboutBluetoothStatus);
    }

    // endregion

    @Override
    protected void onResume() {
        super.onResume();

        BluefruitApplication.activityResumed();
        checkPermissions();

        // Observe disconnections
        registerGattReceiver();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        popFragmentsIfNoPeripheralsConnected();         // check if peripherals were disconnected while the app was in background
    }

    @Override
    protected void onPause() {
        super.onPause();
        BluefruitApplication.activityPaused();
        unregisterGattReceiver();

        // Remove location dialog if present
        if (mRequestLocationDialog != null) {
            mRequestLocationDialog.cancel();
            mRequestLocationDialog = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {

        final boolean areLocationServicesReadyForScanning = PermissionsUtils.manageLocationServiceAvailabilityForScanning(this);
        if (!areLocationServicesReadyForScanning) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mRequestLocationDialog = builder.setMessage(R.string.bluetooth_locationpermission_disabled_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            //DialogUtils.keepDialogOnOrientationChanges(mRequestLocationDialog);
        } else {
            if (mRequestLocationDialog != null) {
                mRequestLocationDialog.cancel();
                mRequestLocationDialog = null;
            }

            // Bluetooth state
            final boolean isBluetoothEnabled = BleUtils.getBleStatus(getApplicationContext()) == BleUtils.STATUS_BLE_ENABLED;
            if ((isBluetoothEnabled || !hasUserAlreadyBeenAskedAboutBluetoothStatus) && !isRequestingPermissions) {     // Don't repeat the check if the user was already informed to avoid showing the "Enable Bluetooth" system prompt several times
                manageBluetoothAvailability();

                if (isBluetoothEnabled) {
                    // Request Bluetooth scanning permissions
                    final boolean isScanningPermissionGranted = requestScanningPermissionsIfNeeded();

                    if (isScanningPermissionGranted) {
                        // All good. Start Scanning
                        BleManager.getInstance().start(MainActivity.this);
                        // Bluetooth was enabled, resume scanning
                        try {
                            mMainFragment.startScanning();
                        } catch (SecurityException e) {
                            Log.e(TAG, "Start scanning failed: " + e);
                        }
                    }
                }
            }
        }
    }

    // region Permissions

    private boolean requestScanningPermissionsIfNeeded() {       // Starting with Android 10, Bluetooth scanning needs Location FINE Permission

        String[] permissions = PermissionsUtils.getNeededPermissionsForScanning();
        if (PermissionsUtils.hasPermissions(this, permissions)) {
            return true;
        } else {
            isRequestingPermissions = true;
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_BLUETOOTHSCANNING);
            return false;
        }

        /*
        boolean permissionGranted = true;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionGranted = false;
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mRequestLocationDialog = builder.setTitle(R.string.bluetooth_locationpermission_title)
                    .setMessage(R.string.bluetooth_locationpermission_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .setOnDismissListener(dialog -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION))
                    .show();
        }
        return permissionGranted;

         */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_BLUETOOTHSCANNING: {
                if (PermissionsUtils.hasPermissions(this, permissions)) {
                    isRequestingPermissions = false;
                    Log.d(TAG, "Bluetooth permissions granted");
                    checkPermissions();

                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.bluetooth_permissions_notavailable_title);
                    builder.setMessage(R.string.bluetooth_permissions_notavailable_text);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(dialog -> {
                        isRequestingPermissions = false;
                    });
                    builder.show();
                }
                break;
            }
            default:
                break;
        }
    }
    // endregion

    // region Bluetooth Setup
    private boolean manageBluetoothAvailability() {
        boolean isEnabled = true;

        // Check Bluetooth HW status
        int errorMessageId = 0;
        final int bleStatus = BleUtils.getBleStatus(getApplicationContext());
        switch (bleStatus) {
            case BleUtils.STATUS_BLE_NOT_AVAILABLE:
                errorMessageId = R.string.bluetooth_unsupported;
                isEnabled = false;
                break;
            case BleUtils.STATUS_BLUETOOTH_NOT_AVAILABLE: {
                errorMessageId = R.string.bluetooth_poweredoff;
                isEnabled = false;      // it was already off
                break;
            }
            case BleUtils.STATUS_BLUETOOTH_DISABLED: {
                isEnabled = false;      // it was already off
                // if no enabled, launch settings dialog to enable it (user should always be prompted before automatically enabling bluetooth)
                try {
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, kActivityRequestCode_EnableBluetooth);
                    // execution will continue at onActivityResult()
                } catch (SecurityException e) {
                    Log.w(TAG, "Permissions not granted when trying to launch enable-bluetooth dialog: " + e);
                }

                break;
            }
        }

        if (errorMessageId != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setMessage(errorMessageId)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            DialogUtils.keepDialogOnOrientationChanges(dialog);
        }

        return isEnabled;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == kActivityRequestCode_EnableBluetooth) {
            if (resultCode == Activity.RESULT_OK) {
                checkPermissions();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (!isFinishing()) {
                    hasUserAlreadyBeenAskedAboutBluetoothStatus = true;     // Remember that
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    AlertDialog dialog = builder.setMessage(R.string.bluetooth_poweredoff)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    DialogUtils.keepDialogOnOrientationChanges(dialog);
                }
            }
        }
    }

    private void popFragmentsIfNoPeripheralsConnected() {
        int numConnectedPeripherals = 0;
        try {
            numConnectedPeripherals = BleManager.getInstance().getConnectedDevices().size();
        } catch (SecurityException e) {
            Log.e(TAG, "security exception: " + e);
        }
        final boolean isLastConnectedPeripheral = numConnectedPeripherals == 0;

        if (isLastConnectedPeripheral) {
            Log.d(TAG, "No peripherals connected. Pop all fragments");
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentManager.executePendingTransactions();
        }
    }
    // endregion

    // region ScannerFragmentListener
    public void scanningIsNotAvailable() {
        checkPermissions();
    }

    public void scannerRequestPermissionsIfNeeded() {
        requestScanningPermissionsIfNeeded();
    }

    public void startPeripheralModules(String peripheralIdentifier) {
        Fragment fragment = ControllerFragment.newInstance(peripheralIdentifier);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                                                         .setCustomAnimations(
                                                                 R.anim.slide_in_left, R.anim.slide_out_right,
                                                                 R.anim.slide_in_right, R.anim.slide_out_left)
                                                         .replace(R.id.contentLayout, fragment, "Controller");
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
    }

    // endregion

    // region Broadcast Listener
    private void registerGattReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BlePeripheral.kBlePeripheral_OnDisconnected);
        LocalBroadcastManager.getInstance(this).registerReceiver(mGattUpdateReceiver, filter);
    }

    private void unregisterGattReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BlePeripheral.kBlePeripheral_OnDisconnected.equals(action)) {
                try {
                    popFragmentsIfNoPeripheralsConnected();
                } catch (SecurityException e) {
                    Log.e(TAG, "security exception: " + e);
                }
            }
        }
    };
}