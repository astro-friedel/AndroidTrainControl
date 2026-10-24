package com.controls.train.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.controls.train.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;

public class MainScreenFragment extends Fragment {

    private static final String TAG = MainScreenFragment.class.getSimpleName();

    public static MainScreenFragment newInstance() {
        return new MainScreenFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewPager2 viewPager = view.findViewById(R.id.viewPager);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);

        // Attach the adapter — uses child fragment manager so tab fragments
        // are scoped to this fragment's lifecycle, not the activity's.
        MainScreenPagerAdapter adapter = new MainScreenPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Keep all tab fragments alive when swiping (optional: remove if
        // memory is a concern; default is 1 page either side).
        viewPager.setOffscreenPageLimit(adapter.getItemCount() - 1);

        // Link TabLayout to ViewPager2.
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Tracks");      break;
                case 1: tab.setText("Switches");    break;
                case 2: tab.setText("Lights");      break;
                case 3: tab.setText("Connections"); break;
            }
        }).attach();
        viewPager.setCurrentItem(MainScreenPagerAdapter.TAB_CONNECTIONS, false);
    }

    // Called by MainActivity when Bluetooth is ready
    @SuppressLint("InlinedApi")
    @RequiresPermission(anyOf = {ACCESS_FINE_LOCATION, BLUETOOTH_SCAN})
    void startScanning() {
        ScannerFragment f = getScannerFragment();
        if (f != null) f.startScanning();
    }

    // Called by MainActivity when back-stack returns to root
    @SuppressLint("InlinedApi")
    @RequiresPermission(allOf = {BLUETOOTH_SCAN, BLUETOOTH_CONNECT})
    void disconnectAllPeripherals() {
        ScannerFragment f = getScannerFragment();
        if (f != null) f.disconnectAllPeripherals();
    }

    // FragmentStateAdapter tags each page as "f" + itemId (itemId == position by default)
    @Nullable
    private ScannerFragment getScannerFragment() {
        Fragment f = getChildFragmentManager()
                .findFragmentByTag("f" + MainScreenPagerAdapter.TAB_CONNECTIONS);
        return (f instanceof ScannerFragment) ? (ScannerFragment) f : null;
    }
}