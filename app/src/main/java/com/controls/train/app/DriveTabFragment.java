package com.controls.train.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.controls.train.R;

public class DriveTabFragment extends Fragment
        implements SpeedControlFragment.OnSpeedChangeListener {

    private TrackViewModel      mViewModel;
    private SpeedControlFragment mSpeedFragment;
    private TextView             mGroupNameView;
    private View                 mEmptyView;
    private View                 mContentView;

    public static DriveTabFragment newInstance() {
        return new DriveTabFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drive_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel     = new ViewModelProvider(requireParentFragment()).get(TrackViewModel.class);
        mGroupNameView = view.findViewById(R.id.drive_group_name);
        mEmptyView     = view.findViewById(R.id.drive_empty_message);
        mContentView   = view.findViewById(R.id.drive_content);

        if (savedInstanceState == null) {
            mSpeedFragment = SpeedControlFragment.newInstance();
            mSpeedFragment.setOnSpeedChangeListener(this);
            getChildFragmentManager().beginTransaction()
                                     .add(R.id.speed_control_container, mSpeedFragment, "speed")
                                     .commit();
        } else {
            mSpeedFragment = (SpeedControlFragment)
                    getChildFragmentManager().findFragmentByTag("speed");
            if (mSpeedFragment != null) mSpeedFragment.setOnSpeedChangeListener(this);
        }

        mViewModel.getActiveGroup().observe(getViewLifecycleOwner(), group -> {
            if (group == null) {
                mEmptyView.setVisibility(View.VISIBLE);
                mContentView.setVisibility(View.GONE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mContentView.setVisibility(View.VISIBLE);
                mGroupNameView.setText(group.getName());
                if (mSpeedFragment != null) mSpeedFragment.bind(group);
            }
        });
    }

    // -------------------------------------------------------------------------
    // SpeedControlFragment.OnSpeedChangeListener
    // -------------------------------------------------------------------------

    @Override
    public void onSpeedChanged(int controllerId, int speed) {
        TrackGroup group = mViewModel.getActiveGroup().getValue();
        if (group != null) group.setSpeed(speed);
        // TODO: send BLE command
    }

    @Override
    public void onStopPressed(int controllerId) {
        TrackGroup group = mViewModel.getActiveGroup().getValue();
        if (group != null) group.setSpeed(0);
        // TODO: send BLE stop command
    }

    @Override
    public void onDirectionChange(int controllerId, boolean on) {
        TrackGroup group = mViewModel.getActiveGroup().getValue();
        if (group != null) group.setDirectionForward(on);
        // TODO: send BLE direction command
    }
}