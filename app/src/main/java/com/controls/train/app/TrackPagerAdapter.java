package com.controls.train.app;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class TrackPagerAdapter extends FragmentStateAdapter {
    private final List<String> svgFiles;

    public TrackPagerAdapter(Fragment fragment, List<String> svgFiles) {
        super(fragment);
        this.svgFiles = svgFiles;
    }

    @Override
    public int getItemCount() {
        return svgFiles.size();
    }

    @NonNull
    public Fragment createFragment(int position) {
        return TrackTabFragment.newInstance(svgFiles.get(position), position);
    }
}
