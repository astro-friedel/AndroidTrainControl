package com.controls.train.ble.peripheral;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.controls.train.ble.UartPacket;
import com.controls.train.ble.UartPacketManagerBase;

public class UartPeripheralModePacketManager extends UartPacketManagerBase {
    // Log
    private final static String TAG = UartPeripheralModePacketManager.class.getSimpleName();

    // region Lifecycle
    public UartPeripheralModePacketManager(@NonNull Context context, @Nullable Listener listener, boolean isPacketCacheEnabled) {
        super(context, listener, isPacketCacheEnabled);
    }
    // endregion

    // region Send data

    public void send(@NonNull UartPeripheralService uartPeripheralService, @NonNull byte[] data/*, BlePeripheral.UpdateDatabaseCompletionHandler completionHandler*/) {
        mSentBytes += data.length;
        uartPeripheralService.setRx(data);
    }

    public void send(@NonNull UartPeripheralService uartPeripheralService, byte[] data, boolean wasReceivedFromMqtt) {
        // Create data and send to Uart
        UartPacket uartPacket = new UartPacket(null, UartPacket.TRANSFERMODE_TX, data);

        try {
            mPacketsSemaphore.acquire();        // don't append more data, till the delegate has finished processing it
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException: " + e.toString());
        }
        mPackets.add(uartPacket);
        mPacketsSemaphore.release();

        Listener listener = mWeakListener.get();
        if (listener != null) {
            mMainHandler.post(() -> listener.onUartPacket(uartPacket));
        }

        send(uartPeripheralService, data);
    }

    // endregion
}
