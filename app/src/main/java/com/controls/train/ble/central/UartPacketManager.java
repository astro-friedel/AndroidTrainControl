package com.controls.train.ble.central;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.controls.train.ble.UartPacket;
import com.controls.train.ble.UartPacketManagerBase;

public class UartPacketManager extends UartPacketManagerBase {
    // Log
    private final static String TAG = UartPacketManager.class.getSimpleName();

    // region Lifecycle
    public UartPacketManager(@NonNull Context context, @Nullable UartPacketManagerBase.Listener listener, boolean isPacketCacheEnabled) {
        super(context, listener, isPacketCacheEnabled);

    }
    // endregion

    // region Send data

    public void send(@NonNull BlePeripheralUart uartPeripheral, @NonNull byte[] data, BlePeripheral.CompletionHandler completionHandler) {
        mSentBytes += data.length;
        uartPeripheral.uartSend(data, completionHandler);
    }

    public void sendAndWaitReply(@NonNull BlePeripheralUart uartPeripheral, @NonNull byte[] data, @NonNull BlePeripheral.CaptureReadCompletionHandler readCompletionHandler) {
        mSentBytes += data.length;
        uartPeripheral.uartSendAndWaitReply(data, null, readCompletionHandler);
    }

    public void sendAndWaitReply(@NonNull BlePeripheralUart uartPeripheral, @NonNull byte[] data, @Nullable BlePeripheral.CompletionHandler writeCompletionHandler, int readTimeout, @NonNull BlePeripheral.CaptureReadCompletionHandler readCompletionHandler) {
        mSentBytes += data.length;
        uartPeripheral.uartSendAndWaitReply(data, writeCompletionHandler, readTimeout, readCompletionHandler);
    }

    public void send(@NonNull BlePeripheralUart uartPeripheral, byte[] data) {
        send(uartPeripheral, data, false);
    }

    public void send(@NonNull BlePeripheralUart uartPeripheral, byte[] data, boolean wasReceivedFromMqtt) {
        // Create data and send to Uart
        UartPacket uartPacket = new UartPacket(uartPeripheral.getIdentifier(), UartPacket.TRANSFERMODE_TX, data);

        try {
            mPacketsSemaphore.acquire();        // don't append more data, till the delegate has finished processing it
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException: " + e.toString());
        }
        mPacketsSemaphore.release();

        Listener listener = mWeakListener.get();
        mPackets.add(uartPacket);
        if (listener != null) {
            mMainHandler.post(() -> listener.onUartPacket(uartPacket));
        }

        send(uartPeripheral, data, null);
    }

    public void sendEachPacketSequentially(@NonNull BlePeripheralUart uartPeripheral, @NonNull byte[] data, int withResponseEveryPacketCount, BlePeripheral.ProgressHandler progressHandler, BlePeripheral.CompletionHandler completionHandler) {
        mSentBytes += data.length;
        uartPeripheral.sendEachPacketSequentially(data, withResponseEveryPacketCount, progressHandler, completionHandler);
    }

    public void cancelOngoingSendPacketSequentiallyInThread(@NonNull BlePeripheralUart uartPeripheral) {
        uartPeripheral.cancelOngoingSendPacketSequentiallyInThread();
    }

    // endregion
}
