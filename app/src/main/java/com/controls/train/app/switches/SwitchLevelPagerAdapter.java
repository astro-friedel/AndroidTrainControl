package com.controls.train.app.switches;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

/**
 * SwitchLevelPagerAdapter
 *
 * Provides one SwitchesLevelFragment per track level tab to the ViewPager2
 * in SwitchesFragment. The fragment list is built once in SwitchesFragment
 * and passed in at construction time.
 */
public class SwitchLevelPagerAdapter extends FragmentStateAdapter {

    private final List<SwitchesLevelFragment> fragments;

    public SwitchLevelPagerAdapter(@NonNull FragmentManager fm,
                                   @NonNull Lifecycle lifecycle,
                                   @NonNull List<SwitchesLevelFragment> fragments) {
        super(fm, lifecycle);
        this.fragments = fragments;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

    /**
     * Returns the SwitchesLevelFragment at the given position so the parent
     * can call onSwitchThrowConfirmed() on it if needed.
     */
    public SwitchesLevelFragment getFragment(int position) {
        return fragments.get(position);
    }
}