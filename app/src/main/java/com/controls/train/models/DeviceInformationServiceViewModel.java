package com.controls.train.models;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.controls.train.ble.peripheral.DeviceInformationPeripheralService;

public class DeviceInformationServiceViewModel extends AndroidViewModel {

    public DeviceInformationServiceViewModel(@NonNull Application application) {
        super(application);
    }

    public DeviceInformationPeripheralService getDeviceInfomationPeripheralService() {
        return PeripheralModeManager.getInstance().getDeviceInformationPeripheralService();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // Save current characteristic values
        PeripheralModeManager.getInstance().getDeviceInformationPeripheralService().saveValues(getApplication());
    }
}
