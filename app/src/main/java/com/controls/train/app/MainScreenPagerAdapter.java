package com.controls.train.app;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.controls.train.app.switches.SwitchesFragment;

public class MainScreenPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 4;

    // Tab index constants — use these if you need to reference a tab by name
    public static final int TAB_TRACKS      = 0;
    public static final int TAB_SWITCHES    = 1;
    public static final int TAB_LIGHTS      = 2;
    public static final int TAB_CONNECTIONS = 3;

    public MainScreenPagerAdapter(@NonNull Fragment fragment) {
        // Pass the *fragment* (not the activity) so the adapter uses the
        // child fragment manager — required when hosted inside a Fragment.
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_TRACKS:      return TrackFragment.newInstance();
            case TAB_SWITCHES:    return SwitchesFragment.newInstance();
            case TAB_LIGHTS:      return LightsFragment.newInstance();
            case TAB_CONNECTIONS: return ScannerFragment.newInstance();
            default: throw new IllegalArgumentException("Unknown tab position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}