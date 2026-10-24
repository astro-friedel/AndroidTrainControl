package com.controls.train.app;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.controls.train.R;
import com.controls.train.ble.central.BlePeripheralUart;
import com.controls.train.ble.central.UartDataManager;
import com.controls.train.utils.DialogUtils;
import com.controls.train.utils.LocalizationManager;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class ControllerFragment extends ConnectedPeripheralFragment
        implements SpeedControlFragment.OnSpeedChangeListener,
                   TrackGroupChangeListener {
    // Log
    private final static String TAG = ControllerFragment.class.getSimpleName();

    // Data
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private UartDataManager mUartDataManager;
    private BlePeripheralUart mBlePeripheralUart;

    // region Fragment Lifecycle
    public static ControllerFragment newInstance(@Nullable String singlePeripheralIdentifier) {
        ControllerFragment fragment = new ControllerFragment();
        fragment.setArguments(createFragmentArgs(singlePeripheralIdentifier));
        return fragment;
    }

    public ControllerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_controller, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Update ActionBar
        setActionBarTitle(R.string.controller_tab_title);

        final Context context = getContext();
        if (context != null && mUartDataManager == null) {
            mUartDataManager = new UartDataManager(context, null, false);            // The listener will be set only for ControlPad
            setupUart();
        }
    }

    @Override
    public void onDestroy() {
        final Context context = getContext();

        if (mUartDataManager != null && context != null) {
            mUartDataManager.setEnabled(context, false);
        }

        if (mBlePeripheralUart != null) {
            mBlePeripheralUart.uartDisable();
        }
        mBlePeripheralUart = null;
        super.onDestroy();
    }

    // endregion

    // region Uart
    private void setupUart() {
        if (mBlePeripheral == null) {
            Log.e(TAG, "setupUart with blePeripheral null");
            return;
        }

        mBlePeripheralUart = new BlePeripheralUart(mBlePeripheral);
        mBlePeripheralUart.uartEnable(mUartDataManager, status -> mMainHandler.post(() -> {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Done
                Log.d(TAG, "Uart enabled");

            } else {
                Context context = getContext();
                if (context != null) {
                    WeakReference<BlePeripheralUart> weakBlePeripheralUart = new WeakReference<>(mBlePeripheralUart);
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    AlertDialog dialog = builder.setMessage(R.string.uart_error_peripheralinit)
                            .setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
                                BlePeripheralUart strongBlePeripheralUart = weakBlePeripheralUart.get();
                                if (strongBlePeripheralUart != null) {
                                    try {
                                        strongBlePeripheralUart.disconnect();
                                    } catch (SecurityException e) {
                                        Log.e(TAG, "security exception: " + e);
                                    }
                                }
                            })
                            .show();
                    DialogUtils.keepDialogOnOrientationChanges(dialog);
                }
            }
        }));
    }

    // endregion

    private void sendCrcData(byte[] data) {
        if (mUartDataManager == null) {     // Check because crash found on logs (mUartDataManager is null)
            return;
        }

        byte[] crcData = BlePeripheralUart.appendCrc(data);
        mUartDataManager.send(mBlePeripheralUart, crcData, null);
    }

    // region Adapter
    private static class ControllerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int[] kModuleTitleKeys = {R.string.controller_module_pad};

        private static final int kCellType_SectionTitle = 0;
        private static final int kCellType_ModuleCell   = 1;

        interface Listener {
            void onModuleSelected(int moduleId);
        }

        private static class SectionViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            SectionViewHolder(View view) {
                super(view);
                titleTextView = view.findViewById(R.id.titleTextView);
            }
        }

        private static class ModuleViewHolder extends RecyclerView.ViewHolder {
            TextView  nameTextView;
            ViewGroup mainViewGroup;
            ModuleViewHolder(View view) {
                super(view);
                nameTextView  = view.findViewById(R.id.nameTextView);
                mainViewGroup = view.findViewById(R.id.mainViewGroup);
            }
        }

        private final Context  mContext;
        private final Listener mListener;

        ControllerAdapter(@NonNull Context context, @NonNull Listener listener) {
            mContext  = context.getApplicationContext();
            mListener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? kCellType_SectionTitle : kCellType_ModuleCell;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == kCellType_SectionTitle) {
                View view = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.layout_common_section_item, parent, false);
                return new SectionViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.layout_common_textview_item, parent, false);
                return new ModuleViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                ((SectionViewHolder) holder).titleTextView.setText(
                        LocalizationManager.getInstance()
                                           .getString(mContext, "pinio_pins_header"));
            } else {
                final int moduleId = position - 1;
                ModuleViewHolder mvh = (ModuleViewHolder) holder;
                mvh.nameTextView.setText(kModuleTitleKeys[moduleId]);
                mvh.mainViewGroup.setOnClickListener(v -> {
                    if (mListener != null) mListener.onModuleSelected(moduleId);
                });
            }
        }

        @Override
        public int getItemCount() {
            return 1; // section header + modules
        }
    }

    // endregion

    @Override
    public void onSpeedChanged(int controllerId, int speed) {
        String data = "!T" + controllerId + speed;
        ByteBuffer buffer = ByteBuffer.allocate(data.length()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data.getBytes());
        sendCrcData(buffer.array());
    }

    @Override
    public void onStopPressed(int controllerId) {
        String data = "!H" + controllerId;
        ByteBuffer buffer = ByteBuffer.allocate(data.length()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data.getBytes());
        sendCrcData(buffer.array());

    }

    @Override
    public void onDirectionChange(int controllerId, boolean on) {
        String data = "!D" + controllerId + (on ? "1" : "0");
        ByteBuffer buffer = ByteBuffer.allocate(data.length()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data.getBytes());
        sendCrcData(buffer.array());
    }

    @Override
    public void onGroupChanged(int groupId, List<Integer> members) {
        String data = "!G" + groupId + "T" + members.size();
        for(Integer m : members) {
            if(m == null) {
                data += 0;
            } else {
                data += m.intValue();
            }
        }
        ByteBuffer buffer = ByteBuffer.allocate(data.length()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data.getBytes());
        sendCrcData(buffer.array());
    }

    @Override
    public void onGroupDeleted(int groupId) {
        String data = "!G" + groupId + "T" + 0;
        ByteBuffer buffer = ByteBuffer.allocate(data.length()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(data.getBytes());
        sendCrcData(buffer.array());
    }
}
