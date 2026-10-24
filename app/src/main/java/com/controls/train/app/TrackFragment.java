package com.controls.train.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.controls.train.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrackFragment extends Fragment {
    private final List<String> SVG_FILES = Arrays.asList("svg/upper_layer.svg",
                                                         "svg/middle_layer.svg",
                                                         "svg/lower_lvl.svg");

    private TrackViewModel   mViewModel;
    private DriveTabFragment mDriveFragment;
    private GroupTabFragment mGroupFragment;
    private TextView         mDriveTab;
    private TextView         mGroupTab;
    private final List<TextView> mMapTabs = new ArrayList<>();

    public static TrackFragment newInstance() {
        return new TrackFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_track_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel scoped to this fragment — all child fragments share the same instance
        mViewModel = new ViewModelProvider(this).get(TrackViewModel.class);
        mViewModel.initializeTracks(requireContext(), SVG_FILES);

        ViewPager2 viewPager  = view.findViewById(R.id.viewPager);

        TrackPagerAdapter adapter = new TrackPagerAdapter(this, SVG_FILES);
        viewPager.setAdapter(new TrackPagerAdapter(this, SVG_FILES));
        viewPager.setOffscreenPageLimit(SVG_FILES.size() - 1);

        mMapTabs.add(view.findViewById(R.id.upper_track));
        mMapTabs.add(view.findViewById(R.id.middle_track));
        mMapTabs.add(view.findViewById(R.id.lower_track));

        for (int i = 0; i < mMapTabs.size(); i++) {
            final int index = i;
            mMapTabs.get(i).setOnClickListener(v -> viewPager.setCurrentItem(index, true));
        }
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                updateMapTabSelection(position);
            }
        });
        updateMapTabSelection(0);

        // --- Drive / Group panel tabs ---
        mDriveTab = view.findViewById(R.id.track_drive);
        mGroupTab  = view.findViewById(R.id.track_group);
        mDriveTab.setOnClickListener(v -> showDriveTab());
        mGroupTab.setOnClickListener(v -> showGroupTab());

        if (savedInstanceState == null) {
            mDriveFragment = DriveTabFragment.newInstance();
            mGroupFragment = GroupTabFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                                     .add(R.id.track_control_frame, mDriveFragment, "drive")
                                     .add(R.id.track_control_frame, mGroupFragment, "group")
                                     .hide(mGroupFragment)
                                     .commit();
            showDriveTabVisuals();
        } else {
            mDriveFragment = (DriveTabFragment)
                    getChildFragmentManager().findFragmentByTag("drive");
            mGroupFragment = (GroupTabFragment)
                    getChildFragmentManager().findFragmentByTag("group");
            if (mGroupFragment != null && !mGroupFragment.isHidden()) {
                showGroupTabVisuals();
            } else {
                showDriveTabVisuals();
            }
        }
    }

    private void showDriveTab() {
        showDriveTabVisuals();
        getChildFragmentManager().beginTransaction()
                                 .show(mDriveFragment).hide(mGroupFragment).commit();
    }

    private void showGroupTab() {
        showGroupTabVisuals();
        getChildFragmentManager().beginTransaction()
                                 .show(mGroupFragment).hide(mDriveFragment).commit();
    }

    private void showDriveTabVisuals() {
        mDriveTab.setAlpha(1.0f);
        mGroupTab.setAlpha(0.4f);
    }

    private void showGroupTabVisuals() {
        mGroupTab.setAlpha(1.0f);
        mDriveTab.setAlpha(0.4f);
    }

    private void updateMapTabSelection(int selectedIndex) {
        for (int i = 0; i < mMapTabs.size(); i++) {
            if (i == selectedIndex) {
                mMapTabs.get(i).setAlpha(1.0f);
                mMapTabs.get(i).setBackground(ContextCompat.getDrawable(
                        requireContext(), R.drawable.roundedborders_orange));
            } else {
                mMapTabs.get(i).setAlpha(0.4f);
                mMapTabs.get(i).setBackground(ContextCompat.getDrawable(
                        requireContext(), R.drawable.roundedborders_gray));
            }
        }
    }

    public void setGroupChangeListener(TrackGroupChangeListener listener) {
        if (mViewModel != null) {
            mViewModel.setGroupChangeListener(listener);
        }
    }
}
