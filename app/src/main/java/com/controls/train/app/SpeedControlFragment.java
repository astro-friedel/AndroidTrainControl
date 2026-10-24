package com.controls.train.app;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

import com.controls.train.R;

public class SpeedControlFragment extends Fragment {
    private SeekBar seekBarSpeed;
    private TextView tvSpeed;
    private Button btnStop;
    private SwitchMaterial dirSwitch;
    private boolean direction = false;
    private TrackGroup mCurrentGroup = null;

    // Amber color for active (lit) digit segments
    //private static final int COLOR_ACTIVE = Color.parseColor("#E07800");
    // Near-black amber for dimmed (ghost) leading-zero segments
    private static final int COLOR_DIM    = Color.parseColor("#1E1000");

    /** Use this factory method to create a new instance of the fragment. */
    public static SpeedControlFragment newInstance() {
        SpeedControlFragment fragment = new SpeedControlFragment();
        return fragment;
    }

    // -------------------------------------------------------------------------
    // Fragment lifecycle
    // -------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_speed_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        seekBarSpeed = view.findViewById(R.id.sb_speed);
        tvSpeed      = view.findViewById(R.id.tv_speed);
        btnStop      = view.findViewById(R.id.btn_stop);
        dirSwitch    = view.findViewById(R.id.direction_switch);

        // Draw the initial display at speed 0
        updateDisplay(0);

        // Update display whenever the lever moves
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDisplay(progress);
                if (listener != null && mCurrentGroup != null) {
                    listener.onSpeedChanged(mCurrentGroup.getId(), progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // STOP: snap lever to zero and clear the display
        btnStop.setOnClickListener(v -> {
            seekBarSpeed.setProgress(0);
            updateDisplay(0);
            if (listener != null && mCurrentGroup != null) {
                listener.onStopPressed(mCurrentGroup.getId());
            }
        });

        dirSwitch.setOnCheckedChangeListener((v,c) -> {
            seekBarSpeed.setProgress(0);
            updateDisplay(0);
            this.direction = c;
            if (listener != null) {
                listener.onDirectionChange(mCurrentGroup.getId(), c);
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Null out view references so they can be garbage-collected when the
        // fragment's view hierarchy is destroyed (e.g. on back-stack pop).
        seekBarSpeed = null;
        tvSpeed      = null;
        btnStop      = null;
        dirSwitch    = null;
    }

    // -------------------------------------------------------------------------
    // Display logic
    // -------------------------------------------------------------------------

    /**
     * Renders the speed value in a 3-digit, 7-segment-style format.
     * <p>
     * Active digits are shown in amber (#E07800).
     * Leading zeros are "ghosted" in a near-black color to simulate
     * the dim-segment effect seen on physical 7-segment displays.
     * <p>
     * Examples:
     *   speed =   7  →  [ghost][ghost][ 7 ]
     *   speed =  42  →  [ghost][  4  ][ 2 ]
     *   speed = 100  →  [  1  ][  0  ][ 0 ]
     *
     * @param speed Integer speed value in the range 0–100.
     */
    private void updateDisplay(int speed) {
        // Zero-pad to 3 characters: "007", "042", "100"
        @SuppressLint("DefaultLocale") String text = String.format("%03d", speed);
        SpannableString spannable = new SpannableString(text);

        // Dim the hundreds digit when speed is below 100
        if (speed < 100) {
            spannable.setSpan(
                    new ForegroundColorSpan(COLOR_DIM),
                    0, 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Dim the tens digit when speed is below 10
        if (speed < 10) {
            spannable.setSpan(
                    new ForegroundColorSpan(COLOR_DIM),
                    1, 2,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvSpeed.setText(spannable);
    }

    public void setOnSpeedChangeListener(OnSpeedChangeListener listener) {
        this.listener = listener;
    }

    public interface OnSpeedChangeListener {
        void onSpeedChanged(int controllerId, int speed);

        void onStopPressed(int controllerId);

        void onDirectionChange(int controllerId, boolean on);
    }

    public void bind(TrackGroup group) {
        mCurrentGroup = group;
        if (group == null) return;
        if (seekBarSpeed != null) {
            seekBarSpeed.setProgress(group.getSpeed());
            updateDisplay(group.getSpeed());
        }
        if (dirSwitch != null) {
            // Suppress the listener while setting programmatically
            dirSwitch.setOnCheckedChangeListener(null);
            dirSwitch.setChecked(group.isDirectionForward());
            this.direction = group.isDirectionForward();
            dirSwitch.setOnCheckedChangeListener((v, c) -> {
                if (seekBarSpeed != null) seekBarSpeed.setProgress(0);
                updateDisplay(0);
                this.direction = c;
                if (listener != null && mCurrentGroup != null) {
                    listener.onDirectionChange(mCurrentGroup.getId(), c);
                }
            });
        }
    }

    private OnSpeedChangeListener listener;

}